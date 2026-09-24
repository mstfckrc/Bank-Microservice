import { useState } from "react";
import { AccountCurrency } from "@/types";
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Label } from "@/components/ui/label";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";

interface CreateAccountModalProps {
  isOpen: boolean;
  onOpenChange: (open: boolean) => void;
  onCreate: (currency: AccountCurrency) => void | Promise<void>;
  isProcessing: boolean;
}

export function CreateAccountModal({ isOpen, onOpenChange, onCreate, isProcessing }: CreateAccountModalProps) {
  const [currency, setCurrency] = useState<AccountCurrency>("TRY");

  return (
    <Dialog open={isOpen} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader><DialogTitle>Yeni Vadesiz Hesap</DialogTitle></DialogHeader>
        <div className="py-6 space-y-2">
          <Label>Döviz Cinsi</Label>
          <Select onValueChange={(value) => {
            if (value === "TRY" || value === "USD" || value === "EUR") setCurrency(value);
          }} defaultValue="TRY">
            <SelectTrigger><SelectValue /></SelectTrigger>
            <SelectContent>
              <SelectItem value="TRY">Türk Lirası (TRY)</SelectItem>
              <SelectItem value="USD">Dolar (USD)</SelectItem>
              <SelectItem value="EUR">Euro (EUR)</SelectItem>
            </SelectContent>
          </Select>
        </div>
        <DialogFooter>
          <Button onClick={() => onCreate(currency)} disabled={isProcessing} className="w-full">Hesabı Oluştur</Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}