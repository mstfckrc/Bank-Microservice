import { NextResponse } from "next/server";
import { getOidcConfiguration } from "@/lib/server/oidc";
import { getActiveBankSession, getPublicAppOrigin } from "@/lib/server/session";

export const runtime = "nodejs";
export const dynamic = "force-dynamic";

function redirect(destination: URL): NextResponse {
  const response = NextResponse.redirect(destination);
  response.headers.set("Cache-Control", "no-store");
  response.headers.set("Referrer-Policy", "no-referrer");
  return response;
}

export async function GET(request: Request) {
  let returnPath = "/login";
  try {
    const session = await getActiveBankSession();
    if (!session) return redirect(new URL("/login?session=expired", getPublicAppOrigin()));
    returnPath = session.user.role === "ADMIN" ? "/admin/settings"
      : session.user.role === "CORPORATE_MANAGER" ? "/company/settings" : "/user/settings";

    const configuration = await getOidcConfiguration();
    const issuer = new URL(configuration.serverMetadata().issuer);
    const destination = new URL(issuer.href.replace(/\/$/, "") + "/account/");
    destination.searchParams.set("referrer", process.env.KEYCLOAK_CLIENT_ID!);
    destination.searchParams.set("referrer_uri", new URL(returnPath, getPublicAppOrigin()).href);
    if (new URL(request.url).searchParams.get("view") === "security") {
      destination.hash = "/account-security/signing-in";
    }
    return redirect(destination);
  } catch {
    return redirect(new URL(returnPath + "?identity=unavailable", getPublicAppOrigin()));
  }
}
