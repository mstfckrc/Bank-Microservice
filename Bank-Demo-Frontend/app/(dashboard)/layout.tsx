"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { Button } from "@/components/ui/button";
import SessionExpiryModal from "@/components/dashboard/modals/SessionExpiryModal";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { useAuthStore } from "@/store/useAuthStore";
import type { UserProfileResponse } from "@/types";

interface SessionResponse {
  user: UserProfileResponse;
  csrfToken: string;
  idleExpiresAt: number;
  absoluteExpiresAt: number;
}

const ACTIVITY_THROTTLE_MS = 30_000;

export default function DashboardLayout({ children }: { children: React.ReactNode }) {
  const router = useRouter();
  const { user, csrfToken, idleExpiresAt, absoluteExpiresAt, login, updateSession, logout } = useAuthStore();
  const [sessionReady, setSessionReady] = useState(false);
  const [sessionLoadError, setSessionLoadError] = useState("");
  const [sessionLoadAttempt, setSessionLoadAttempt] = useState(0);
  const [timeLeft, setTimeLeft] = useState<number | null>(null);
  const [isSessionExpired, setIsSessionExpired] = useState(false);
  const [isLogoutConfirmOpen, setIsLogoutConfirmOpen] = useState(false);
  const [isLogoutSubmitting, setIsLogoutSubmitting] = useState(false);
  const logoutFormRef = useRef<HTMLFormElement>(null);
  const lastActivityRef = useRef(0);
  const activityRequestRef = useRef(false);
  const logoutInProgressRef = useRef(false);

  useEffect(() => {
    let cancelled = false;
    const restoreSession = async () => {
      try {
        const response = await fetch("/api/auth/session", { cache: "no-store", credentials: "same-origin" });
        if (response.status === 401) {
          if (!cancelled) {
            logout();
            router.replace("/login?session=expired");
          }
          return;
        }
        if (!response.ok) throw new Error("Server session unavailable");
        const session = await response.json() as SessionResponse;
        if (!session.user || !session.csrfToken || !Number.isFinite(session.idleExpiresAt)
          || !Number.isFinite(session.absoluteExpiresAt) || !session.user.accessRestrictionStatus) {
          throw new Error("Invalid server session response");
        }
        if (!cancelled) {
          login(session.user, session.csrfToken, session.idleExpiresAt, session.absoluteExpiresAt);
          if (session.user.accessRestrictionStatus !== "UNRESTRICTED") {
            router.replace("/account-restricted");
            return;
          }
          setSessionReady(true);
        }
      } catch {
        if (!cancelled) {
          setSessionLoadError("Oturum bilgilerinize şu anda ulaşılamıyor. Lütfen tekrar deneyin.");
        }
      }
    };
    void restoreSession();
    return () => { cancelled = true; };
  }, [login, logout, router, sessionLoadAttempt]);

  const endSession = useCallback(() => {
    if (logoutInProgressRef.current) return;
    logoutInProgressRef.current = true;
    setIsLogoutSubmitting(true);
    logoutFormRef.current?.requestSubmit();
  }, []);

  useEffect(() => {
    if (!sessionReady || !csrfToken) return;
    const noteActivity = () => {
      const now = Date.now();
      if (activityRequestRef.current || now - lastActivityRef.current < ACTIVITY_THROTTLE_MS) return;
      lastActivityRef.current = now;
      activityRequestRef.current = true;

      void fetch("/api/auth/activity", {
        method: "POST",
        headers: { "X-CSRF-Token": csrfToken },
        credentials: "same-origin",
        cache: "no-store",
      }).then(async (response) => {
        if (!response.ok) {
          if (response.status === 401) {
            logout();
            router.replace("/login?session=expired");
          }
          return;
        }
        const session = await response.json() as Pick<SessionResponse, "idleExpiresAt" | "absoluteExpiresAt">;
        if (Number.isFinite(session.idleExpiresAt) && Number.isFinite(session.absoluteExpiresAt)) {
          updateSession(session.idleExpiresAt, session.absoluteExpiresAt);
        }
      }).catch(() => undefined).finally(() => {
        activityRequestRef.current = false;
      });
    };

    const events: Array<keyof WindowEventMap> = ["pointerdown", "keydown", "touchstart", "wheel"];
    for (const event of events) window.addEventListener(event, noteActivity, { passive: true });
    return () => {
      for (const event of events) window.removeEventListener(event, noteActivity);
    };
  }, [csrfToken, logout, router, sessionReady, updateSession]);

  useEffect(() => {
    if (!sessionReady || !idleExpiresAt || !absoluteExpiresAt) return;
    const updateCountdown = () => {
      const remaining = Math.max(0, Math.floor((Math.min(idleExpiresAt, absoluteExpiresAt) - Date.now()) / 1000));
      setTimeLeft(remaining);
      setIsSessionExpired(remaining <= 0);
    };
    updateCountdown();
    const timer = window.setInterval(updateCountdown, 1000);
    return () => window.clearInterval(timer);
  }, [absoluteExpiresAt, idleExpiresAt, sessionReady]);

  if (!sessionReady) {
    return (
      <div className="min-h-screen flex flex-col items-center justify-center gap-4 bg-slate-50 text-slate-600">
        <p role={sessionLoadError ? "alert" : "status"}>{sessionLoadError || "Oturum doğrulanıyor..."}</p>
        {sessionLoadError && (
          <Button onClick={() => {
            setSessionLoadError("");
            setSessionLoadAttempt((attempt) => attempt + 1);
          }}>Tekrar Dene</Button>
        )}
      </div>
    );
  }

  const getRoleLabel = (role?: string) => {
    if (role === "ADMIN") return "Yönetici Paneli";
    if (role === "CORPORATE_MANAGER") return "Kurumsal Şube";
    return "Bireysel Şube";
  };

  return (
    <div className="min-h-screen bg-slate-50 flex flex-col relative">
      <SessionExpiryModal isOpen={isSessionExpired} onLogout={endSession} />
      <Dialog open={isLogoutConfirmOpen} onOpenChange={(open) => {
        if (!isLogoutSubmitting) setIsLogoutConfirmOpen(open);
      }}>
        <DialogContent showCloseButton={false}>
          <DialogHeader>
            <DialogTitle>Oturumu kapatmak istiyor musunuz?</DialogTitle>
            <DialogDescription>
              Güvenli çıkış yapılacak ve bu cihazdaki uygulama oturumu sonlandırılacak.
            </DialogDescription>
          </DialogHeader>
          <DialogFooter>
            <Button
              type="button"
              variant="outline"
              disabled={isLogoutSubmitting}
              onClick={() => setIsLogoutConfirmOpen(false)}
            >
              Vazgeç
            </Button>
            <Button type="button" disabled={isLogoutSubmitting} onClick={endSession}>
              {isLogoutSubmitting ? "Çıkış yapılıyor…" : "Çıkış Yap"}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
      <form ref={logoutFormRef} action="/api/auth/logout" method="post" hidden>
        <input type="hidden" name="csrfToken" value={csrfToken ?? ""} />
      </form>

      <header className={`bg-white border-b border-slate-200 sticky top-0 z-10 transition-all duration-500 ${isSessionExpired ? "blur-sm pointer-events-none" : ""}`}>
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 h-16 flex items-center justify-between">
          <div className="flex items-center gap-4">
            <Link href="/" className="text-xl font-bold text-slate-900 tracking-tight">Sec-Demo Bank</Link>
            <span className="bg-slate-100 text-slate-600 text-xs font-semibold px-2 py-1 rounded-md">{getRoleLabel(user?.role)}</span>
            {timeLeft !== null && (
              <div className={`px-3 py-1 rounded-full text-xs font-mono font-bold border ${timeLeft < 60 ? "bg-red-50 text-red-600 border-red-200 animate-pulse" : "bg-slate-50 text-slate-600 border-slate-200"}`}>
                OTURUM: {Math.floor(timeLeft / 60)}:{(timeLeft % 60).toString().padStart(2, "0")}
              </div>
            )}
          </div>

          <div className="flex items-center gap-4">
            <span className="text-sm font-medium text-slate-700">Hoş geldin, {user?.profileName}</span>
            <Button
              variant="outline"
              size="sm"
              onClick={() => setIsLogoutConfirmOpen(true)}
            >
              Çıkış Yap
            </Button>
          </div>
        </div>
      </header>

      <main className={`flex-1 max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 w-full transition-all duration-500 ${isSessionExpired ? "blur-sm pointer-events-none select-none" : ""}`}>
        {children}
      </main>
    </div>
  );
}
