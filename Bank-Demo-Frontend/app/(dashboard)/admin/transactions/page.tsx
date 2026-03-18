"use client";

import { useEffect, useState } from "react";
import { adminService } from "@/services/admin.service";
import { TransactionResponse, TransactionStatus } from "@/types";
import { Card, CardContent, CardHeader } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { toast } from "sonner";
import { CheckCircle, XCircle, Clock, Search, ShieldAlert, ArrowRight } from "lucide-react";
import { PageHeader } from "@/components/shared/PageHeader";

export default function AdminTransactionsPage() {
  const [transactions, setTransactions] = useState<TransactionResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [filter, setFilter] = useState<TransactionStatus | "ALL">("PENDING_APPROVAL");

  useEffect(() => {
    fetchTransactions();
  }, [filter]);

  const fetchTransactions = async () => {
    try {
      setLoading(true);
      const data = await adminService.getAllTransactions(filter === "ALL" ? undefined : filter);
      setTransactions(data);
    } catch (error) {
      toast.error("İşlemler yüklenemedi.");
    } finally {
      setLoading(false);
    }
  };

  const handleApprove = async (referenceNo: string) => {
    try {
      await adminService.approveTransaction(referenceNo);
      toast.success("İşlem Onaylandı", { description: "Para alıcının hesabına aktarıldı." });
      fetchTransactions();
    } catch (error: any) {
      toast.error("Hata", { description: error.response?.data?.message || "Onaylanamadı." });
    }
  };

  const handleReject = async (referenceNo: string) => {
    try {
      await adminService.rejectTransaction(referenceNo);
      toast.success("İşlem Reddedildi", { description: "Tutar gönderenin hesabına iade edildi." });
      fetchTransactions();
    } catch (error: any) {
      toast.error("Hata", { description: error.response?.data?.message || "Reddedilemedi." });
    }
  };

  return (
    <div className="space-y-6 max-w-7xl mx-auto p-4 md:p-0">
      <PageHeader
        title="Merkezi İşlem İzleme"
        description="Bankadaki tüm para akışını ve onaya takılan yüklü işlemleri yönetin."
      />

      <Card className="border-none shadow-xl">
        <CardHeader className="border-b bg-slate-50/50 pb-4">
          <div className="flex flex-wrap gap-2">
            <Button variant={filter === "PENDING_APPROVAL" ? "default" : "outline"} onClick={() => setFilter("PENDING_APPROVAL")} className={filter === "PENDING_APPROVAL" ? "bg-amber-500 hover:bg-amber-600" : ""}>
              <Clock className="w-4 h-4 mr-2" /> Onay Bekleyenler
            </Button>
            <Button variant={filter === "COMPLETED" ? "default" : "outline"} onClick={() => setFilter("COMPLETED")} className={filter === "COMPLETED" ? "bg-green-600 hover:bg-green-700" : ""}>
              <CheckCircle className="w-4 h-4 mr-2" /> Tamamlananlar
            </Button>
            <Button variant={filter === "REJECTED" ? "default" : "outline"} onClick={() => setFilter("REJECTED")} className={filter === "REJECTED" ? "bg-red-600 hover:bg-red-700" : ""}>
              <XCircle className="w-4 h-4 mr-2" /> Reddedilenler
            </Button>
            <Button variant={filter === "ALL" ? "default" : "outline"} onClick={() => setFilter("ALL")}>
              <Search className="w-4 h-4 mr-2" /> Tümü
            </Button>
          </div>
        </CardHeader>
        <CardContent className="p-0">
          {loading ? (
            <div className="p-10 text-center text-slate-500 animate-pulse">İşlemler yükleniyor...</div>
          ) : transactions.length === 0 ? (
            <div className="p-16 text-center text-slate-400 flex flex-col items-center">
              <ShieldAlert className="w-12 h-12 mb-3 opacity-20" />
              <p>Bu kategoride işlem bulunamadı.</p>
            </div>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-sm text-left">
                <thead className="text-xs text-slate-500 uppercase bg-slate-50 border-b">
                  <tr>
                    <th className="px-4 py-4">Tarih / Ref</th>
                    {/* 🚀 YENİ EKLENEN SÜTUNLAR */}
                    <th className="px-4 py-4 text-center">Para Akışı (Gönderen ➔ Alıcı)</th>
                    <th className="px-4 py-4">İşlem Detayı</th>
                    <th className="px-4 py-4 text-right">Tutar</th>
                    <th className="px-4 py-4 text-center">Durum</th>
                    <th className="px-4 py-4 text-right">Aksiyon</th>
                  </tr>
                </thead>
                <tbody>
                  {transactions.map((t) => (
                    <tr key={t.referenceNo} className="border-b hover:bg-slate-50/80 transition-colors">
                      
                      <td className="px-4 py-4 whitespace-nowrap">
                        <div className="font-semibold text-slate-700">
                          {new Date(t.transactionDate).toLocaleDateString('tr-TR')}
                        </div>
                        <div className="text-[10px] text-slate-400 font-mono mt-0.5">{t.referenceNo.split('-')[0]}...</div>
                      </td>

                      {/* 🚀 YENİ EKLENEN PARA AKIŞI GÖSTERİMİ */}
                      <td className="px-4 py-4">
                        <div className="flex items-center justify-center gap-2">
                          {/* GÖNDEREN */}
                          {t.senderAccountId ? (
                            <span className="text-[11px] font-mono bg-slate-100 text-slate-600 px-2 py-1 rounded border border-slate-200 shadow-sm">
                              ID: {t.senderAccountId}
                            </span>
                          ) : (
                            <span className="text-[11px] bg-slate-50 text-slate-400 px-2 py-1 rounded border border-slate-100 italic">Dış Kaynak</span>
                          )}
                          
                          <ArrowRight className="w-3 h-3 text-slate-300" />
                          
                          {/* ALICI */}
                          {t.receiverAccountId ? (
                            <span className="text-[11px] font-mono bg-blue-50 text-blue-700 px-2 py-1 rounded border border-blue-100 shadow-sm">
                              ID: {t.receiverAccountId}
                            </span>
                          ) : (
                            <span className="text-[11px] bg-slate-50 text-slate-400 px-2 py-1 rounded border border-slate-100 italic">Dış Kaynak</span>
                          )}
                        </div>
                      </td>

                      <td className="px-4 py-4">
                        <div className="font-medium text-slate-800">{t.transactionType}</div>
                        <div className="text-xs text-slate-500 mt-1 max-w-50 truncate" title={t.description}>{t.description}</div>
                      </td>
                      
                      <td className="px-4 py-4 text-right font-bold text-slate-700 whitespace-nowrap">
                        {t.amount.toLocaleString('tr-TR')} ₺
                      </td>
                      
                      <td className="px-4 py-4 text-center">
                        {t.status === 'PENDING_APPROVAL' && <span className="px-2 py-1 bg-amber-100 text-amber-700 rounded-md text-xs font-bold border border-amber-200">ONAY BEKLİYOR</span>}
                        {t.status === 'COMPLETED' && <span className="px-2 py-1 bg-green-100 text-green-700 rounded-md text-xs font-bold border border-green-200">TAMAMLANDI</span>}
                        {t.status === 'REJECTED' && <span className="px-2 py-1 bg-red-100 text-red-700 rounded-md text-xs font-bold border border-red-200">İADE EDİLDİ</span>}
                      </td>
                      
                      <td className="px-4 py-4 text-right">
                        {t.status === 'PENDING_APPROVAL' ? (
                          <div className="flex justify-end gap-2">
                            <Button size="sm" className="bg-green-600 hover:bg-green-700 h-8 text-xs" onClick={() => handleApprove(t.referenceNo)}>
                              Onayla
                            </Button>
                            <Button size="sm" variant="destructive" className="h-8 text-xs" onClick={() => handleReject(t.referenceNo)}>
                              Reddet
                            </Button>
                          </div>
                        ) : (
                          <span className="text-xs text-slate-400">-</span>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  );
}