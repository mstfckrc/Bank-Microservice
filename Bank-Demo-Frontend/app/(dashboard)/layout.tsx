"use client";

import Cookies from "js-cookie";
import { useEffect, useState, useRef } from "react";
import { useAuthStore } from "@/store/useAuthStore";
import Link from "next/link";
import { Button } from "@/components/ui/button"; // 🚀 BUTON İTHALATI GERİ GELDİ
import SessionExpiryModal from "@/components/dashboard/modals/SessionExpiryModal";

export default function DashboardLayout({ children }: { children: React.ReactNode }) {
  const { user, logout } = useAuthStore();
  const [isClient, setIsClient] = useState(false);
  const [timeLeft, setTimeLeft] = useState<number | null>(null);
  const [isSessionExpired, setIsSessionExpired] = useState(false);
  const timerRef = useRef<NodeJS.Timeout | null>(null);

  useEffect(() => {
    setIsClient(true);
    startTokenTimer();
    return () => { if (timerRef.current) clearInterval(timerRef.current); };
  }, []);

  const handleLogout = () => {
    const idToken = Cookies.get("id_token");
    logout(); 
    Cookies.remove("id_token", { path: "/" });
    Cookies.remove("token", { path: "/" });

    const keycloakUrl = process.env.NEXT_PUBLIC_KEYCLOAK_URL || "http://localhost:9090";
    const realm = "bank-realm";
    const redirectUri = encodeURIComponent(window.location.origin + "/login"); // 🚀 ESKİ YÖNLENDİRME GERİ GELDİ

    let logoutUrl = `${keycloakUrl}/realms/${realm}/protocol/openid-connect/logout?post_logout_redirect_uri=${redirectUri}&client_id=bank-auth-client`;

    if (idToken) {
      logoutUrl += `&id_token_hint=${idToken}`;
    }

    window.location.href = logoutUrl; 
  };

  const startTokenTimer = () => {
    const token = Cookies.get("token");
    if (!token) return;

    try {
      const payloadBase64 = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/');
      const payload = JSON.parse(atob(payloadBase64));
      const expirationTime = payload.exp * 1000; 

      timerRef.current = setInterval(() => {
        const now = Date.now();
        const distance = Math.floor((expirationTime - now) / 1000);

        if (distance <= 0) {
          clearInterval(timerRef.current!);
          setTimeLeft(0);
          setIsSessionExpired(true);
        } else {
          setTimeLeft(distance);
        }
      }, 1000);
    } catch (e) { console.error(e); }
  };

  if (!isClient) return null;

  // 🚀 ROL ROZETİ MANTIĞI GERİ GELDİ
  const getRoleLabel = (role?: string) => {
    if (role === "ADMIN") return "Yönetici Paneli";
    if (role === "CORPORATE_MANAGER") return "Kurumsal Şube";
    return "Bireysel Şube";
  };

  return (
    <div className="min-h-screen bg-slate-50 flex flex-col relative">
      
      {/* 🛡️ MODAL BURADA ÇAĞRILIYOR */}
      <SessionExpiryModal 
        isOpen={isSessionExpired} 
        onLogout={handleLogout} 
      />

      {/* HEADER */}
      <header className={`bg-white border-b border-slate-200 sticky top-0 z-10 transition-all duration-500 ${isSessionExpired ? "blur-sm pointer-events-none" : ""}`}>
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 h-16 flex items-center justify-between">
          
          <div className="flex items-center gap-4">
            <Link href="/" className="text-xl font-bold text-slate-900 tracking-tight">Sec-Demo Bank</Link>
            
            {/* 🚀 ROL ROZETİ */}
            <span className="bg-slate-100 text-slate-600 text-xs font-semibold px-2 py-1 rounded-md">
              {getRoleLabel(user?.role)}
            </span>

            {/* 🕒 ZAMANLAYICI */}
            {timeLeft !== null && (
              <div className={`px-3 py-1 rounded-full text-xs font-mono font-bold border ${
                timeLeft < 60 ? "bg-red-50 text-red-600 border-red-200 animate-pulse" : "bg-slate-50 text-slate-600 border-slate-200"
              }`}>
                OTURUM: {Math.floor(timeLeft / 60)}:{(timeLeft % 60).toString().padStart(2, '0')}
              </div>
            )}
          </div>
          
          {/* 🚀 SAĞ KANAT VE ÇIKIŞ BUTONU GERİ GELDİ */}
          <div className="flex items-center gap-4">
            <span className="text-sm font-medium text-slate-700">
              Hoş geldin, {user?.profileName}
            </span>
            <Button variant="outline" size="sm" onClick={handleLogout}>
              Çıkış Yap
            </Button>
          </div>

        </div>
      </header>

      {/* MAIN CONTENT */}
      <main className={`flex-1 max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 w-full transition-all duration-500 ${isSessionExpired ? "blur-sm pointer-events-none select-none" : ""}`}>
        {children}
      </main>
    </div>
  );
}