import { useState, type FormEvent } from "react";
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription } from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Loader2, Save } from "lucide-react";
import type { UpdateProfileRequest, UserProfileResponse } from "@/types";

interface EditCustomerModalProps {
  isOpen: boolean;
  onOpenChange: (open: boolean) => void;
  customer: UserProfileResponse | null;
  onUpdate: (identityNumber: string, updatedData: UpdateProfileRequest) => Promise<void>;
}

export function EditCustomerModal(props: EditCustomerModalProps) {
  if (!props.isOpen || !props.customer) return null;
  return <EditCustomerForm {...props} customer={props.customer} key={props.customer.identityNumber} />;
}

function EditCustomerForm({ onOpenChange, customer, onUpdate }: Omit<EditCustomerModalProps, "customer"> & { customer: UserProfileResponse }) {
  const [loading, setLoading] = useState(false);
  const [formData, setFormData] = useState({
    profileName: customer.profileName,
    firstName: customer.firstName ?? "",
    lastName: customer.lastName ?? "",
    email: customer.email,
  });
  const corporate = customer.role === "CORPORATE_MANAGER";
  const editable = customer.profileEditable !== false;
  const valid = editable && formData.email.trim().length <= 100
    && /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(formData.email.trim())
    && (corporate ? formData.profileName.trim().length >= 3 && formData.profileName.length <= 150
      : formData.firstName.trim().length > 0 && formData.firstName.length <= 100
        && formData.lastName.trim().length > 0 && formData.lastName.length <= 100);

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!valid || loading) return;
    try {
      setLoading(true);
      await onUpdate(customer.identityNumber, corporate
        ? { profileName: formData.profileName.trim(), email: formData.email.trim() }
        : { firstName: formData.firstName.trim(), lastName: formData.lastName.trim(), email: formData.email.trim() });
      onOpenChange(false);
    } catch {
      // Retain the form on failure; the API interceptor displays the error.
    } finally {
      setLoading(false);
    }
  };

  return (
    <Dialog open onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <DialogTitle className="text-xl font-bold">Müşteri Profilini Düzenle</DialogTitle>
          <DialogDescription>{customer.identityNumber} kimlik/vergi numaralı müşterinin bankacılık profilini güncelliyorsunuz.</DialogDescription>
        </DialogHeader>
        <form onSubmit={handleSubmit} className="space-y-4 mt-4">
          {corporate || !editable ? (
            <div className="space-y-2">
              <Label htmlFor="edit-profile-name">{corporate ? "Şirket Unvanı" : "Ad Soyad"}</Label>
              <Input id="edit-profile-name" value={formData.profileName} minLength={3} maxLength={150}
                onChange={(event) => setFormData({ ...formData, profileName: event.target.value })} required disabled={loading || !editable} />
            </div>
          ) : (
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label htmlFor="edit-first-name">Ad</Label>
                <Input id="edit-first-name" value={formData.firstName} maxLength={100} required disabled={loading}
                  onChange={(event) => setFormData({ ...formData, firstName: event.target.value })} />
              </div>
              <div className="space-y-2">
                <Label htmlFor="edit-last-name">Soyad</Label>
                <Input id="edit-last-name" value={formData.lastName} maxLength={100} required disabled={loading}
                  onChange={(event) => setFormData({ ...formData, lastName: event.target.value })} />
              </div>
            </div>
          )}
          <div className="space-y-2">
            <Label htmlFor="edit-profile-email">İletişim E-postası</Label>
            <Input id="edit-profile-email" type="email" value={formData.email} maxLength={100} required disabled={loading || !editable}
              onChange={(event) => setFormData({ ...formData, email: event.target.value })} />
          </div>
          {!editable && <p className="text-sm text-slate-500">Bu yönetici profili merkezi kimlik yönetiminden güncellenir.</p>}
          <div className="flex justify-end gap-3 pt-4 border-t">
            <Button type="button" variant="outline" onClick={() => onOpenChange(false)} disabled={loading}>İptal</Button>
            <Button type="submit" className="bg-orange-600 hover:bg-orange-700" disabled={loading || !valid}>
              {loading ? <Loader2 className="w-4 h-4 animate-spin" /> : <Save className="w-4 h-4" />}
              {loading ? "Kaydediliyor..." : "Değişiklikleri Kaydet"}
            </Button>
          </div>
        </form>
      </DialogContent>
    </Dialog>
  );
}
