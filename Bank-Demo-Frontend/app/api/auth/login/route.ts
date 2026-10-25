import { NextResponse } from "next/server";
import {
  buildAuthorizationUrl,
  calculatePKCECodeChallenge,
  randomNonce,
  randomPKCECodeVerifier,
  randomState,
} from "openid-client";
import { getOidcConfiguration } from "@/lib/server/oidc";
import { getCallbackUrl, getOidcTransactionStore, getPublicAppOrigin } from "@/lib/server/session";

export const runtime = "nodejs";
export const dynamic = "force-dynamic";

export async function GET() {
  try {
    const [configuration, transaction] = await Promise.all([
      getOidcConfiguration(),
      getOidcTransactionStore(),
    ]);
    const codeVerifier = randomPKCECodeVerifier();
    const codeChallenge = await calculatePKCECodeChallenge(codeVerifier);
    const state = randomState();
    const nonce = randomNonce();

    transaction.state = state;
    transaction.nonce = nonce;
    transaction.codeVerifier = codeVerifier;
    transaction.createdAt = Date.now();
    await transaction.save();

    const authorizationUrl = buildAuthorizationUrl(configuration, {
      client_id: process.env.KEYCLOAK_CLIENT_ID!,
      redirect_uri: getCallbackUrl().href,
      response_type: "code",
      scope: "openid profile email",
      state,
      nonce,
      code_challenge: codeChallenge,
      code_challenge_method: "S256",
      max_age: "0",
      prompt: "login",
    });

    const response = NextResponse.redirect(authorizationUrl);
    response.headers.set("Cache-Control", "no-store");
    response.headers.set("Referrer-Policy", "no-referrer");
    return response;
  } catch {
    const destination = new URL("/login?error=identity_provider_unavailable", getPublicAppOrigin());
    const response = NextResponse.redirect(destination);
    response.headers.set("Cache-Control", "no-store");
    response.headers.set("Referrer-Policy", "no-referrer");
    return response;
  }
}
