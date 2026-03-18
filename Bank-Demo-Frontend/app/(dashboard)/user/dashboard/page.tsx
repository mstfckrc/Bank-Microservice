"use client";

import { useEffect, useState } from "react";
import { useAuthStore } from "@/store/useAuthStore";
import { useRouter } from "next/navigation";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import {
  Send,
  Clock,
  PlusCircle,
  ArrowRightLeft,
  Plus,
  Globe,
  Settings,
  AlertTriangle, 
  XOctagon, 
  RefreshCw,
  Wallet,
  Receipt,
  Trash2,
  Droplets,
  Zap,
  Wifi,
  Flame, 
} from "lucide-react";

import { PageHeader } from "@/components/shared/PageHeader";
import { useUserDashboard } from "@/hooks/useUserDashboard";
import { BalanceCard } from "@/components/dashboard/balance-card";
import { AccountCard } from "@/components/dashboard/account-card";
import { TransactionTable } from "@/components/dashboard/transaction-table";
import { BillInstructionRequest } from "@/types";

import { DepositModal } from "@/components/dashboard/modals/DepositModal";
import { TransferModal } from "@/components/dashboard/modals/TransferModal";
import { CreateAccountModal } from "@/components/dashboard/modals/CreateAccountModal";
import { CloseAccountModal } from "@/components/dashboard/modals/CloseAccountModal";
import { useBills } from "@/hooks/useBills";
import { CreateBillInstructionModal } from "@/components/dashboard/modals/CreateBillInstructionModal";

export default function UserDashboardPage() {
  const router = useRouter();
  const { user } = useAuthStore();

  const {
    accounts,
    transactions,
    selectedAccountNo,
    loading,
    historyLoading,
    isProcessing,
    selectAccount,
    createAccount,
    transferMoney,
    depositMoney,
    closeAccount,
    appealRejection, 
  } = useUserDashboard();

  const [isAccountDialogOpen, setIsAccountDialogOpen] = useState(false);
  const [isTransferDialogOpen, setIsTransferDialogOpen] = useState(false);
  const [isDepositDialogOpen, setIsDepositDialogOpen] = useState(false);
  const [accountToClose, setAccountToClose] = useState<string | null>(null);
  // Fatura Hook'u ve State'i
  const { instructions, fetchInstructions, createInstruction, deleteInstruction, isProcessing: billProcessing } = useBills();
  const [isBillModalOpen, setIsBillModalOpen] = useState(false);

  const [accountFilter, setAccountFilter] = useState<'ALL' | 'ACTIVE' | 'INACTIVE'>('ACTIVE');

  const currentAccountId = accounts.find(
    (acc) => acc.accountNumber === selectedAccountNo,
  )?.id;

  const isPending = user?.status === "PENDING";
  const isRejected = user?.status === "REJECTED";
  const isRestricted = isPending || isRejected;

  const filteredAccounts = accounts.filter(acc => {
    const isActive = (acc as any).isActive !== false && (acc as any).active !== false;
    
    if (accountFilter === 'ACTIVE') return isActive;
    if (accountFilter === 'INACTIVE') return !isActive;
    return true; 
  });

  const billTypeTrMap: Record<string, string> = {
  ELECTRICITY: 'ELEKTRİK',
  WATER: 'SU',
  INTERNET: 'İNTERNET',
  GAS: 'DOĞALGAZ'
};


  // Sayfa yüklendiğinde talimatları çek
  useEffect(() => {
    fetchInstructions();
  }, [fetchInstructions]);
  
  return (
    <div className="space-y-6 max-w-7xl mx-auto p-4 md:p-0">
      <PageHeader
        // 🚀 V2: fullName -> profileName güncellendi
        title={`Hoş Geldiniz, ${user?.profileName || "Müşterimiz"}`}
        description="Varlıklarınızı ve hesap hareketlerinizi buradan takip edebilirsiniz."
        showBackButton={false}
        action={
          !isRestricted && (
            <Button
              className="bg-blue-600 hover:bg-blue-700 shadow-md transition-all active:scale-95"
              onClick={() => setIsTransferDialogOpen(true)}
            >
              <Send className="w-4 h-4 mr-2" /> Hızlı Transfer
            </Button>
          )
        }
      />

      {isPending && (
        <div className="bg-amber-50 border border-amber-200 text-amber-800 px-5 py-4 rounded-xl flex items-start gap-4 shadow-sm animate-in fade-in slide-in-from-top-4 duration-500">
          <AlertTriangle className="w-6 h-6 text-amber-600 shrink-0 mt-0.5" />
          <div>
            <h3 className="font-bold text-amber-900 text-base">Hesabınız Yönetici Onayındadır</h3>
            <p className="text-sm mt-1 text-amber-700">
              Güvenlik prosedürlerimiz gereği hesabınız incelenmektedir. Onay işlemi tamamlanana kadar para transferi, hesap açılışı ve para yatırma işlemleri geçici olarak kilitlenmiştir.
            </p>
          </div>
        </div>
      )}

      {isRejected && (
        <div className="bg-red-50 border border-red-200 text-red-800 px-5 py-4 rounded-xl flex items-start sm:items-center gap-4 shadow-sm animate-in fade-in slide-in-from-top-4 duration-500 flex-col sm:flex-row">
          <XOctagon className="w-6 h-6 text-red-600 shrink-0 mt-0.5 sm:mt-0" />
          <div className="flex-1">
            <h3 className="font-bold text-red-900 text-base">Müşteri Onayınız Reddedildi!</h3>
            <p className="text-sm mt-1 text-red-700">
              Hesap bilgileriniz güvenlik politikalarımıza uymadığı için onaylanmamıştır. Bilgilerinizi <span onClick={() => router.push("/user/settings")} className="font-bold underline cursor-pointer hover:text-red-900">Profil Ayarları</span> üzerinden güncelleyip tekrar değerlendirme talep edebilirsiniz.
            </p>
          </div>
          <Button 
            variant="destructive" 
            className="w-full sm:w-auto shrink-0 shadow-sm"
            onClick={appealRejection}
            disabled={isProcessing}
          >
            {isProcessing ? (
              <RefreshCw className="w-4 h-4 mr-2 animate-spin" />
            ) : (
              <RefreshCw className="w-4 h-4 mr-2" />
            )}
            Tekrar İtiraz Et
          </Button>
        </div>
      )}

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <BalanceCard accounts={accounts} loading={loading} />

        <Card className="lg:col-span-2 border-slate-200 shadow-sm">
          <CardHeader className="pb-2">
            <CardTitle className="text-sm font-medium text-slate-500 uppercase tracking-wider">
              Hızlı İşlemler
            </CardTitle>
          </CardHeader>
          <CardContent className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-5 gap-3 pt-2">
            {/* YENİ FATURA BUTONU */}
            <Button
              variant="outline"
              disabled={isRestricted}
              className={`h-24 flex flex-col gap-2 group ${isRestricted ? 'opacity-50 cursor-not-allowed bg-slate-50' : 'border-indigo-100 hover:bg-indigo-50'}`}
              onClick={() => setIsBillModalOpen(true)}
            >
              <Receipt className={`w-6 h-6 ${isRestricted ? 'text-slate-400' : 'text-indigo-600 group-hover:scale-110 transition-transform'}`} />
              <span className="text-[11px] font-bold text-slate-700">Fatura Öde</span>
            </Button>
            <Button
              variant="outline"
              className="h-24 flex flex-col gap-2 border-purple-100 hover:bg-purple-50 group"
              onClick={() => router.push("/currencies")}
            >
              <Globe className="w-6 h-6 text-purple-600 group-hover:scale-110 transition-transform" />
              <span className="text-[11px] font-bold text-slate-700">Piyasa Ekranı</span>
            </Button>

            <Button
              variant="outline"
              disabled={isRestricted}
              className={`h-24 flex flex-col gap-2 group ${isRestricted ? 'opacity-50 cursor-not-allowed bg-slate-50' : 'border-green-100 hover:bg-green-50'}`}
              onClick={() => setIsDepositDialogOpen(true)}
            >
              <PlusCircle className={`w-6 h-6 ${isRestricted ? 'text-slate-400' : 'text-green-600 group-hover:scale-110 transition-transform'}`} />
              <span className="text-[11px] font-bold text-slate-700">Para Yatır</span>
            </Button>

            <Button
              variant="outline"
              disabled={isRestricted}
              className={`h-24 flex flex-col gap-2 group ${isRestricted ? 'opacity-50 cursor-not-allowed bg-slate-50' : 'border-blue-100 hover:bg-blue-50'}`}
              onClick={() => setIsTransferDialogOpen(true)}
            >
              <ArrowRightLeft className={`w-6 h-6 ${isRestricted ? 'text-slate-400' : 'text-blue-600 group-hover:scale-110 transition-transform'}`} />
              <span className="text-[11px] font-bold text-slate-700">Havale / EFT</span>
            </Button>

            <Button
              variant="outline"
              disabled={isRestricted}
              className={`h-24 flex flex-col gap-2 group ${isRestricted ? 'opacity-50 cursor-not-allowed bg-slate-50' : 'border-slate-100 hover:bg-slate-50'}`}
              onClick={() => setIsAccountDialogOpen(true)}
            >
              <Plus className={`w-6 h-6 ${isRestricted ? 'text-slate-400' : 'text-slate-600 group-hover:scale-110 transition-transform'}`} />
              <span className="text-[11px] font-bold text-slate-700">Yeni Hesap</span>
            </Button>

            <Button
              variant="outline"
              className="h-24 flex flex-col gap-2 border-orange-100 hover:bg-orange-50 group transition-all"
              onClick={() => router.push("/user/settings")}
            >
              <div className="p-1 rounded-lg bg-orange-50 text-orange-600 group-hover:bg-orange-100 transition-colors">
                <Settings className="w-6 h-6 group-hover:rotate-45 transition-transform duration-300" />
              </div>
              <span className="text-[11px] font-bold text-slate-700">Profil Ayarları</span>
            </Button>
          </CardContent>
        </Card>
      </div>

      <div className="space-y-4">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
          <h2 className="text-xl font-bold text-slate-900 flex items-center gap-2">
            Vadesiz Hesaplarım
          </h2>
          
          {accounts.length > 0 && (
            <div className="flex bg-slate-100/80 p-1 rounded-lg self-start sm:self-auto border border-slate-200 shadow-inner">
              <button 
                onClick={() => setAccountFilter('ACTIVE')}
                className={`px-4 py-1.5 text-xs sm:text-sm font-bold rounded-md transition-all ${accountFilter === 'ACTIVE' ? 'bg-white text-slate-800 shadow-sm' : 'text-slate-500 hover:text-slate-700'}`}
              >
                Aktif
              </button>
              <button 
                onClick={() => setAccountFilter('INACTIVE')}
                className={`px-4 py-1.5 text-xs sm:text-sm font-bold rounded-md transition-all ${accountFilter === 'INACTIVE' ? 'bg-white text-slate-800 shadow-sm' : 'text-slate-500 hover:text-slate-700'}`}
              >
                Kapatılan
              </button>
              <button 
                onClick={() => setAccountFilter('ALL')}
                className={`px-4 py-1.5 text-xs sm:text-sm font-bold rounded-md transition-all ${accountFilter === 'ALL' ? 'bg-white text-slate-800 shadow-sm' : 'text-slate-500 hover:text-slate-700'}`}
              >
                Tümü
              </button>
            </div>
          )}
        </div>

        {accounts.length === 0 ? (
          <div className="border-2 border-dashed border-slate-200 rounded-xl p-10 flex flex-col items-center justify-center text-center bg-slate-50/50">
            <Wallet className="w-12 h-12 text-slate-300 mb-3" />
            <h3 className="text-lg font-bold text-slate-700">Hesabınız Bulunmuyor</h3>
            <p className="text-sm text-slate-500 mt-1 max-w-sm">
              Bankamızda aktif bir vadesiz hesabınız bulunmamaktadır. İşlem yapabilmek için hemen bir hesap oluşturabilirsiniz.
            </p>
            {!isRestricted && (
              <Button 
                variant="outline" 
                className="mt-5 border-slate-300 hover:bg-slate-100"
                onClick={() => setIsAccountDialogOpen(true)}
              >
                <Plus className="w-4 h-4 mr-2" /> Hemen Hesap Aç
              </Button>
            )}
          </div>
        ) : filteredAccounts.length === 0 ? (
          <div className="border border-slate-200 rounded-xl p-8 flex flex-col items-center justify-center text-center bg-slate-50/50">
            <p className="text-sm font-medium text-slate-500">
              Bu kategoride gösterilecek hesap bulunamadı.
            </p>
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            {filteredAccounts.map((acc) => (
              <AccountCard
                key={acc.id}
                acc={acc}
                isSelected={selectedAccountNo === acc.accountNumber}
                onSelect={selectAccount}
                onCloseRequest={(e, no) => {
                  e.stopPropagation();
                  setAccountToClose(no);
                }}
              />
            ))}
          </div>
        )}
      </div>

      {/* FATURA TALİMATLARIM KARTI */}
      {instructions.length > 0 && (
        <Card className="border-slate-200 shadow-sm overflow-hidden mb-6">
          <CardHeader className="border-b bg-indigo-50/30 pb-3">
            <CardTitle className="text-lg flex items-center gap-2 text-indigo-900">
              <Receipt className="w-5 h-5 text-indigo-500" /> Aktif Fatura Talimatlarım
            </CardTitle>
          </CardHeader>
          <CardContent className="p-0">
            <div className="divide-y divide-slate-100">
              {instructions.map((inst) => (
                <div key={inst.id} className="flex items-center justify-between p-4 hover:bg-slate-50 transition-colors">
                  <div className="flex items-center gap-4">
                    <div className={`p-2 rounded-full ${inst.billType === 'ELECTRICITY' ? 'bg-yellow-100 text-yellow-600' : inst.billType === 'WATER' ? 'bg-blue-100 text-blue-600' : inst.billType === 'INTERNET' ? 'bg-purple-100 text-purple-600' : 'bg-orange-100 text-orange-600'}`}>
                      {inst.billType === 'ELECTRICITY' ? <Zap className="w-5 h-5"/> : inst.billType === 'WATER' ? <Droplets className="w-5 h-5"/> : inst.billType === 'INTERNET' ? <Wifi className="w-5 h-5"/> : <Flame className="w-5 h-5"/>}
                    </div>
                    <div>
                      {/* 🚀 ÇÖZÜM BURADA: İngilizce tipi Türkçe sözlükten geçirip yazdırıyoruz */}
                      <p className="font-bold text-slate-800 text-sm">{billTypeTrMap[inst.billType]} FATURASI</p>
                      <p className="text-xs text-slate-500">Abone No: {inst.subscriberNo} • Her ayın {inst.paymentDay}. günü</p>
                    </div>
                  </div>
                  <div className="flex items-center gap-4">
                    <div className="text-right hidden sm:block">
                      <p className="text-xs font-semibold text-slate-600">Bağlı Kasa</p>
                      <p className="text-xs font-mono text-slate-400">{inst.accountNumber}</p>
                    </div>
                    <button 
                      onClick={() => {
                        if (window.confirm("Bu ödeme talimatını iptal etmek istediğinize emin misiniz?")) {
                          deleteInstruction(inst.id);
                        }
                      }}
                      disabled={billProcessing}
                      className="text-red-400 hover:text-red-600 p-2 hover:bg-red-50 rounded-full transition-colors"
                      title="Talimatı İptal Et"
                    >
                      <Trash2 className="w-5 h-5" />
                    </button>
                  </div>
                </div>
              ))}
            </div>
          </CardContent>
        </Card>
      )}

      <Card className="border-slate-200 shadow-sm overflow-hidden">
        <CardHeader className="border-b bg-slate-50/50">
          <CardTitle className="text-lg flex items-center justify-between">
            <div className="flex items-center gap-2">
              <Clock className="w-5 h-5 text-slate-400" /> Hesap Hareketleri
            </div>
            {selectedAccountNo && (
              <span className="text-[10px] font-mono bg-white border px-2 py-1 rounded shadow-sm">
                Seçili: {selectedAccountNo}
              </span>
            )}
          </CardTitle>
        </CardHeader>
        <CardContent className="p-0">
          {!selectedAccountNo ? (
            <div className="flex flex-col items-center justify-center py-16 text-slate-400">
              <Clock className="w-12 h-12 mb-3 text-slate-200" />
              <p className="font-medium text-slate-500">Hesap Seçilmedi</p>
              <p className="text-sm mt-1">Geçmiş işlemleri görmek için yukarıdan bir hesaba tıklayın.</p>
            </div>
          ) : (
            <TransactionTable
              transactions={transactions}
              loading={historyLoading}
              currentAccountId={currentAccountId}
            />
          )}
        </CardContent>
      </Card>

      <DepositModal
        isOpen={isDepositDialogOpen}
        onOpenChange={setIsDepositDialogOpen}
        accounts={accounts}
        onDeposit={async (iban, amount) => {
          if (await depositMoney(iban, amount)) setIsDepositDialogOpen(false);
        }}
        isProcessing={isProcessing}
      />
      <TransferModal
        isOpen={isTransferDialogOpen}
        onOpenChange={setIsTransferDialogOpen}
        accounts={accounts}
        onTransfer={async (data) => {
          if (await transferMoney(data)) setIsTransferDialogOpen(false);
        }}
        isProcessing={isProcessing}
      />
      <CreateAccountModal
        isOpen={isAccountDialogOpen}
        onOpenChange={setIsAccountDialogOpen}
        onCreate={async (currency: string) => {
          if (await createAccount(currency)) setIsAccountDialogOpen(false);
        }}
        isProcessing={isProcessing}
      />
      <CloseAccountModal
        accountToClose={accountToClose}
        isClosing={isProcessing}
        onConfirm={async () => {
          if (accountToClose && (await closeAccount(accountToClose)))
            setAccountToClose(null);
        }}
        onCancel={() => setAccountToClose(null)}
      />

      <CreateBillInstructionModal
        isOpen={isBillModalOpen}
        onOpenChange={setIsBillModalOpen}
        accounts={accounts}
        onCreate={async (data) => {
          // 🚀 ÇÖZÜM BURADA: Gelen verinin kesinlikle BillInstructionRequest yapısında olduğunu TypeScript'e garanti ediyoruz (as BillInstructionRequest)
          if (await createInstruction(data as BillInstructionRequest)) {
            setIsBillModalOpen(false);
          }
        }}
        isProcessing={billProcessing}
      />
    </div>
  );
}