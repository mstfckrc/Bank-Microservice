import { useState, useEffect } from "react";
import { accountService } from "@/services/account.service";
import { transactionService } from "@/services/transaction.service";
import { customerService } from "@/services/customer.service";
import { useAuthStore } from "@/store/useAuthStore";
import { AccountResponse, TransactionResponse } from "@/types";
import { toast } from "sonner";

export function useUserDashboard() {
  const [accounts, setAccounts] = useState<AccountResponse[]>([]);
  const [transactions, setTransactions] = useState<TransactionResponse[]>([]);
  const [selectedAccountNo, setSelectedAccountNo] = useState<string | null>(null);

  const [loading, setLoading] = useState(true);
  const [historyLoading, setHistoryLoading] = useState(false);
  const [isProcessing, setIsProcessing] = useState(false);

  useEffect(() => {
    fetchMyAccounts();
  }, []);

  const fetchMyAccounts = async () => {
    try {
      setLoading(true);
      const data = await accountService.getMyAccounts();
      setAccounts(data);
      
      // Sayfa açıldığında otomatik hesap seçimi İPTAL (UX için en doğrusu)
    } finally {
      setLoading(false);
    }
  };

  const selectAccount = async (accountNumber: string) => {
    setSelectedAccountNo(accountNumber);
    
    // 🚀 GÜNCELLEME 1: Başka hesaba tıklar tıklamaz eski hesabın işlemlerini ekrandan sil!
    // Böylece eski veride takılı kalma bug'ını sonsuza dek çözüyoruz.
    setTransactions([]); 
    
    try {
      setHistoryLoading(true);
      const data = await transactionService.getAccountTransactions(accountNumber);
      // Gelen data boş liste ([]) olsa bile sorunsuz set edilir.
      setTransactions(data || []);
    } catch (error) {
      // 🚀 GÜNCELLEME 2: Herhangi bir API hatasında da tabloyu temiz bırak!
      setTransactions([]);
    } finally {
      setHistoryLoading(false);
    }
  };

  const createAccount = async (currency: string) => {
    try {
      setIsProcessing(true);
      const newAccount = await accountService.createAccount({ currency: currency as any });
      setAccounts((prev) => [...prev, newAccount]);
      toast.success("Yeni Hesap Açıldı", { description: `${currency} hesabınız hazır.` });
      return true; 
    } finally {
      setIsProcessing(false);
    }
  };

  const transferMoney = async (transferData: any) => {
    try {
      setIsProcessing(true);
      await transactionService.transfer({ ...transferData, amount: Number(transferData.amount) });
      toast.success("Transfer Başarılı");
      await fetchMyAccounts(); 
      if (selectedAccountNo) await selectAccount(selectedAccountNo); 
      return true;
    } finally {
      setIsProcessing(false);
    }
  };

  const depositMoney = async (iban: string, amount: string) => {
    try {
      setIsProcessing(true);
      await transactionService.deposit({ iban, amount: Number(amount) });
      toast.success("Para Hesabınıza Yatırıldı");
      await fetchMyAccounts();
      if (selectedAccountNo) await selectAccount(selectedAccountNo);
      return true;
    } finally {
      setIsProcessing(false);
    }
  };

  const closeAccount = async (accountToClose: string) => {
    try {
      setIsProcessing(true);
      await accountService.deleteAccount(accountToClose);
      toast.success("Hesap Kapatıldı");
      
      setAccounts((prev) => prev.map((acc) => acc.accountNumber === accountToClose ? { ...acc, isActive: false } : acc));
      
      if (selectedAccountNo === accountToClose) {
        setSelectedAccountNo(null);
        setTransactions([]);
      }
      return true;
    } finally {
      setIsProcessing(false);
    }
  };

  const appealRejection = async () => {
    try {
      setIsProcessing(true);
      
      await customerService.appealRejection();
      
      const token = useAuthStore.getState().token;
      
      if (token) {
        const freshProfile = await customerService.getProfile(token);
        useAuthStore.setState({ user: freshProfile });
      }

      toast.success("Talebiniz alınmıştır", {
        description: "Hesabınız tekrar değerlendirmeye gönderildi."
      });
      
    } catch (error: any) {
      toast.error("İşlem Başarısız", {
        description: error.response?.data?.message || "Talebiniz iletilemedi."
      });
    } finally {
      setIsProcessing(false);
    }
  };

  return {
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
  };
}