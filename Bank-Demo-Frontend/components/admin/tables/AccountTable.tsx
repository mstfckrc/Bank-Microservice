import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { Button } from "@/components/ui/button";
import { History, Trash2 } from "lucide-react";
import { AccountResponse } from "@/types";
import { StatusBadge } from "@/components/shared/StatusBadge";

interface AccountTableProps {
  accounts: AccountResponse[];
  onOpenHistory: (accountNo: string, accountId: number) => void;
  onCloseAccount: (accountNo: string) => void;
}

export function AccountTable({
  accounts,
  onOpenHistory,
  onCloseAccount,
}: AccountTableProps) {
  if (accounts.length === 0) {
    return (
      <div className="text-center py-10 text-slate-500 border rounded-lg bg-slate-50 border-dashed">
        Müşteriye ait herhangi bir hesap bulunmamaktadır. Sağ üstten yeni hesap
        açabilirsiniz.
      </div>
    );
  }

  return (
    <div className="rounded-md border">
      <Table>
        <TableHeader>
          <TableRow>
            <TableHead className="font-bold">Hesap Numarası</TableHead>
            <TableHead className="font-bold">IBAN</TableHead>
            <TableHead className="text-right font-bold">Bakiye</TableHead>
            <TableHead className="font-bold">Döviz</TableHead>
            <TableHead className="text-center font-bold">Durum</TableHead>
            <TableHead className="text-right font-bold">İşlemler</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {accounts.map((acc: any) => {
            const isAccountActive =
              acc.isActive !== false && acc.active !== false;

            return (
              <TableRow
                key={acc.id}
                className={`transition-colors ${isAccountActive ? "hover:bg-slate-50" : "bg-slate-50/50 opacity-60"}`}
              >
                <TableCell
                  className={`font-mono font-medium ${!isAccountActive ? "line-through text-slate-400" : "text-slate-900"}`}
                >
                  {acc.accountNumber}
                </TableCell>
                <TableCell className="font-mono text-sm text-slate-600">
                  {acc.iban}
                </TableCell>
                <TableCell
                  className={`text-right font-bold text-lg ${!isAccountActive ? "text-slate-400" : "text-slate-700"}`}
                >
                  {acc.balance.toLocaleString("tr-TR")}
                </TableCell>
                <TableCell>
                  <StatusBadge
                    type="currency"
                    value={acc.currency}
                    isActive={isAccountActive}
                  />
                </TableCell>
                <TableCell className="text-center">
                  <StatusBadge type="status" value={isAccountActive} />
                </TableCell>

                <TableCell className="text-right">
                  <div className="flex justify-end gap-1">
                    <Button
                      variant="ghost"
                      size="icon"
                      onClick={() => onOpenHistory(acc.accountNumber, acc.id)}
                      className="text-slate-400 hover:text-blue-600 hover:bg-blue-50 transition-colors"
                      title="Hesap Geçmişini Gör"
                    >
                      <History className="h-4 w-4" />
                    </Button>

                    {isAccountActive && (
                      <Button
                        variant="ghost"
                        size="icon"
                        onClick={() => onCloseAccount(acc.accountNumber)}
                        className="text-slate-400 hover:text-red-600 hover:bg-red-50 transition-colors"
                        title="Hesabı Kapat"
                      >
                        <Trash2 className="h-4 w-4" />
                      </Button>
                    )}
                  </div>
                </TableCell>
              </TableRow>
            );
          })}
        </TableBody>
      </Table>
    </div>
  );
}
