import api from '../lib/axios';
import { 
  AutoPaymentSettingsRequest,
  AutoPaymentSettingsResponse,
  CompanyEmployeeResponse, 
  HireEmployeeRequest, 
  TransactionResponse, 
  UpdateEmployeeRequest 
} from '../types';

const BASE_URL = '/companies/employees';

export const companyService = {
  
  // 1. Kurumsal yöneticinin kendi çalışanlarını getirir
  getMyEmployees: async (): Promise<CompanyEmployeeResponse[]> => {
    const response = await api.get(BASE_URL);
    return response.data;
  },

  // 2. Yeni personel işe alım talebi atar
  hireEmployee: async (data: HireEmployeeRequest): Promise<CompanyEmployeeResponse> => {
    const response = await api.post(BASE_URL, data);
    return response.data;
  },

  // 3. Mevcut personelin maaşını veya IBAN'ını günceller
  updateEmployee: async (
    employeeIdentityNumber: string, 
    data: UpdateEmployeeRequest
  ): Promise<CompanyEmployeeResponse> => {
    const response = await api.put(`${BASE_URL}/${employeeIdentityNumber}`, data);
    return response.data;
  },

  // 4. Personeli işten çıkarır
  removeEmployee: async (employeeIdentityNumber: string): Promise<{ message: string }> => {
    const response = await api.delete(`${BASE_URL}/${employeeIdentityNumber}`);
    return response.data;
  },

  // 5. Şirket kasasından tüm personellere toplu maaş dağıtır
  paySalaries: async (senderIban: string): Promise<TransactionResponse[]> => {
    const response = await api.post(`${BASE_URL}/pay-salaries`, { senderIban });
    return response.data;
  },

  // 6. Otomatik maaş ödeme ayarlarını günceller (PUT)
  updateAutoPaymentSettings: async (data: AutoPaymentSettingsRequest): Promise<AutoPaymentSettingsResponse> => {
    const response = await api.put(`${BASE_URL}/auto-payment-settings`, data);
    return response.data;
  },

  // 🚀 7. YENİ: Otomatik maaş ödeme ayarlarını getirir (GET)
  getAutoPaymentSettings: async (): Promise<AutoPaymentSettingsResponse> => {
    const response = await api.get(`${BASE_URL}/auto-payment-settings`);
    return response.data;
  }
};