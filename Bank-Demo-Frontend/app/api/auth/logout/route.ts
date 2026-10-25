import { randomBytes } from "node:crypto";
import { cookies } from "next/headers";
import { NextResponse } from "next/server";
import {
  getBankLogoutContext,
  getOidcTransactionStore,
  getPublicAppOrigin,
  hasValidCsrfTokenValue,
  isSafeOrigin,
} from "@/lib/server/session";
import { getOidcConfiguration } from "@/lib/server/oidc";

export const runtime = "nodejs";
export const dynamic = "force-dynamic";

const MAX_CSRF_FORM_BYTES = 1024;

async function readCsrfFormValue(request: Request): Promise<string | null> {
  const mediaType = request.headers.get("content-type")?.split(";", 1)[0].trim().toLowerCase();
  if (mediaType !== "application/x-www-form-urlencoded") return null;

  const declaredLength = request.headers.get("content-length");
  if (declaredLength && (!/^\d+$/.test(declaredLength) || Number(declaredLength) > MAX_CSRF_FORM_BYTES)) {
    return null;
  }

  const reader = request.body?.getReader();
  if (!reader) return null;

  const decoder = new TextDecoder();
  let body = "";
  let bytesRead = 0;
  try {
    while (true) {
      const { done, value } = await reader.read();
      if (done) break;
      bytesRead += value.byteLength;
      if (bytesRead > MAX_CSRF_FORM_BYTES) {
        await reader.cancel();
        return null;
      }
      body += decoder.decode(value, { stream: true });
    }
    body += decoder.decode();
  } finally {
    reader.releaseLock();
  }

  const form = new URLSearchParams(body);
  const values = form.getAll("csrfToken");
  if (form.size !== 1 || values.length !== 1 || values[0].length > 128) return null;
  return values[0];
}

function localLogoutRedirect(): NextResponse {
  const response = NextResponse.redirect(new URL("/login?logout=local", getPublicAppOrigin()), 303);
  response.headers.set("Cache-Control", "no-store");
  response.headers.set("Referrer-Policy", "no-referrer");
  return response;
}

function escapeHtml(value: string): string {
  return value.replace(/[&<>"']/g, (character) => ({
    "&": "&amp;",
    "<": "&lt;",
    ">": "&gt;",
    '"': "&quot;",
    "'": "&#39;",
  })[character]!);
}

function keycloakLogoutPost(
  endpoint: URL,
  clientId: string,
  idToken: string,
  postLogoutRedirectUri: string,
): NextResponse {
  const nonce = randomBytes(18).toString("base64url");
  const action = escapeHtml(endpoint.href);
  const client = escapeHtml(clientId);
  const token = escapeHtml(idToken);
  const redirect = escapeHtml(postLogoutRedirectUri);
  const html = `<!doctype html><html lang="tr"><head><meta charset="utf-8"><meta name="referrer" content="no-referrer"><meta name="viewport" content="width=device-width,initial-scale=1"><title>Güvenli çıkış</title></head><body><p>Güvenli çıkış yapılıyor…</p><form id="keycloak-logout" method="post" action="${action}"><input type="hidden" name="client_id" value="${client}"><input type="hidden" name="id_token_hint" value="${token}"><input type="hidden" name="post_logout_redirect_uri" value="${redirect}"></form><script nonce="${nonce}">document.getElementById("keycloak-logout").submit()</script></body></html>`;
  const response = new NextResponse(html, {
    status: 200,
    headers: {
      "Content-Type": "text/html; charset=utf-8",
      "Cache-Control": "no-store",
      "Referrer-Policy": "no-referrer",
      // Chromium applies form-action to the redirect chain as well. Keycloak's
      // post-logout redirect must be allowed to return to this app's origin.
      "Content-Security-Policy": `default-src 'none'; base-uri 'none'; frame-ancestors 'none'; form-action 'self' ${endpoint.origin}; script-src 'nonce-${nonce}'`,
      "X-Content-Type-Options": "nosniff",
    },
  });
  return response;
}

export async function POST(request: Request) {
  if (!isSafeOrigin(request)) {
    return NextResponse.json({ code: "CSRF_REJECTED" }, {
      status: 403,
      headers: { "Cache-Control": "no-store" },
    });
  }

  const cookieStore = await cookies();
  const session = await getBankLogoutContext(cookieStore);
  const csrfToken = await readCsrfFormValue(request);
  if (session.csrfToken && (!csrfToken || !hasValidCsrfTokenValue(csrfToken, session.csrfToken))) {
    return NextResponse.json({ code: "CSRF_REJECTED" }, {
      status: 403,
      headers: { "Cache-Control": "no-store" },
    });
  }

  // End the local session before asking Keycloak to close its browser SSO session.
  session.destroy();
  const transaction = await getOidcTransactionStore(cookieStore);
  transaction.destroy();

  // Older encrypted sessions may not contain an ID token. Clear them locally and
  // require fresh authentication rather than sending Keycloak a hint-less logout
  // that would show the confirmation screen the user asked to avoid.
  if (!session.idToken) return localLogoutRedirect();

  try {
    const configuration = await getOidcConfiguration();
    const endpoint = configuration.serverMetadata().end_session_endpoint;
    if (!endpoint) return localLogoutRedirect();

    const destination = new URL(endpoint);
    const issuer = process.env.OAUTH_ISSUER_URI;
    if (!issuer || destination.protocol !== "https:" || destination.origin !== new URL(issuer).origin) {
      return localLogoutRedirect();
    }
    return keycloakLogoutPost(
      destination,
      process.env.KEYCLOAK_CLIENT_ID!,
      session.idToken,
      new URL("/login?logout=complete", getPublicAppOrigin()).href,
    );
  } catch {
    // Local cookies are already cleared. Never claim that Keycloak SSO ended.
    return localLogoutRedirect();
  }
}
