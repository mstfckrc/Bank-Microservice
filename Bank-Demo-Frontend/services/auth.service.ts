// services/auth.service.ts
import api from '../lib/axios';
import { LoginRequest, RegisterRequest, AuthResponse } from '../types';

export const authService = {
  /**
   * 1. Kullanıcı Girişi (Login)
   * Backend'e TC Kimlik ve Şifre gönderir, karşılığında JWT Token alır.
   */
  login: async (data: LoginRequest): Promise<AuthResponse> => {
    // Axios kuryemiz '/auth/login' adresine POST isteği atıyor
    const response = await api.post<AuthResponse>('/auth/login', data);
    return response.data; // Gelen JSON'ın içindeki { token: "..." } kısmını döner
  },

  /**
   * 2. Yeni Müşteri Kaydı (Register)
   * Yeni kullanıcı bilgilerini gönderir.
   */
  register: async (data: RegisterRequest): Promise<void> => {
    // Kayıt başarılıysa backend genellikle bir şey dönmez veya mesaj döner.
    // O yüzden tipi Promise<void> veya dönen mesaja göre ayarlayabiliriz.
    await api.post('/auth/register', data);
  }
};