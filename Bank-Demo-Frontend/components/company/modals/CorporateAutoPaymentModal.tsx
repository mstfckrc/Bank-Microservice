import React, { useState, useEffect } from 'react';
import { AccountResponse, AutoPaymentSettingsRequest, AutoPaymentSettingsResponse } from '@/types';
import { toast } from 'sonner';

interface CorporateAutoPaymentModalProps {
  isOpen: boolean;
  onClose: () => void;
  accounts: AccountResponse[];
  onConfirm: (data: AutoPaymentSettingsRequest) => Promise<boolean>;
  isProcessing: boolean;
  initialData?: AutoPaymentSettingsResponse | null; // 🚀 YENİ: Mevcut ayarları alacağımız prop
}

export default function CorporateAutoPaymentModal({
  isOpen,
  onClose,
  accounts,
  onConfirm,
  isProcessing,
  initialData // 🚀 YENİ
}: CorporateAutoPaymentModalProps) {
  
  const [isEnabled, setIsEnabled] = useState<boolean>(false);
  const [selectedDay, setSelectedDay] = useState<number | ''>('');
  const [selectedIban, setSelectedIban] = useState<string>('');

  // 🚀 YENİ: Modal her açıldığında mevcut ayarları formun içine dolduran beyin!
  useEffect(() => {
    if (isOpen) {
      if (initialData && initialData.autoPaymentEnabled) {
        setIsEnabled(true);
        setSelectedDay(initialData.paymentDay);
        setSelectedIban(initialData.defaultSalaryIban);
      } else {
        setIsEnabled(false);
        setSelectedDay('');
        setSelectedIban('');
      }
    }
  }, [isOpen, initialData]);

  if (!isOpen) return null;

  const activeAccounts = accounts.filter(acc => acc.isActive !== false && (acc as any).active !== false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    
    if (isEnabled) {
      if (!selectedDay || selectedDay < 1 || selectedDay > 31) {
        toast.error("Geçersiz Gün", { description: "Lütfen 1 ile 31 arasında geçerli bir gün seçin." });
        return;
      }
      if (!selectedIban) {
        toast.error("Kasa Seçilmedi", { description: "Lütfen otomatik ödemenin çıkacağı kasayı seçin." });
        return;
      }
    }

    const success = await onConfirm({
      autoPaymentEnabled: isEnabled,
      paymentDay: isEnabled ? Number(selectedDay) : null,
      defaultSalaryIban: isEnabled ? selectedIban : "",
    });

    if (success) {
        onClose();
    }
  };

  return (
    <div className="fixed inset-0 bg-black/60 z-50 flex items-center justify-center p-4 backdrop-blur-sm transition-opacity">
      <div className="bg-white rounded-xl shadow-2xl w-full max-w-lg overflow-hidden transform transition-all">
        
        {/* BAŞLIK */}
        <div className="p-6 border-b border-gray-100 flex justify-between items-center bg-blue-50">
          <div className="flex items-center gap-3">
            <div className="bg-blue-100 text-blue-600 p-2 rounded-lg shadow-sm">
              <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" /></svg>
            </div>
            <div>
              <h2 className="text-xl font-bold text-gray-800">Otomatik Maaş Talimatı</h2>
              <p className="text-xs text-blue-600 font-medium mt-0.5">Sistem her ay personelleri otomatik öder</p>
            </div>
          </div>
          <button onClick={onClose} disabled={isProcessing} className="text-gray-400 hover:text-gray-600 transition-colors">
            <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" /></svg>
          </button>
        </div>

        {/* FORM */}
        <form onSubmit={handleSubmit} className="p-6 space-y-6">
          
          {/* SWITCH (AÇ/KAPAT) */}
          <div className="flex items-center justify-between p-4 bg-slate-50 border border-slate-200 rounded-lg">
            <div>
              <p className="font-bold text-slate-800">Otomatik Ödeme Motoru</p>
              <p className="text-xs text-slate-500 mt-1">Kapalıysa ödemeleri manuel (elle) yapmalısınız.</p>
            </div>
            <button
              type="button"
              onClick={() => setIsEnabled(!isEnabled)}
              className={`relative inline-flex h-7 w-14 items-center rounded-full transition-colors focus:outline-none ${isEnabled ? 'bg-blue-600' : 'bg-gray-300'}`}
            >
              <span className={`inline-block h-5 w-5 transform rounded-full bg-white transition-transform ${isEnabled ? 'translate-x-8' : 'translate-x-1'}`} />
            </button>
          </div>

          {/* EĞER AÇIKSA DETAYLARI GÖSTER */}
          {isEnabled && (
            <div className="space-y-5 animate-in fade-in slide-in-from-top-2 duration-300">
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-semibold text-gray-700 mb-2">Ödeme Günü (Ayın)</label>
                  <input
                    type="number"
                    min="1"
                    max="31"
                    value={selectedDay}
                    onChange={(e) => setSelectedDay(Number(e.target.value))}
                    disabled={isProcessing}
                    placeholder="Örn: 5"
                    className="w-full border border-gray-300 focus:ring-blue-500 rounded-lg p-3 text-gray-900 focus:ring-2 outline-none font-medium"
                  />
                </div>
              </div>

              <div>
                <label className="block text-sm font-semibold text-gray-700 mb-2">Çıkış Yapılacak Ana Kasa</label>
                <select
                  value={selectedIban}
                  onChange={(e) => setSelectedIban(e.target.value)}
                  disabled={isProcessing}
                  className="w-full border border-gray-300 focus:ring-blue-500 rounded-lg p-3 text-gray-900 focus:ring-2 outline-none font-medium"
                >
                  <option value="">-- Kasa Seçiniz --</option>
                  {activeAccounts.map(acc => (
                    <option key={acc.id} value={acc.iban}>
                      {acc.accountNumber} - Bakiye: {acc.balance.toLocaleString('tr-TR')} {acc.currency}
                    </option>
                  ))}
                </select>
              </div>
              
              <div className="bg-yellow-50 border-l-4 border-yellow-500 p-3 text-xs text-yellow-800 rounded-r">
                <strong>Uyarı:</strong> Otomatik ödeme gününde seçili kasanızda yeterli bakiye bulunmazsa ödemeler gerçekleşmez!
              </div>
            </div>
          )}

          {/* AKSİYON BUTONLARI */}
          <div className="flex justify-end gap-3 mt-6 pt-4 border-t border-gray-100">
            <button 
              type="button" 
              onClick={onClose} 
              disabled={isProcessing} 
              className="px-5 py-2.5 text-sm font-medium text-gray-700 bg-gray-100 hover:bg-gray-200 rounded-lg transition-colors"
            >
              İptal
            </button>
            <button 
              type="submit" 
              disabled={isProcessing} 
              className="px-5 py-2.5 text-sm font-bold text-white bg-blue-600 hover:bg-blue-700 rounded-lg transition-colors flex items-center gap-2"
            >
              {isProcessing ? 'Kaydediliyor...' : 'Ayarları Kaydet'}
            </button>
          </div>
        </form>

      </div>
    </div>
  );
}