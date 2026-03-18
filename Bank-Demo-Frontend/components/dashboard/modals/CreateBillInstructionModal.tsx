import React, { useState } from 'react';
import { AccountResponse } from '@/types';
import { X, Receipt, Zap, Droplets, Wifi, Flame } from 'lucide-react';
import { toast } from 'sonner';

interface CreateBillInstructionModalProps {
  isOpen: boolean;
  onOpenChange: (open: boolean) => void;
  accounts: AccountResponse[];
  onCreate: (data: { accountId: number; billType: string; subscriberNo: string; paymentDay: number }) => Promise<void>;
  isProcessing: boolean;
}

export function CreateBillInstructionModal({
  isOpen,
  onOpenChange,
  accounts,
  onCreate,
  isProcessing
}: CreateBillInstructionModalProps) {
  
  const [accountId, setAccountId] = useState<number | ''>('');
  const [billType, setBillType] = useState<string>('');
  const [subscriberNo, setSubscriberNo] = useState('');
  const [paymentDay, setPaymentDay] = useState<number | ''>('');

  if (!isOpen) return null;

  // Sadece aktif hesapları filtrele
  const activeAccounts = accounts.filter(acc => acc.isActive !== false && (acc as any).active !== false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!accountId || !billType || !subscriberNo || !paymentDay) {
      toast.error("Eksik Bilgi", { description: "Lütfen tüm alanları doldurunuz." });
      return;
    }
    
    if (paymentDay < 1 || paymentDay > 31) {
      toast.error("Geçersiz Gün", { description: "Ödeme günü 1 ile 31 arasında olmalıdır." });
      return;
    }

    await onCreate({
      accountId: Number(accountId),
      billType,
      subscriberNo,
      paymentDay: Number(paymentDay)
    });

    // Formu sıfırla
    setAccountId('');
    setBillType('');
    setSubscriberNo('');
    setPaymentDay('');
  };

  return (
    <div className="fixed inset-0 bg-black/60 z-50 flex items-center justify-center p-4 backdrop-blur-sm transition-opacity">
      <div className="bg-white rounded-xl shadow-2xl w-full max-w-md overflow-hidden transform transition-all animate-in fade-in zoom-in-95 duration-200">
        
        {/* ÜST BİLGİ */}
        <div className="p-5 border-b border-slate-100 flex justify-between items-center bg-slate-50">
          <div className="flex items-center gap-3">
            <div className="bg-indigo-100 text-indigo-600 p-2 rounded-lg shadow-sm">
              <Receipt className="w-5 h-5" />
            </div>
            <div>
              <h2 className="text-lg font-bold text-slate-800">Otomatik Fatura Talimatı</h2>
              <p className="text-xs text-slate-500 font-medium mt-0.5">Her ay otomatik ödensin</p>
            </div>
          </div>
          <button onClick={() => onOpenChange(false)} disabled={isProcessing} className="text-slate-400 hover:text-slate-600 transition-colors">
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* FORM */}
        <form onSubmit={handleSubmit} className="p-5 space-y-4">
          
          {/* Fatura Tipi */}
          <div>
            <label className="block text-sm font-semibold text-slate-700 mb-2">Fatura Tipi</label>
            <div className="grid grid-cols-2 gap-2">
              <button type="button" onClick={() => setBillType('ELECTRICITY')} className={`flex items-center gap-2 p-2 border rounded-lg text-sm font-medium transition-colors ${billType === 'ELECTRICITY' ? 'bg-yellow-50 border-yellow-400 text-yellow-700' : 'hover:bg-slate-50 text-slate-600 border-slate-200'}`}>
                <Zap className="w-4 h-4" /> Elektrik
              </button>
              <button type="button" onClick={() => setBillType('WATER')} className={`flex items-center gap-2 p-2 border rounded-lg text-sm font-medium transition-colors ${billType === 'WATER' ? 'bg-blue-50 border-blue-400 text-blue-700' : 'hover:bg-slate-50 text-slate-600 border-slate-200'}`}>
                <Droplets className="w-4 h-4" /> Su
              </button>
              <button type="button" onClick={() => setBillType('INTERNET')} className={`flex items-center gap-2 p-2 border rounded-lg text-sm font-medium transition-colors ${billType === 'INTERNET' ? 'bg-purple-50 border-purple-400 text-purple-700' : 'hover:bg-slate-50 text-slate-600 border-slate-200'}`}>
                <Wifi className="w-4 h-4" /> İnternet
              </button>
              <button type="button" onClick={() => setBillType('GAS')} className={`flex items-center gap-2 p-2 border rounded-lg text-sm font-medium transition-colors ${billType === 'GAS' ? 'bg-orange-50 border-orange-400 text-orange-700' : 'hover:bg-slate-50 text-slate-600 border-slate-200'}`}>
                <Flame className="w-4 h-4" /> Doğalgaz
              </button>
            </div>
          </div>

          {/* Abone No & Ödeme Günü */}
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-sm font-semibold text-slate-700 mb-1">Abone / Tesisat No</label>
              <input type="text" value={subscriberNo} onChange={(e) => setSubscriberNo(e.target.value)} disabled={isProcessing} placeholder="Örn: 98765432" className="w-full border border-slate-300 focus:ring-indigo-500 rounded-lg p-2.5 text-slate-900 focus:ring-2 outline-none text-sm" />
            </div>
            <div>
              <label className="block text-sm font-semibold text-slate-700 mb-1">Ödeme Günü</label>
              <input type="number" min="1" max="31" value={paymentDay} onChange={(e) => setPaymentDay(Number(e.target.value))} disabled={isProcessing} placeholder="Ayın Kaçı? (1-31)" className="w-full border border-slate-300 focus:ring-indigo-500 rounded-lg p-2.5 text-slate-900 focus:ring-2 outline-none text-sm" />
            </div>
          </div>

          {/* Kasa (Hesap) Seçimi */}
          <div>
            <label className="block text-sm font-semibold text-slate-700 mb-1">Ödemenin Çekileceği Kasa</label>
            <select value={accountId} onChange={(e) => setAccountId(Number(e.target.value))} disabled={isProcessing} className="w-full border border-slate-300 focus:ring-indigo-500 rounded-lg p-2.5 text-slate-900 focus:ring-2 outline-none text-sm font-medium appearance-none">
              <option value="">-- Kasa Seçiniz --</option>
              {activeAccounts.map(acc => (
                <option key={acc.id} value={acc.id}>
                  {acc.accountNumber} - {acc.balance.toLocaleString('tr-TR')} {acc.currency}
                </option>
              ))}
            </select>
          </div>

          {/* BUTONLAR */}
          <div className="flex justify-end gap-2 mt-4 pt-4 border-t border-slate-100">
            <button type="button" onClick={() => onOpenChange(false)} disabled={isProcessing} className="px-4 py-2 text-sm font-medium text-slate-600 bg-slate-100 hover:bg-slate-200 rounded-lg transition-colors">
              İptal
            </button>
            <button type="submit" disabled={isProcessing} className="px-4 py-2 text-sm font-bold text-white bg-indigo-600 hover:bg-indigo-700 rounded-lg transition-colors flex items-center gap-2">
              {isProcessing ? 'Kaydediliyor...' : 'Talimatı Kaydet'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}