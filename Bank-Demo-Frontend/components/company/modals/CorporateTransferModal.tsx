import React, { useState } from 'react';
import { transactionService } from '@/services/transaction.service';
import { toast } from 'sonner';

interface CorporateTransferModalProps {
  isOpen: boolean;
  onClose: () => void;
  senderIban: string;
  accountNumber: string; // Sadece ekranda göstermek için
  currency: string;
  onSuccess: () => void;
}

export default function CorporateTransferModal({ 
  isOpen, 
  onClose, 
  senderIban, 
  accountNumber,
  currency, 
  onSuccess 
}: CorporateTransferModalProps) {
  const [receiverIban, setReceiverIban] = useState('');
  const [amount, setAmount] = useState<number | ''>('');
  const [description, setDescription] = useState('');
  const [isProcessing, setIsProcessing] = useState(false);

  if (!isOpen) return null;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    
    if (!receiverIban || receiverIban.length < 10) {
      toast.error("Geçersiz IBAN", { description: "Lütfen geçerli bir alıcı IBAN'ı girin." });
      return;
    }

    if (!amount || amount <= 0) {
      toast.error("Geçersiz Tutar", { description: "Lütfen transfer edilecek geçerli bir tutar girin." });
      return;
    }

    try {
      setIsProcessing(true);
      
      // 🚀 MOTOR ATEŞLENİYOR (TransferRequest Tipine %100 Uyumlu)
      await transactionService.transfer({
        senderIban: senderIban,
        receiverIban: receiverIban.replace(/\s+/g, ''), // Boşlukları temizleyerek gönderiyoruz
        amount: Number(amount),
        description: description || undefined // Boşsa undefined gitsin
      });
      
      toast.success("Transfer Başarılı", { description: `${amount} ${currency} tutarındaki transfer emriniz işleme alındı.` });
      
      // Formu Temizle
      setReceiverIban('');
      setAmount('');
      setDescription('');
      
      onSuccess();
      onClose();
    } catch (error: any) {
      console.error("Transfer hatası:", error);
      toast.error("İşlem Başarısız", { 
        description: error?.response?.data?.message || "Transfer işlemi sırasında bir sorun oluştu. Bakiyenizi kontrol edin." 
      });
    } finally {
      setIsProcessing(false);
    }
  };

  return (
    <div className="fixed inset-0 bg-black/60 z-50 flex items-center justify-center p-4 backdrop-blur-sm">
      <div className="bg-white rounded-xl shadow-2xl w-full max-w-lg overflow-hidden transform transition-all">
        
        {/* BAŞLIK */}
        <div className="p-6 border-b border-gray-100 flex justify-between items-center bg-indigo-50">
          <div className="flex items-center gap-3">
            <div className="bg-indigo-100 text-indigo-600 p-2 rounded-lg">
              <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8 7h12m0 0l-4-4m4 4l-4 4m0 6H4m0 0l4 4m-4-4l4-4" /></svg>
            </div>
            <div>
              <h2 className="text-xl font-bold text-gray-800">Kurumsal Para Transferi</h2>
              <p className="text-xs text-indigo-600 font-medium mt-0.5">Havale / EFT / FAST İşlemleri</p>
            </div>
          </div>
          <button onClick={onClose} disabled={isProcessing} className="text-gray-400 hover:text-gray-600 transition-colors">
            <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" /></svg>
          </button>
        </div>

        {/* FORM GÖVDESİ */}
        <form onSubmit={handleSubmit} className="p-6">
          
          {/* Gönderen Kasa (Kilitli) */}
          <div className="mb-5">
            <label className="block text-sm font-semibold text-gray-700 mb-1">Çıkış Yapılacak Kasa</label>
            <div className="p-3 bg-gray-50 rounded-lg border border-gray-200 font-mono text-sm text-gray-500 flex justify-between items-center">
              <div>
                <span className="text-gray-800 font-bold block mb-0.5">{accountNumber}</span>
                {senderIban}
              </div>
              <span className="bg-gray-200 text-gray-600 text-xs px-2 py-1 rounded font-bold">{currency}</span>
            </div>
          </div>

          {/* Alıcı IBAN */}
          <div className="mb-5">
            <label className="block text-sm font-semibold text-gray-700 mb-2">Alıcı IBAN</label>
            <input
              type="text"
              value={receiverIban}
              onChange={(e) => setReceiverIban(e.target.value)}
              disabled={isProcessing}
              placeholder="TR..."
              className="w-full border border-gray-300 rounded-lg p-3 text-gray-900 focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 outline-none font-mono uppercase tracking-wider"
            />
          </div>

          <div className="grid grid-cols-2 gap-4 mb-5">
            {/* Tutar */}
            <div>
              <label className="block text-sm font-semibold text-gray-700 mb-2">Tutar</label>
              <div className="relative">
                <input
                  type="number"
                  min="0.01"
                  step="0.01"
                  value={amount}
                  onChange={(e) => setAmount(e.target.value === '' ? '' : Number(e.target.value))}
                  disabled={isProcessing}
                  placeholder="0.00"
                  className="w-full border border-gray-300 rounded-lg p-3 pr-12 text-gray-900 focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 outline-none font-bold"
                />
                <span className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-500 font-bold text-sm">{currency}</span>
              </div>
            </div>

            {/* Açıklama */}
            <div>
              <label className="block text-sm font-semibold text-gray-700 mb-2">Açıklama <span className="text-gray-400 font-normal">(İsteğe Bağlı)</span></label>
              <input
                type="text"
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                disabled={isProcessing}
                maxLength={50}
                placeholder="Örn: Kira Ödemesi"
                className="w-full border border-gray-300 rounded-lg p-3 text-gray-900 focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 outline-none"
              />
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
              disabled={isProcessing || !amount || !receiverIban} 
              className="px-5 py-2.5 text-sm font-medium text-white bg-indigo-600 hover:bg-indigo-700 rounded-lg transition-colors flex items-center gap-2 disabled:opacity-50 shadow-md shadow-indigo-500/30"
            >
              {isProcessing ? (
                <>
                  <svg className="animate-spin h-4 w-4 text-white" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24"><circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle><path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path></svg>
                  İşleniyor...
                </>
              ) : 'Transferi Onayla'}
            </button>
          </div>
        </form>

      </div>
    </div>
  );
}