import { useState, useEffect } from "react";
import { adminService } from "@/services/admin.service";
import { UserProfileResponse } from "@/types"; // 🚀 V2: CustomerResponse yerine UserProfileResponse
import { toast } from "sonner";

export function useCustomers() {
  const [customers, setCustomers] = useState<UserProfileResponse[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchCustomers();
  }, []);

  const fetchCustomers = async () => {
/*************  ✨ Windsurf Command ⭐  *************/
/**
 * Fetches all customers from the server.
 * If successful, sets the customers state to the received data.
 * If not successful, shows a toast error message.
 * Finally, sets the loading state to false.
 * @returns {Promise<void>} A promise that resolves when the operation is complete.
 */
/*******  4a70a9e6-43a7-4ed9-a02c-9795dd684ebe  *******/    try {
      setLoading(true);
      const data = await adminService.getAllCustomers();
      setCustomers(data);
    } finally {
      setLoading(false);
    }
  };

  const removeCustomer = async (identityNumber: string) => {
    await adminService.deleteCustomer(identityNumber);
    // 🚀 V2: Filtreleme identityNumber üzerinden
    setCustomers((prev) => prev.filter((c) => c.identityNumber !== identityNumber));
    toast.success("Müşteri/Kurum başarıyla silindi.");
  };

  // 🚀 V2: fullName yerine profileName
  const editCustomer = async (identityNumber: string, updatedData: { profileName: string; email: string }) => {
    const updatedCustomer = await adminService.updateCustomer(identityNumber, updatedData);
    toast.success("Bilgiler başarıyla güncellendi!");
    setCustomers((prev) => prev.map((c) => (c.identityNumber === identityNumber ? { ...c, ...updatedCustomer } : c)));
    return true; 
  };

  const updateCustomerStatus = async (identityNumber: string, status: 'APPROVED' | 'REJECTED') => {
    try {
      await adminService.updateCustomerStatus(identityNumber, status);
      toast.success(`Durum başarıyla ${status === 'APPROVED' ? 'onaylandı' : 'reddedildi'}.`);
      await fetchCustomers();
      return true;
    } catch (error: any) {
      toast.error("İşlem gerçekleştirilemedi!", { 
        description: error.response?.data?.message || "Bilinmeyen bir hata oluştu." 
      });
      return false;
    }
  };

  return { customers, loading, fetchCustomers, removeCustomer, editCustomer, updateCustomerStatus };
}