import { NextResponse } from "next/server";
import { BANK_SESSION_IDLE_SECONDS, getActiveBankSession, hasValidCsrfToken, isSafeOrigin } from "@/lib/server/session";

export const runtime = "nodejs";
export const dynamic = "force-dynamic";

export async function POST(request: Request) {
  const session = await getActiveBankSession();
  if (!session) {
    return NextResponse.json({ code: "SESSION_REQUIRED" }, {
      status: 401,
      headers: { "Cache-Control": "no-store" },
    });
  }
  if (!isSafeOrigin(request) || !hasValidCsrfToken(request, session.activity.csrfToken)) {
    return NextResponse.json({ code: "CSRF_REJECTED" }, {
      status: 403,
      headers: { "Cache-Control": "no-store" },
    });
  }

  session.activity.idleExpiresAt = Math.min(
    Date.now() + BANK_SESSION_IDLE_SECONDS * 1000,
    session.tokens.absoluteExpiresAt,
  );
  await session.activity.save();

  return NextResponse.json({
    idleExpiresAt: session.activity.idleExpiresAt,
    absoluteExpiresAt: session.tokens.absoluteExpiresAt,
  }, { headers: { "Cache-Control": "no-store" } });
}
