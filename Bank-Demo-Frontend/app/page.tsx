// app/page.tsx
"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { currencyService } from "../services/currency.service";
import {
  Card,
  CardContent,
  CardHeader,
  CardTitle,
} from "../components/ui/card";
import { Button } from "../components/ui/button";
import { TrendingUp, ArrowRight, Globe, ShieldCheck, Zap } from "lucide-react";

export default function LandingPage() {
  const [rates, setRates] = useState<Record<string, number> | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchRates = async () => {
      try {
        const response = await currencyService.getRates("TRY");
        // Backend'den direkt rates objesi geliyorsa (response.rates)
        setRates(response.rates || response);
      } catch (error) {
        console.error("Kurlar çekilirken hata oluştu:", error);
      } finally {
        setLoading(false);
      }
    };
    fetchRates();
  }, []);

  return (
    <div className="min-h-screen bg-slate-50 flex flex-col items-center p-6 pt-20">
      {/* 1. KAHRAMAN (HERO) BÖLÜMÜ */}
      <div className="text-center max-w-3xl mb-16">
        <div className="inline-flex items-center gap-2 bg-blue-50 text-blue-700 px-4 py-1.5 rounded-full text-xs font-black tracking-widest uppercase mb-6">
          <Zap className="w-3 h-3" /> Yeni Nesil Bankacılık
        </div>
        <h1 className="text-6xl font-black text-slate-900 tracking-tighter mb-6 leading-[0.9]">
          Geleceğin Finans <br /> Dünyasına Hoş Geldin
        </h1>
        <p className="text-lg text-slate-500 mb-10 max-w-xl mx-auto font-medium">
          Sec-Demo Bank ile hesaplarını shadow-only modern arayüzle yönet,
          saniyeler içinde para gönder ve piyasayı anlık takip et.
        </p>
        <div className="flex flex-col sm:flex-row gap-4 justify-center">
          <Link href="/login">
            <Button
              size="lg"
              className="font-bold text-md px-10 py-7 bg-slate-900 hover:bg-slate-800 shadow-xl hover:shadow-2xl transition-all group"
            >
              Müşteri Girişi{" "}
              <ArrowRight className="ml-2 w-4 h-4 group-hover:translate-x-1 transition-transform" />
            </Button>
          </Link>
          <Link href="/register">
            <Button
              size="lg"
              variant="outline"
              className="font-bold text-md px-10 py-7 border-none shadow-md hover:shadow-lg bg-white transition-all"
            >
              Hemen Kayıt Ol
            </Button>
          </Link>
        </div>
      </div>

      {/* 2. CANLI KURLAR (VİTRİN) BÖLÜMÜ */}
      <div className="w-full max-w-5xl">
        <div className="flex items-center justify-between mb-8 px-2">
          <div className="flex items-center gap-2">
            <Globe className="w-5 h-5 text-blue-600" />
            <h2 className="text-2xl font-black text-slate-900 tracking-tight">
              Canlı Piyasalar
            </h2>
            <Link href="/currencies">
              <Button
                variant="ghost"
                className="text-blue-600 font-bold hover:bg-blue-50"
              >
                Tümünü Gör <ArrowRight className="ml-2 w-4 h-4" />
              </Button>
            </Link>
          </div>
          <div className="flex items-center gap-2">
            <div className="w-2 h-2 bg-green-500 rounded-full animate-pulse" />
            <span className="text-[10px] font-black text-slate-400 uppercase tracking-widest leading-none">
              Anlık Veri
            </span>
          </div>
        </div>

        {loading ? (
          <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
            {[1, 2, 3].map((i) => (
              <div
                key={i}
                className="h-32 bg-slate-200/50 animate-pulse rounded-2xl"
              />
            ))}
          </div>
        ) : rates ? (
          <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
            {["USD", "EUR", "GBP"].map((currency) => {
              // Backend'den 0.03 gibi geliyorsa 1/0.03 yapıp 32.50 buluyoruz
              const rawRate = rates[currency];
              const displayRate = rawRate < 1 ? 1 / rawRate : rawRate;

              return (
                <Card
                  key={currency}
                  className="border-none shadow-md hover:shadow-xl transition-all duration-300 bg-white group cursor-default"
                >
                  <CardHeader className="pb-2 flex flex-row justify-between items-center">
                    <CardTitle className="text-slate-400 text-[10px] font-black uppercase tracking-[0.2em]">
                      {currency} / TRY
                    </CardTitle>
                    <TrendingUp className="w-4 h-4 text-green-500 opacity-0 group-hover:opacity-100 transition-opacity" />
                  </CardHeader>
                  <CardContent>
                    <div className="text-4xl font-black text-slate-900 tracking-tighter">
                      ₺{displayRate.toFixed(2)}
                    </div>
                    <div className="mt-4 h-1 w-full bg-slate-50 rounded-full overflow-hidden">
                      <div className="h-full bg-green-500 w-2/3 rounded-full" />
                    </div>
                  </CardContent>
                </Card>
              );
            })}
          </div>
        ) : (
          <div className="text-center text-red-500 font-bold bg-white shadow-sm p-8 rounded-2xl">
            Kurlar şu an çekilemiyor.
          </div>
        )}
      </div>

      {/* 3. KISA ÖZELLİKLER (OPSİYONEL - GÖRSELLİĞİ ARTIRIR) */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-12 mt-24 w-full max-w-5xl">
        <div className="flex flex-col items-center text-center space-y-3">
          <div className="p-4 bg-white shadow-md rounded-2xl">
            <ShieldCheck className="w-6 h-6 text-blue-600" />
          </div>
          <h3 className="font-bold text-slate-900">Güvenli Altyapı</h3>
          <p className="text-sm text-slate-500 font-medium">
            Spring Security ve JWT ile tüm varlıklarınız koruma altında.
          </p>
        </div>
        <div className="flex flex-col items-center text-center space-y-3">
          <div className="p-4 bg-white shadow-md rounded-2xl">
            <Zap className="w-6 h-6 text-orange-500" />
          </div>
          <h3 className="font-bold text-slate-900">Işık Hızında Transfer</h3>
          <p className="text-sm text-slate-500 font-medium">
            IBAN ile anında para gönderin, açıklamayı siz belirleyin.
          </p>
        </div>
        <div className="flex flex-col items-center text-center space-y-3">
          <div className="p-4 bg-white shadow-md rounded-2xl">
            <Globe className="w-6 h-6 text-green-500" />
          </div>
          <h3 className="font-bold text-slate-900">7/24 Döviz İşlemi</h3>
          <p className="text-sm text-slate-500 font-medium">
            Piyasaları anlık takip edin, fırsatları kaçırmayın.
          </p>
        </div>
      </div>
    </div>
  );
}
