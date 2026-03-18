import { Button } from "@/components/ui/button";

// 🚀 DÜZELTME: accountNo olan ismi accountToClose yaptık (Dashboard ile uyumlu)
export function CloseAccountModal({ accountToClose, isClosing, onConfirm, onCancel }: any) {
  
  // Kontrolü de yeni isme göre yapıyoruz
  if (!accountToClose) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/40 backdrop-blur-sm">
      <div className="bg-white rounded-2xl shadow-2xl w-full max-w-md p-6 animate-in fade-in zoom-in-95 duration-200">
        <h3 className="text-xl font-black text-slate-900 mb-2">Hesabı Kapat</h3>
        <p className="text-sm text-slate-500 mb-6">
          <strong className="text-slate-800">{accountToClose}</strong> numaralı hesabınızı kapatmak üzeresiniz. Emin misiniz?
        </p>
        <div className="flex gap-3 justify-end">
          <Button variant="outline" onClick={onCancel} disabled={isClosing}>İptal</Button>
          <Button variant="destructive" onClick={onConfirm} disabled={isClosing}>
            {isClosing ? "Kapatılıyor..." : "Evet, Hesabı Kapat"}
          </Button>
        </div>
      </div>
    </div>
  );
}