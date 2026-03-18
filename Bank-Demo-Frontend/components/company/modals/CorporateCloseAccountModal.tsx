import React from 'react';

interface CorporateCloseAccountModalProps {
  accountToClose: string | null;
  isClosing: boolean;
  onConfirm: () => void;
  onCancel: () => void;
}

export default function CorporateCloseAccountModal({
  accountToClose,
  isClosing,
  onConfirm,
  onCancel,
}: CorporateCloseAccountModalProps) {
  if (!accountToClose) return null;

  return (
    <div className="fixed inset-0 bg-black/60 z-60 flex items-center justify-center p-4 backdrop-blur-sm">
      <div className="bg-white rounded-xl shadow-2xl w-full max-w-md overflow-hidden transform transition-all animate-in zoom-in-95 duration-200">
        
        {/* KIRMIZI UYARI BAŞLIĞI */}
        <div className="p-6 border-b border-red-100 bg-red-50 flex flex-col items-center text-center">
          <div className="bg-red-100 text-red-600 p-4 rounded-full mb-4 shadow-sm border border-red-200">
            <svg className="w-8 h-8" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2.5} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
            </svg>
          </div>
          <h2 className="text-xl font-extrabold text-red-800">Kasayı Kapatma Onayı</h2>
          <p className="text-sm text-red-600 font-medium mt-1">Bu işlem geri alınamaz!</p>
        </div>

        {/* İÇERİK */}
        <div className="p-6 text-center">
          <p className="text-gray-600 mb-4">
            <span className="font-mono font-bold text-gray-900 bg-gray-100 px-2 py-1 rounded">{accountToClose}</span> numaralı kurumsal kasanızı kapatmak üzeresiniz.
          </p>
          <div className="bg-yellow-50 border-l-4 border-yellow-500 p-3 text-left text-sm text-yellow-800 rounded-r">
            <strong>Önemli Not:</strong> Kasayı kapatabilmeniz için içindeki bakiyenin tamamen <strong>0.00</strong> olması gerekmektedir. Aksi takdirde işlem banka tarafından reddedilecektir.
          </div>
        </div>

        {/* BUTONLAR */}
        <div className="p-4 bg-gray-50 flex justify-end gap-3 border-t border-gray-100">
          <button
            onClick={onCancel}
            disabled={isClosing}
            className="px-5 py-2.5 text-sm font-semibold text-gray-700 bg-white border border-gray-300 hover:bg-gray-50 rounded-lg transition-colors disabled:opacity-50"
          >
            Vazgeç
          </button>
          <button
            onClick={onConfirm}
            disabled={isClosing}
            className="px-5 py-2.5 text-sm font-semibold text-white bg-red-600 hover:bg-red-700 rounded-lg transition-colors flex items-center gap-2 disabled:opacity-50 shadow-md shadow-red-500/30"
          >
            {isClosing ? (
              <>
                <svg className="animate-spin h-4 w-4 text-white" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24"><circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle><path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path></svg>
                Kapatılıyor...
              </>
            ) : (
              'Evet, Kasayı Kapat'
            )}
          </button>
        </div>

      </div>
    </div>
  );
}