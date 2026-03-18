import { useState } from "react";
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription, DialogFooter } from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Label } from "@/components/ui/label";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Loader2, PlusCircle, WalletCards } from "lucide-react";
import { adminService } from "@/services/admin.service";
import { toast } from "sonner";

interface OpenAccountModalProps {
  isOpen: boolean;
  onOpenChange: (open: boolean) => void;
  // 🚀 V2: tcNo yerine identityNumber
  identityNumber: string;
  onSuccess: (newAccount: any) => void;
}

export function OpenAccountModal({ isOpen, onOpenChange, identityNumber, onSuccess }: OpenAccountModalProps) {
  const [loading, setLoading] = useState(false);
  const [currency, setCurrency] = useState("TRY");

  const handleSubmit = async () => {
    try {
      setLoading(true);
      // 🚀 V2: Servis artık identityNumber bekliyor
      const newAccount = await adminService.openAccountForCustomer(identityNumber, currency);
      
      toast.success("Hesap Başarıyla Açıldı", {
        description: `${newAccount.iban} numaralı ${newAccount.currency} hesabı oluşturuldu.`
      });
      
      onSuccess(newAccount); 
      onOpenChange(false);
      setCurrency("TRY"); 
    } catch (error: any) {
      const backendMessage = error.response?.data?.message || "Hesap açılamadı.";
      toast.error("İşlem Başarısız", { description: backendMessage });
    } finally {
      setLoading(false);
    }
  };

  return (
    <Dialog open={isOpen} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2">
            <WalletCards className="w-5 h-5 text-blue-600" />
            Yeni Hesap Açılışı
          </DialogTitle>
          <DialogDescription>
            {/* 🚀 V2: identityNumber gösterimi */}
            <strong>{identityNumber}</strong> kimlik/vergi numaralı müşteri için yeni bir banka hesabı oluşturuyorsunuz.
          </DialogDescription>
        </DialogHeader>

        <div className="space-y-4 py-4">
          <div className="space-y-2">
            <Label>Döviz Cinsi</Label>
            <Select value={currency} onValueChange={setCurrency} disabled={loading}>
              <SelectTrigger>
                <SelectValue placeholder="Para birimi seçin" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="TRY">
                  <div className="flex items-center gap-2 font-bold"><span className="text-slate-500 font-normal">TRY</span> Türk Lirası</div>
                </SelectItem>
                <SelectItem value="USD">
                  <div className="flex items-center gap-2 font-bold"><span className="text-slate-500 font-normal">USD</span> Amerikan Doları</div>
                </SelectItem>
                <SelectItem value="EUR">
                  <div className="flex items-center gap-2 font-bold"><span className="text-slate-500 font-normal">EUR</span> Euro</div>
                </SelectItem>
              </SelectContent>
            </Select>
          </div>
        </div>

        <DialogFooter>
          <Button variant="ghost" onClick={() => onOpenChange(false)} disabled={loading}>
            İptal
          </Button>
          <Button 
            onClick={handleSubmit} 
            disabled={loading} 
            className="bg-blue-600 hover:bg-blue-700"
          >
            {loading ? <Loader2 className="w-4 h-4 mr-2 animate-spin" /> : <PlusCircle className="w-4 h-4 mr-2" />}
            {loading ? "Açılıyor..." : "Hesabı Oluştur"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}