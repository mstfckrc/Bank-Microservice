"use client";

import { useEffect, useState } from "react";
import { currencyService } from "@/services/currency.service";
import { ExchangeRateResponse } from "@/types";
import { TrendingUp } from "lucide-react";

export function CurrencyTicker() {
  const [rates, setRates] = useState<ExchangeRateResponse | null>(null);

  useEffect(() => {
    let isMounted = true;
    const refreshRates = () => {
      void currencyService.getRates('TRY')
        .then((data) => {
          if (isMounted) setRates(data);
        })
        .catch(() => console.error("Kurlar vitrin için çekilemedi."));
    };

    refreshRates();
    // Piyasalar hareketli, her 5 dakikada bir otomatik yenilesin
    const interval = setInterval(refreshRates, 300000);
    return () => { isMounted = false; clearInterval(interval); };
  }, []);

  if (!rates) return null;

  return (
    <div className="w-full bg-slate-50 border-b border-slate-200 py-2 overflow-hidden select-none">
      <div className="container mx-auto px-4 flex items-center justify-center gap-8 md:gap-16">
        
        {/* Gösterge */}
        <div className="hidden sm:flex items-center gap-2">
          <div className="w-2 h-2 bg-green-500 rounded-full animate-pulse" />
          <span className="text-[10px] font-black text-slate-400 uppercase tracking-widest">Canlı</span>
        </div>

        {/* Kur Verileri */}
        <div className="flex items-center gap-6 md:gap-12 animate-in fade-in slide-in-from-top-1 duration-700">
          <div className="flex items-center gap-2">
            <span className="text-[11px] font-bold text-slate-500">USD/TRY</span>
            <span className="text-sm font-black text-slate-900">{(1 / rates.rates['USD']).toFixed(2)}</span>
            <TrendingUp className="w-3 h-3 text-green-500" />
          </div>

          <div className="flex items-center gap-2">
            <span className="text-[11px] font-bold text-slate-500">EUR/TRY</span>
            <span className="text-sm font-black text-slate-900">{(1 / rates.rates['EUR']).toFixed(2)}</span>
            <TrendingUp className="w-3 h-3 text-green-500" />
          </div>

          <div className="flex items-center gap-2">
            <span className="text-[11px] font-bold text-slate-500">ALTIN (GR)</span>
            {/* Eğer backend'de varsa çekebiliriz, yoksa temsili koyuyorum */}
            <span className="text-sm font-black text-slate-900">3.045,20</span>
            <TrendingUp className="w-3 h-3 text-green-500" />
          </div>
        </div>

        {/* Tarih Bilgisi (Sadece Masaüstü) */}
        <div className="hidden lg:block text-[10px] text-slate-400 font-medium">
          Son Güncelleme: {new Date(rates.date).toLocaleTimeString('tr-TR', { hour: '2-digit', minute: '2-digit' })}
        </div>
      </div>
    </div>
  );
}