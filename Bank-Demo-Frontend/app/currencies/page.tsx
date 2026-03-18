// app/currencies/page.tsx
"use client";

import { useEffect, useState } from "react";
import { currencyService } from "@/services/currency.service";
import { Card } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { ArrowLeft, Search, Globe, TrendingUp } from "lucide-react";
import Link from "next/link";
import { useRouter } from "next/navigation";

export default function AllCurrenciesPage() {
  const [rates, setRates] = useState<Record<string, number> | null>(null);
  const [searchTerm, setSearchTerm] = useState("");
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchAll = async () => {
      try {
        const response = await currencyService.getRates("TRY");
        setRates(response.rates || response);
      } catch (e) {
        console.error(e);
      } finally {
        setLoading(false);
      }
    };
    fetchAll();
  }, []);

  const filteredRates = rates
    ? Object.entries(rates).filter(([key]) =>
        key.toLowerCase().includes(searchTerm.toLowerCase()),
      )
    : [];

  const router = useRouter();

  return (
    <div className="min-h-screen bg-slate-50 p-6 md:p-12">
      <div className="max-w-6xl mx-auto space-y-8">
        {/* Üst Kısım */}
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
          <div className="flex items-center gap-4">
            <Button
              variant="ghost"
              size="icon"
              className="rounded-full bg-white shadow-sm cursor-pointer hover:bg-slate-50 transition-colors"
              onClick={() => router.back()} // 👈 Seni bir önceki sayfaya fırlatır
            >
              <ArrowLeft className="w-5 h-5 text-slate-700" />
            </Button>
            <div>
              <h1 className="text-3xl font-black text-slate-900 tracking-tight">
                Canlı Piyasalar
              </h1>
              <p className="text-slate-500 font-medium text-sm text-center">
                Tüm dünya para birimlerinin TRY karşılığı
              </p>
            </div>
          </div>

          <div className="relative w-full md:w-64">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-400" />
            <Input
              placeholder="Döviz ara (USD, EUR...)"
              className="pl-10 border-none shadow-sm focus-visible:ring-blue-500"
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
            />
          </div>
        </div>

        {loading ? (
          <div className="grid grid-cols-2 md:grid-cols-4 lg:grid-cols-6 gap-4">
            {[1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12].map((i) => (
              <div
                key={i}
                className="h-24 bg-slate-200 animate-pulse rounded-2xl"
              />
            ))}
          </div>
        ) : (
          <div className="grid grid-cols-2 md:grid-cols-4 lg:grid-cols-6 gap-4">
            {filteredRates.map(([curr, rate]) => {
              const displayRate = rate < 1 ? 1 / rate : rate;
              return (
                <Card
                  key={curr}
                  className="border-none shadow-md hover:shadow-xl transition-all p-4 bg-white group"
                >
                  <div className="flex justify-between items-start mb-2">
                    <span className="text-[10px] font-black text-slate-400 tracking-widest">
                      {curr}
                    </span>
                    <TrendingUp className="w-3 h-3 text-green-500 opacity-0 group-hover:opacity-100 transition-opacity" />
                  </div>
                  <div className="text-xl font-black text-slate-900 leading-tight">
                    ₺{displayRate.toFixed(4)}
                  </div>
                </Card>
              );
            })}
          </div>
        )}
      </div>
    </div>
  );
}
