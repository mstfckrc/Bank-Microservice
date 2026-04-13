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
        // 1. Cüzdandaki (Cookie) bileti al
        const token = Cookies.get("token");
        if (!token) throw new Error("Güvenlik jetonu bulunamadı!");

        // 2. Karargahtan (Backend) Ham Profili Çek (Backend 'Sistem Yöneticisi' dese bile umurumuzda değil)
        const backendProfile = await customerService.getProfile(token);

        // 🚀 3. MİMARİ STANDART: Next.js elindeki bileti açar ve gerçek kimliği okur!
        const payloadBase64 = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/');
        const pad = payloadBase64.length % 4;
        const paddedBase64 = pad > 0 ? payloadBase64 + '='.repeat(4 - pad) : payloadBase64;
        
        // 🚀 TÜRKÇE KARAKTER ZIRHI (UTF-8 Decoding)
        const binaryString = atob(paddedBase64);
        const decodedJson = decodeURIComponent(
          binaryString.split('').map(function(c) {
            return '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2);
          }).join('')
        );
        
        const keycloakData = JSON.parse(decodedJson);

        // Keycloak'un biletindeki GERÇEK verileri al
        const realName = keycloakData.name || (keycloakData.given_name ? `${keycloakData.given_name} ${keycloakData.family_name}` : null);
        const realEmail = keycloakData.email;

        // 4. Backend'den gelen veriyi, Token'dan gelen GERÇEK veriyle harmanla
        const finalProfile = {
          ...backendProfile,
          // Eğer token'da isim varsa onu kullan, yoksa backend'in dediğini kullan
          profileName: realName || backendProfile.profileName, 
          email: realEmail || backendProfile.email
        };

        // 5. Zustand Kapsülüne (State) GERÇEK PROFİLİ Kaydet
        login(finalProfile, token);

        // 6. Yetkiye Göre İlgili Karargaha Fırlat!
        if (finalProfile.role.includes("ADMIN")) {
          router.push("/admin/dashboard");
        } else if (finalProfile.role.includes("CORPORATE_MANAGER")) {
          router.push("/company/dashboard");
        } else {
          router.push("/user/dashboard");
        }
        
        router.refresh();

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