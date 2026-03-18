"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { authService } from "@/services/auth.service";
import { customerService } from "@/services/customer.service";
import { useAuthStore } from "@/store/useAuthStore";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from "@/components/ui/card";
import { AlertCircle, Loader2, ShieldCheck, ArrowLeft } from "lucide-react";

export default function LoginPage() {
  const router = useRouter();
  const { login } = useAuthStore();
  
  // 🚀 V2: tcNo yerine identityNumber
  const [identityNumber, setIdentityNumber] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError("");
    setLoading(true);

    try {
      // 🚀 V2: Servise identityNumber gönderiyoruz
      const { token } = await authService.login({ identityNumber, password });
      
      const userProfile = await customerService.getProfile(token);
      
      login(userProfile, token);
      
      // Admin ise admin paneline, Kurumsal ise şirket paneline, Bireysel ise müşteri paneline
      if (userProfile.role.includes("ADMIN")) {
        router.push("/admin/dashboard");
      } else if (userProfile.role.includes("CORPORATE_MANAGER")) {
        // 🚀 V2: Kurumsal Yönetici Yönlendirmesi
        router.push("/company/dashboard");
      } else {
        // Geriye kalanlar (RETAIL_CUSTOMER - Bireysel)
        router.push("/user/dashboard");
      }
      router.refresh();
    } catch (err: any) {
      setError(err.response?.data?.message || "Giriş başarısız. Bilgilerinizi kontrol edin.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex flex-col items-center justify-center bg-slate-50 p-4">
      
      {/* 🚀 YENİ: Ana Menüye Dön Butonu */}
      <div className="w-full max-w-md mb-4">
        <Link href="/" className="inline-flex items-center text-sm font-semibold text-slate-500 hover:text-blue-600 transition-colors">
          <ArrowLeft className="w-4 h-4 mr-2" />
          Ana Menüye Dön
        </Link>
      </div>

      <Card className="w-full max-w-md border-none shadow-2xl">
        <CardHeader className="space-y-3 pb-6 border-b border-slate-100 bg-white rounded-t-xl">
          <div className="flex justify-center mb-2">
            <div className="p-3 bg-blue-50 rounded-full">
              <ShieldCheck className="w-8 h-8 text-blue-600" />
            </div>
          </div>
          <CardTitle className="text-2xl font-black text-center text-slate-900">Sec-Demo Bank</CardTitle>
          <CardDescription className="text-center font-medium">
            Bireysel ve Kurumsal Dijital Şubeye Giriş
          </CardDescription>
        </CardHeader>
        
        <CardContent className="pt-6 bg-white">
          <form onSubmit={handleSubmit} className="space-y-5">
            {error && (
              <div className="p-3 bg-red-50 border border-red-100 rounded-lg flex items-start gap-3 text-red-600 text-sm font-medium animate-in fade-in zoom-in duration-300">
                <AlertCircle className="w-5 h-5 shrink-0" />
                <p>{error}</p>
              </div>
            )}
            
            <div className="space-y-2">
              {/* 🚀 V2: Etiket güncellendi */}
              <Label htmlFor="identityNumber" className="text-slate-700 font-bold">TC Kimlik / Vergi Numarası</Label>
              <Input
                id="identityNumber"
                type="text"
                placeholder="11 Haneli TC veya 10 Haneli Vergi No"
                value={identityNumber}
                onChange={(e) => setIdentityNumber(e.target.value)}
                required
                disabled={loading}
                className="h-12 bg-slate-50 border-slate-200 focus-visible:ring-blue-600 font-mono text-lg transition-colors"
              />
            </div>
            
            <div className="space-y-2">
              <div className="flex items-center justify-between">
                <Label htmlFor="password" className="text-slate-700 font-bold">Dijital Şifre</Label>
              </div>
              <Input
                id="password"
                type="password"
                placeholder="••••••••"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                required
                disabled={loading}
                className="h-12 bg-slate-50 border-slate-200 focus-visible:ring-blue-600 font-mono text-lg transition-colors"
              />
            </div>
            
            <Button 
              type="submit" 
              className="w-full h-12 text-base font-bold bg-blue-600 hover:bg-blue-700 shadow-lg shadow-blue-200 transition-all"
              disabled={loading}
            >
              {loading ? <Loader2 className="w-5 h-5 animate-spin mr-2" /> : null}
              {loading ? "Giriş Yapılıyor..." : "Giriş Yap"}
            </Button>
          </form>
        </CardContent>
        
        <CardFooter className="flex justify-center bg-slate-50/50 rounded-b-xl py-6 border-t border-slate-100">
          <p className="text-sm text-slate-500 font-medium">
            Müşterimiz değil misiniz?{" "}
            <Link href="/register" className="text-blue-600 font-bold hover:underline underline-offset-4">
              Hemen Kayıt Olun
            </Link>
          </p>
        </CardFooter>
      </Card>
    </div>
  );
}