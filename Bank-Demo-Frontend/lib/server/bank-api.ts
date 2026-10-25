import "server-only";

import { isIP } from "node:net";
import type { UserProfileResponse } from "@/types";
import { getBankApiBaseUrl } from "./session";

function bankApiUrl(path: string): URL {
  const base = getBankApiBaseUrl();
  const prefix = base.href.endsWith("/") ? base.href : `${base.href}/`;
  return new URL(path.replace(/^\/+/, ""), prefix);
}

export function trustedClientIp(request: Request): string | null {
  const value = request.headers.get("x-real-ip")?.trim();
  return value && isIP(value) ? value : null;
}

export function isUserProfile(value: unknown): value is UserProfileResponse {
  if (!value || typeof value !== "object") return false;
  const profile = value as Partial<UserProfileResponse>;
  return typeof profile.identityNumber === "string"
    && typeof profile.profileName === "string"
    && typeof profile.email === "string"
    && (profile.role === "ADMIN" || profile.role === "RETAIL_CUSTOMER" || profile.role === "CORPORATE_MANAGER")
    && (profile.status === "PENDING" || profile.status === "APPROVED" || profile.status === "REJECTED");
}

export class BankProfileError extends Error {
  constructor(readonly status: number) {
    super("Bank profile could not be loaded");
    this.name = "BankProfileError";
  }
}

export async function fetchBankProfile(accessToken: string, clientIp: string | null): Promise<UserProfileResponse> {
  const headers = new Headers({
    accept: "application/json",
    authorization: `Bearer ${accessToken}`,
  });
  if (clientIp) {
    headers.set("x-forwarded-for", clientIp);
    headers.set("x-real-ip", clientIp);
  }

  const response = await fetch(bankApiUrl("customers/profile"), {
    method: "GET",
    headers,
    cache: "no-store",
    signal: AbortSignal.timeout(10_000),
  });

  if (!response.ok) throw new BankProfileError(response.status);
  const profile: unknown = await response.json();
  if (!isUserProfile(profile)) throw new Error("Bank profile response was invalid");
  return profile;
}
