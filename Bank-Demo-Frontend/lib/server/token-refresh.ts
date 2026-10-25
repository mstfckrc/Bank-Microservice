import "server-only";

import { refreshTokenGrant, ResponseBodyError } from "openid-client";
import { getOidcConfiguration } from "./oidc";
import type { ActiveBankSession, ActiveBankTokenSession } from "./session";
import { SessionCookieSizeError } from "./session-cookies";

const REFRESH_SKEW_MS = 30_000;
const REFRESH_RESULT_REUSE_MS = 30_000;
const MAX_REFRESH_ENTRIES = 256;

export type AccessTokenResult =
  | { status: "ready"; accessToken: string }
  | { status: "reauthenticate" }
  | { status: "unavailable" };

interface RefreshedCredentials {
  accessToken: string;
  refreshToken: string;
  accessTokenExpiresAt: number;
}

interface RefreshEntry {
  sourceExpiry: number;
  reuseUntil: number;
  promise: Promise<RefreshedCredentials>;
}

// One Node process per current frontend container. This short-lived cache only
// coalesces/replays one refresh result; it is not a session store or revocation list.
const refreshes = new Map<string, RefreshEntry>();

function evictExpired(now: number): void {
  for (const [sessionId, entry] of refreshes) {
    if (entry.reuseUntil <= now) refreshes.delete(sessionId);
  }
  while (refreshes.size >= MAX_REFRESH_ENTRIES) {
    const oldestKey = refreshes.keys().next().value;
    if (oldestKey === undefined) break;
    refreshes.delete(oldestKey);
  }
}

async function requestRefresh(session: ActiveBankTokenSession): Promise<RefreshedCredentials> {
  const now = Date.now();
  evictExpired(now);

  const existing = refreshes.get(session.sessionId);
  if (existing && existing.sourceExpiry === session.accessTokenExpiresAt && existing.reuseUntil > now) {
    return existing.promise;
  }

  const promise = (async () => {
    const configuration = await getOidcConfiguration();
    const tokens = await refreshTokenGrant(configuration, session.refreshToken);
    const accessToken = tokens.access_token;
    const expiresIn = tokens.expires_in;
    if (!accessToken || !Number.isFinite(expiresIn) || Number(expiresIn) <= 0) {
      throw new Error("OIDC refresh response is incomplete");
    }

    return {
      accessToken,
      refreshToken: tokens.refresh_token || session.refreshToken,
      accessTokenExpiresAt: Date.now() + Number(expiresIn) * 1000,
    };
  })();

  refreshes.set(session.sessionId, {
    sourceExpiry: session.accessTokenExpiresAt,
    reuseUntil: now + REFRESH_RESULT_REUSE_MS,
    promise,
  });

  try {
    return await promise;
  } catch (error) {
    const current = refreshes.get(session.sessionId);
    if (current?.promise === promise) refreshes.delete(session.sessionId);
    throw error;
  }
}

export async function getFreshAccessToken(
  session: ActiveBankSession,
): Promise<AccessTokenResult> {
  const tokens = session.tokens;
  const now = Date.now();
  if (tokens.accessTokenExpiresAt - now > REFRESH_SKEW_MS) {
    return { status: "ready", accessToken: tokens.accessToken };
  }

  try {
    const credentials = await requestRefresh(tokens);
    tokens.accessToken = credentials.accessToken;
    tokens.refreshToken = credentials.refreshToken;
    tokens.accessTokenExpiresAt = credentials.accessTokenExpiresAt;
    await session.saveTokens();
    return { status: "ready", accessToken: credentials.accessToken };
  } catch (error) {
    const invalidGrant = error instanceof ResponseBodyError && error.error === "invalid_grant";
    const cannotPersist = error instanceof SessionCookieSizeError;
    console.warn(JSON.stringify({
      event: "bff_token_refresh_failed",
      reason: invalidGrant ? "invalid_grant" : cannotPersist ? "cookie_header_budget_exceeded" : "identity_provider_unavailable",
      ...(cannotPersist ? { headerBytes: error.headerBytes } : {}),
    }));
    // A rotated refresh token that cannot be persisted requires a fresh login.
    // Network/provider failures do not prove that the user's session is invalid.
    return { status: invalidGrant || cannotPersist ? "reauthenticate" : "unavailable" };
  }
}
