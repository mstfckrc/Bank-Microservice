"use client";

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
    logout(); // 1. Zustand ve Çerezler (path: "/" ile) temizlendi
    router.push("/login"); // 2. Rota Login'e yönlendirildi
    router.refresh(); // 🚀 3. SİHİRLİ DOKUNUŞ: Next.js önbelleğini patlat ve Proxy'yi uyanmaya zorla!
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