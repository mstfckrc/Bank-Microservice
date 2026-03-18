"use client";

import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Building, Lock, Save, Loader2 } from "lucide-react"; // 🚀 User yerine Building (Kurum) ikonu aldık
import { PageHeader } from "@/components/shared/PageHeader";
import { useSettings } from "@/hooks/useSettings";

export default function CompanySettingsPage() {
  // Bireysel ve Kurumsal sistem aynı "AppUser" altyapısını kullandığı için 
  // useSettings(false) motoru burada da kusursuz çalışacaktır.
  const { 
    user, 
    profileForm, 
    setProfileForm, 
    passForm, 
    setPassForm, 
    loadingProfile, 
    loadingPass, 
    handleUpdateProfile, 
    handleChangePassword 
  } = useSettings(false);

  return (
    <div className="max-w-4xl mx-auto space-y-8 p-6">
      <PageHeader 
        title="Kurumsal Hesap Ayarları" 
        description="Şirket bilgilerinizi, iletişim adreslerinizi ve hesap güvenliğinizi buradan yönetin." 
      />

      <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
        
        {/* FİRMA BİLGİLERİ KARTI */}
        <Card className="border-none shadow-lg bg-white overflow-hidden">
          <CardHeader className="flex flex-row items-center gap-2 bg-slate-50/50 border-b mb-4">
            <Building className="w-5 h-5 text-indigo-600" />
            <CardTitle className="text-lg font-bold">Firma Bilgileri</CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="space-y-2">
              <Label className="text-slate-500 text-xs uppercase tracking-wider">Vergi / Kimlik Numarası</Label>
              <Input 
                value={user?.identityNumber || ""} 
                disabled 
                className="bg-slate-50 text-slate-500 font-mono" 
              />
            </div>
            <div className="space-y-2">
              <Label>Şirket Ünvanı / Yetkili Adı</Label>
              <Input 
                value={profileForm.profileName} 
                onChange={(e) => setProfileForm({...profileForm, profileName: e.target.value})} 
                disabled={loadingProfile} 
              />
            </div>
            <div className="space-y-2">
              <Label>Kurumsal E-posta</Label>
              <Input 
                value={profileForm.email} 
                onChange={(e) => setProfileForm({...profileForm, email: e.target.value})} 
                disabled={loadingProfile} 
              />
            </div>
            <Button 
              onClick={handleUpdateProfile} 
              disabled={loadingProfile} 
              className="w-full bg-indigo-600 hover:bg-indigo-700 text-white transition-colors"
            >
              {loadingProfile ? <Loader2 className="animate-spin mr-2 w-4 h-4" /> : <Save className="w-4 h-4 mr-2" />}
              {loadingProfile ? "Kaydediliyor..." : "Değişiklikleri Kaydet"}
            </Button>
          </CardContent>
        </Card>

        {/* GÜVENLİK KARTI (Aynı Tasarım) */}
        <Card className="border-none shadow-lg bg-white overflow-hidden">
          <CardHeader className="flex flex-row items-center gap-2 bg-slate-50/50 border-b mb-4">
            <Lock className="w-5 h-5 text-orange-600" />
            <CardTitle className="text-lg font-bold">Güvenlik</CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="space-y-2">
              <Label>Mevcut Şifre</Label>
              <Input 
                type="password" 
                placeholder="••••••••" 
                value={passForm.oldPassword} 
                onChange={(e) => setPassForm({...passForm, oldPassword: e.target.value})} 
                disabled={loadingPass} 
              />
            </div>
            <div className="space-y-2">
              <Label>Yeni Şifre</Label>
              <Input 
                type="password" 
                placeholder="En az 6 karakter" 
                value={passForm.newPassword} 
                onChange={(e) => setPassForm({...passForm, newPassword: e.target.value})} 
                disabled={loadingPass} 
              />
            </div>
            <Button 
              onClick={handleChangePassword} 
              disabled={loadingPass} 
              variant="outline" 
              className="w-full border-slate-200 hover:bg-orange-50 hover:text-orange-700 transition-colors"
            >
               {loadingPass ? <Loader2 className="animate-spin mr-2 w-4 h-4" /> : null}
               {loadingPass ? "Güncelleniyor..." : "Şifreyi Güncelle"}
            </Button>
          </CardContent>
        </Card>
      </div>
    </div>
  );
}