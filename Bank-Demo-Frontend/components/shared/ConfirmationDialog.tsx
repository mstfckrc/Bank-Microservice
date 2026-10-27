"use client";

import { useRef, useState } from "react";
import { Button } from "@/components/ui/button";
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from "@/components/ui/dialog";

type ConfirmationDialogProps = {
  open: boolean;
  title: string;
  description: string;
  confirmLabel: string;
  error?: string | null;
  onClose: () => void;
  onConfirm: () => Promise<boolean>;
  onCloseAutoFocus?: (event: Event) => void;
};

export function ConfirmationDialog({ open, title, description, confirmLabel, error, onClose, onConfirm, onCloseAutoFocus }: ConfirmationDialogProps) {
  const [pending, setPending] = useState(false);
  const inFlight = useRef(false);

  const executeConfirmation = async () => {
    if (inFlight.current) return;
    inFlight.current = true;
    setPending(true);
    try {
      if (await onConfirm()) onClose();
    } catch {
      // API hata bildirimi ilgili çağıran tarafından gösterilir.
    } finally {
      inFlight.current = false;
      setPending(false);
    }
  };

  return (
    <Dialog open={open} onOpenChange={(nextOpen) => { if (!nextOpen && !inFlight.current) onClose(); }}>
      <DialogContent showCloseButton={!pending} onCloseAutoFocus={onCloseAutoFocus}>
        <DialogHeader>
          <DialogTitle>{title}</DialogTitle>
          <DialogDescription>{description}</DialogDescription>
        </DialogHeader>
        {error && <p role="alert" className="text-sm text-red-600">{error}</p>}
        <DialogFooter>
          <Button type="button" variant="outline" autoFocus disabled={pending} onClick={onClose}>Vazgeç</Button>
          <Button type="button" variant="destructive" disabled={pending} onClick={() => void executeConfirmation()}>
            {pending ? "İşleniyor…" : confirmLabel}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
