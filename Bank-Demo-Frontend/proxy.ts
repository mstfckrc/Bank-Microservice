import { nextProxyCookies } from "iron-session";
import { NextResponse } from "next/server";
import type { NextRequest } from "next/server";
import { BANK_ACTIVITY_COOKIE, BANK_SESSION_COOKIE, getActiveBankSession } from "@/lib/server/session";

function copyCookies(source: NextResponse, target: NextResponse): void {
  for (const cookie of source.cookies.getAll()) {
    target.cookies.set(cookie.name, cookie.value, cookie);
  }
}

function expireLegacyCookies(request: NextRequest, response: NextResponse): void {
  for (const name of ["token", "id_token"]) {
    if (request.cookies.has(name)) {
      response.cookies.set(name, "", { path: "/", maxAge: 0, httpOnly: true, secure: true, sameSite: "lax" });
    }
  }
}

function expireBankSessionCookies(request: NextRequest, response: NextResponse): void {
  const names = request.cookies.getAll().map((cookie) => cookie.name);
  const sessionNames = [BANK_SESSION_COOKIE, ...Array.from({ length: 4 }, (_, index) => `${BANK_SESSION_COOKIE}.${index}`), BANK_ACTIVITY_COOKIE];
  for (const name of sessionNames) {
    if (names.includes(name)) {
      response.cookies.set(name, "", { path: "/", maxAge: 0, httpOnly: true, secure: true, sameSite: "lax" });
    }
  }
}

function redirect(request: NextRequest, pathname: string, previous: NextResponse): NextResponse {
  const response = NextResponse.redirect(new URL(pathname, request.url));
  copyCookies(previous, response);
  expireLegacyCookies(request, response);
  return response;
}

function homeForRole(role: string): string {
  if (role === "ADMIN") return "/admin/dashboard";
  if (role === "CORPORATE_MANAGER") return "/company/dashboard";
  return "/user/dashboard";
}

export async function proxy(request: NextRequest) {
  const { pathname } = request.nextUrl;
  const isAuthRoute = pathname === "/login" || pathname.startsWith("/register");
  const isAdminRoute = pathname.startsWith("/admin");
  const isUserRoute = pathname.startsWith("/user") || pathname.startsWith("/dashboard");
  const isCompanyRoute = pathname.startsWith("/company");
  const isProtectedRoute = isAdminRoute || isUserRoute || isCompanyRoute;

  const response = NextResponse.next();
  expireLegacyCookies(request, response);

  let session;
  try {
    session = await getActiveBankSession(nextProxyCookies(request, response));
  } catch {
    // A missing or invalid BFF configuration must fail closed for protected UI.
    expireBankSessionCookies(request, response);
    if (isProtectedRoute) return redirect(request, "/login?error=session_unavailable", response);
    return response;
  }

  if (!session) {
    if (isProtectedRoute) return redirect(request, "/login", response);
    return response;
  }

  const role = session.user.role;
  if (isAuthRoute) return redirect(request, homeForRole(role), response);
  if (isAdminRoute && role !== "ADMIN") return redirect(request, homeForRole(role), response);
  if (isCompanyRoute && role !== "CORPORATE_MANAGER") return redirect(request, homeForRole(role), response);
  if (isUserRoute && (role === "ADMIN" || role === "CORPORATE_MANAGER")) {
    return redirect(request, homeForRole(role), response);
  }

  return response;
}

export const config = {
  matcher: [
    "/login",
    "/register/:path*",
    "/admin/:path*",
    "/user/:path*",
    "/dashboard/:path*",
    "/company/:path*",
  ],
};
