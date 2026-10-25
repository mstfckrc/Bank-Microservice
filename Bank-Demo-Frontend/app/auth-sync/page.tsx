"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { Loader2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import { useAuthStore } from "@/store/useAuthStore";
import type { UserProfileResponse } from "@/types";

interface SessionResponse {
  user: UserProfileResponse;
  csrfToken: string;
  idleExpiresAt: number;
  absoluteExpiresAt: number;
}

export default function AuthSyncPage() {
  const router = useRouter();
  const login = useAuthStore((state) => state.login);
  const logout = useAuthStore((state) => state.logout);
  const [error, setError] = useState("");
  const [attempt, setAttempt] = useState(0);

  useEffect(() => {
    const syncSession = async () => {
      try {
        const response = await fetch("/api/auth/session", {
          cache: "no-store",
          credentials: "same-origin",
        });
        if (response.status === 401) {
          logout();
          router.replace("/login?session=expired");
          return;
        }
        if (!response.ok) throw new Error("The server session is unavailable");

        const session = await response.json() as SessionResponse;
        if (!session.user || !session.csrfToken || !Number.isFinite(session.idleExpiresAt)
          || !Number.isFinite(session.absoluteExpiresAt)) {
          throw new Error("The server session response is incomplete");
        }

        login(session.user, session.csrfToken, session.idleExpiresAt, session.absoluteExpiresAt);
        if (session.user.role === "ADMIN") {
          router.replace("/admin/dashboard");
        } else if (session.user.role === "CORPORATE_MANAGER") {
          router.replace("/company/dashboard");
        } else {
          router.replace("/user/dashboard");
        }
        router.refresh();
      } catch {
        setError("Oturum bilgileriniz şu anda yüklenemiyor. Lütfen tekrar deneyin.");
      }
    };

    void syncSession();
  }, [attempt, login, logout, router]);

  return (
    <div className="min-h-screen flex flex-col items-center justify-center bg-slate-50">
      {error ? (
        <div className="text-center space-y-4">
          <p role="alert" className="text-red-600 font-bold p-4 bg-red-50 rounded-lg border border-red-200">{error}</p>
          <Button onClick={() => {
            setError("");
            setAttempt((value) => value + 1);
          }}>Tekrar Dene</Button>
        </div>
      ) : (
        <div className="flex flex-col items-center animate-in fade-in zoom-in duration-500">
          <Loader2 className="w-12 h-12 text-blue-600 animate-spin mb-4" />
          <h2 className="text-xl font-bold text-slate-800">Güvenli Bağlantı Kuruluyor</h2>
          <p className="text-slate-500 mt-2 font-medium">Oturumunuz doğrulanıyor...</p>
        </div>
      )}
    </div>
  );
}
