import { TransactionResponse, UserProfileResponse, AccountResponse, UpdateProfileRequest } from "../types";
import api from "../lib/axios";

export const adminService = {
  // --- MÜŞTERİ YÖNETİMİ ---

  // Tüm müşterileri listele
  getAllCustomers: async (): Promise<UserProfileResponse[]> => {
    const response = await api.get<UserProfileResponse[]>('/admin/customers');
    return response.data;
  },

  // Müşteri bilgilerini güncelle
  updateCustomer: async (identityNumber: string, data: UpdateProfileRequest) => {
    const response = await api.put(`/admin/customers/${identityNumber}`, data);
    return response.data; 
  },

  // Müşteri Onay/Red Durum Güncelleme
  updateCustomerStatus: async (identityNumber: string, status: 'APPROVED' | 'REJECTED' | 'PENDING') => {
    const response = await api.put(`/admin/customers/${identityNumber}/status`, null, {
      params: { status }
    });
    return response.data;
  },

  // Müşteriyi sil
  deleteCustomer: async (identityNumber: string): Promise<void> => {
    await api.delete(`/admin/customers/${identityNumber}`);
  },


  // --- HESAP YÖNETİMİ ---

  // Müşteriye ait tüm hesapları getirme
  getCustomerAccounts: async (identityNumber: string): Promise<AccountResponse[]> => {
    const response = await api.get<AccountResponse[]>(`/admin/customers/${identityNumber}/accounts`);
    return response.data;
  },

  // Admin yetkisiyle müşteriye yeni hesap açma
  openAccountForCustomer: async (identityNumber: string, currency: string) => {
    const response = await api.post(`/admin/customers/${identityNumber}/accounts`, { currency });
    return response.data;
  },

  // Admin için belirli bir hesabın hareketlerini getir
  getAccountTransactions: async (accountNumber: string): Promise<TransactionResponse[]> => {
    const response = await api.get<TransactionResponse[]>(`/admin/accounts/${accountNumber}/transactions`);
    return response.data;
  },

  // Tüm banka trafiğini getir
  getAllTransactions: async (status?: string): Promise<TransactionResponse[]> => {
    const response = await api.get<TransactionResponse[]>('/admin/transactions', {
      params: { status }
    });
    return response.data;
  },

  // Bekleyen işlemi ONAYLA
  approveTransaction: async (referenceNo: string): Promise<TransactionResponse> => {
    const response = await api.put(`/admin/transactions/${referenceNo}/approve`);
    return response.data;
  },

  // Bekleyen işlemi REDDET ve iade et
  rejectTransaction: async (referenceNo: string): Promise<TransactionResponse> => {
    const response = await api.put(`/admin/transactions/${referenceNo}/reject`);
    return response.data;
  }
};