import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Wallet, Loader2 } from "lucide-react";
import { AccountResponse } from "@/types";

interface BalanceCardProps {
  accounts: AccountResponse[];
  loading: boolean;
}

export function BalanceCard({ accounts, loading }: BalanceCardProps) {
  // Sadece aktif olanları toplama dahil et
  const activeAccounts = accounts.filter(acc => (acc as any).isActive !== false && (acc as any).active !== false);
  
  const totalTRY = activeAccounts.filter((acc) => acc.currency === "TRY").reduce((sum, acc) => sum + acc.balance, 0);
  const totalUSD = activeAccounts.filter((acc) => acc.currency === "USD").reduce((sum, acc) => sum + acc.balance, 0);
  const totalEUR = activeAccounts.filter((acc) => acc.currency === "EUR").reduce((sum, acc) => sum + acc.balance, 0);

  return (
    <Card className="bg-slate-900 text-white border-none shadow-lg flex flex-col justify-between h-full">
      <CardHeader className="pb-0 pt-5">
        <CardTitle className="text-sm font-medium text-slate-400 flex items-center gap-2">
          <Wallet className="w-4 h-4" /> Varlık Özeti
        </CardTitle>
      </CardHeader>
      <CardContent className="pb-5 pt-3">
        {loading ? (
          <Loader2 className="h-6 w-6 animate-spin mt-2" />
        ) : (
          <div className="space-y-4 mt-2">
            <div className="flex items-baseline gap-1">
              <span className="text-3xl font-bold text-white">
                ₺{totalTRY.toLocaleString("tr-TR")}
              </span>
            </div>
            <div className="flex gap-8 border-t border-slate-700/50 pt-3">
              <div className="flex flex-col">
                <span className="text-[10px] text-slate-400 uppercase tracking-widest mb-0.5">USD</span>
                <span className="text-sm font-bold text-green-400">${totalUSD.toLocaleString("en-US")}</span>
              </div>
              <div className="flex flex-col">
                <span className="text-[10px] text-slate-400 uppercase tracking-widest mb-0.5">EUR</span>
                <span className="text-sm font-bold text-blue-400">€{totalEUR.toLocaleString("de-DE")}</span>
              </div>
            </div>
          </div>
        )}
      </CardContent>
    </Card>
  );
}