"use client";

import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Trash2, Lock } from "lucide-react";
import { AccountResponse } from "@/types";
import { useAuthStore } from "@/store/useAuthStore";

interface AccountCardProps {
  acc: AccountResponse;
  isSelected: boolean;
  onSelect: (accountNo: string) => void;
  onCloseRequest: (e: React.MouseEvent, accountNo: string) => void;
}

export function AccountCard({ acc, isSelected, onSelect, onCloseRequest }: AccountCardProps) {
  const { user } = useAuthStore(); 
  
  // 🚀 YENİ: Sadece PENDING değil, ONAYLI OLMAYAN herkesi kısıtlıyoruz (PENDING ve REJECTED)
  const isRestricted = user?.status !== "APPROVED"; 

  const isAccountActive = (acc as any).isActive !== false && (acc as any).active !== false;

  let borderClasses = "";
  if (!isAccountActive) {
    borderClasses = "border-slate-200 bg-slate-100 opacity-50 grayscale cursor-not-allowed";
  } else {
    const colorStyles = {
      TRY: isSelected ? "border-red-500 shadow-md scale-[1.02] bg-red-50/10" : "border-red-100 hover:border-red-300 bg-white",
      USD: isSelected ? "border-green-500 shadow-md scale-[1.02] bg-green-50/10" : "border-green-100 hover:border-green-300 bg-white",
      EUR: isSelected ? "border-blue-500 shadow-md scale-[1.02] bg-blue-50/10" : "border-blue-100 hover:border-blue-300 bg-white",
    };
    borderClasses = colorStyles[acc.currency as keyof typeof colorStyles] || "bg-white";
  }

  return (
    <Card
      onClick={() => isAccountActive && onSelect(acc.accountNumber)}
      // 🚀 YENİ: isRestricted kontrolü eklendi
      className={`relative overflow-hidden transition-all border-2 ${borderClasses} ${isRestricted ? 'cursor-default' : 'cursor-pointer'}`}
    >
      <div className={`absolute left-0 top-0 bottom-0 w-1 ${
        !isAccountActive ? "bg-slate-400" : acc.currency === "TRY" ? "bg-red-500" : acc.currency === "USD" ? "bg-green-500" : "bg-blue-500"
      }`} />
      
      <CardHeader className="pb-2 flex flex-row justify-between items-start">
        <div>
          <CardTitle className={`text-lg font-bold ${!isAccountActive ? "text-slate-500 line-through decoration-slate-400" : "text-slate-900"}`}>
            Vadesiz {acc.currency}
          </CardTitle>
          <p className="text-[10px] font-mono text-slate-400 mt-1">{acc.iban}</p>
        </div>
        <div className="flex flex-col items-end gap-2">
          <span className={`px-2 py-0.5 rounded text-[10px] font-bold ${
            !isAccountActive ? "bg-slate-300 text-slate-600" : 
            (acc.currency === "TRY" ? "bg-red-50 text-red-700" : acc.currency === "USD" ? "bg-green-50 text-green-700" : "bg-blue-50 text-blue-700")
          }`}>
            {!isAccountActive ? "KAPATILDI" : acc.currency}
          </span>

          {/* 🚀 GÜNCELLEME: Kullanıcı kısıtlıysa (Onaylı değilse) çöp kutusu yerine kilit ikonu gösteriyoruz */}
          {isAccountActive && (
            isRestricted ? (
              <div title="Hesabınız onaylı olmadığı için işlem yapamazsınız" className="p-1 text-slate-300">
                <Lock className="w-4 h-4" />
              </div>
            ) : (
              <button
                onClick={(e) => onCloseRequest(e, acc.accountNumber)}
                className="text-slate-300 hover:text-red-500 transition-colors p-1 rounded-md hover:bg-red-50 z-10"
              >
                <Trash2 className="w-4 h-4" />
              </button>
            )
          )}
        </div>
      </CardHeader>
      <CardContent>
        <div className={`text-2xl font-bold ${!isAccountActive ? "text-slate-500" : "text-slate-900"}`}>
          {acc.balance.toLocaleString("tr-TR")} {acc.currency === "TRY" ? "₺" : acc.currency === "USD" ? "$" : "€"}
        </div>
        <p className="text-[10px] text-slate-400 mt-1 uppercase tracking-wider">No: {acc.accountNumber}</p>
      </CardContent>
    </Card>
  );
}