"use client";

import { useEffect, useState } from "react";
import { currencyService } from "@/services/currency.service";
import {
  compareCurrencies, formatRateTimestamp, formatTryRate, getCurrencyName,
  normalizeCurrencySearch, toTryRate,
} from "@/lib/currency";
import type { ExchangeRateResponse } from "@/types";
import { Card } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { ArrowLeft, LayoutGrid, List, RefreshCw, Search } from "lucide-react";
import { useRouter } from "next/navigation";

export default function AllCurrenciesPage() {
  const [rates, setRates] = useState<ExchangeRateResponse | null>(null);
  const [searchTerm, setSearchTerm] = useState("");
  const [loading, setLoading] = useState(true);
  const [view, setView] = useState<"grid" | "list">("grid");
  const [reload, setReload] = useState(0);
  const router = useRouter();

  useEffect(() => {
    let active = true;
    void currencyService.getRates("TRY")
      .then((response) => { if (active) setRates(response); })
      .catch(() => { if (active) setRates(null); })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [reload]);

  const currencies = Object.entries(rates?.rates ?? {})
    .filter(([code]) => code !== "TRY" && /^[A-Z]{3}$/.test(code))
    .map(([code, rate]) => ({ code, name: getCurrencyName(code), value: toTryRate(rate) }))
    .filter((item) => item.value !== null)
    .sort(compareCurrencies);
  const search = normalizeCurrencySearch(searchTerm);
  const filtered = currencies.filter(({ code, name }) =>
    normalizeCurrencySearch(code + " " + name).includes(search));
  const updatedAt = formatRateTimestamp(rates?.time_last_update_unix);
  const retry = () => {
    setLoading(true);
    setReload((value) => value + 1);
  };

  return (
    <div className="min-h-screen bg-slate-50 p-6 md:p-12">
      <div className="max-w-6xl mx-auto space-y-8">
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
          <div className="flex items-center gap-4">
            <Button
              variant="ghost"
              size="icon"
              aria-label="Önceki sayfaya dön"
              className="rounded-full bg-white shadow-sm cursor-pointer hover:bg-slate-50 transition-colors shrink-0"
              onClick={() => router.back()}
            >
              <ArrowLeft className="w-5 h-5 text-slate-700" />
            </Button>
            <div>
              <h1 className="text-3xl font-black text-slate-900 tracking-tight">Döviz Piyasaları</h1>
              <p className="text-slate-500 font-medium text-sm mt-1">
                1 birim dövizin Türk lirası karşılığı
              </p>
            </div>
          </div>

          <div className="flex items-center gap-3">
            <div className="relative flex-1 md:w-64">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-400" />
              <Input
                aria-label="Para birimi adı veya koduyla ara"
                placeholder="Döviz ara: dolar, USD..."
                className="pl-10 border-none shadow-sm focus-visible:ring-blue-500 bg-white"
                value={searchTerm}
                onChange={(event) => setSearchTerm(event.target.value)}
              />
            </div>
            <div role="group" aria-label="Kur görünümü" className="flex gap-1 rounded-xl bg-white p-1 shadow-sm">
              <Button
                variant={view === "grid" ? "secondary" : "ghost"}
                size="icon"
                aria-label="Kart görünümü"
                aria-pressed={view === "grid"}
                onClick={() => setView("grid")}
                className="rounded-lg"
              >
                <LayoutGrid className="w-4 h-4" />
              </Button>
              <Button
                variant={view === "list" ? "secondary" : "ghost"}
                size="icon"
                aria-label="Liste görünümü"
                aria-pressed={view === "list"}
                onClick={() => setView("list")}
                className="rounded-lg"
              >
                <List className="w-4 h-4" />
              </Button>
            </div>
          </div>
        </div>

        <div className="flex flex-wrap items-center justify-between gap-2 px-1 text-xs text-slate-500">
          <p aria-live="polite">{loading ? "Kurlar yükleniyor..." : filtered.length + " para birimi"} · Önce öne çıkanlar, sonra ada göre</p>
          {updatedAt && <p>Sağlayıcı güncellemesi: {updatedAt} (TSİ)</p>}
        </div>

        {loading ? (
          <div aria-label="Kurlar yükleniyor" className={view === "grid" ? "grid grid-cols-2 md:grid-cols-4 lg:grid-cols-6 gap-4" : "space-y-3"}>
            {Array.from({ length: 12 }, (_, index) => (
              <div key={index} className={(view === "grid" ? "h-36" : "h-20") + " bg-slate-200 animate-pulse motion-reduce:animate-none rounded-2xl"} />
            ))}
          </div>
        ) : !rates || currencies.length === 0 ? (
          <Card className="border-none bg-white p-10 text-center items-center gap-4 shadow-sm" role="alert">
            <p className="font-semibold text-slate-700">Kurlar şu anda yüklenemiyor.</p>
            <p className="text-sm text-slate-500">Lütfen kısa bir süre sonra yeniden deneyin.</p>
            <Button variant="outline" onClick={retry}><RefreshCw className="w-4 h-4 mr-2" />Yeniden dene</Button>
          </Card>
        ) : filtered.length === 0 ? (
          <Card className="border-none bg-white p-10 text-center items-center gap-4 shadow-sm">
            <Search className="w-6 h-6 text-slate-400" />
            <p className="font-semibold text-slate-700">Aramanıza uygun para birimi bulunamadı.</p>
            <Button variant="ghost" onClick={() => setSearchTerm("")}>Aramayı temizle</Button>
          </Card>
        ) : (
          <ul
            key={view}
            aria-label="Döviz kurları"
            className={view === "grid" ? "grid grid-cols-2 md:grid-cols-4 lg:grid-cols-6 gap-4" : "space-y-3"}
          >
            {filtered.map(({ code, name, value }, index) => (
              <li
                key={code}
                className="min-w-0 animate-in fade-in slide-in-from-bottom-1 duration-200 motion-reduce:animate-none"
                style={{ animationDelay: Math.min(index, 8) * 12 + "ms", animationFillMode: "both" }}
              >
                <Card className={"border-none shadow-md hover:shadow-xl transition-shadow p-4 bg-white group h-full " + (view === "grid" ? "gap-2" : "flex-row items-center justify-between gap-4")}>
                  <div className={view === "grid" ? "space-y-2" : "flex items-center gap-4 min-w-0"}>
                    <div className="flex justify-between items-center gap-2">
                      <span className={view === "grid" ? "text-[10px] font-black text-slate-400 tracking-widest" : "text-xs font-black text-blue-700 bg-blue-50 rounded-xl px-3 py-2.5 w-16 text-center shrink-0"}>
                        {code}
                      </span>
                    </div>
                    <p title={name} className={"font-medium text-slate-500 " + (view === "grid" ? "text-xs leading-5 min-h-10 line-clamp-2" : "text-sm truncate")}>
                      {name}
                    </p>
                  </div>
                  <div className={view === "grid" ? "mt-auto" : "text-right shrink-0"}>
                    <p className="text-lg font-black text-slate-900 leading-tight tabular-nums tracking-tight">{formatTryRate(value)}</p>
                  </div>
                </Card>
              </li>
            ))}
          </ul>
        )}

        <div className="text-xs text-slate-400 leading-relaxed px-1 space-y-1">
          <p>Veri kaynağı: <a href="https://www.exchangerate-api.com" target="_blank" rel="noopener noreferrer" className="underline underline-offset-2 hover:text-slate-600">Rates By Exchange Rate API</a></p>
        </div>
      </div>
    </div>
  );
}
