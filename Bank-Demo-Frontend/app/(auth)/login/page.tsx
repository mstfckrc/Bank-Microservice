"use client";

import { Suspense, useState } from "react";
import Link from "next/link";
import { useSearchParams } from "next/navigation";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { ShieldCheck, ArrowLeft, Loader2 } from "lucide-react";

const LOGIN_ERRORS: Record<string, string> = {
  identity_provider_unavailable: "Giriş hizmetine şu anda ulaşılamıyor. Lütfen biraz sonra tekrar deneyin.",
  invalid_authentication_response: "Giriş isteğinizin süresi dolmuş veya yanıt doğrulanamamış. Lütfen tekrar giriş yapın.",
  authentication_failed: "Giriş tamamlanamadı. Lütfen yeniden deneyin.",
  profile_unavailable: "Profil bilgileriniz şu anda yüklenemiyor. Lütfen biraz sonra tekrar giriş yapın.",
  session_unavailable: "Güvenli oturum oluşturulamadı. Sorun devam ederse uygulama yöneticisiyle iletişime geçin.",
};

function LoginContent() {
  const [loading, setLoading] = useState(false);
  const params = useSearchParams();
  const errorCode = params.get("error");
  const errorMessage = errorCode ? LOGIN_ERRORS[errorCode] ?? LOGIN_ERRORS.authentication_failed : null;
  const notice = params.get("session") === "expired"
    ? "Oturumunuz sona erdi. Devam etmek için tekrar giriş yapın."
    : params.get("logout") === "local"
      ? "Uygulama oturumunuz kapatıldı. Yeniden girişte kimlik doğrulaması istenecek."
      : null;

  const handleKeycloakLogin = () => {
    setLoading(true);
    window.location.assign("/api/auth/login");
  };

  return (
    <div className="min-h-screen flex flex-col items-center justify-center bg-slate-50 p-4">
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
            Güvenliğiniz için giriş işlemleri merkezi kimlik sistemimiz üzerinden yapılmaktadır.
          </CardDescription>
        </CardHeader>
        
        <CardContent className="pt-8 pb-8 bg-white flex flex-col gap-4">
            {errorMessage && <p role="alert" className="rounded-md border border-red-200 bg-red-50 p-3 text-sm text-red-700">{errorMessage}</p>}
            {!errorMessage && notice && <p role="status" className="rounded-md border border-blue-200 bg-blue-50 p-3 text-sm text-blue-800">{notice}</p>}
            <Button 
              onClick={handleKeycloakLogin}
              className="w-full h-14 text-lg font-bold bg-blue-600 hover:bg-blue-700 shadow-lg shadow-blue-200 transition-all"
              disabled={loading}
            >
              {loading ? <Loader2 className="w-6 h-6 animate-spin mr-2" /> : <ShieldCheck className="w-6 h-6 mr-2" />}
              {loading ? "Güvenli Alana Bağlanıyor..." : "Güvenli Giriş Yap"}
            </Button>
        </CardContent>
      </Card>
    </div>
  );
}

export default function LoginPage() {
  return (
    <Suspense fallback={<div className="min-h-screen grid place-items-center bg-slate-50">Giriş ekranı yükleniyor...</div>}>
      <LoginContent />
    </Suspense>
  );
}
