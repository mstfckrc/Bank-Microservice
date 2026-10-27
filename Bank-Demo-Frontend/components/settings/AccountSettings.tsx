"use client";

import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Building, ExternalLink, Lock, Save, Loader2, User } from "lucide-react";
import { PageHeader } from "@/components/shared/PageHeader";
import { useSettings } from "@/hooks/useSettings";

export function AccountSettings({ title, description }: { title: string; description: string }) {
  const {
    user, profileForm, setProfileForm, isCorporate, profileEditable, fetchingProfile,
    loadingProfile, loadFailed, canSave, handleUpdateProfile, reloadProfile,
  } = useSettings();
  const disabled = fetchingProfile || loadingProfile || !profileEditable || loadFailed;
  const ProfileIcon = isCorporate ? Building : User;

  return (
    <div className="max-w-4xl mx-auto space-y-8 p-6">
      <PageHeader title={title} description={description} />
      <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
        <Card className="border-none shadow-lg bg-white overflow-hidden">
          <CardHeader className="flex flex-row items-center gap-2 bg-slate-50/50 border-b mb-4">
            <ProfileIcon className={"w-5 h-5 " + (isCorporate ? "text-indigo-600" : "text-blue-600")} />
            <CardTitle className="text-lg font-bold">{isCorporate ? "Firma Bilgileri" : "Profil Bilgileri"}</CardTitle>
          </CardHeader>
          <CardContent>
            {loadFailed ? (
              <div role="alert" className="space-y-4 text-sm text-slate-600">
                <p>Profil bilgileri yüklenemedi.</p>
                <Button variant="outline" onClick={reloadProfile} disabled={fetchingProfile}>
                  {fetchingProfile && <Loader2 className="w-4 h-4 animate-spin" />}Yeniden dene
                </Button>
              </div>
            ) : (
              <form onSubmit={handleUpdateProfile} className="space-y-4" aria-busy={fetchingProfile || loadingProfile}>
                {fetchingProfile && <p role="status" className="text-sm text-slate-500">Profil yükleniyor...</p>}
                <div className="space-y-2">
                  <Label htmlFor="profile-identity" className="text-slate-500 text-xs uppercase tracking-wider">
                    {isCorporate ? "Vergi Numarası" : "T.C. Kimlik Numarası"}
                  </Label>
                  <Input id="profile-identity" value={user?.identityNumber ?? ""} disabled className="bg-slate-50 text-slate-500 font-mono" />
                </div>

                {isCorporate ? (
                  <div className="space-y-2">
                    <Label htmlFor="profile-company">Şirket Unvanı</Label>
                    <Input id="profile-company" value={profileForm.profileName} minLength={3} maxLength={150} required
                      onChange={(event) => setProfileForm({ ...profileForm, profileName: event.target.value })} disabled={disabled} />
                  </div>
                ) : profileEditable ? (
                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                    <div className="space-y-2">
                      <Label htmlFor="profile-first-name">Ad</Label>
                      <Input id="profile-first-name" autoComplete="given-name" value={profileForm.firstName} maxLength={100} required
                        onChange={(event) => setProfileForm({ ...profileForm, firstName: event.target.value })} disabled={disabled} />
                    </div>
                    <div className="space-y-2">
                      <Label htmlFor="profile-last-name">Soyad</Label>
                      <Input id="profile-last-name" autoComplete="family-name" value={profileForm.lastName} maxLength={100} required
                        onChange={(event) => setProfileForm({ ...profileForm, lastName: event.target.value })} disabled={disabled} />
                    </div>
                  </div>
                ) : (
                  <div className="space-y-2">
                    <Label htmlFor="profile-name">Ad Soyad</Label>
                    <Input id="profile-name" value={profileForm.profileName} disabled className="bg-slate-50" />
                  </div>
                )}

                <div className="space-y-2">
                  <Label htmlFor="profile-email">{isCorporate ? "Kurumsal İletişim E-postası" : "İletişim E-postası"}</Label>
                  <Input id="profile-email" type="email" autoComplete="email" value={profileForm.email} maxLength={100} required
                    onChange={(event) => setProfileForm({ ...profileForm, email: event.target.value })} disabled={disabled} />
                </div>

                {!fetchingProfile && !profileEditable ? (
                  <div className="space-y-3">
                    <p className="text-xs text-slate-500 leading-relaxed">Bu yönetici profili merkezi kimlik sisteminde tutuluyor. Değişikliklerden sonra uygulamaya yeniden giriş yapın.</p>
                    <Button asChild variant="outline" className="w-full">
                      <a href="/api/auth/account" target="_blank" rel="noopener noreferrer">Merkezi profili yönet<ExternalLink className="w-4 h-4" /></a>
                    </Button>
                  </div>
                ) : (
                  <>
                    <p className="text-xs text-slate-500 leading-relaxed">Buradaki bilgiler bankacılık profilinize aittir; giriş kimliğiniz değişmez.</p>
                    <Button type="submit" disabled={!canSave} className={"w-full " + (isCorporate ? "bg-indigo-600 hover:bg-indigo-700" : "bg-blue-600 hover:bg-blue-700")}>
                      {loadingProfile ? <Loader2 className="animate-spin w-4 h-4" /> : <Save className="w-4 h-4" />}
                      {loadingProfile ? "Kaydediliyor..." : "Değişiklikleri Kaydet"}
                    </Button>
                  </>
                )}
              </form>
            )}
          </CardContent>
        </Card>

        <Card className="border-none shadow-lg bg-white overflow-hidden">
          <CardHeader className="flex flex-row items-center gap-2 bg-slate-50/50 border-b mb-4">
            <Lock className="w-5 h-5 text-orange-600" />
            <CardTitle className="text-lg font-bold">Güvenlik</CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="rounded-xl bg-orange-50/60 p-4 text-sm text-slate-600 leading-relaxed space-y-2">
              <p className="font-semibold text-slate-800">Şifrenizi güvenli hesap ekranından yönetin.</p>
              <p>Açılan Keycloak hesap ekranının güvenlik bölümünden şifrenizi değiştirebilirsiniz. Yeni şifreniz merkezi parola kurallarına göre kontrol edilir.</p>
            </div>
            <Button asChild variant="outline" className="w-full border-slate-200 hover:bg-orange-50 hover:text-orange-700">
              <a href="/api/auth/account?view=security" target="_blank" rel="noopener noreferrer">Şifreyi Keycloak’ta yönet<ExternalLink className="w-4 h-4" /></a>
            </Button>
            <p className="text-xs text-slate-400">Yeni sekmede açılır; işiniz bitince uygulamaya dönebilirsiniz.</p>
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
