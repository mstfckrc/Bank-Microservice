"use client";

import { useState } from "react";
import Link from "next/link";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { ShieldCheck, ArrowLeft, Loader2 } from "lucide-react";

export default function LoginPage() {
  const [loading, setLoading] = useState(false);

  const handleKeycloakLogin = () => {
    setLoading(true);
    
    // 1. Keycloak'un Yönlendirme (Authorization) Adresi
    const keycloakUrl = process.env.NEXT_PUBLIC_KEYCLOAK_URL;
    const realm = "bank-realm";
    const clientId = "bank-auth-client";
    // Şimdilik dönüş adresi olarak bunu veriyoruz, 3. adımda burayı karşılayacağız
    const redirectUri = encodeURIComponent("http://localhost:3000/api/auth/callback");

    // 2. Müşteriyi Nüfus Müdürlüğüne Fırlat!
    const authUrl = `${keycloakUrl}/realms/${realm}/protocol/openid-connect/auth?client_id=${clientId}&redirect_uri=${redirectUri}&response_type=code&scope=openid`;
    
    window.location.href = authUrl;
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
        
        <CardContent className="pt-8 pb-8 bg-white flex justify-center">
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