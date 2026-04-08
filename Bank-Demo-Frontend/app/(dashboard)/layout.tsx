"use client";

import Cookies from "js-cookie";
import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { useAuthStore } from "@/store/useAuthStore";
import { Button } from "@/components/ui/button";
import Link from "next/link";

export default function DashboardLayout({ children }: { children: React.ReactNode }) {
  const router = useRouter();
  const { user, logout } = useAuthStore();
  const [isClient, setIsClient] = useState(false);

  useEffect(() => {
    setIsClient(true);
  }, []);

  const handleLogout = () => {
    // 1. Çıkış biletini (id_token) çerezden al
    const idToken = Cookies.get("id_token");
    
    // 2. Kendi içimizi temizle (Zustand ve "token" çerezi silinir)
    logout(); 
    
    // 3. id_token çerezini de manuel sil
    Cookies.remove("id_token", { path: "/" });

    // 4. Keycloak'un Çıkış Kapısına Yönlendirme Hazırlığı
    const keycloakUrl = process.env.NEXT_PUBLIC_KEYCLOAK_URL || "http://localhost:9090";
    const realm = "bank-realm";
    // Çıkış yapınca adamı tekrar bizim login sayfasına fırlatması için dönüş adresi:
    const redirectUri = encodeURIComponent("http://localhost:3000/login");

    // 5. Müşteriyi Keycloak'tan kovuyoruz!
    let logoutUrl = `${keycloakUrl}/realms/${realm}/protocol/openid-connect/logout?post_logout_redirect_uri=${redirectUri}&client_id=bank-auth-client`;

    // Eğer id_token varsa linke ekle (Keycloak 24 bunun sayesinde onay ekranı sormadan direkt dışarı atar)
    if (idToken) {
      logoutUrl += `&id_token_hint=${idToken}`;
    }

    // 🚀 SİHİRLİ DOKUNUŞ: Next.js'in router'ı yerine window.location.href ile adamı gerçekten siteden koparıp Keycloak'a atıyoruz!
    window.location.href = logoutUrl; 
  };

  if (!isClient) return null; 

  // 🚀 V2: Rol isimlendirme mantığını yeni rollere göre genişlettik
  const getRoleLabel = (role?: string) => {
    if (role === "ADMIN") return "Yönetici Paneli";
    if (role === "CORPORATE_MANAGER") return "Kurumsal Şube";
    return "Bireysel Şube";
  };

  return (
    <div className="min-h-screen bg-slate-50 flex flex-col">
      <header className="bg-white border-b border-slate-200 sticky top-0 z-10">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 h-16 flex items-center justify-between">
          <div className="flex items-center gap-4">
            <Link href="/" className="text-xl font-bold text-slate-900 tracking-tight">Sec-Demo Bank</Link>
            <span className="bg-slate-100 text-slate-600 text-xs font-semibold px-2 py-1 rounded-md">
              {getRoleLabel(user?.role)}
            </span>
          </div>
          
          <div className="flex items-center gap-4">
            <span className="text-sm font-medium text-slate-700">
              {/* 🚀 V2: fullName yerine profileName kullanıyoruz */}
              Hoş geldin, {user?.profileName}
            </span>
            <Button variant="outline" size="sm" onClick={handleLogout}>
              Çıkış Yap
            </Button>
          </div>
        </div>
      </header>

      <main className="flex-1 max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 w-full">
        {children}
      </main>
    </div>
  );
}