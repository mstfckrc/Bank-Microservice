import { NextResponse } from "next/server";
import { BankProfileError, fetchBankProfile, trustedClientIp } from "@/lib/server/bank-api";
import { getActiveBankSession } from "@/lib/server/session";
import { SessionCookieSizeError } from "@/lib/server/session-cookies";
import { getFreshAccessToken } from "@/lib/server/token-refresh";

export const runtime = "nodejs";
export const dynamic = "force-dynamic";

function errorResponse(status: number, code: string): NextResponse {
  return NextResponse.json({ code }, { status, headers: { "Cache-Control": "no-store" } });
}

export async function GET(request: Request) {
  const session = await getActiveBankSession();
  if (!session) return errorResponse(401, "SESSION_REQUIRED");

  const tokenResult = await getFreshAccessToken(session);
  if (tokenResult.status === "reauthenticate") {
    session.destroy();
    return errorResponse(401, "SESSION_EXPIRED");
  }
  if (tokenResult.status === "unavailable") {
    return errorResponse(503, "IDENTITY_PROVIDER_UNAVAILABLE");
  }

  try {
    // Refresh display/status on session restore, not on every Bank API request.
    // This does not extend either the idle deadline or the absolute deadline.
    const user = await fetchBankProfile(tokenResult.accessToken, trustedClientIp(request));
    const fields = ["identityNumber", "profileName", "email", "role", "status"] as const;
    if (fields.some((field) => session.user[field] !== user[field])) {
      session.tokens.user = user;
      await session.saveTokens();
    }

    return NextResponse.json({
      user,
      csrfToken: session.activity.csrfToken,
      idleExpiresAt: session.activity.idleExpiresAt,
      absoluteExpiresAt: session.tokens.absoluteExpiresAt,
    }, { headers: { "Cache-Control": "no-store" } });
  } catch (error) {
    if (error instanceof BankProfileError && error.status === 401) {
      session.destroy();
      return errorResponse(401, "SESSION_INVALID");
    }
    console.warn(JSON.stringify({
      event: "bff_session_profile_failed",
      reason: error instanceof SessionCookieSizeError ? "cookie_header_budget_exceeded" : "profile_unavailable",
    }));
    return errorResponse(503, "PROFILE_UNAVAILABLE");
  }
}
