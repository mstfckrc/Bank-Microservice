import { useState } from "react";
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { AccountResponse } from "@/types";

interface DepositModalProps {
  isOpen: boolean;
  onOpenChange: (open: boolean) => void;
  accounts: AccountResponse[];
  onDeposit: (iban: string, amount: string) => Promise<void>;
  isProcessing: boolean;
}

export function DepositModal({ isOpen, onOpenChange, accounts, onDeposit, isProcessing }: DepositModalProps) {
  const [data, setData] = useState({ iban: "", amount: "" });

  const activeAccounts = accounts.filter(acc => (acc as any).isActive !== false && (acc as any).active !== false);

  const handleSubmit = async () => {
    await onDeposit(data.iban, data.amount);
    setData({ iban: "", amount: "" });
  };

  return (
    <Dialog open={isOpen} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader><DialogTitle>Hesaba Para Yatır</DialogTitle></DialogHeader>
        <div className="space-y-4 py-4">
          <div className="space-y-2">
            <Label>Yatırılacak Hesap</Label>
            <Select onValueChange={(val) => setData({ ...data, iban: val })}>
              <SelectTrigger><SelectValue placeholder="Hesap seçin" /></SelectTrigger>
              <SelectContent>
                {activeAccounts.map((acc) => (
                  <SelectItem key={acc.id} value={acc.iban}>{acc.currency} - {acc.iban.slice(-4)}</SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>
          <div className="space-y-2">
            <Label>Miktar</Label>
            <Input type="number" placeholder="0.00" value={data.amount} onChange={(e) => setData({ ...data, amount: e.target.value })} />
          </div>
        </div>
        <DialogFooter>
          <Button onClick={handleSubmit} disabled={isProcessing || !data.iban || !data.amount} className="w-full bg-green-600 hover:bg-green-700">
            {isProcessing ? "İşleniyor..." : "Onayla ve Yatır"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}