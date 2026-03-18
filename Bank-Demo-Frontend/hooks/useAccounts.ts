import { useState, useEffect } from "react";
import { accountService } from "@/services/account.service";
import { adminService } from "@/services/admin.service";
import { AccountResponse } from "@/types";
import { toast } from "sonner";

// 🚀 V2: tcNo yerine identityNumber bekliyoruz
// 🚀 YENİ: fetchAll adında bir admin anahtarı ekledik (Varsayılan: false)
export function useAccounts(identityNumber?: string, fetchAll: boolean = false) {
  const [accounts, setAccounts] = useState<AccountResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [isProcessing, setIsProcessing] = useState(false);

  const fetchAccounts = async () => {
    try {
      setLoading(true);
      
      let data;
      // 🚀 YÖNLENDİRME MERKEZİ
      if (fetchAll) {
        // 1. Admin "Tüm Hesaplar" sayfasındaysa: Bütün sistemi tara
        data = await accountService.getAllAccounts();
      } else if (identityNumber) {
        // 2. Müşteri Detay sayfasındaysa: Sadece o müşteriyi getir
        data = await adminService.getCustomerAccounts(identityNumber);
      } else {
        // 3. Şirket veya Birey kendi panelindeyse: Kendi hesaplarını getir
        data = await accountService.getMyAccounts();
      }
      
      setAccounts(data);
    } catch (error) {
      console.error("Hesaplar çekilirken hata:", error);
    } finally {
      setLoading(false);
    }
  };

  const closeAccount = async (accountNumber: string) => {
    try {
      setIsProcessing(true);
      await accountService.deleteAccount(accountNumber);
      toast.success("İşlem Başarılı", { description: `${accountNumber} numaralı hesap kapatıldı.` });
      
      setAccounts((prev) =>
        prev.map((acc) =>
          acc.accountNumber === accountNumber ? { ...acc, isActive: false } : acc
        )
      );
      return true; 
    } finally {
      setIsProcessing(false);
    }
  };

  // 🚀 V2: Müşterinin identityNumber bilgisini yolluyoruz
  const openAccount = async (customerIdentityNumber: string, currency: string) => {
    try {
      setIsProcessing(true);
      const newAccount = await adminService.openAccountForCustomer(customerIdentityNumber, currency);
      toast.success("Hesap Başarıyla Açıldı", { description: `${newAccount.iban} numaralı ${newAccount.currency} hesabı oluşturuldu.` });
      setAccounts((prev) => [...prev, newAccount]);
      return true;
    } finally {
      setIsProcessing(false);
    }
  };

 // 🚀 TİP DÜZELTİLDİ: Artık sadece geçerli para birimlerini kabul ediyor
  const createMyAccount = async (currency: "TRY" | "USD" | "EUR") => {
    try {
      setIsProcessing(true);
      
      const newAccount = await accountService.createAccount({ currency });
      
      toast.success("Kasa Başarıyla Açıldı", { 
        description: `${newAccount.iban} numaralı ${newAccount.currency} kasası oluşturuldu.` 
      });
      
      setAccounts((prev) => [...prev, newAccount]);
      
      return true;
    } catch (error) {
      console.error("Kasa açılırken hata:", error);
      toast.error("İşlem Başarısız", { description: "Kasa açılamadı. Lütfen tekrar deneyin." });
      return false;
    } finally {
      setIsProcessing(false);
    }
  };

  return { accounts, loading, isProcessing, fetchAccounts, closeAccount, openAccount , createMyAccount };
}