"use client";

import { useState, useRef } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import ReCAPTCHA from "react-google-recaptcha"; // 🚀 YENİ MÜHİMMAT
import { authService } from "@/services/auth.service";
import { Role } from "@/types";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from "@/components/ui/card";
import { AlertCircle, Loader2, ShieldCheck, User, Building2, ArrowLeft } from "lucide-react";
import { toast } from "sonner";

export default function RegisterPage() {
  const router = useRouter();
  const recaptchaRef = useRef<ReCAPTCHA>(null); // 🚀 CAPTCHA REFERANSI
  
  const [accountType, setAccountType] = useState<"RETAIL" | "CORPORATE">("RETAIL");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [captchaToken, setCaptchaToken] = useState<string | null>(null); // 🚀 CAPTCHA BİLETİ

  const [formData, setFormData] = useState({
    identityNumber: "",
    email: "",
    password: "",
    firstName: "",
    lastName: "",
    companyName: "",
    taxOffice: "",
  });

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setFormData({ ...formData, [e.target.name]: e.target.value });
  };

  const onCaptchaChange = (token: string | null) => {
    setCaptchaToken(token);
    if (token) setError(""); // Token alındıysa robot hatasını sil
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError("");

    // 🚀 SİBER KALKAN KONTROLÜ: Robot mu İnsan mı?
    if (!captchaToken) {
      setError("Güvenlik ihlali: Lütfen robot olmadığınızı doğrulayın!");
      return;
    }

    setLoading(true);

    try {
      const role: Role = accountType === "RETAIL" ? "RETAIL_CUSTOMER" : "CORPORATE_MANAGER";
      
      const payload = {
        identityNumber: formData.identityNumber,
        email: formData.email,
        password: formData.password,
        role,
        captchaToken, // 🚀 Karargaha (Backend) iletilecek güvenlik bileti
        ...(accountType === "RETAIL" 
            ? { firstName: formData.firstName, lastName: formData.lastName } 
            : { companyName: formData.companyName, taxOffice: formData.taxOffice }
        )
      };

      await authService.register(payload);
      
      toast.success("Kayıt Başarılı!", { description: "Hesabınız oluşturuldu. Lütfen giriş yapın." });
      router.push("/login");
      
    } catch (err: any) {
      setError(err.response?.data?.message || "Kayıt işlemi başarısız oldu. Bilgilerinizi kontrol edin.");
      // Hata alırsak CAPTCHA'yı sıfırla ki adam tekrar işaretlesin
      recaptchaRef.current?.reset();
      setCaptchaToken(null);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex flex-col items-center justify-center bg-slate-50 p-4 py-12">
      <div className="w-full max-w-lg mb-4">
        <Link href="/" className="inline-flex items-center text-sm font-semibold text-slate-500 hover:text-blue-600 transition-colors">
          <ArrowLeft className="w-4 h-4 mr-2" />
          Ana Menüye Dön
        </Link>
      </div>

      <Card className="w-full max-w-lg border-none shadow-2xl">
        <CardHeader className="space-y-3 pb-6 border-b border-slate-100 bg-white rounded-t-xl">
          <div className="flex justify-center mb-2">
            <div className="p-3 bg-blue-50 rounded-full">
              <ShieldCheck className="w-8 h-8 text-blue-600" />
            </div>
          </div>
          <CardTitle className="text-2xl font-black text-center text-slate-900">Aramıza Katılın</CardTitle>
          <CardDescription className="text-center font-medium">
            Yeni nesil dijital bankacılık deneyimi için hesap oluşturun
          </CardDescription>
        </CardHeader>
        
        <CardContent className="pt-6 bg-white">
          <div className="grid grid-cols-2 gap-2 bg-slate-100 p-1 rounded-lg mb-6">
            <Button
              type="button"
              variant="ghost"
              className={`h-10 transition-all ${accountType === "RETAIL" ? "bg-white text-slate-900 shadow-sm hover:bg-white hover:text-slate-900" : "text-slate-500 hover:text-slate-700 hover:bg-slate-200/50"}`}
              onClick={() => {
                setAccountType("RETAIL");
                setError("");
              }}
            >
              <User className="w-4 h-4 mr-2" />
              Bireysel
            </Button>
            <Button
              type="button"
              variant="ghost"
              className={`h-10 transition-all ${accountType === "CORPORATE" ? "bg-white text-slate-900 shadow-sm hover:bg-white hover:text-slate-900" : "text-slate-500 hover:text-slate-700 hover:bg-slate-200/50"}`}
              onClick={() => {
                setAccountType("CORPORATE");
                setError("");
              }}
            >
              <Building2 className="w-4 h-4 mr-2" />
              Kurumsal
            </Button>
          </div>

          <form onSubmit={handleSubmit} className="space-y-5">
            {error && (
              <div className="p-3 bg-red-50 border border-red-100 rounded-lg flex items-start gap-3 text-red-600 text-sm font-medium animate-in fade-in zoom-in duration-300">
                <AlertCircle className="w-5 h-5 shrink-0 mt-0.5" />
                <p>{error}</p>
              </div>
            )}

            {accountType === "RETAIL" && (
              <>
                <div className="grid grid-cols-2 gap-4">
                  <div className="space-y-2">
                    <Label htmlFor="firstName" className="text-slate-700 font-bold">Ad</Label>
                    <Input id="firstName" name="firstName" placeholder="Örn: Ahmet" value={formData.firstName} onChange={handleInputChange} required disabled={loading} className="bg-slate-50" />
                  </div>
                  <div className="space-y-2">
                    <Label htmlFor="lastName" className="text-slate-700 font-bold">Soyad</Label>
                    <Input id="lastName" name="lastName" placeholder="Örn: Yılmaz" value={formData.lastName} onChange={handleInputChange} required disabled={loading} className="bg-slate-50" />
                  </div>
                </div>
                <div className="space-y-2">
                  <Label htmlFor="identityNumber" className="text-slate-700 font-bold">TC Kimlik Numarası</Label>
                  <Input id="identityNumber" name="identityNumber" maxLength={11} placeholder="11 Haneli TC Kimlik No" value={formData.identityNumber} onChange={handleInputChange} required disabled={loading} className="bg-slate-50 font-mono" />
                </div>
              </>
            )}

            {accountType === "CORPORATE" && (
              <>
                <div className="space-y-2">
                  <Label htmlFor="companyName" className="text-slate-700 font-bold">Şirket Ünvanı</Label>
                  <Input id="companyName" name="companyName" placeholder="Tam şirket ünvanı" value={formData.companyName} onChange={handleInputChange} required disabled={loading} className="bg-slate-50" />
                </div>
                <div className="grid grid-cols-2 gap-4">
                  <div className="space-y-2">
                    <Label htmlFor="identityNumber" className="text-slate-700 font-bold">Vergi Numarası</Label>
                    <Input id="identityNumber" name="identityNumber" maxLength={10} placeholder="10 Haneli VKN" value={formData.identityNumber} onChange={handleInputChange} required disabled={loading} className="bg-slate-50 font-mono" />
                  </div>
                  <div className="space-y-2">
                    <Label htmlFor="taxOffice" className="text-slate-700 font-bold">Vergi Dairesi</Label>
                    <Input id="taxOffice" name="taxOffice" placeholder="Bağlı olunan VD" value={formData.taxOffice} onChange={handleInputChange} required disabled={loading} className="bg-slate-50" />
                  </div>
                </div>
              </>
            )}

            <div className="space-y-2">
              <Label htmlFor="email" className="text-slate-700 font-bold">E-posta Adresi</Label>
              <Input id="email" name="email" type="email" placeholder="ornek@mail.com" value={formData.email} onChange={handleInputChange} required disabled={loading} className="bg-slate-50" />
            </div>
            
            <div className="space-y-2">
              <Label htmlFor="password" className="text-slate-700 font-bold">Dijital Şifre</Label>
              <Input id="password" name="password" type="password" placeholder="En az 6 karakter" value={formData.password} onChange={handleInputChange} required disabled={loading} className="bg-slate-50 font-mono" />
            </div>

            {/* 🚀 GOOGLE RECAPTCHA KUTUCUĞU EKLENDİ */}
            <div className="flex justify-center pt-2">
              <ReCAPTCHA
                ref={recaptchaRef}
                sitekey={process.env.NEXT_PUBLIC_RECAPTCHA_SITE_KEY || "6LeIxAcTAAAAAJcZVRqyHh71UMIEGNQ_MXjiZKhI"} 
                onChange={onCaptchaChange}
                hl="tr"
              />
            </div>
            
            <Button 
              type="submit" 
              className="w-full h-12 text-base font-bold bg-blue-600 hover:bg-blue-700 shadow-lg shadow-blue-200 transition-all mt-4"
              disabled={loading}
            >
              {loading ? <Loader2 className="w-5 h-5 animate-spin mr-2" /> : null}
              {loading ? "Hesap Oluşturuluyor..." : "Hesabı Oluştur"}
            </Button>
          </form>
        </CardContent>
        
        <CardFooter className="flex justify-center bg-slate-50/50 rounded-b-xl py-6 border-t border-slate-100">
          <p className="text-sm text-slate-500 font-medium">
            Zaten bir hesabınız var mı?{" "}
            <Link href="/login" className="text-blue-600 font-bold hover:underline underline-offset-4">
              Giriş Yapın
            </Link>
          </p>
        </CardFooter>
      </Card>
    </div>
  );
}