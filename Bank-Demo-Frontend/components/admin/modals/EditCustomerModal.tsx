import { useEffect, useState } from "react";
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription } from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Loader2, Save } from "lucide-react";

interface EditCustomerModalProps {
  isOpen: boolean;
  onOpenChange: (open: boolean) => void;
  customer: any; 
  // 🚀 V2: tcNo ve fullName yerini identityNumber ve profileName aldı
  onUpdate: (identityNumber: string, updatedData: { profileName: string; email: string }) => Promise<void>;
}

export function EditCustomerModal({ isOpen, onOpenChange, customer, onUpdate }: EditCustomerModalProps) {
  const [loading, setLoading] = useState(false);
  // 🚀 V2: fullName -> profileName
  const [formData, setFormData] = useState({ profileName: "", email: "" });

  useEffect(() => {
    if (customer) {
      setFormData({
        profileName: customer.profileName || "",
        email: customer.email || "",
      });
    }
  }, [customer]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!customer) return;

    try {
      setLoading(true);
      // 🚀 V2: customer.tcNo yerine customer.identityNumber fırlatıyoruz
      await onUpdate(customer.identityNumber, formData);
      onOpenChange(false); 
    } catch (error) {
      // Hata olursa modal kapanmaz
    } finally {
      setLoading(false);
    }
  };

  return (
    <Dialog open={isOpen} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <DialogTitle className="text-xl font-bold">Müşteri Profilini Düzenle</DialogTitle>
          <DialogDescription>
            {/* 🚀 V2: identityNumber gösterimi */}
            {customer?.identityNumber} kimlik/vergi numaralı müşterinin bilgilerini güncelliyorsunuz.
          </DialogDescription>
        </DialogHeader>

        <form onSubmit={handleSubmit} className="space-y-4 mt-4">
          <div className="space-y-2">
            <Label>Ad Soyad / Şirket Ünvanı</Label>
            {/* 🚀 V2: profileName inputu */}
            <Input 
              value={formData.profileName} 
              onChange={(e) => setFormData({...formData, profileName: e.target.value})} 
              required
              disabled={loading}
            />
          </div>
          <div className="space-y-2">
            <Label>E-posta Adresi</Label>
            <Input 
              type="email" 
              value={formData.email} 
              onChange={(e) => setFormData({...formData, email: e.target.value})} 
              required
              disabled={loading}
            />
          </div>

          <div className="flex justify-end gap-3 pt-4 border-t">
            <Button type="button" variant="outline" onClick={() => onOpenChange(false)} disabled={loading}>
              İptal
            </Button>
            <Button type="submit" className="bg-orange-600 hover:bg-orange-700" disabled={loading}>
              {loading ? <Loader2 className="w-4 h-4 mr-2 animate-spin" /> : <Save className="w-4 h-4 mr-2" />}
              {loading ? "Kaydediliyor..." : "Değişiklikleri Kaydet"}
            </Button>
          </div>
        </form>
      </DialogContent>
    </Dialog>
  );
}