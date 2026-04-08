"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import Cookies from "js-cookie";
import { customerService } from "@/services/customer.service";
import { useAuthStore } from "@/store/useAuthStore";
import { Loader2 } from "lucide-react";

export default function AuthSyncPage() {
  const router = useRouter();
  const { login } = useAuthStore();
  const [error, setError] = useState("");

  useEffect(() => {
    const syncProfile = async () => {
      try {
        // 1. Ajanımızın çereze bastığı Token'ı al
        const token = Cookies.get("token");
        if (!token) throw new Error("Güvenlik jetonu bulunamadı!");

        // 2. Token ile Karargahtan (Backend) Gerçek Müşteri Profilini Çek!
        const userProfile = await customerService.getProfile(token);

        // 3. Zustand Kapsülüne (State) Kaydet
        login(userProfile, token);

        // 4. Yetkiye Göre İlgili Karargaha Fırlat!
        if (userProfile.role.includes("ADMIN")) {
          router.push("/admin/dashboard");
        } else if (userProfile.role.includes("CORPORATE_MANAGER")) {
          router.push("/company/dashboard");
        } else {
          router.push("/user/dashboard");
        }
        
        router.refresh(); // Önbelleği temizle, Proxy'yi tetikle!

      } catch (err) {
        console.error("Senkronizasyon Hatası:", err);
        setError("Profil bilgileri alınamadı. Lütfen tekrar giriş yapın.");
        setTimeout(() => router.push("/login"), 3000);
      }
    };

    syncProfile();
  }, [login, router]);

  return (
    <div className="min-h-screen flex flex-col items-center justify-center bg-slate-50">
      {error ? (
        <div className="text-red-600 font-bold p-4 bg-red-50 rounded-lg border border-red-200">
          {error}
        </div>
      ) : (
        <div className="flex flex-col items-center animate-in fade-in zoom-in duration-500">
          <Loader2 className="w-12 h-12 text-blue-600 animate-spin mb-4" />
          <h2 className="text-xl font-bold text-slate-800">Güvenli Bağlantı Kuruluyor</h2>
          <p className="text-slate-500 mt-2 font-medium">Profiliniz senkronize ediliyor, lütfen bekleyin...</p>
        </div>
      )}
    </div>
  );
}