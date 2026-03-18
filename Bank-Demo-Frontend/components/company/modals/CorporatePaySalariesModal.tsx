import React, { useState } from 'react';
import { AccountResponse, CompanyEmployeeResponse } from '@/types';
import { toast } from 'sonner';

interface CorporatePaySalariesModalProps {
  isOpen: boolean;
  onClose: () => void;
  accounts: AccountResponse[];
  employees: CompanyEmployeeResponse[];
  onConfirm: (senderIban: string) => Promise<void>;
  isProcessing: boolean;
}

export default function CorporatePaySalariesModal({
  isOpen,
  onClose,
  accounts,
  employees,
  onConfirm,
  isProcessing
}: CorporatePaySalariesModalProps) {
  const [selectedIban, setSelectedIban] = useState<string>('');

  if (!isOpen) return null;

  // Sadece aktif olan kasaları (hesapları) filtrele
  const activeAccounts = accounts.filter(acc => acc.isActive !== false && (acc as any).active !== false);

  // Toplam personel sayısı ve TRY bazında toplam maaş yükü
  const totalEmployees = employees.length;
  const totalSalaryTRY = employees.reduce((sum, emp) => sum + emp.salaryAmount, 0);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedIban) {
      toast.error("Kasa Seçilmedi", { description: "Lütfen ödemenin yapılacağı kasayı seçin." });
      return;
    }
    if (totalEmployees === 0) {
      toast.error("Personel Yok", { description: "Şirketinize kayıtlı personel bulunmuyor." });
      return;
    }

    // 🚀 Bakiye kontrolünü TAMAMEN Backend'e bıraktık! 
    // Eğer bakiye yetmezse Backend Exception fırlatacak, hook'umuz bunu yakalayıp ekrana basacak.
    await onConfirm(selectedIban);
  };

  return (
    <div className="fixed inset-0 bg-black/60 z-50 flex items-center justify-center p-4 backdrop-blur-sm">
      <div className="bg-white rounded-xl shadow-2xl w-full max-w-lg overflow-hidden transform transition-all">
        
        {/* BAŞLIK */}
        <div className="p-6 border-b border-gray-100 flex justify-between items-center bg-emerald-50">
          <div className="flex items-center gap-3">
            <div className="bg-emerald-100 text-emerald-600 p-2 rounded-lg shadow-sm">
              <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8c-1.657 0-3 .895-3 2s1.343 2 3 2 3 .895 3 2-1.343 2-3 2m0-8c1.11 0 2.08.402 2.599 1M12 8V7m0 1v8m0 0v1m0-1c-1.11 0-2.08-.402-2.599-1M21 12a9 9 0 11-18 0 9 9 0 0118 0z" /></svg>
            </div>
            <div>
              <h2 className="text-xl font-bold text-gray-800">Toplu Maaş Dağıtımı</h2>
              <p className="text-xs text-emerald-600 font-medium mt-0.5">Otomatik Transfer Motoru</p>
            </div>
          </div>
          <button onClick={onClose} disabled={isProcessing} className="text-gray-400 hover:text-gray-600 transition-colors">
            <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" /></svg>
          </button>
        </div>

        {/* FORM GÖVDESİ */}
        <form onSubmit={handleSubmit} className="p-6 space-y-5">
          
          {/* ÖZET PANELİ */}
          <div className="bg-slate-50 border border-slate-200 rounded-lg p-4 flex justify-between items-start shadow-inner">
            <div>
              <p className="text-sm font-semibold text-slate-500 mb-1">Toplam Personel</p>
              <p className="text-2xl font-bold text-slate-800">{totalEmployees} <span className="text-sm font-medium">Kişi</span></p>
            </div>
            <div className="text-right">
              <p className="text-sm font-semibold text-slate-500 mb-1">Ödenecek Toplam Tutar</p>
              <p className="text-2xl font-black text-emerald-600">{totalSalaryTRY.toLocaleString('tr-TR')} <span className="text-sm">TL</span></p>
              {/* 🚀 DÖVİZ BİLGİLENDİRMESİ BURAYA EKLENDİ */}
              <p className="text-[10px] text-slate-400 mt-1.5 leading-tight max-w-45 ml-auto">
                (USD/EUR gibi farklı döviz kasalarından yapılan ödemelerde <b>anlık canlı kur</b> uygulanarak bakiyeden düşülür.)
              </p>
            </div>
          </div>

          {/* KASA SEÇİMİ */}
          <div>
            <label className="block text-sm font-semibold text-gray-700 mb-2">Çıkış Yapılacak Kasa (Hesap)</label>
            <select
              value={selectedIban}
              onChange={(e) => setSelectedIban(e.target.value)}
              disabled={isProcessing || totalEmployees === 0}
              className="w-full border border-gray-300 focus:ring-emerald-500 rounded-lg p-3 text-gray-900 focus:ring-2 outline-none font-medium appearance-none"
            >
              <option value="">-- Lütfen Maaşların Çıkacağı Kasayı Seçin --</option>
              {activeAccounts.map(acc => (
                <option key={acc.id} value={acc.iban}>
                  {acc.accountNumber} - Bakiye: {acc.balance.toLocaleString('tr-TR')} {acc.currency}
                </option>
              ))}
            </select>
            
            {activeAccounts.length === 0 && (
              <p className="text-xs text-amber-600 font-semibold mt-2">Aktif bir kasanız bulunmuyor.</p>
            )}
          </div>

          <div className="bg-blue-50 border-l-4 border-blue-500 p-3 text-sm text-blue-800 rounded-r">
            <strong>Bilgi:</strong> 50.000 TL üzeri olan maaş ödemeleri Banka/MASAK onayına takılabilir. Diğer personellerin ödemeleri anında hesaplarına geçecektir.
          </div>

          {/* AKSİYON BUTONLARI */}
          <div className="flex justify-end gap-3 mt-6 pt-4 border-t border-gray-100">
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
              disabled={isProcessing || !selectedIban || totalEmployees === 0} 
              className="px-5 py-2.5 text-sm font-bold text-white bg-emerald-600 hover:bg-emerald-700 rounded-lg transition-colors flex items-center gap-2 disabled:opacity-50 shadow-md shadow-emerald-500/30"
            >
              {isProcessing ? (
                <>
                  <svg className="animate-spin h-4 w-4 text-white" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24"><circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle><path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path></svg>
                  Dağıtılıyor...
                </>
              ) : 'Maaşları Onayla ve Dağıt'}
            </button>
          </div>
        </form>

      </div>
    </div>
  );
}