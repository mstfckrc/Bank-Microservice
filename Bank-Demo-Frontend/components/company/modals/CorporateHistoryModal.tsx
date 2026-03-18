import React, { useEffect, useState } from "react";
import { transactionService } from "@/services/transaction.service";
import { TransactionResponse } from "@/types";
import {
  Clock,
  CheckCircle2,
  XCircle,
  ArrowUpRight,
  ArrowDownRight,
} from "lucide-react"; // 🚀 LÜCIDE İKONLARI EKLENDİ

interface CorporateHistoryModalProps {
  isOpen: boolean;
  onClose: () => void;
  accountId: number;
  accountNumber: string;
  currency: string;
}

export default function CorporateHistoryModal({
  isOpen,
  onClose,
  accountId,
  accountNumber,
  currency,
}: CorporateHistoryModalProps) {
  const [transactions, setTransactions] = useState<TransactionResponse[]>([]);
  const [isLoading, setIsLoading] = useState(false);

  useEffect(() => {
    if (isOpen && accountNumber) {
      fetchHistory();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [isOpen, accountNumber]);

  const fetchHistory = async () => {
    try {
      setIsLoading(true);
      const data =
        await transactionService.getAccountTransactions(accountNumber);
      const sortedData = data.sort(
        (a, b) =>
          new Date(b.transactionDate).getTime() -
          new Date(a.transactionDate).getTime(),
      );
      setTransactions(sortedData);
    } catch (error) {
      console.error("Geçmiş çekilirken hata:", error);
    } finally {
      setIsLoading(false);
    }
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 bg-black/60 z-50 flex items-center justify-center p-4 backdrop-blur-sm">
      <div className="bg-white rounded-xl shadow-2xl w-full max-w-2xl overflow-hidden flex flex-col max-h-[85vh]">
        {/* BAŞLIK */}
        <div className="p-6 border-b border-gray-100 flex justify-between items-center bg-slate-50">
          <div className="flex items-center gap-3">
            <div className="bg-blue-100 text-blue-600 p-2 rounded-lg">
              <svg
                className="w-6 h-6"
                fill="none"
                stroke="currentColor"
                viewBox="0 0 24 24"
              >
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth={2}
                  d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"
                />
              </svg>
            </div>
            <div>
              <h2 className="text-xl font-bold text-gray-800">
                Kasa Hareketleri
              </h2>
              <p className="text-xs text-gray-500 font-mono mt-1">
                {accountNumber}
              </p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="text-gray-400 hover:text-gray-600 transition-colors"
          >
            <svg
              className="w-6 h-6"
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M6 18L18 6M6 6l12 12"
              />
            </svg>
          </button>
        </div>

        {/* İÇERİK (LİSTE) */}
        <div className="overflow-y-auto p-4 md:p-6 bg-gray-50 flex-1">
          {isLoading ? (
            <div className="flex flex-col justify-center items-center py-12">
              <Clock className="w-8 h-8 animate-spin mb-4 text-blue-500" />
              <p className="text-gray-500 font-medium animate-pulse">
                Kasa hareketleri yükleniyor...
              </p>
            </div>
          ) : transactions.length === 0 ? (
            <div className="text-center py-12 text-gray-500">
              <svg
                className="w-12 h-12 mx-auto text-gray-300 mb-3"
                fill="none"
                stroke="currentColor"
                viewBox="0 0 24 24"
              >
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth={1.5}
                  d="M20 12H4m16 0a8 8 0 11-16 0 8 8 0 0116 0z"
                />
              </svg>
              <p>Bu kasaya ait henüz bir işlem hareketi bulunmuyor.</p>
            </div>
          ) : (
            <div className="space-y-3">
              {transactions.map((tx) => {
                // 🚀 1. GİREN Mİ ÇIKAN MI?
                const isIncoming =
                  tx.transactionType === "DEPOSIT" ||
                  tx.receiverAccountId === accountId;

                // 🚀 2. DURUM (STATUS) KONTROLÜ
                const status = tx.status || "COMPLETED";
                const isRejected = status === "REJECTED";
                const isPending = status === "PENDING_APPROVAL";

                // 🚀 3. RENK PALETLERİ
                const amountColor = isRejected
                  ? "text-slate-400"
                  : isPending
                    ? "text-amber-500"
                    : isIncoming
                      ? "text-green-600"
                      : "text-red-600";
                const iconBgColor = isRejected
                  ? "bg-slate-100 text-slate-400"
                  : isIncoming
                    ? "bg-green-100 text-green-600"
                    : "bg-red-100 text-red-600";

                return (
                  <div
                    key={tx.referenceNo}
                    className={`bg-white p-4 rounded-xl border border-gray-100 shadow-sm flex items-center justify-between transition-all ${isRejected ? "opacity-70" : "hover:shadow-md"}`}
                  >
                    {/* SOL TARAF: İkon, Başlık ve Tarih */}
                    <div className="flex items-center gap-3 md:gap-4">
                      <div className={`p-2.5 rounded-full ${iconBgColor}`}>
                        {isIncoming ? (
                          <ArrowDownRight className="w-5 h-5" />
                        ) : (
                          <ArrowUpRight className="w-5 h-5" />
                        )}
                      </div>
                      <div>
                        <div className="flex items-center gap-2 mb-0.5">
                          <p
                            className={`font-semibold text-sm ${isRejected ? "text-slate-500 line-through" : "text-gray-800"}`}
                          >
                            {tx.transactionType === "DEPOSIT"
                              ? "Para Yatırma"
                              : tx.transactionType === "WITHDRAWAL"
                                ? "Para Çekme"
                                : "Para Transferi (Havale/EFT)"}
                          </p>

                          {/* 🚀 DURUM ETİKETLERİ (BADGES) */}
                          {isPending && (
                            <span className="inline-flex items-center gap-1 px-1.5 py-0.5 bg-amber-50 text-amber-700 rounded text-[10px] font-bold border border-amber-200">
                              <Clock className="w-3 h-3" /> BEKLİYOR
                            </span>
                          )}
                          {status === "COMPLETED" && (
                            <span className="inline-flex items-center gap-1 px-1.5 py-0.5 bg-green-50 text-green-700 rounded text-[10px] font-bold border border-green-200">
                              <CheckCircle2 className="w-3 h-3" /> ONAYLANDI
                            </span>
                          )}
                          {isRejected && (
                            <span className="inline-flex items-center gap-1 px-1.5 py-0.5 bg-red-50 text-red-700 rounded text-[10px] font-bold border border-red-200">
                              <XCircle className="w-3 h-3" /> İADE
                            </span>
                          )}
                        </div>

                        <p className="text-xs text-gray-500">
                          {new Date(tx.transactionDate).toLocaleString(
                            "tr-TR",
                            {
                              day: "2-digit",
                              month: "long",
                              year: "numeric",
                              hour: "2-digit",
                              minute: "2-digit",
                            },
                          )}
                        </p>
                      </div>
                    </div>

                    {/* SAĞ TARAF: Tutar ve Açıklama */}
                    <div className="text-right">
                      <p
                        className={`font-bold text-base md:text-lg tracking-tight ${amountColor}`}
                      >
                        {/* 🚀 DÜZELTME: Eğer para bu kasaya GİRİYORSA ve çevrilmiş bir tutar varsa, onu göster. 
                            Aksi takdirde ana tutarı göster. */}
                        {isIncoming ? "+" : "-"}
                        {(isIncoming && tx.convertedAmount
                          ? tx.convertedAmount
                          : tx.amount
                        ).toLocaleString("tr-TR", {
                          minimumFractionDigits: 2,
                          maximumFractionDigits: 2,
                        })}{" "}
                        {currency}
                      </p>
                      <div className="flex flex-col items-end mt-1">
                        {tx.description && (
                          <p
                            className={`text-xs max-w-30 md:max-w-45 truncate ${isRejected ? "text-slate-400 line-through" : "text-gray-500"}`}
                            title={tx.description}
                          >
                            {tx.description}
                          </p>
                        )}
                        <p className="text-[9px] text-gray-300 font-mono mt-0.5">
                          Ref: {tx.referenceNo}
                        </p>
                      </div>
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
