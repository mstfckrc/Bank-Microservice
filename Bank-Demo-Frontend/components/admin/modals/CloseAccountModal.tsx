import { Button } from "@/components/ui/button";

interface CloseAccountModalProps {
  accountToClose: string | null;
  isClosing: boolean;
  onClose: () => void;
  onConfirm: () => void;
}

export function CloseAccountModal({ accountToClose, isClosing, onClose, onConfirm }: CloseAccountModalProps) {
  if (!accountToClose) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/40 backdrop-blur-sm">
      <div className="bg-white rounded-2xl shadow-2xl w-full max-w-md p-6 animate-in fade-in zoom-in-95 duration-200">
        <h3 className="text-xl font-black text-slate-900 mb-2">Hesap Kapatma Onayı</h3>
        <p className="text-sm text-slate-500 mb-6 leading-relaxed">
          <strong className="text-slate-800">{accountToClose}</strong> numaralı hesabı kapatmak üzeresiniz. Bakiye 0.00 değilse işlem reddedilecektir. Emin misiniz?
        </p>
        
        <div className="flex gap-3 justify-end">
          <Button
            variant="outline"
            className="font-bold border-slate-200"
            onClick={onClose}
            disabled={isClosing}
          >
            İptal Et
          </Button>
          <Button
            variant="destructive"
            className="font-bold bg-red-500 hover:bg-red-600"
            onClick={onConfirm}
            disabled={isClosing}
          >
            {isClosing ? "Kapatılıyor..." : "Evet, Hesabı Kapat"}
          </Button>
        </div>
      </div>
    </div>
  );
}