import React, { useState } from 'react';
import { transactionService } from '@/services/transaction.service';
import { toast } from 'sonner';

interface CorporateDepositModalProps {
  isOpen: boolean;
  onClose: () => void;
  accountNumber: string; // Ekranda göstermek için
  iban: string;          // Backend'e (DepositRequest) göndermek için
  currency: string;
  onSuccess: () => void; // İşlem bitince sayfadaki verileri yenilemek için
}

export default function CorporateDepositModal({ 
  isOpen, 
  onClose, 
  accountNumber, 
  iban, 
  currency, 
  onSuccess 
}: CorporateDepositModalProps) {
  const [amount, setAmount] = useState<number | ''>('');
  const [isProcessing, setIsProcessing] = useState(false);

  if (!isOpen) return null;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!amount || amount <= 0) {
      toast.error("Geçersiz Tutar", { description: "Lütfen yatırmak istediğiniz geçerli bir tutar girin." });
      return;
    }

    try {
      setIsProcessing(true);
      
      // 🚀 MOTOR ATEŞLENİYOR (DepositRequest arayüzüne tam uyumlu)
      await transactionService.deposit({
        iban: iban, 
        amount: Number(amount)
      });
      
      toast.success("İşlem Başarılı", { description: `Kasaya ${amount} ${currency} aktarıldı.` });
      setAmount(''); // Formu temizle
      onSuccess();   // Sayfadaki fetchAccounts'u tetikle
      onClose();     // Modalı kapat
    } catch (error) {
      console.error("Para yatırma hatası:", error);
      toast.error("İşlem Başarısız", { description: "Kasaya para aktarılırken bir sorun oluştu." });
    } finally {
      setIsProcessing(false);
    }
  };

  return (
    <div className="fixed inset-0 bg-black/60 z-50 flex items-center justify-center p-4 backdrop-blur-sm">
      <div className="bg-white rounded-xl shadow-2xl w-full max-w-md overflow-hidden transform transition-all">
        
        {/* BAŞLIK */}
        <div className="p-6 border-b border-gray-100 flex justify-between items-center bg-emerald-50">
          <div className="flex items-center gap-3">
            <div className="bg-emerald-100 text-emerald-600 p-2 rounded-lg">
              <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 6v6m0 0v6m0-6h6m-6 0H6" />
              </svg>
            </div>
            <h2 className="text-xl font-bold text-gray-800">Kasaya Fon Aktar</h2>
          </div>
          <button onClick={onClose} disabled={isProcessing} className="text-gray-400 hover:text-gray-600 transition-colors">
            <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>

        {/* FORM GÖVDESİ */}
        <form onSubmit={handleSubmit} className="p-6">
          <div className="mb-4">
            <label className="block text-sm font-semibold text-gray-700 mb-1">Hedef Kasa</label>
            <div className="p-3 bg-gray-50 rounded-lg border border-gray-200 font-mono text-sm text-gray-600">
              {accountNumber}
              <div className="text-xs text-gray-400 mt-1">{iban}</div>
            </div>
          </div>

          <div className="mb-6">
            <label className="block text-sm font-semibold text-gray-700 mb-2">Yatırılacak Tutar ({currency})</label>
            <div className="relative">
              <input
                type="number"
                min="1"
                step="0.01"
                value={amount}
                onChange={(e) => setAmount(e.target.value === '' ? '' : Number(e.target.value))}
                disabled={isProcessing}
                placeholder="0.00"
                className="w-full border border-gray-300 rounded-lg p-3 pl-4 pr-16 text-gray-900 focus:ring-2 focus:ring-emerald-500 focus:border-emerald-500 outline-none font-bold text-lg"
              />
              <span className="absolute right-4 top-1/2 -translate-y-1/2 text-gray-500 font-bold">{currency}</span>
            </div>
          </div>

          <div className="flex justify-end gap-3 mt-8">
            <button 
              type="button" 
              onClick={onClose} 
              disabled={isProcessing} 
              className="px-5 py-2.5 text-sm font-medium text-gray-700 bg-gray-100 hover:bg-gray-200 rounded-lg transition-colors disabled:opacity-50"
            >
              İptal
            </button>
            <button 
              type="submit" 
              disabled={isProcessing || !amount} 
              className="px-5 py-2.5 text-sm font-medium text-white bg-emerald-600 hover:bg-emerald-700 rounded-lg transition-colors flex items-center gap-2 disabled:opacity-50 shadow-md shadow-emerald-500/30"
            >
              {isProcessing ? (
                <>
                  <svg className="animate-spin h-4 w-4 text-white" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24"><circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle><path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path></svg>
                  İşleniyor...
                </>
              ) : 'Aktarımı Onayla'}
            </button>
          </div>
        </form>

      </div>
    </div>
  );
}