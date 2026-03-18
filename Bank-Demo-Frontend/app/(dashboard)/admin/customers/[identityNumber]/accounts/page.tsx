"use client";

import { useEffect, useState } from "react";
import { useParams, useRouter } from "next/navigation";
import { adminService } from "@/services/admin.service";
import { accountService } from "@/services/account.service";
import { AccountResponse } from "@/types";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Loader2, Landmark, Plus } from "lucide-react";
import { toast } from "sonner"; 

import { AdminTransactionHistoryModal } from "@/components/admin/modals/AdminTransactionHistoryModal";
import { OpenAccountModal } from "@/components/admin/modals/OpenAccountModal";
import { CloseAccountModal } from "@/components/admin/modals/CloseAccountModal";
import { AccountTable } from "@/components/admin/tables/AccountTable";
import { PageHeader } from "@/components/shared/PageHeader";

export default function CustomerAccountsPage() {
  const params = useParams();
  // 🚀 V2: Klasör adını değiştirdiğimiz için artık URL'den identityNumber okuyoruz
  const identityNumber = params.identityNumber as string;

  const [accounts, setAccounts] = useState<AccountResponse[]>([]);
  const [loading, setLoading] = useState(true);

  const [isOpenAccountModalOpen, setIsOpenAccountModalOpen] = useState(false);
  
  const [accountToClose, setAccountToClose] = useState<string | null>(null);
  const [isClosing, setIsClosing] = useState(false);
  
  const [historyAccountNo, setHistoryAccountNo] = useState<string | null>(null);
  const [historyAccountId, setHistoryAccountId] = useState<number | null>(null);
  const [isHistoryOpen, setIsHistoryOpen] = useState(false);

  useEffect(() => {
    if (identityNumber) fetchAccounts();
  }, [identityNumber]);

  const fetchAccounts = async () => {
    try {
      setLoading(true);
      const data = await adminService.getCustomerAccounts(identityNumber); 
      setAccounts(data);
    } catch (error) {
      toast.error("Hata", { description: "Müşteri/Kurum hesapları getirilemedi." });
    } finally {
      setLoading(false);
    }
  };

  const confirmCloseAccount = async () => {
    if (!accountToClose) return;
    setIsClosing(true);

    try {
      await accountService.deleteAccount(accountToClose);
      toast.success("İşlem Başarılı", { description: `${accountToClose} numaralı hesap kapatıldı.` });
      
      setAccounts((prevAccounts) =>
        prevAccounts.map((acc) =>
          acc.accountNumber === accountToClose ? { ...acc, isActive: false } : acc
        )
      );
      setAccountToClose(null); 
    } catch (error: any) {
      toast.error("İşlem Başarısız", { description: error.response?.data?.message || "Hesap kapatılamadı." });
    } finally {
      setIsClosing(false);
    }
  };

  return (
    <div className="space-y-6 relative">
      <PageHeader 
        title="Müşteri/Kurum Hesapları" 
        // 🚀 V2: Dinamik başlık
        description={`Kimlik/Vergi No: ${identityNumber} olan müşterinin tüm varlıkları.`}
        action={<Button onClick={fetchAccounts} variant="secondary" size="sm">Listeyi Yenile</Button>}
      />

      <Card>
        <CardHeader className="flex flex-row items-center justify-between border-b bg-slate-50/50 pb-4">
          <CardTitle className="flex items-center gap-2 text-slate-700">
            <Landmark className="h-5 w-5 text-slate-400" /> Hesap Listesi
          </CardTitle>
          
          <div className="flex items-center gap-3">
            <span className="text-xs font-bold bg-white border px-3 py-1.5 rounded-full text-slate-600 shadow-sm">
              Toplam {accounts.length} Kayıt
            </span>
            <Button size="sm" className="bg-blue-600 hover:bg-blue-700 text-white shadow-md transition-all" onClick={() => setIsOpenAccountModalOpen(true)}>
              <Plus className="w-4 h-4 mr-1.5" /> Yeni Hesap Aç
            </Button>
          </div>
        </CardHeader>

        <CardContent className="pt-6">
          {loading ? (
            <div className="flex justify-center py-20"><Loader2 className="h-8 w-8 animate-spin text-slate-500" /></div>
          ) : (
            <AccountTable 
              accounts={accounts}
              onOpenHistory={(accountNo, accountId) => {
                setHistoryAccountNo(accountNo);
                setHistoryAccountId(accountId);
                setIsHistoryOpen(true);
              }}
              onCloseAccount={(accountNo) => setAccountToClose(accountNo)}
            />
          )}
        </CardContent>
      </Card>

      <AdminTransactionHistoryModal
        isOpen={isHistoryOpen}
        onOpenChange={setIsHistoryOpen}
        accountNumber={historyAccountNo}
        accountId={historyAccountId}
      />

      <OpenAccountModal
        isOpen={isOpenAccountModalOpen}
        onOpenChange={setIsOpenAccountModalOpen}
        identityNumber={identityNumber} // 🚀 V2: Modal'a tcNo yerine identityNumber gönderiliyor
        onSuccess={(newAccount) => setAccounts((prev) => [...prev, newAccount])}
      />

      <CloseAccountModal 
        accountToClose={accountToClose}
        isClosing={isClosing}
        onClose={() => setAccountToClose(null)}
        onConfirm={confirmCloseAccount}
      />
    </div>
  );
}