import { cookies } from "next/headers";
import { NextResponse } from "next/server";
import { authorizationCodeGrant } from "openid-client";
import { fetchBankProfile, trustedClientIp } from "@/lib/server/bank-api";
import { getOidcConfiguration } from "@/lib/server/oidc";
import { SessionCookieSizeError } from "@/lib/server/session-cookies";
import {
  BANK_SESSION_IDLE_SECONDS,
  BANK_SESSION_MAX_SECONDS,
  createCsrfToken,
  createSessionId,
  getBankActivityStore,
  getBankSessionStore,
  getCallbackUrl,
  getOidcTransactionStore,
  getPublicAppOrigin,
} from "@/lib/server/session";

export const runtime = "nodejs";
export const dynamic = "force-dynamic";

function loginRedirect(error: string): NextResponse {
  const response = NextResponse.redirect(new URL(`/login?error=${error}`, getPublicAppOrigin()));
  response.headers.set("Cache-Control", "no-store");
  response.headers.set("Referrer-Policy", "no-referrer");
  return response;
}

function validatedCallbackUrl(request: Request): URL {
  const incoming = new URL(request.url);
  const callbackUrl = getCallbackUrl();
  const allowed = new Set(["code", "state", "session_state", "iss", "error", "error_description", "error_uri"]);
  const seen = new Set<string>();
  let queryLength = 0;

  for (const [name, value] of incoming.searchParams) {
    queryLength += name.length + value.length;
    if (!allowed.has(name) || seen.has(name) || value.length > 4096 || queryLength > 8192) {
      throw new Error("OIDC callback parameters are invalid");
    }
    seen.add(name);
  }

  callbackUrl.search = incoming.search;
  return callbackUrl;
}

export async function GET(request: Request) {
  let stage = "transaction";
  let transaction: Awaited<ReturnType<typeof getOidcTransactionStore>> | undefined;
  let issuedTokens: Awaited<ReturnType<typeof getBankSessionStore>> | undefined;
  let issuedActivity: Awaited<ReturnType<typeof getBankActivityStore>> | undefined;

  try {
    transaction = await getOidcTransactionStore();
    const now = Date.now();
    if (!transaction.state || !transaction.nonce || !transaction.codeVerifier
      || typeof transaction.createdAt !== "number"
      || now - transaction.createdAt > 5 * 60 * 1000
      || now < transaction.createdAt) {
      console.warn(JSON.stringify({ event: "bff_auth_callback_failed", stage, reason: "missing_or_expired_transaction" }));
      transaction.destroy();
      return loginRedirect("invalid_authentication_response");
    }

    stage = "configuration";
    const [configuration, cookieStore] = await Promise.all([
      getOidcConfiguration(),
      cookies(),
    ]);
    stage = "token_exchange";
    const tokens = await authorizationCodeGrant(configuration, validatedCallbackUrl(request), {
      expectedState: transaction.state,
      expectedNonce: transaction.nonce,
      pkceCodeVerifier: transaction.codeVerifier,
      maxAge: 0,
    });

    const accessToken = tokens.access_token;
    const refreshToken = tokens.refresh_token;
    const idToken = tokens.id_token;
    const expiresIn = tokens.expires_in;
    const claims = tokens.claims();
    if (!accessToken || !refreshToken || !idToken || !Number.isFinite(expiresIn) || !claims || typeof claims.sub !== "string") {
      throw new Error("Required OIDC response fields are missing");
    }

    stage = "bank_profile";
    const user = await fetchBankProfile(accessToken, trustedClientIp(request));
    const absoluteExpiresAt = now + BANK_SESSION_MAX_SECONDS * 1000;
    const sessionId = createSessionId();
    stage = "session_cookie";
    const [tokenStore, activity] = await Promise.all([
      getBankSessionStore(cookieStore),
      getBankActivityStore(cookieStore),
    ]);
    issuedTokens = tokenStore;
    issuedActivity = activity;
    const session = tokenStore.data;

    session.version = 1;
    session.sessionId = sessionId;
    session.accessToken = accessToken;
    session.refreshToken = refreshToken;
    session.idToken = idToken;
    session.accessTokenExpiresAt = now + Number(expiresIn) * 1000;
    session.absoluteExpiresAt = absoluteExpiresAt;
    session.user = user;
    activity.sessionId = sessionId;
    activity.csrfToken = createCsrfToken();
    activity.idleExpiresAt = Math.min(now + BANK_SESSION_IDLE_SECONDS * 1000, absoluteExpiresAt);
    await tokenStore.save();
    await activity.save();

    transaction.destroy();
    for (const name of ["token", "id_token"]) {
      if (cookieStore.has(name)) cookieStore.delete(name);
    }

    const response = NextResponse.redirect(new URL("/auth-sync", getPublicAppOrigin()));
    response.headers.set("Cache-Control", "no-store");
    response.headers.set("Referrer-Policy", "no-referrer");
    return response;
  } catch (error) {
    // Never serialize the OIDC error: it can include callback URLs or tokens.
    console.warn(JSON.stringify({
      event: "bff_auth_callback_failed",
      stage,
      reason: error instanceof SessionCookieSizeError ? "cookie_header_budget_exceeded" : "callback_rejected",
      ...(error instanceof SessionCookieSizeError ? { headerBytes: error.headerBytes } : {}),
    }));
    issuedTokens?.data.destroy();
    issuedActivity?.destroy();
    transaction?.destroy();
    return loginRedirect(error instanceof SessionCookieSizeError
      ? "session_unavailable"
      : stage === "bank_profile" ? "profile_unavailable" : "authentication_failed");
  }
}
