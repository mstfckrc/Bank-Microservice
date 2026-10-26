import { NextResponse } from "next/server";
import { getFreshAccessToken } from "@/lib/server/token-refresh";
import { trustedClientIp } from "@/lib/server/bank-api";
import {
  getActiveBankSession,
  getBankApiBaseUrl,
  hasValidCsrfToken,
  isSafeOrigin,
} from "@/lib/server/session";

export const runtime = "nodejs";
export const dynamic = "force-dynamic";

type Method = "GET" | "POST" | "PUT" | "DELETE";
type PathPart = string | RegExp;
type Access = "public" | "authenticated";

interface RouteRule {
  method: Method;
  parts: PathPart[];
  access: Access;
  query?: Record<string, RegExp>;
}

const identity = /^\d{10,11}$/;
const account = /^[A-Za-z0-9_-]{1,64}$/;
const reference = /^[A-Za-z0-9_-]{1,80}$/;
const numericId = /^\d{1,18}$/;
const MAX_REQUEST_BODY_BYTES = 128_000;

const rules: RouteRule[] = [
  { method: "GET", parts: ["currencies", "rates"], access: "public", query: { base: /^[A-Z]{3}$/ } },
  { method: "POST", parts: ["auth", "register"], access: "public" },

  { method: "GET", parts: ["customers", "profile"], access: "authenticated" },
  { method: "PUT", parts: ["customers", "profile"], access: "authenticated" },
  { method: "POST", parts: ["customers", "appeal"], access: "authenticated" },
  { method: "POST", parts: ["customers", "access-review"], access: "authenticated" },

  { method: "GET", parts: ["accounts"], access: "authenticated" },
  { method: "POST", parts: ["accounts"], access: "authenticated" },
  { method: "DELETE", parts: ["accounts", account], access: "authenticated" },
  { method: "GET", parts: ["transactions", "account", account], access: "authenticated" },
  { method: "POST", parts: ["transactions", "transfer"], access: "authenticated" },
  { method: "POST", parts: ["transactions", "deposit"], access: "authenticated" },

  { method: "GET", parts: ["bills", "instructions"], access: "authenticated" },
  { method: "POST", parts: ["bills", "instructions"], access: "authenticated" },
  { method: "DELETE", parts: ["bills", "instructions", numericId], access: "authenticated" },

  { method: "GET", parts: ["companies", "employees"], access: "authenticated" },
  { method: "POST", parts: ["companies", "employees"], access: "authenticated" },
  { method: "POST", parts: ["companies", "employees", "pay-salaries"], access: "authenticated" },
  { method: "GET", parts: ["companies", "employees", "auto-payment-settings"], access: "authenticated" },
  { method: "PUT", parts: ["companies", "employees", "auto-payment-settings"], access: "authenticated" },
  { method: "PUT", parts: ["companies", "employees", identity], access: "authenticated" },
  { method: "DELETE", parts: ["companies", "employees", identity], access: "authenticated" },

  { method: "GET", parts: ["admin", "accounts"], access: "authenticated" },
  { method: "GET", parts: ["admin", "customers"], access: "authenticated" },
  { method: "GET", parts: ["admin", "customers", identity, "accounts"], access: "authenticated" },
  { method: "POST", parts: ["admin", "customers", identity, "accounts"], access: "authenticated" },
  { method: "PUT", parts: ["admin", "customers", identity], access: "authenticated" },
  { method: "DELETE", parts: ["admin", "customers", identity], access: "authenticated" },
  { method: "PUT", parts: ["admin", "customers", identity, "status"], access: "authenticated", query: { status: /^(APPROVED|REJECTED|PENDING)$/ } },
  { method: "PUT", parts: ["admin", "customers", identity, "access-restriction"], access: "authenticated" },
  { method: "POST", parts: ["admin", "customers", identity, "access-review"], access: "authenticated" },
  { method: "GET", parts: ["admin", "accounts", account, "transactions"], access: "authenticated" },
  { method: "GET", parts: ["admin", "transactions"], access: "authenticated", query: { status: /^[A-Z_]{1,40}$/ } },
  { method: "PUT", parts: ["admin", "transactions", reference, "approve"], access: "authenticated" },
  { method: "PUT", parts: ["admin", "transactions", reference, "reject"], access: "authenticated" },
  { method: "GET", parts: ["admin", "logs"], access: "authenticated", query: { limit: /^\d{1,3}$/, level: /^(ALL|ERROR|WARN|INFO|DEBUG|TRACE)$/ } },
];

function matches(rule: RouteRule, method: string, parts: string[]): boolean {
  return rule.method === method
    && rule.parts.length === parts.length
    && rule.parts.every((expected, index) => typeof expected === "string"
      ? parts[index] === expected
      : expected.test(parts[index]));
}

function validQuery(rule: RouteRule, searchParams: URLSearchParams): boolean {
  const allowed = rule.query ?? {};
  const seen = new Set<string>();
  for (const [name, value] of searchParams.entries()) {
    const validator = allowed[name];
    if (!validator || seen.has(name) || value.length > 80 || !validator.test(value)) return false;
    seen.add(name);
  }
  return true;
}

function jsonError(status: number, code: string, message: string): NextResponse {
  return NextResponse.json({ code, message }, {
    status,
    headers: { "Cache-Control": "no-store" },
  });
}

function routeUrl(parts: string[], query: URLSearchParams): URL {
  const base = getBankApiBaseUrl();
  const prefix = base.href.endsWith("/") ? base.href : `${base.href}/`;
  const url = new URL(parts.map((part) => encodeURIComponent(part)).join("/"), prefix);
  url.search = query.toString();
  return url;
}

class RequestBodyTooLargeError extends Error {}

async function readLimitedBody(request: Request): Promise<ArrayBuffer | undefined> {
  if (!request.body) return undefined;
  const reader = request.body.getReader();
  const chunks: Uint8Array[] = [];
  let totalBytes = 0;

  while (true) {
    const { done, value } = await reader.read();
    if (done) break;
    totalBytes += value.byteLength;
    if (totalBytes > MAX_REQUEST_BODY_BYTES) {
      await reader.cancel();
      throw new RequestBodyTooLargeError();
    }
    chunks.push(value);
  }

  const body = new Uint8Array(totalBytes);
  let offset = 0;
  for (const chunk of chunks) {
    body.set(chunk, offset);
    offset += chunk.byteLength;
  }
  return body.buffer;
}

async function handle(request: Request, method: Method, context: { params: Promise<{ path: string[] }> }) {
  const { path } = await context.params;
  if (!Array.isArray(path) || path.length === 0 || path.some((part) => !part || part === "." || part === ".." || part.includes("/") || part.includes("\\"))) {
    return jsonError(404, "ROUTE_NOT_ALLOWED", "Bank API route not found.");
  }

  const url = new URL(request.url);
  const rule = rules.find((candidate) => matches(candidate, method, path) && validQuery(candidate, url.searchParams));
  if (!rule) return jsonError(404, "ROUTE_NOT_ALLOWED", "Bank API route not found.");

  const publicRegistration = rule.access === "public" && method === "POST";
  if (publicRegistration) {
    if (!isSafeOrigin(request) || !request.headers.get("content-type")?.toLowerCase().startsWith("application/json")) {
      return jsonError(403, "REQUEST_ORIGIN_REJECTED", "The request origin is not allowed.");
    }
  }

  const session = rule.access === "authenticated" ? await getActiveBankSession() : null;
  if (rule.access === "authenticated" && !session) {
    return jsonError(401, "SESSION_REQUIRED", "Your session has expired. Please sign in again.");
  }

  const writes = method === "POST" || method === "PUT" || method === "DELETE";
  if (writes && session && (!isSafeOrigin(request) || !hasValidCsrfToken(request, session.activity.csrfToken))) {
    return jsonError(403, "CSRF_REJECTED", "The request could not be verified.");
  }
  if (writes && request.body && !request.headers.get("content-type")?.toLowerCase().startsWith("application/json")) {
    return jsonError(415, "UNSUPPORTED_MEDIA_TYPE", "Only JSON request bodies are accepted.");
  }

  let accessToken: string | null = null;
  if (session) {
    const tokenResult = await getFreshAccessToken(session);
    if (tokenResult.status === "reauthenticate") {
      session.destroy();
      return jsonError(401, "SESSION_EXPIRED", "Your session has expired. Please sign in again.");
    }
    if (tokenResult.status === "unavailable") {
      return jsonError(503, "IDENTITY_PROVIDER_UNAVAILABLE", "Kimlik hizmetine şu anda ulaşılamıyor. Lütfen tekrar deneyin.");
    }
    accessToken = tokenResult.accessToken;
  }

  const headers = new Headers({ accept: request.headers.get("accept") ?? "application/json" });
  if (accessToken) headers.set("authorization", `Bearer ${accessToken}`);
  if (writes && request.headers.get("content-type")) headers.set("content-type", request.headers.get("content-type")!);

  const clientIp = trustedClientIp(request);
  if (clientIp) {
    headers.set("x-forwarded-for", clientIp);
    headers.set("x-real-ip", clientIp);
  }
  headers.set("x-forwarded-proto", "https");
  headers.set("x-forwarded-host", "api.bank.local");

  let body: ArrayBuffer | undefined;
  if (writes && request.body) {
    const declaredLength = request.headers.get("content-length");
    if (declaredLength && (!/^\d+$/.test(declaredLength) || Number(declaredLength) > MAX_REQUEST_BODY_BYTES)) {
      return jsonError(413, "REQUEST_TOO_LARGE", "The request payload is too large.");
    }
    try {
      body = await readLimitedBody(request);
    } catch (error) {
      if (error instanceof RequestBodyTooLargeError) {
        return jsonError(413, "REQUEST_TOO_LARGE", "The request payload is too large.");
      }
      return jsonError(400, "INVALID_REQUEST_BODY", "The request payload could not be read.");
    }
  }

  try {
    const upstream = await fetch(routeUrl(path, url.searchParams), {
      method,
      headers,
      body,
      cache: "no-store",
      redirect: "manual",
      signal: AbortSignal.timeout(60_000),
    });

    if (upstream.status >= 300 && upstream.status < 400) {
      return jsonError(502, "UPSTREAM_REDIRECT_REJECTED", "The Bank API returned an unexpected redirect.");
    }
    if (upstream.status === 401 && session) {
      session.destroy();
      return jsonError(401, "SESSION_INVALID", "Your session is no longer valid. Please sign in again.");
    }
    if (upstream.status >= 500) {
      const status = upstream.status === 503 ? 503 : 502;
      return jsonError(status, "UPSTREAM_UNAVAILABLE", "The Bank API is temporarily unavailable.");
    }

    const responseHeaders = new Headers({ "Cache-Control": "no-store" });
    const contentType = upstream.headers.get("content-type");
    if (contentType && /^(application\/(json|problem\+json)|text\/plain)(;|$)/i.test(contentType)) {
      responseHeaders.set("content-type", contentType);
    }
    const noBody = upstream.status === 204;
    return new Response(noBody ? null : upstream.body, {
      status: upstream.status,
      headers: responseHeaders,
    });
  } catch {
    return jsonError(502, "UPSTREAM_UNAVAILABLE", "The Bank API is temporarily unavailable.");
  }
}

export async function GET(request: Request, context: { params: Promise<{ path: string[] }> }) {
  return handle(request, "GET", context);
}

export async function POST(request: Request, context: { params: Promise<{ path: string[] }> }) {
  return handle(request, "POST", context);
}

export async function PUT(request: Request, context: { params: Promise<{ path: string[] }> }) {
  return handle(request, "PUT", context);
}

export async function DELETE(request: Request, context: { params: Promise<{ path: string[] }> }) {
  return handle(request, "DELETE", context);
}
