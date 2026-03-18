// services/account.service.ts
import api from '../lib/axios';
import { AccountResponse, CreateAccountRequest } from '../types';

export const accountService = {
  // Sistemdeki tüm banka hesaplarını getirir
  getAllAccounts: async (): Promise<AccountResponse[]> => {
    // Backend'inde bu endpoint'in (GET /admin/accounts) olduğunu varsayıyoruz
    const response = await api.get<AccountResponse[]>('/admin/accounts');
    return response.data;
  },

  // SADECE GİRİŞ YAPAN MÜŞTERİNİN HESAPLARINI GETİRİR
  getMyAccounts: async (): Promise<AccountResponse[]> => {
    // 🚀 DOĞRUSU BU: Sadece '/accounts' adresine gidiyoruz. 
    // Backend (Spring Security) kimin istek attığını Token'dan kendisi anlıyor.
    const response = await api.get<AccountResponse[]>('/accounts');
    return response.data;
  },

  // 🚀 YENİ HESAP AÇMA
  createAccount: async (data: CreateAccountRequest): Promise<AccountResponse> => {
    // Backend'deki POST /api/v1/accounts adresine istek atıyoruz
    const response = await api.post<AccountResponse>('/accounts', data);
    return response.data;
  },

  deleteAccount: async (accountNumber: string) => {
    // API yolunu kendi backend URL yapılandırmana göre kontrol et
    const response = await api.delete(`/accounts/${accountNumber}`);
    return response.data;
  },
};