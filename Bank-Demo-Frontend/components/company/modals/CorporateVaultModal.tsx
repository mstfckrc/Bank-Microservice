import React, { useState } from 'react';

interface CorporateVaultModalProps {
  isOpen: boolean;
  onClose: () => void;
  onCreate: (currency: "TRY" | "USD" | "EUR") => Promise<boolean>;
  isProcessing: boolean;
}

export default function CorporateVaultModal({ isOpen, onClose, onCreate, isProcessing }: CorporateVaultModalProps) {
  const [currency, setCurrency] = useState<"TRY" | "USD" | "EUR">("TRY");

  if (!isOpen) return null;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    const success = await onCreate(currency);
    if (success) {
      onClose();
      setCurrency("TRY"); // İşlem başarılıysa bir sonraki açılış için sıfırla
    }
  };

  return (
    <div className="fixed inset-0 bg-black/60 z-50 flex items-center justify-center p-4 backdrop-blur-sm">
      <div className="bg-white rounded-xl shadow-2xl w-full max-w-md overflow-hidden transform transition-all">
        
        {/* MODAL BAŞLIĞI */}
        <div className="p-6 border-b border-gray-100 flex justify-between items-center bg-slate-50">
          <div className="flex items-center gap-3">
            <div className="bg-indigo-100 text-indigo-600 p-2 rounded-lg">
              <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8c-1.657 0-3 .895-3 2s1.343 2 3 2 3 .895 3 2-1.343 2-3 2m0-8c1.11 0 2.08.402 2.599 1M12 8V7m0 1v8m0 0v1m0-1c-1.11 0-2.08-.402-2.599-1M21 12a9 9 0 11-18 0 9 9 0 0118 0z" /></svg>
            </div>
            <h2 className="text-xl font-bold text-gray-800">Yeni Kurumsal Kasa</h2>
          </div>
          <button 
            onClick={onClose} 
            disabled={isProcessing} 
            className="text-gray-400 hover:text-gray-600 transition-colors disabled:opacity-50"
          >
            <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" /></svg>
          </button>
        </div>

        {/* MODAL GÖVDESİ */}
        <form onSubmit={handleSubmit} className="p-6">
          <div className="mb-6">
            <label className="block text-sm font-semibold text-gray-700 mb-2">Kasa Para Birimi</label>
            <select
              value={currency}
              onChange={(e) => setCurrency(e.target.value as "TRY" | "USD" | "EUR")}
              disabled={isProcessing}
              className="w-full border border-gray-300 rounded-lg p-3 text-gray-700 focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 outline-none transition-all font-medium bg-white"
            >
              <option value="TRY">TRY - Türk Lirası</option>
              <option value="USD">USD - Amerikan Doları</option>
              <option value="EUR">EUR - Euro</option>
            </select>
            
            {/* Şirkete Özel Bilgi Notu */}
            <div className="bg-indigo-50 border-l-4 border-indigo-500 p-4 rounded-r mt-5 text-indigo-800 text-sm">
              <p className="font-semibold flex items-center gap-2">
                <svg className="w-4 h-4" fill="currentColor" viewBox="0 0 20 20"><path fillRule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7-4a1 1 0 11-2 0 1 1 0 012 0zM9 9a1 1 0 000 2v3a1 1 0 001 1h1a1 1 0 100-2v-3a1 1 0 00-1-1H9z" clipRule="evenodd" /></svg>
                Operasyonel Bilgi
              </p>
              <p className="mt-1 ml-6 text-indigo-700/80">Bu kasa, şirketinizin personel maaş ödemeleri ve ticari fon aktarımları (EFT/Havale) için ana merkez olarak kullanılacaktır.</p>
            </div>
          </div>

          {/* BUTONLAR */}
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
              disabled={isProcessing}
              className="px-5 py-2.5 text-sm font-medium text-white bg-indigo-600 hover:bg-indigo-700 rounded-lg transition-colors flex items-center gap-2 disabled:opacity-50 shadow-md shadow-indigo-500/30"
            >
              {isProcessing ? (
                <>
                  <svg className="animate-spin h-4 w-4 text-white" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24"><circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle><path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path></svg>
                  Kasa Açılıyor...
                </>
              ) : 'Kasayı Oluştur'}
            </button>
          </div>
        </form>

      </div>
    </div>
  );
}