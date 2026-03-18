"use client";

import Link from "next/link";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Settings, ShieldAlert } from "lucide-react"; 
import { PageHeader } from "@/components/shared/PageHeader";

export default function AdminDashboardPage() {
  return (
    <div className="space-y-6">
      <PageHeader
        title="Sistem Özeti"
        description="Bankanın genel durumunu ve kurumsal/bireysel tüm işlemleri buradan yönetebilirsiniz."
        showBackButton={false}
      />

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        
        {/* Müşteri Yönetimi Kartı */}
        <Card className="hover:border-slate-300 transition-colors flex flex-col h-full">
          <CardHeader className="flex-1">
            <CardTitle>Müşteri ve Kurum Yönetimi</CardTitle>
            <CardDescription>
              Sistemdeki tüm bireysel ve kurumsal müşterileri görüntüle, düzenle veya sil.
            </CardDescription>
          </CardHeader>
          <CardContent>
            <Link href="/admin/customers">
              <Button className="w-full">Hesapları Listele</Button>
            </Link>
          </CardContent>
        </Card>

        {/* Hesap Yönetimi Kartı */}
        <Card className="hover:border-slate-300 transition-colors flex flex-col h-full">
          <CardHeader className="flex-1">
            <CardTitle>Banka Hesapları (Vadesiz)</CardTitle>
            <CardDescription>
              Açılmış olan tüm hesapları ve güncel bakiyelerini detaylı olarak incele.
            </CardDescription>
          </CardHeader>
          <CardContent>
            <Link href="/admin/accounts">
              <Button className="w-full" variant="secondary">
                Hesapları Görüntüle
              </Button>
            </Link>
          </CardContent>
        </Card>

        {/* Merkezi İşlem İzleme Kartı (God Mode) */}
        <Card className="hover:border-amber-300 border-amber-200 bg-amber-50/30 transition-colors flex flex-col h-full relative overflow-hidden">
          <div className="absolute top-0 right-0 -mt-4 -mr-4 w-24 h-24 bg-amber-500/10 rounded-full blur-2xl pointer-events-none"></div>
          
          <CardHeader className="flex-1">
            <div className="flex items-center gap-2 mb-1">
              <ShieldAlert className="w-5 h-5 text-amber-600" />
              <CardTitle className="text-amber-900">Merkezi İşlem İzleme</CardTitle>
            </div>
            <CardDescription className="text-amber-700/80">
              Tüm para akışını izleyin. MASAK kurallarına takılan şüpheli veya yüklü işlemleri onaylayın.
            </CardDescription>
          </CardHeader>
          <CardContent>
            <Link href="/admin/transactions">
              <Button className="w-full bg-amber-500 hover:bg-amber-600 text-white shadow-md">
                İşlemleri İncele
              </Button>
            </Link>
          </CardContent>
        </Card>

        {/* Profil ve Ayarlar Kartı */}
        <Card className="hover:border-slate-300 transition-colors flex flex-col h-full">
          <CardHeader className="flex-1">
            <div className="flex items-center gap-2 mb-1">
              <Settings className="w-5 h-5 text-slate-500" />
              <CardTitle>Profil ve Ayarlar</CardTitle>
            </div>
            <CardDescription>
              Yönetici hesap bilgilerinizi ve güvenlik (şifre) ayarlarınızı yönetin.
            </CardDescription>
          </CardHeader>
          <CardContent>
            <Link href="/admin/settings">
              <Button className="w-full" variant="outline">
                Ayarlara Git
              </Button>
            </Link>
          </CardContent>
        </Card>

        {/* İstatistik Kartı */}
        <Card className="bg-slate-900 text-white border-none flex flex-col h-full lg:col-span-2">
          <CardHeader className="flex-1">
            <CardTitle className="text-slate-100">Sistem Durumu (V2)</CardTitle>
            <CardDescription className="text-slate-400">
              Veritabanı, AppUser Mimarisi ve Güvenlik Duvarı Statüsü
            </CardDescription>
          </CardHeader>
          <CardContent>
            <div className="flex flex-wrap items-center gap-6">
              <div className="flex items-center gap-2 bg-slate-800 px-4 py-2 rounded-lg border border-slate-700">
                <div className="w-3 h-3 rounded-full bg-green-500 animate-pulse shadow-[0_0_10px_rgba(34,197,94,0.6)]"></div>
                <span className="font-medium text-slate-200 text-sm">
                  Ana Sistemler Aktif
                </span>
              </div>
              <div className="flex items-center gap-2 bg-slate-800 px-4 py-2 rounded-lg border border-slate-700">
                <div className="w-3 h-3 rounded-full bg-blue-500 shadow-[0_0_10px_rgba(59,130,246,0.6)]"></div>
                <span className="font-medium text-slate-200 text-sm">
                  MASAK Filtresi Devrede
                </span>
              </div>
            </div>
          </CardContent>
        </Card>

      </div>
    </div>
  );
}