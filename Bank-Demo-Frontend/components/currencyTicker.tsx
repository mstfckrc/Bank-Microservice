"use client";

import { useEffect, useState } from "react";
import { currencyService } from "@/services/currency.service";
import type { ExchangeRateResponse } from "@/types";
import { FEATURED_CURRENCIES, formatRateTimestamp, formatTryRate, getCurrencyName, toTryRate } from "@/lib/currency";

export function CurrencyTicker() {
  const [rates, setRates] = useState<ExchangeRateResponse | null>(null);
  const [refreshFailed, setRefreshFailed] = useState(false);

  useEffect(() => {
    let active = true;
    const refreshRates = () => {
      void currencyService.getRates("TRY")
        .then((data) => {
          if (active) {
            setRates(data);
            setRefreshFailed(false);
          }
        })
        .catch(() => { if (active) setRefreshFailed(true); });
    };
    refreshRates();
    const interval = setInterval(refreshRates, 300000);
    return () => { active = false; clearInterval(interval); };
  }, []);

  if (!rates) return null;
  const updatedAt = formatRateTimestamp(rates.time_last_update_unix);

  return (
    <div className="w-full bg-slate-50 border-b border-slate-200 py-2 select-none">
      <div className="container mx-auto px-4 flex flex-wrap items-center justify-center gap-x-8 gap-y-2 md:gap-x-12">
        <a
          href="https://www.exchangerate-api.com"
          target="_blank"
          rel="noopener noreferrer"
          title="Veri kaynağı: ExchangeRate-API"
          className="flex items-center gap-2 text-[10px] font-black text-slate-400 tracking-wide hover:text-slate-600"
        >
          <span className="w-2 h-2 bg-blue-500 rounded-full" />
          Rates By Exchange Rate API
        </a>

        <div className="flex flex-wrap justify-center items-center gap-x-6 gap-y-1 md:gap-x-10">
          {FEATURED_CURRENCIES.map((code) => (
            <div key={code} title={getCurrencyName(code)} className="flex items-center gap-2">
              <span className="text-[11px] font-bold text-slate-500">{code}/TRY</span>
              <span className="text-sm font-black text-slate-900 tabular-nums">{formatTryRate(toTryRate(rates.rates[code]))}</span>
            </div>
          ))}
        </div>

        {refreshFailed ? (
          <span role="status" className="text-[10px] text-amber-700">Güncelleme alınamadı; son veri gösteriliyor.</span>
        ) : updatedAt && (
          <span className="hidden lg:block text-[10px] text-slate-400 font-medium">
            Sağlayıcı: {updatedAt} (TSİ)
          </span>
        )}
      </div>
    </div>
  );
}
