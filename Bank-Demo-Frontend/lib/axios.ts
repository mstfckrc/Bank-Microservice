import axios from 'axios';
import Cookies from 'js-cookie';
import { toast } from 'sonner';

// 1. Temel Axios Kopyamız
const api = axios.create({
  baseURL: process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080/api/v1',
  headers: {
    'Content-Type': 'application/json',
  },
});

// 2. İSTEK (REQUEST) INTERCEPTOR
api.interceptors.request.use(
  (config) => {
    const token = Cookies.get('token');
    if (token && config.headers) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// 3. YANIT (RESPONSE) INTERCEPTOR
api.interceptors.response.use(
  (response) => response, 
  (error) => {
    const isLoginRequest = error.config?.url?.includes('/login');
    const status = error.response?.status;
    const message = error.response?.data?.message || "Beklenmedik bir hata oluştu.";

    // 401 ve 403 DIŞINDAKİ Hatalar
    if (status !== 401 && status !== 403) {
      toast.error("İşlem Başarısız", {
        description: message,
      });
    }

    // 🚀 401 veya 403 Hataları (Yetki Sorunları) -> KEYCLOAK ÇIKIŞI
    if (status === 401 || status === 403) {
      if (!isLoginRequest) {
        const idToken = Cookies.get('id_token');
        
        // Kendi iç çerezlerimizi temizle
        Cookies.remove('token');
        Cookies.remove('id_token');
        
        if (typeof window !== 'undefined') {
          toast.error("Oturum Geçersiz", { description: "Lütfen tekrar giriş yapın." });
          
          // Keycloak Çıkış URL'si Hazırlığı
          const keycloakUrl = process.env.NEXT_PUBLIC_KEYCLOAK_URL || "http://localhost:9090";
          const redirectUri = encodeURIComponent("http://localhost:3000/"); // Anasayfaya döner
          let logoutUrl = `${keycloakUrl}/realms/bank-realm/protocol/openid-connect/logout?post_logout_redirect_uri=${redirectUri}&client_id=bank-auth-client`;
          
          if (idToken) {
            logoutUrl += `&id_token_hint=${idToken}`;
          }

          // Adamı siteden kopar ve Keycloak'a fırlat
          window.location.href = logoutUrl;
        }
      }
    }
    
    return Promise.reject(error);
  }
);

export default api;