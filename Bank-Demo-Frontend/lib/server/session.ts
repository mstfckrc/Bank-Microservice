import "server-only";

import { randomBytes, randomUUID } from "node:crypto";
import { cookies } from "next/headers";
import { getIronSession, type CookieJar, type CookieStore, type IronSession, type SessionOptions } from "iron-session";
import type { UserProfileResponse } from "@/types";
import { createSessionCookieWriter } from "./session-cookies";

export const BANK_SESSION_COOKIE = "__Host-bank_session";
export const BANK_ACTIVITY_COOKIE = "__Host-bank_activity";
export const OIDC_TRANSACTION_COOKIE = "__Host-bank_oidc_tx";
export const BANK_SESSION_IDLE_SECONDS = 300;
export const BANK_SESSION_MAX_SECONDS = 900;
export const OIDC_TRANSACTION_TTL_SECONDS = 300;

export interface BankSessionData {
  version: 1;
  sessionId: string;
  accessToken: string;
  refreshToken: string;
  // Kept encrypted in the HttpOnly session cookie for standard RP-initiated logout.
  // Optional only so sessions issued by the immediately previous app version can
  // still be read and locally cleared during rollout.
  idToken?: string;
  accessTokenExpiresAt: number;
  absoluteExpiresAt: number;
  user: UserProfileResponse;
}

export interface BankActivityData {
  sessionId: string;
  csrfToken: string;
  idleExpiresAt: number;
}

export type ActiveBankTokenSession = IronSession<BankSessionData> & BankSessionData;
export type ActiveBankActivitySession = IronSession<BankActivityData> & BankActivityData;

export interface ActiveBankSession {
  tokens: ActiveBankTokenSession;
  activity: ActiveBankActivitySession;
  user: UserProfileResponse;
  saveTokens: () => Promise<void>;
  destroy: () => void;
}

export interface BankTokenStore {
  data: IronSession<BankSessionData>;
  save: () => Promise<void>;
}

export interface OidcTransactionData {
  state: string;
  nonce: string;
  codeVerifier: string;
  createdAt: number;
}

function requiredSessionPassword(): string {
  const password = process.env.BFF_SESSION_PASSWORD;
  if (!password || password.length < 32) {
    throw new Error("BFF session encryption is not configured");
  }
  return password;
}

function cookieOptions(ttl: number): SessionOptions["cookieOptions"] {
  return {
    httpOnly: true,
    secure: true,
    sameSite: "lax",
    path: "/",
    maxAge: ttl,
  };
}

function sessionOptions(cookieName: string, ttl: number, chunk = false): SessionOptions {
  return {
    cookieName,
    password: requiredSessionPassword(),
    ttl,
    chunk,
    cookieOptions: cookieOptions(ttl),
  };
}

export async function getBankSessionStore(
  cookieStore?: CookieStore | CookieJar,
): Promise<BankTokenStore> {
  const writer = createSessionCookieWriter(cookieStore ?? (await cookies()));
  const data = await getIronSession<BankSessionData>(
    writer.jar,
    sessionOptions(BANK_SESSION_COOKIE, BANK_SESSION_MAX_SECONDS, true),
  );
  return { data, save: () => writer.save(() => data.save()) };
}

export async function getBankActivityStore(
  cookieStore?: CookieStore | CookieJar,
): Promise<IronSession<BankActivityData>> {
  return getIronSession<BankActivityData>(
    cookieStore ?? (await cookies()),
    sessionOptions(BANK_ACTIVITY_COOKIE, BANK_SESSION_MAX_SECONDS),
  );
}

export async function getOidcTransactionStore(
  cookieStore?: CookieStore | CookieJar,
): Promise<IronSession<OidcTransactionData>> {
  return getIronSession<OidcTransactionData>(
    cookieStore ?? (await cookies()),
    sessionOptions(OIDC_TRANSACTION_COOKIE, OIDC_TRANSACTION_TTL_SECONDS),
  );
}

function isUserProfile(value: unknown): value is UserProfileResponse {
  if (!value || typeof value !== "object") return false;
  const user = value as Partial<UserProfileResponse>;
  return typeof user.identityNumber === "string"
    && typeof user.profileName === "string"
    && typeof user.email === "string"
    && (user.role === "ADMIN" || user.role === "RETAIL_CUSTOMER" || user.role === "CORPORATE_MANAGER")
    && (user.status === "PENDING" || user.status === "APPROVED" || user.status === "REJECTED")
    && (user.accessRestrictionStatus === undefined
      || user.accessRestrictionStatus === "UNRESTRICTED"
      || user.accessRestrictionStatus === "RESTRICTED"
      || user.accessRestrictionStatus === "REVIEW_PENDING"
      || user.accessRestrictionStatus === "REVIEW_REJECTED");
}

function isBankSession(value: Partial<BankSessionData>): value is BankSessionData {
  return value.version === 1
    && typeof value.sessionId === "string"
    && typeof value.accessToken === "string"
    && typeof value.refreshToken === "string"
    && (value.idToken === undefined || typeof value.idToken === "string")
    && Number.isFinite(value.accessTokenExpiresAt)
    && Number.isFinite(value.absoluteExpiresAt)
    && isUserProfile(value.user);
}

function isBankActivity(value: Partial<BankActivityData>): value is BankActivityData {
  return typeof value.sessionId === "string"
    && typeof value.csrfToken === "string"
    && Number.isFinite(value.idleExpiresAt);
}

function hasCookie(jar: CookieStore | CookieJar, name: string, chunked = false): boolean {
  const names = chunked ? [name, ...Array.from({ length: 4 }, (_, index) => `${name}.${index}`)] : [name];
  if ("read" in jar) return names.some((cookieName) => Boolean(jar.read(cookieName)));
  return names.some((cookieName) => Boolean(jar.get(cookieName)))
    || Boolean(jar.getAll?.().some((cookie) => names.includes(cookie.name)));
}

export async function getActiveBankSession(
  cookieStore?: CookieStore | CookieJar,
): Promise<ActiveBankSession | null> {
  const jar = cookieStore ?? (await cookies());
  const [tokenStore, activity] = await Promise.all([
    getBankSessionStore(jar),
    getBankActivityStore(jar),
  ]);
  const tokens = tokenStore.data;
  const now = Date.now();

  if (!isBankSession(tokens) || !isBankActivity(activity)
    || tokens.sessionId !== activity.sessionId
    || now >= tokens.absoluteExpiresAt
    || now >= activity.idleExpiresAt) {
    if (hasCookie(jar, BANK_SESSION_COOKIE, true)) tokens.destroy();
    if (hasCookie(jar, BANK_ACTIVITY_COOKIE)) activity.destroy();
    return null;
  }

  const remainingSeconds = Math.max(1, Math.ceil((tokens.absoluteExpiresAt - now) / 1000));
  tokens.updateConfig(sessionOptions(BANK_SESSION_COOKIE, remainingSeconds, true));
  activity.updateConfig(sessionOptions(BANK_ACTIVITY_COOKIE, remainingSeconds));

  return {
    tokens: tokens as ActiveBankTokenSession,
    activity: activity as ActiveBankActivitySession,
    user: tokens.user,
    saveTokens: tokenStore.save,
    destroy: () => {
      tokens.destroy();
      activity.destroy();
    },
  };
}

export interface BankLogoutContext {
  csrfToken: string | null;
  idToken: string | null;
  destroy: () => void;
}

/**
 * Read the signed cookie pair for logout without applying the idle deadline.
 * An idle-expired session still needs its ID token to end the Keycloak SSO
 * session; this context must never be used to authorize API requests.
 */
export async function getBankLogoutContext(
  cookieStore?: CookieStore | CookieJar,
): Promise<BankLogoutContext> {
  const jar = cookieStore ?? (await cookies());
  const [tokenStore, activity] = await Promise.all([
    getBankSessionStore(jar),
    getBankActivityStore(jar),
  ]);
  const tokens = tokenStore.data;
  const matches = isBankSession(tokens)
    && isBankActivity(activity)
    && tokens.sessionId === activity.sessionId;
  const withinAbsoluteLifetime = matches && Date.now() < tokens.absoluteExpiresAt;

  return {
    csrfToken: matches ? activity.csrfToken : null,
    idToken: withinAbsoluteLifetime && typeof tokens.idToken === "string" ? tokens.idToken : null,
    destroy: () => {
      if (hasCookie(jar, BANK_SESSION_COOKIE, true)) tokens.destroy();
      if (hasCookie(jar, BANK_ACTIVITY_COOKIE)) activity.destroy();
    },
  };
}

export function createCsrfToken(): string {
  return randomBytes(32).toString("base64url");
}

export function createSessionId(): string {
  return randomUUID();
}

export function isSafeOrigin(request: Request): boolean {
  const origin = request.headers.get("origin");
  if (!origin) return false;

  try {
    return new URL(origin).origin === getPublicAppOrigin();
  } catch {
    return false;
  }
}

export function hasValidCsrfToken(request: Request, expected: string): boolean {
  const provided = request.headers.get("x-csrf-token");
  return provided !== null && hasValidCsrfTokenValue(provided, expected);
}

export function hasValidCsrfTokenValue(provided: string, expected: string): boolean {
  if (!provided || provided.length !== expected.length) return false;

  let difference = 0;
  for (let index = 0; index < expected.length; index += 1) {
    difference |= expected.charCodeAt(index) ^ provided.charCodeAt(index);
  }
  return difference === 0;
}

export function getPublicAppOrigin(): string {
  const configured = process.env.NEXT_PUBLIC_FRONTEND_URL;
  if (!configured) throw new Error("Public application URL is not configured");

  const url = new URL(configured);
  if (url.protocol !== "https:" || url.pathname !== "/" || url.username || url.password || url.search || url.hash) {
    throw new Error("Public application URL must be a clean HTTPS URL");
  }
  return url.origin;
}

export function getCallbackUrl(): URL {
  return new URL("/api/auth/callback", getPublicAppOrigin());
}

export function getBankApiBaseUrl(): URL {
  const configured = process.env.BFF_API_BASE_URL;
  if (!configured) throw new Error("Internal Bank API URL is not configured");

  const url = new URL(configured);
  if (url.protocol !== "http:" && url.protocol !== "https:") {
    throw new Error("Internal Bank API URL has an unsupported protocol");
  }
  if (url.username || url.password || url.search || url.hash) {
    throw new Error("Internal Bank API URL must not contain credentials or query parameters");
  }
  return url;
}
