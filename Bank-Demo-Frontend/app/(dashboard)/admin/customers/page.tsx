"use client";

import { useRef, useState } from "react";
import Link from "next/link";
import { DropdownMenu } from "radix-ui";
import { useAuthStore } from "@/store/useAuthStore";
import { UserProfileResponse, ApprovalStatus, Role, UpdateProfileRequest } from "@/types";
import { Table, TableBody, TableCell, TableHeader, TableRow } from "@/components/ui/table";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { 
  Loader2, Trash2, UserSearch, Settings, CheckCircle2, XCircle, 
  Users, Building2, Clock, Search, Filter, ChevronDown, ShieldCheck, ShieldOff
} from "lucide-react";

import { PageHeader } from "@/components/shared/PageHeader";
import { ConfirmationDialog } from "@/components/shared/ConfirmationDialog";
import { EditCustomerModal } from "@/components/admin/modals/EditCustomerModal";
import { useCustomers } from "@/hooks/useCustomers";

type AccessAction = "restrict" | "lift" | "approve" | "reject";

const actionMenuItemClassName = "flex cursor-pointer items-center gap-2 rounded-md px-3 py-2 text-sm outline-none data-[highlighted]:bg-slate-100 data-[disabled]:pointer-events-none data-[disabled]:opacity-40";

const accessActionCopy = {
  restrict: {
    title: "Hesap erişimi kısıtlansın mı?",
    description: "Bankacılık işlemleri durdurulacak. Kullanıcı erişim durumunu görebilecek ve inceleme talebi gönderebilecek.",
    confirmLabel: "Erişimi kısıtla",
    destructive: true,
  },
  lift: {
    title: "Erişim kısıtlaması kaldırılsın mı?",
    description: "Erişim kısıtlaması kaldırılacak. Mevcut müşteri onayı ve işlem yetkileri geçerli kalacak.",
    confirmLabel: "Kısıtlamayı kaldır",
    destructive: false,
  },
  approve: {
    title: "İnceleme talebi onaylansın mı?",
    description: "İnceleme talebi onaylanacak ve kullanıcının erişim kısıtlaması kaldırılacak.",
    confirmLabel: "Erişimi onayla",
    destructive: false,
  },
  reject: {
    title: "İnceleme talebi reddedilsin mi?",
    description: "Erişim kısıtlaması sürecek. Kullanıcı bu kısıtlama dönemi için yeni bir inceleme talebi gönderemeyecek.",
    confirmLabel: "Talebi reddet",
    destructive: true,
  },
};

export default function CustomerListPage() {
  const { user: currentUser } = useAuthStore();
  const { customers, loading, fetchCustomers, removeCustomer, editCustomer, updateCustomerStatus, updateAccessRestriction, decideAccessReview } = useCustomers();

  const [editingCustomer, setEditingCustomer] = useState<UserProfileResponse | null>(null);
  const [isEditModalOpen, setIsEditModalOpen] = useState(false);
  const [customerToDelete, setCustomerToDelete] = useState<UserProfileResponse | null>(null);
  const [accessConfirmation, setAccessConfirmation] = useState<{
    identityNumber: string;
    profileName: string;
    action: AccessAction;
  } | null>(null);
  const [isAccessConfirming, setIsAccessConfirming] = useState(false);
  const accessActionInFlight = useRef(false);
  const customerDialogOpening = useRef(false);
  const lastActionTrigger = useRef<HTMLButtonElement | null>(null);
  const confirmationCopy = accessConfirmation ? accessActionCopy[accessConfirmation.action] : null;

  const requestAccessAction = (customer: UserProfileResponse, action: AccessAction) => {
    customerDialogOpening.current = true;
    setAccessConfirmation({ identityNumber: customer.identityNumber, profileName: customer.profileName, action });
  };

  const confirmAccessAction = async () => {
    if (!accessConfirmation || accessActionInFlight.current) return;
    accessActionInFlight.current = true;
    setIsAccessConfirming(true);
    try {
      const { identityNumber, action } = accessConfirmation;
      const success = action === "restrict" || action === "lift"
        ? await updateAccessRestriction(identityNumber, action === "restrict")
        : await decideAccessReview(identityNumber, action === "approve" ? "APPROVE" : "REJECT");
      if (success) setAccessConfirmation(null);
    } finally {
      accessActionInFlight.current = false;
      setIsAccessConfirming(false);
    }
  };

  // 🚀 YENİ: FİLTRELEME STATE'LERİ
  const [statusFilter, setStatusFilter] = useState<ApprovalStatus | "ALL">("ALL");
  const [roleFilter, setRoleFilter] = useState<Role | "ALL">("ALL");

  const handleUpdate = async (identityNumber: string, updatedData: UpdateProfileRequest) => {
    const success = await editCustomer(identityNumber, updatedData);
    if (success) setIsEditModalOpen(false);
  };

  // 🚀 YENİ: FİLTRELEME MANTIĞI
  const filteredCustomers = customers.filter(c => {
    const matchesStatus = statusFilter === "ALL" || c.status === statusFilter;
    const matchesRole = roleFilter === "ALL" || c.role === roleFilter;
    return matchesStatus && matchesRole;
  });

  return (
    <div className="space-y-6 max-w-7xl mx-auto">
      
      <PageHeader 
        title="Müşteri ve Kurum Yönetimi" 
        description="Bankadaki tüm kullanıcıları kategorize et, denetle ve yetkilendir."
        action={<Button onClick={fetchCustomers} variant="secondary" size="sm">Listeyi Yenile</Button>}
      />

      {/* 🚀 1. BÖLÜM: FİLTRELEME PANELİ (HIZLI BUTONLAR) */}
      <Card className="border-none shadow-md bg-slate-50/50">
        <CardContent className="p-4 space-y-4">
          
          {/* ROL FİLTRESİ */}
          <div className="flex flex-col gap-2">
            <label className="text-[10px] font-bold text-slate-400 uppercase tracking-widest ml-1">Hesap Türü</label>
            <div className="flex flex-wrap gap-2">
              <Button 
                variant={roleFilter === "ALL" ? "default" : "outline"} 
                size="sm"
                onClick={() => setRoleFilter("ALL")}
                className="h-8 text-xs"
              >
                <Search className="w-3.5 h-3.5 mr-1.5" /> Tümü
              </Button>
              <Button 
                variant={roleFilter === "RETAIL_CUSTOMER" ? "default" : "outline"} 
                size="sm"
                onClick={() => setRoleFilter("RETAIL_CUSTOMER")}
                className={`h-8 text-xs ${roleFilter === "RETAIL_CUSTOMER" ? "bg-blue-600" : ""}`}
              >
                <Users className="w-3.5 h-3.5 mr-1.5" /> Bireysel Müşteriler
              </Button>
              <Button 
                variant={roleFilter === "CORPORATE_MANAGER" ? "default" : "outline"} 
                size="sm"
                onClick={() => setRoleFilter("CORPORATE_MANAGER")}
                className={`h-8 text-xs ${roleFilter === "CORPORATE_MANAGER" ? "bg-indigo-600" : ""}`}
              >
                <Building2 className="w-3.5 h-3.5 mr-1.5" /> Kurumsal Yöneticiler
              </Button>
            </div>
          </div>

          <hr className="border-slate-200/60" />

          {/* DURUM FİLTRESİ */}
          <div className="flex flex-col gap-2">
            <label className="text-[10px] font-bold text-slate-400 uppercase tracking-widest ml-1">Onay Durumu</label>
            <div className="flex flex-wrap gap-2">
              <Button 
                variant={statusFilter === "ALL" ? "secondary" : "outline"} 
                size="sm"
                onClick={() => setStatusFilter("ALL")}
                className="h-8 text-xs"
              >
                Tümü
              </Button>
              <Button 
                variant={statusFilter === "PENDING" ? "default" : "outline"} 
                size="sm"
                onClick={() => setStatusFilter("PENDING")}
                className={`h-8 text-xs ${statusFilter === "PENDING" ? "bg-amber-500 hover:bg-amber-600" : ""}`}
              >
                <Clock className="w-3.5 h-3.5 mr-1.5" /> Onay Bekleyenler
              </Button>
              <Button 
                variant={statusFilter === "APPROVED" ? "default" : "outline"} 
                size="sm"
                onClick={() => setStatusFilter("APPROVED")}
                className={`h-8 text-xs ${statusFilter === "APPROVED" ? "bg-green-600 hover:bg-green-700" : ""}`}
              >
                <CheckCircle2 className="w-3.5 h-3.5 mr-1.5" /> Onaylı Hesaplar
              </Button>
              <Button 
                variant={statusFilter === "REJECTED" ? "default" : "outline"} 
                size="sm"
                onClick={() => setStatusFilter("REJECTED")}
                className={`h-8 text-xs ${statusFilter === "REJECTED" ? "bg-red-600 hover:bg-red-700" : ""}`}
              >
                <XCircle className="w-3.5 h-3.5 mr-1.5" /> Reddedilenler
              </Button>
            </div>
          </div>
        </CardContent>
      </Card>

      {/* 2. BÖLÜM: TABLO PANELİ */}
      <Card className="border-none shadow-xl">
        <CardHeader className="border-b bg-slate-50/50 pb-4">
          <div className="flex justify-between items-center">
            <CardTitle className="text-lg flex items-center gap-2">
              <Filter className="w-5 h-5 text-slate-400" />
              Liste ({filteredCustomers.length})
            </CardTitle>
          </div>
        </CardHeader>
        <CardContent className="p-0">
          {loading ? (
            <div className="flex flex-col justify-center items-center py-20 text-slate-500">
              <Loader2 className="h-8 w-8 animate-spin mb-4" />
              <p className="animate-pulse">Sistem verileri taranıyor...</p>
            </div>
          ) : (
            <div className="overflow-x-auto">
              <Table>
                <TableHeader>
                  <TableRow className="bg-slate-50/80">
                    <th className="px-4 py-4 font-bold text-slate-700">Müşteri / Kurum Bilgisi</th>
                    <th className="px-4 py-4 font-bold text-slate-700">Kimlik / Vergi No</th>
                    <th className="px-4 py-4 font-bold text-slate-700">E-posta</th>
                    <th className="px-4 py-4 font-bold text-slate-700 text-center">Durum</th>
                    <th className="px-4 py-4 font-bold text-slate-700 text-center">Erişim</th>
                    <th className="w-32 px-4 py-4 font-bold text-slate-700 text-right">İşlemler</th>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {filteredCustomers.length === 0 ? (
                    <TableRow>
                      <TableCell colSpan={6} className="text-center py-20 text-slate-400">
                        <Search className="w-12 h-12 mx-auto mb-3 opacity-10" />
                        Aradığınız kriterlere uygun kayıt bulunamadı.
                      </TableCell>
                    </TableRow>
                  ) : (
                    filteredCustomers.map((customer) => {
                      const isMe = customer.identityNumber === currentUser?.identityNumber;

                      return (
                        <TableRow key={customer.identityNumber} className="hover:bg-slate-50/80 transition-colors border-b last:border-0">
                          <TableCell className="px-4 py-4">
                            <div className="flex items-center gap-3">
                              <div className={`p-2 rounded-lg ${customer.role === 'CORPORATE_MANAGER' ? 'bg-indigo-50 text-indigo-600' : 'bg-blue-50 text-blue-600'}`}>
                                {customer.role === 'CORPORATE_MANAGER' ? <Building2 className="w-4 h-4" /> : <Users className="w-4 h-4" />}
                              </div>
                              <div>
                                <div className="font-bold text-slate-900">
                                  {customer.profileName}
                                  {isMe && <span className="ml-2 text-[10px] bg-slate-200 text-slate-600 px-1.5 py-0.5 rounded uppercase tracking-tighter">Sistem Yöneticisi</span>}
                                </div>
                                <div className="text-[10px] text-slate-400 font-medium uppercase tracking-wider">{customer.role}</div>
                              </div>
                            </div>
                          </TableCell>
                          
                          <TableCell className="px-4 py-4 font-mono text-sm text-slate-600">{customer.identityNumber}</TableCell>
                          <TableCell className="px-4 py-4 text-slate-600">{customer.email}</TableCell>

                          <TableCell className="px-4 py-4 text-center">
                            {customer.status === "PENDING" && <Badge className="bg-amber-100 text-amber-700 hover:bg-amber-100 border-amber-200 shadow-none">Beklemede</Badge>}
                            {customer.status === "APPROVED" && <Badge className="bg-green-100 text-green-700 hover:bg-green-100 border-green-200 shadow-none">Onaylı</Badge>}
                            {customer.status === "REJECTED" && <Badge className="bg-red-100 text-red-700 hover:bg-red-100 border-red-200 shadow-none">Reddedildi</Badge>}
                          </TableCell>

                          <TableCell className="px-4 py-4 text-center">
                            {!customer.accessRestrictionStatus
                              ? <Badge variant="outline">Bilinmiyor</Badge>
                              : customer.accessRestrictionStatus === "UNRESTRICTED"
                                ? <Badge className="bg-emerald-100 text-emerald-700 hover:bg-emerald-100 border-emerald-200 shadow-none">Açık</Badge>
                                : <Badge className="bg-orange-100 text-orange-700 hover:bg-orange-100 border-orange-200 shadow-none">
                                {customer.accessRestrictionStatus === "REVIEW_PENDING" ? "İnceleme bekliyor"
                                  : customer.accessRestrictionStatus === "REVIEW_REJECTED" ? "İnceleme reddedildi" : "Kısıtlı"}
                              </Badge>}
                          </TableCell>

                          <TableCell className="px-4 py-4 text-right">
                            <DropdownMenu.Root modal={false}>
                              <DropdownMenu.Trigger asChild>
                                <Button size="sm" variant="outline" className="w-28" aria-label={`${customer.profileName} için işlemler`} onFocus={(event) => {
                                  lastActionTrigger.current = event.currentTarget;
                                }}>
                                  İşlemler <ChevronDown className="ml-1 h-4 w-4" />
                                </Button>
                              </DropdownMenu.Trigger>
                              <DropdownMenu.Portal>
                                <DropdownMenu.Content align="end" sideOffset={6} collisionPadding={12} className="z-50 w-60 rounded-lg border border-slate-200 bg-white p-1 text-slate-700 shadow-lg" onCloseAutoFocus={(event) => {
                                  if (customerDialogOpening.current) {
                                    event.preventDefault();
                                    customerDialogOpening.current = false;
                                  }
                                }}>
                                  <DropdownMenu.Item asChild className={actionMenuItemClassName}>
                                    <Link href={`/admin/customers/${customer.identityNumber}/accounts`}>
                                      <UserSearch className="h-4 w-4" /> Hesapları gör
                                    </Link>
                                  </DropdownMenu.Item>
                                  <DropdownMenu.Item className={actionMenuItemClassName} disabled={isMe} onSelect={() => {
                                    customerDialogOpening.current = true;
                                    setEditingCustomer(customer);
                                    setIsEditModalOpen(true);
                                  }}>
                                    <Settings className="h-4 w-4" /> Bilgileri düzenle
                                  </DropdownMenu.Item>

                                  {!isMe && customer.role !== "ADMIN" && (
                                    <>
                                      {(customer.status === "PENDING" || customer.status === "REJECTED") && (
                                        <>
                                          <DropdownMenu.Separator className="my-1 h-px bg-slate-100" />
                                          <DropdownMenu.Label className="px-3 py-1 text-xs font-semibold text-slate-400">Kayıt onayı</DropdownMenu.Label>
                                          <DropdownMenu.Item className={`${actionMenuItemClassName} text-green-700`} onSelect={() => void updateCustomerStatus(customer.identityNumber, "APPROVED")}>
                                            <CheckCircle2 className="h-4 w-4" /> {customer.status === "PENDING" ? "Kaydı onayla" : "Kaydı yeniden onayla"}
                                          </DropdownMenu.Item>
                                          {customer.status === "PENDING" && (
                                            <DropdownMenu.Item className={`${actionMenuItemClassName} text-red-700`} onSelect={() => void updateCustomerStatus(customer.identityNumber, "REJECTED")}>
                                              <XCircle className="h-4 w-4" /> Kaydı reddet
                                            </DropdownMenu.Item>
                                          )}
                                        </>
                                      )}
                                      {customer.accessRestrictionStatus && (
                                        <>
                                          <DropdownMenu.Separator className="my-1 h-px bg-slate-100" />
                                          <DropdownMenu.Label className="px-3 py-1 text-xs font-semibold text-slate-400">Bankacılık erişimi</DropdownMenu.Label>
                                          {customer.accessRestrictionStatus === "REVIEW_PENDING" ? (
                                            <>
                                              <DropdownMenu.Item className={`${actionMenuItemClassName} text-green-700`} onSelect={() => requestAccessAction(customer, "approve")}>
                                                <ShieldCheck className="h-4 w-4" /> İncelemeyi onayla
                                              </DropdownMenu.Item>
                                              <DropdownMenu.Item className={`${actionMenuItemClassName} text-red-700`} onSelect={() => requestAccessAction(customer, "reject")}>
                                                <ShieldOff className="h-4 w-4" /> İncelemeyi reddet
                                              </DropdownMenu.Item>
                                            </>
                                          ) : customer.accessRestrictionStatus === "UNRESTRICTED" ? (
                                            <DropdownMenu.Item className={`${actionMenuItemClassName} text-red-700`} onSelect={() => requestAccessAction(customer, "restrict")}>
                                              <ShieldOff className="h-4 w-4" /> Erişimi kısıtla
                                            </DropdownMenu.Item>
                                          ) : (
                                            <DropdownMenu.Item className={`${actionMenuItemClassName} text-green-700`} onSelect={() => requestAccessAction(customer, "lift")}>
                                              <ShieldCheck className="h-4 w-4" /> Kısıtlamayı kaldır
                                            </DropdownMenu.Item>
                                          )}
                                        </>
                                      )}
                                    </>
                                  )}

                                  <DropdownMenu.Separator className="my-1 h-px bg-slate-100" />
                                  <DropdownMenu.Item className={`${actionMenuItemClassName} text-red-700`} disabled={isMe} onSelect={() => {
                                    customerDialogOpening.current = true;
                                    setCustomerToDelete(customer);
                                  }}>
                                    <Trash2 className="h-4 w-4" /> Müşteriyi sil
                                  </DropdownMenu.Item>
                                </DropdownMenu.Content>
                              </DropdownMenu.Portal>
                            </DropdownMenu.Root>
                          </TableCell>
                        </TableRow>
                      );
                    })
                  )}
                </TableBody>
              </Table>
            </div>
          )}
        </CardContent>
      </Card>

      <Dialog open={accessConfirmation !== null} onOpenChange={(open) => {
        if (!open && !accessActionInFlight.current) setAccessConfirmation(null);
      }}>
        <DialogContent showCloseButton={!isAccessConfirming} onCloseAutoFocus={(event) => {
          event.preventDefault();
          lastActionTrigger.current?.focus();
        }}>
          <DialogHeader>
            <DialogTitle>{confirmationCopy?.title}</DialogTitle>
            <DialogDescription>
              <strong className="text-slate-900">{accessConfirmation?.profileName}</strong>
              {" — "}{confirmationCopy?.description}
            </DialogDescription>
          </DialogHeader>
          <DialogFooter>
            <Button type="button" variant="outline" autoFocus disabled={isAccessConfirming} onClick={() => setAccessConfirmation(null)}>
              Vazgeç
            </Button>
            <Button type="button" variant={confirmationCopy?.destructive ? "destructive" : "default"} disabled={isAccessConfirming} onClick={() => void confirmAccessAction()}>
              {isAccessConfirming ? "İşleniyor…" : confirmationCopy?.confirmLabel}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      <ConfirmationDialog
        open={customerToDelete !== null}
        title="Müşteri silinsin mi?"
        description={`${customerToDelete?.profileName ?? "Müşteri"} kaydı silinecek ve banka hesapları pasife alınacak.`}
        confirmLabel="Müşteriyi sil"
        onClose={() => setCustomerToDelete(null)}
        onConfirm={async () => {
          if (!customerToDelete) return false;
          await removeCustomer(customerToDelete.identityNumber);
          return true;
        }}
        onCloseAutoFocus={(event) => {
          event.preventDefault();
          lastActionTrigger.current?.focus();
        }}
      />
      <EditCustomerModal
        isOpen={isEditModalOpen}
        onOpenChange={setIsEditModalOpen}
        customer={editingCustomer}
        onUpdate={handleUpdate}
      />
    </div>
  );
}
