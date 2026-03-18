"use client";

import { useEffect, useState } from "react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Loader2, Building2, History, Trash2 } from "lucide-react";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";

import { PageHeader } from "@/components/shared/PageHeader";
import { StatusBadge } from "@/components/shared/StatusBadge";
import { useAccounts } from "@/hooks/useAccounts"; 
import { AdminTransactionHistoryModal } from "@/components/admin/modals/AdminTransactionHistoryModal";
import { CloseAccountModal } from "@/components/admin/modals/CloseAccountModal";

export default function AllAccountsPage() {
  // 🚀 DÜZELTME: İlk parametre boş (undefined), ikinci parametre (fetchAll) TRUE!
  // Böylece motor "Tüm Hesapları Getir" modunda çalışır.
  const { accounts, loading, isProcessing, fetchAccounts, closeAccount } = useAccounts(undefined, true);

  const [accountToClose, setAccountToClose] = useState<string | null>(null);
  const [historyAccountNo, setHistoryAccountNo] = useState<string | null>(null);
  const [historyAccountId, setHistoryAccountId] = useState<number | null>(null);
  const [isHistoryOpen, setIsHistoryOpen] = useState(false);

  const confirmCloseAccount = async () => {
    if (!accountToClose) return;
    const success = await closeAccount(accountToClose);
    if (success) setAccountToClose(null); 
  };

  // 🚀 DÜZELTME 2: Sayfa açılır açılmaz hesapları çekme emrini veriyoruz!
  useEffect(() => {
    fetchAccounts();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  return (
    <div className="space-y-6 relative">
      <PageHeader 
        title="Tüm Banka Hesapları" 
        description="Sistemdeki tüm aktif ve kapalı hesapların genel görünümü."
        action={<Button onClick={fetchAccounts} variant="secondary" size="sm">Listeyi Yenile</Button>}
      />

      <Card>
        <CardHeader className="flex flex-row items-center gap-2 border-b bg-slate-50/50 pb-4">
          <Building2 className="h-5 w-5 text-slate-500" />
          <CardTitle>Genel Hesap Havuzu</CardTitle>
        </CardHeader>
        <CardContent className="pt-6">
          {loading ? (
            <div className="flex justify-center py-20">
              <Loader2 className="h-8 w-8 animate-spin text-slate-500" />
            </div>
          ) : (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead className="font-bold">Müşteri / Kurum</TableHead>
                  <TableHead className="font-bold">Hesap Numarası</TableHead>
                  <TableHead className="font-bold">IBAN</TableHead>
                  <TableHead className="text-right font-bold">Bakiye</TableHead>
                  <TableHead className="font-bold">Döviz</TableHead>
                  <TableHead className="text-center font-bold">Durum</TableHead>
                  <TableHead className="text-right font-bold">İşlemler</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {accounts.length === 0 ? (
                  <TableRow>
                    <TableCell colSpan={7} className="text-center py-10 text-slate-500">
                      Sistemde henüz açılmış bir banka hesabı bulunmuyor.
                    </TableCell>
                  </TableRow>
                ) : (
                  accounts.map((acc: any) => {
                    const isAccountActive = acc.isActive !== false && acc.active !== false;

                    return (
                      <TableRow 
                        key={acc.id} 
                        className={`transition-colors ${isAccountActive ? "hover:bg-slate-50" : "bg-slate-50/50 opacity-60"}`}
                      >
                        <TableCell>
                          <div className="font-bold text-slate-900">{acc.ownerName || "Bilinmiyor"}</div>
                          <div className="text-[11px] text-slate-500 font-mono mt-0.5">ID: {acc.identityNumber || "-"}</div>
                        </TableCell>

                        <TableCell className={`font-mono font-medium ${!isAccountActive ? "line-through text-slate-400" : ""}`}>
                          {acc.accountNumber}
                        </TableCell>
                        
                        <TableCell className="font-mono text-sm text-slate-600">{acc.iban}</TableCell>
                        
                        <TableCell className={`text-right font-bold text-lg ${!isAccountActive ? "text-slate-400" : "text-slate-700"}`}>
                          {acc.balance.toLocaleString('tr-TR')}
                        </TableCell>
                        
                        <TableCell>
                          <StatusBadge type="currency" value={acc.currency} isActive={isAccountActive} />
                        </TableCell>
                        
                        <TableCell className="text-center">
                          <StatusBadge type="status" value={isAccountActive} />
                        </TableCell>
                        
                        <TableCell className="text-right">
                          <div className="flex justify-end gap-1">
                            <Button
                              variant="ghost" size="icon"
                              onClick={() => {
                                setHistoryAccountNo(acc.accountNumber);
                                setHistoryAccountId(acc.id);
                                setIsHistoryOpen(true);
                              }}
                              className="text-slate-400 hover:text-blue-600 hover:bg-blue-50 transition-colors"
                              title="Hesap Geçmişini Gör"
                            >
                              <History className="h-4 w-4" />
                            </Button>

                            {isAccountActive && (
                              <Button
                                variant="ghost" size="icon"
                                onClick={() => setAccountToClose(acc.accountNumber)}
                                className="text-slate-400 hover:text-red-600 hover:bg-red-50 transition-colors"
                                title="Hesabı Kapat (Admin)"
                              >
                                <Trash2 className="h-4 w-4" />
                              </Button>
                            )}
                          </div>
                        </TableCell>
                      </TableRow>
                    );
                  })
                )}
              </TableBody>
            </Table>
          )}
        </CardContent>
      </Card>

      <AdminTransactionHistoryModal
        isOpen={isHistoryOpen}
        onOpenChange={setIsHistoryOpen}
        accountNumber={historyAccountNo}
        accountId={historyAccountId}
      />

      <CloseAccountModal 
        accountToClose={accountToClose}
        isClosing={isProcessing}
        onClose={() => setAccountToClose(null)}
        onConfirm={confirmCloseAccount}
      />
    </div>
  );
}