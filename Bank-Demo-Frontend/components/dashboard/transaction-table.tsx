// components/dashboard/transaction-table.tsx
import { TransactionResponse } from "@/types";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import {
  ArrowDownRight,
  ArrowUpRight,
  Clock,
  AlertCircle,
  CheckCircle2,
  XCircle,
} from "lucide-react";

interface TransactionTableProps {
  transactions: TransactionResponse[];
  loading: boolean;
  currentAccountId?: number;
}

export function TransactionTable({
  transactions,
  loading,
  currentAccountId,
}: TransactionTableProps) {
  if (loading) {
    return (
      <div className="flex flex-col items-center justify-center py-12 text-slate-400">
        <Clock className="w-8 h-8 animate-spin mb-4 text-blue-500" />
        <p className="font-medium animate-pulse">İşlem geçmişi yükleniyor...</p>
      </div>
    );
  }

  if (!transactions || transactions.length === 0) {
    return (
      <div className="flex flex-col items-center justify-center py-12 text-slate-400">
        <Clock className="w-12 h-12 mb-4 opacity-20" />
        <p className="font-medium">Henüz bir hesap hareketi bulunmuyor.</p>
        <p className="text-xs mt-1">
          Yapılan tüm transferler burada listelenecektir.
        </p>
      </div>
    );
  }

  return (
    <Table>
      <TableHeader className="bg-slate-50">
        <TableRow>
          <TableHead className="font-bold text-slate-700">Tarih</TableHead>
          <TableHead className="font-bold text-slate-700">İşlem</TableHead>
          <TableHead className="font-bold text-slate-700 w-1/3">
            Açıklama
          </TableHead>
          <TableHead className="font-bold text-slate-700 text-center">
            Durum
          </TableHead>
          <TableHead className="text-right font-bold text-slate-700">
            Tutar
          </TableHead>
        </TableRow>
      </TableHeader>
      <TableBody>
        {transactions.map((txn, index) => {
          // 1. PARA GİRİYOR MU, ÇIKIYOR MU? (Kırmızı / Yeşil Mantığı)
          const isPositive =
            txn.receiverAccountId === currentAccountId ||
            txn.transactionType === "DEPOSIT";

          // 2. EKRANA HANGİ TUTARI BASACAĞIZ?
          const finalAmount =
            isPositive && txn.convertedAmount
              ? txn.convertedAmount
              : txn.amount;
          const displayAmount = finalAmount.toLocaleString("tr-TR", {
            minimumFractionDigits: 2,
            maximumFractionDigits: 2,
          });

          // 3. STATÜ KONTROLÜ (Eski işlemler veya null gelenler için varsayılan COMPLETED kabul edilir)
          const status = txn.status || "COMPLETED";

          // 4. İADE EDİLEN İŞLEMLER İÇİN UX MANTIĞI
          // Eğer işlem reddedildiyse, aslında para hesaba geri dönmüştür ama tabloda eksi olarak görünür (çünkü transferdir).
          // Kullanıcıyı korkutmamak için reddedilen işlemlerin rengini grileştiriyoruz.
          const isRejected = status === "REJECTED";
          const isPending = status === "PENDING_APPROVAL";

          return (
            <TableRow
              key={`${txn.referenceNo}-${index}`}
              className={`hover:bg-slate-50/50 transition-colors ${isRejected ? "opacity-70" : ""}`}
            >
              <TableCell className="text-xs text-slate-500 font-medium">
                <div>
                  {new Date(txn.transactionDate).toLocaleDateString("tr-TR")}
                </div>
                <div>
                  {new Date(txn.transactionDate).toLocaleTimeString("tr-TR", {
                    hour: "2-digit",
                    minute: "2-digit",
                  })}
                </div>
              </TableCell>

              <TableCell>
                <div className="flex items-center gap-2">
                  {isPositive ? (
                    <ArrowUpRight
                      className={`w-4 h-4 ${isRejected ? "text-slate-400" : "text-green-500"}`}
                    />
                  ) : (
                    <ArrowDownRight
                      className={`w-4 h-4 ${isRejected ? "text-slate-400" : "text-red-500"}`}
                    />
                  )}
                  <span
                    className={`text-[10px] font-bold px-2 py-1 rounded-full tracking-wider ${
                      isRejected
                        ? "bg-slate-100 text-slate-600"
                        : isPositive
                          ? "bg-green-100 text-green-700"
                          : "bg-red-100 text-red-700"
                    }`}
                  >
                    {txn.transactionType}
                  </span>
                </div>
              </TableCell>

              <TableCell
                className="text-sm font-medium text-slate-700 max-w-30 sm:max-w-50 md:max-w-87.5 truncate"
                title={txn.description} // 🚀 Faresini üzerine getirene tam metni gösterir
              >
                <span
                  className={isRejected ? "line-through text-slate-400" : ""}
                >
                  {txn.description}
                </span>
              </TableCell>

              {/* 🚀 YENİ: DURUM (STATUS) SÜTUNU */}
              <TableCell className="text-center">
                {status === "PENDING_APPROVAL" && (
                  <div className="inline-flex items-center gap-1 px-2 py-1 bg-amber-50 text-amber-700 rounded-md text-[10px] font-bold border border-amber-200">
                    <Clock className="w-3 h-3" /> BEKLİYOR
                  </div>
                )}
                {status === "COMPLETED" && (
                  <div className="inline-flex items-center gap-1 px-2 py-1 bg-green-50 text-green-700 rounded-md text-[10px] font-bold border border-green-200">
                    <CheckCircle2 className="w-3 h-3" /> ONAYLANDI
                  </div>
                )}
                {status === "REJECTED" && (
                  <div className="inline-flex items-center gap-1 px-2 py-1 bg-red-50 text-red-700 rounded-md text-[10px] font-bold border border-red-200">
                    <XCircle className="w-3 h-3" /> İADE
                  </div>
                )}
              </TableCell>

              <TableCell
                className={`text-right font-black tracking-tight ${
                  isRejected
                    ? "text-slate-400"
                    : isPending
                      ? "text-amber-600"
                      : isPositive
                        ? "text-green-600"
                        : "text-red-600"
                }`}
              >
                {isPositive ? "+" : "-"} {displayAmount}
              </TableCell>
            </TableRow>
          );
        })}
      </TableBody>
    </Table>
  );
}
