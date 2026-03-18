"use client";

import { useState } from "react";
import Link from "next/link";
import { useAuthStore } from "@/store/useAuthStore";
import { UserProfileResponse, ApprovalStatus, Role } from "@/types"; 
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { 
  Loader2, Trash2, UserSearch, Settings, CheckCircle2, XCircle, 
  Users, Building2, Clock, Search, Filter 
} from "lucide-react";

import { PageHeader } from "@/components/shared/PageHeader";
import { EditCustomerModal } from "@/components/admin/modals/EditCustomerModal";
import { useCustomers } from "@/hooks/useCustomers";

export default function CustomerListPage() {
  const { user: currentUser } = useAuthStore();
  const { customers, loading, fetchCustomers, removeCustomer, editCustomer, updateCustomerStatus } = useCustomers();

  const [editingCustomer, setEditingCustomer] = useState<UserProfileResponse | null>(null);
  const [isEditModalOpen, setIsEditModalOpen] = useState(false);

  // 🚀 YENİ: FİLTRELEME STATE'LERİ
  const [statusFilter, setStatusFilter] = useState<ApprovalStatus | "ALL">("ALL");
  const [roleFilter, setRoleFilter] = useState<Role | "ALL">("ALL");

  const handleDelete = async (identityNumber: string) => {
    if (confirm("Bu müşteri/kurumu silmek istediğinize emin misiniz?")) {
      await removeCustomer(identityNumber);
    }
  };

  const handleUpdate = async (identityNumber: string, updatedData: { profileName: string; email: string }) => {
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
                    <th className="px-4 py-4 font-bold text-slate-700 text-right">İşlemler</th>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {filteredCustomers.length === 0 ? (
                    <TableRow>
                      <TableCell colSpan={5} className="text-center py-20 text-slate-400">
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

                          <TableCell className="px-4 py-4 text-right">
                            <div className="flex justify-end gap-1">
                              {(customer.status === "PENDING" || customer.status === "REJECTED") && !isMe && (
                                <Button 
                                  size="icon" variant="ghost" className="text-green-600 hover:text-green-700 hover:bg-green-50"
                                  onClick={() => updateCustomerStatus && updateCustomerStatus(customer.identityNumber, 'APPROVED')}
                                  title="Onayla"
                                >
                                  <CheckCircle2 className="h-4 w-4" />
                                </Button>
                              )}

                              {(customer.status === "PENDING" || customer.status === "APPROVED") && !isMe && (
                                <Button 
                                  size="icon" variant="ghost" className="text-red-600 hover:text-red-700 hover:bg-red-50"
                                  onClick={() => updateCustomerStatus && updateCustomerStatus(customer.identityNumber, 'REJECTED')}
                                  title="Reddet"
                                >
                                  <XCircle className="h-4 w-4" />
                                </Button>
                              )}

                              <Link href={`/admin/customers/${customer.identityNumber}/accounts`}>
                                <Button variant="ghost" size="icon" className="text-blue-600 hover:bg-blue-50" title="Hesapları Gör">
                                  <UserSearch className="h-4 w-4" />
                                </Button>
                              </Link>

                              <Button
                                variant="ghost" size="icon"
                                className={`text-orange-600 ${isMe ? "opacity-30 cursor-not-allowed" : "hover:bg-orange-50"}`}
                                onClick={() => {
                                  setEditingCustomer(customer);
                                  setIsEditModalOpen(true);
                                }}
                                disabled={isMe}
                                title="Düzenle"
                              >
                                <Settings className="h-4 w-4" />
                              </Button>

                              <Button
                                variant="ghost" size="icon"
                                className={`text-red-600 ${isMe ? "opacity-30 cursor-not-allowed" : "hover:bg-red-50"}`}
                                onClick={() => handleDelete(customer.identityNumber)}
                                disabled={isMe}
                                title="Sil"
                              >
                                <Trash2 className="h-4 w-4" />
                              </Button>
                            </div>
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

      <EditCustomerModal
        isOpen={isEditModalOpen}
        onOpenChange={setIsEditModalOpen}
        customer={editingCustomer}
        onUpdate={handleUpdate}
      />
    </div>
  );
}