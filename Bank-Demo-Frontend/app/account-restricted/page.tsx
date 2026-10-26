"use client";

import { useEffect, useRef, useState } from "react";
import { useRouter } from "next/navigation";
import { AlertTriangle, Loader2, LogOut, Send } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import api from "@/lib/axios";
import { useAuthStore } from "@/store/useAuthStore";
import type { UserProfileResponse } from "@/types";

interface SessionResponse {
  user: UserProfileResponse;
  csrfToken: string;
  idleExpiresAt: number;
  absoluteExpiresAt: number;
}

const stateCopy = {
  RESTRICTED: {
    title: "Hesap erişiminiz kısıtlandı",
    description: "Bankacılık işlemleriniz geçici olarak durduruldu. İnceleme talebi gönderebilir veya güvenli şekilde çıkış yapabilirsiniz.",
  },
  REVIEW_PENDING: {
    title: "İnceleme talebiniz alındı",
    description: "Talebiniz banka yöneticisinin incelemesini bekliyor. Sonuçlanana kadar bankacılık işlemleri kullanılamaz.",
  },
  REVIEW_REJECTED: {
    title: "İnceleme talebi sonuçlandı",
    description: "Talebiniz onaylanmadı. Erişim kısıtlaması, banka yöneticisi kaldırana kadar devam eder.",
  },
} as const;

export default function AccountRestrictedPage() {
  const router = useRouter();
  const { user, csrfToken, login, logout } = useAuthStore();
  const [ready, setReady] = useState(false);
  const [loadError, setLoadError] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const logoutForm = useRef<HTMLFormElement>(null);
  const state = user?.accessRestrictionStatus;

  useEffect(() => {
    let cancelled = false;
    void fetch("/api/auth/session", { cache: "no-store", credentials: "same-origin" }).then(async (response) => {
      if (response.status === 401) {
        logout();
        router.replace("/login?session=expired");
        return;
      }
      if (!response.ok) throw new Error("Current access status is unavailable");
      const session = await response.json() as SessionResponse;
      if (!session.user?.accessRestrictionStatus || !session.csrfToken
        || !Number.isFinite(session.idleExpiresAt) || !Number.isFinite(session.absoluteExpiresAt)) {
        throw new Error("Current access status is invalid");
      }
      if (cancelled) return;
      login(session.user, session.csrfToken, session.idleExpiresAt, session.absoluteExpiresAt);
      if (session.user.accessRestrictionStatus === "UNRESTRICTED") {
        const destination = session.user.role === "ADMIN" ? "/admin/dashboard"
          : session.user.role === "CORPORATE_MANAGER" ? "/company/dashboard" : "/user/dashboard";
        router.replace(destination);
        return;
      }
      setReady(true);
    }).catch(() => {
      if (!cancelled) setLoadError("Erişim durumunu şu anda doğrulayamıyoruz. Lütfen tekrar deneyin.");
    });
    return () => { cancelled = true; };
  }, [login, logout, router]);

  const requestReview = async () => {
    setSubmitting(true);
    try {
      await api.post("/customers/access-review", {});
      useAuthStore.getState().updateUser({ accessRestrictionStatus: "REVIEW_PENDING" });
    } catch {
      // The shared API interceptor already presents the safe error message.
    } finally {
      setSubmitting(false);
    }
  };

  if (!ready || !state || state === "UNRESTRICTED") {
    return (
      <main className="min-h-screen grid place-items-center bg-slate-50 p-6">
        <div role={loadError ? "alert" : "status"} className="flex items-center gap-3 text-slate-600">
          {!loadError && <Loader2 className="h-5 w-5 animate-spin" />}
          <span>{loadError || "Hesap erişimi doğrulanıyor…"}</span>
          {loadError && <Button variant="outline" onClick={() => window.location.reload()}>Tekrar dene</Button>}
        </div>
      </main>
    );
  }

  const copy = stateCopy[state];
  return (
    <main className="min-h-screen grid place-items-center bg-slate-50 p-6">
      <Card className="w-full max-w-xl shadow-lg">
        <CardHeader>
          <div className="mb-2 flex h-11 w-11 items-center justify-center rounded-full bg-amber-100 text-amber-700">
            <AlertTriangle className="h-6 w-6" />
          </div>
          <CardTitle>{copy.title}</CardTitle>
          <CardDescription>{copy.description}</CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          {state === "RESTRICTED" && (
            <Button className="w-full" onClick={requestReview} disabled={submitting}>
              {submitting ? <Loader2 className="mr-2 h-4 w-4 animate-spin" /> : <Send className="mr-2 h-4 w-4" />}
              İnceleme talebi gönder
            </Button>
          )}
          {state === "REVIEW_PENDING" && <p role="status" className="rounded-md bg-amber-50 p-3 text-sm text-amber-900">Talebiniz başarıyla iletildi. İnceleme sonuçlanana kadar yeni bir talep göndermeniz gerekmiyor.</p>}
          {state === "REVIEW_REJECTED" && <p className="rounded-md bg-red-50 p-3 text-sm text-red-900">Bu kısıtlama dönemi için yeni bir talep gönderilemez.</p>}
          <form ref={logoutForm} action="/api/auth/logout" method="post">
            <input type="hidden" name="csrfToken" value={csrfToken ?? ""} />
            <Button type="submit" variant="outline" className="w-full">
              <LogOut className="mr-2 h-4 w-4" /> Güvenli çıkış yap
            </Button>
          </form>
        </CardContent>
      </Card>
    </main>
  );
}
