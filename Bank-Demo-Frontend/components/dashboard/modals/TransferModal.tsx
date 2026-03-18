// components/dashboard/modals/TransferModal.tsx
import { useState, useEffect } from "react";
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { AccountResponse } from "@/types";
import { Loader2, Building, User, AlertCircle } from "lucide-react";

interface TransferModalProps {
  isOpen: boolean;
  onOpenChange: (open: boolean) => void;
  accounts: AccountResponse[];
  onTransfer: (data: any) => Promise<void>;
  isProcessing: boolean;
}

export function TransferModal({ isOpen, onOpenChange, accounts, onTransfer, isProcessing }: TransferModalProps) {
  const [transferType, setTransferType] = useState<"iban" | "self">("iban");
  const [error, setError] = useState<string | null>(null); // 🚀 Hata mesajı state'i
  
  const [formData, setFormData] = useState({ 
    senderIban: "", 
    receiverIban: "", 
    amount: "", 
    description: "" 
  });

  const activeAccounts = accounts.filter(acc => (acc as any).isActive !== false && (acc as any).active !== false);

  // 🚀 Seçili gönderen hesabın tüm bilgilerini bulalım (Bakiye kontrolü için)
  const selectedSenderAccount = activeAccounts.find(acc => acc.iban === formData.senderIban);

  // 🚀 ANLIK VALIDASYON (Hata Kontrolü)
  useEffect(() => {
    const amount = parseFloat(formData.amount);

    if (formData.amount && amount <= 0) {
      setError("Transfer tutarı 0'dan büyük olmalıdır!");
    } else if (selectedSenderAccount && amount > selectedSenderAccount.balance) {
      setError(`Yetersiz bakiye! (Mevcut: ${selectedSenderAccount.balance.toLocaleString('tr-TR')} ${selectedSenderAccount.currency})`);
    } else if (formData.senderIban && formData.receiverIban && formData.senderIban === formData.receiverIban) {
      setError("Kendi hesabınıza transfer yapamazsınız!");
    } else {
      setError(null);
    }
  }, [formData, selectedSenderAccount]);

  const handleSubmit = async () => {
    if (error) return; // Hata varsa gönderme
    await onTransfer(formData);
    setFormData({ senderIban: "", receiverIban: "", amount: "", description: "" });
  };

  const handleOpenChange = (open: boolean) => {
    if (!open) {
      setFormData({ senderIban: "", receiverIban: "", amount: "", description: "" });
      setTransferType("iban");
      setError(null);
    }
    onOpenChange(open);
  };

  return (
    <Dialog open={isOpen} onOpenChange={handleOpenChange}>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2">
            Para Transferi
          </DialogTitle>
        </DialogHeader>

        <div className="grid grid-cols-2 gap-2 bg-slate-100 p-1 rounded-lg mt-2">
          <Button
            type="button"
            variant={transferType === "iban" ? "default" : "ghost"}
            className={`h-9 ${transferType === "iban" ? "bg-white text-slate-900 shadow-sm" : "text-slate-500 hover:text-slate-700"}`}
            onClick={() => {
              setTransferType("iban");
              setFormData({ ...formData, receiverIban: "" });
            }}
          >
            <Building className="w-4 h-4 mr-2" />
            IBAN'a
          </Button>
          <Button
            type="button"
            variant={transferType === "self" ? "default" : "ghost"}
            className={`h-9 ${transferType === "self" ? "bg-white text-slate-900 shadow-sm" : "text-slate-500 hover:text-slate-700"}`}
            onClick={() => {
              setTransferType("self");
              setFormData({ ...formData, receiverIban: "" });
            }}
          >
            <User className="w-4 h-4 mr-2" />
            Kendi Hesabıma
          </Button>
        </div>

        <div className="space-y-4 py-4">
          <div className="space-y-2">
            <Label>Gönderen Hesabınız</Label>
            <Select 
              value={formData.senderIban} 
              onValueChange={(val) => setFormData({ ...formData, senderIban: val })}
            >
              <SelectTrigger><SelectValue placeholder="Hesap seçin" /></SelectTrigger>
              <SelectContent>
                {activeAccounts.map((acc) => (
                  <SelectItem key={acc.id} value={acc.iban}>
                    <div className="flex justify-between items-center w-full gap-4">
                      <span className="font-bold">{acc.currency}</span>
                      <span className="text-xs text-slate-500">{acc.iban.slice(-6)}</span>
                      <span className="text-xs">{acc.balance.toLocaleString('tr-TR')}</span>
                    </div>
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          <div className="space-y-2">
            {transferType === "iban" ? (
              <>
                <Label>Alıcı IBAN</Label>
                <Input 
                  placeholder="TR..." 
                  className={error?.includes("IBAN") || (formData.receiverIban && formData.receiverIban === formData.senderIban) ? "border-red-500" : ""}
                  value={formData.receiverIban} 
                  onChange={(e) => setFormData({ ...formData, receiverIban: e.target.value })} 
                />
              </>
            ) : (
              <>
                <Label>Alıcı Hesabınız</Label>
                <Select 
                  value={formData.receiverIban} 
                  onValueChange={(val) => setFormData({ ...formData, receiverIban: val })}
                >
                  <SelectTrigger><SelectValue placeholder="Hesap seçin" /></SelectTrigger>
                  <SelectContent>
                    {activeAccounts
                      .filter(acc => acc.iban !== formData.senderIban)
                      .map((acc) => (
                      <SelectItem key={acc.id} value={acc.iban}>
                        <div className="flex justify-between items-center w-full gap-4">
                          <span className="font-bold">{acc.currency}</span>
                          <span className="text-xs text-slate-500">{acc.iban.slice(-6)}</span>
                        </div>
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </>
            )}
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div className="space-y-2">
              <Label>Tutar</Label>
              <Input 
                type="number" 
                placeholder="0.00" 
                className={error && !error.includes("IBAN") ? "border-red-500" : ""}
                value={formData.amount} 
                onChange={(e) => setFormData({ ...formData, amount: e.target.value })} 
              />
            </div>
            <div className="space-y-2">
              <Label>Açıklama</Label>
              <Input 
                placeholder="Örn: Kira" 
                value={formData.description} 
                onChange={(e) => setFormData({ ...formData, description: e.target.value })} 
              />
            </div>
          </div>

          {/* 🚀 HATA MESAJI GÖSTERİMİ */}
          {error && (
            <div className="flex items-center gap-2 text-red-600 bg-red-50 p-2.5 rounded-md border border-red-100 animate-in fade-in slide-in-from-top-1">
              <AlertCircle className="w-4 h-4 shrink-0" />
              <p className="text-xs font-semibold">{error}</p>
            </div>
          )}
        </div>

        <DialogFooter>
          <Button 
            onClick={handleSubmit} 
            disabled={isProcessing || !!error || !formData.senderIban || !formData.receiverIban || !formData.amount} 
            className="w-full bg-blue-600 hover:bg-blue-700 py-6 text-base font-bold shadow-lg shadow-blue-100"
          >
            {isProcessing ? <Loader2 className="animate-spin mr-2 h-5 w-5" /> : null}
            {isProcessing ? "İşleniyor..." : "Onayla ve Gönder"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}