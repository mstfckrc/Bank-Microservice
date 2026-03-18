import { useEffect, useState } from "react";
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription } from "@/components/ui/dialog";
import { adminService } from "@/services/admin.service";
import { TransactionResponse } from "@/types";
import { TransactionTable } from "@/components/dashboard/transaction-table";

interface AdminTransactionHistoryModalProps {
  isOpen: boolean;
  onOpenChange: (open: boolean) => void;
  accountNumber: string | null;
  accountId: number | null; // 🚀 YENİ: Hesabın ID'sini de alıyoruz
}

export function AdminTransactionHistoryModal({ isOpen, onOpenChange, accountNumber, accountId }: AdminTransactionHistoryModalProps) {
  const [transactions, setTransactions] = useState<TransactionResponse[]>([]);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (isOpen && accountNumber) {
      fetchTransactions();
    } else {
      setTransactions([]);
    }
  }, [isOpen, accountNumber]);

  const fetchTransactions = async () => {
    try {
      setLoading(true);
      const data = await adminService.getAccountTransactions(accountNumber!);
      setTransactions(data || []);
    } catch (error: any) {
      setTransactions([]); 
    } finally {
      setLoading(false);
    }
  };

  return (
    <Dialog open={isOpen} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-4xl max-h-[85vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle className="text-xl font-bold">Hesap Hareketleri</DialogTitle>
          <DialogDescription>
            <strong className="text-slate-800">{accountNumber}</strong> numaralı hesabın tüm işlem geçmişini inceliyorsunuz.
          </DialogDescription>
        </DialogHeader>
        
        <div className="mt-4 border rounded-lg overflow-hidden">
          {/* 🚀 YENİ: accountId'yi tabloya gönderiyoruz ki kimin perspektifinden baktığını anlasın! */}
          <TransactionTable transactions={transactions} loading={loading} currentAccountId={accountId || undefined} />
        </div>
        
      </DialogContent>
    </Dialog>
  );
}