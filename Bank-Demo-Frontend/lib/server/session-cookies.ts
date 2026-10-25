import "server-only";

import { NextResponse } from "next/server";
import type { CookieJar, CookieStore } from "iron-session";

// Leave room below Nginx's 8 KiB request Cookie limit for activity/transaction
// cookies. Nginx separately allows 16 KiB for the complete upstream response head.
export const MAX_SESSION_SET_COOKIE_BYTES = 6 * 1024;

export class SessionCookieSizeError extends Error {
  constructor(readonly headerBytes: number) {
    super("The encrypted session exceeds the cookie header budget");
    this.name = "SessionCookieSizeError";
  }
}

type CookieWrite = Parameters<CookieJar["write"]>;

function adaptCookieStore(store: CookieStore | CookieJar): CookieJar {
  if ("read" in store) return store;
  return {
    read: (name) => store.get(name)?.value,
    write: (name, value, options) => { store.set(name, value, options); },
    names: () => store.getAll?.().map((cookie) => cookie.name) ?? [],
  };
}

export function createSessionCookieWriter(store: CookieStore | CookieJar) {
  const destination = adaptCookieStore(store);
  let pending: CookieWrite[] | null = null;

  const jar: CookieJar = {
    read: (name) => destination.read(name),
    names: () => destination.names?.() ?? [],
    assertWritable: () => destination.assertWritable?.(),
    write: (...entry) => {
      if (pending) pending.push(entry);
      else destination.write(...entry);
    },
  };

  return {
    jar,
    async save(persist: () => Promise<void>): Promise<void> {
      if (pending) throw new Error("A session cookie save is already in progress");
      pending = [];
      try {
        await persist();
        const serialized = new NextResponse(null);
        for (const [name, value, options] of pending) {
          serialized.cookies.set(name, value, options);
        }
        const headerBytes = serialized.headers.getSetCookie().reduce(
          (total, value) => total + Buffer.byteLength(`Set-Cookie: ${value}\r\n`),
          0,
        );
        if (headerBytes > MAX_SESSION_SET_COOKIE_BYTES) {
          throw new SessionCookieSizeError(headerBytes);
        }
        // No cookie is sent until encryption, chunking and the full size check
        // succeed. A rejected save cannot leave half a session in the browser.
        for (const entry of pending) destination.write(...entry);
      } finally {
        pending = null;
      }
    },
  };
}
