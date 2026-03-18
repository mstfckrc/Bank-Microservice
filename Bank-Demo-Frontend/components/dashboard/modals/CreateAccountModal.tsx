import { useState } from "react";
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Label } from "@/components/ui/label";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";

export function CreateAccountModal({ isOpen, onOpenChange, onCreate, isProcessing }: any) {
  const [currency, setCurrency] = useState("TRY");

  return (
    <Dialog open={isOpen} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader><DialogTitle>Yeni Vadesiz Hesap</DialogTitle></DialogHeader>
        <div className="py-6 space-y-2">
          <Label>Döviz Cinsi</Label>
          <Select onValueChange={(val: any) => setCurrency(val)} defaultValue="TRY">
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