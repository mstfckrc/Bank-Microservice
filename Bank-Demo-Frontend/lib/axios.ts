import axios from 'axios';
import Cookies from 'js-cookie';
import { toast } from 'sonner'; // 🚀 Bildirimler için eklendi

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
  (response) => response, // Başarılı cevapları olduğu gibi geçir
  (error) => {
    const isLoginRequest = error.config?.url?.includes('/login');
    const status = error.response?.status;
    const message = error.response?.data?.message || "Beklenmedik bir hata oluştu.";

    // 🚀 MERKEZİ HATA BİLDİRİMİ (Global Toast)
    // 401 ve 403 hatalarını zaten aşağıda redirect ile yönetiyoruz, 
    // onlar dışındaki tüm hataları (400, 404, 500 vb.) burada kullanıcıya patlatıyoruz.
    if (status !== 401 && status !== 403) {
      toast.error("İşlem Başarısız", {
        description: message,
      });
    }

    // 401 veya 403 Hataları (Yetki Sorunları)
    if (status === 401 || status === 403) {
      if (!isLoginRequest) {
        // Eğer zaten login sayfasında değilsek dışarı at
        Cookies.remove('token');
        if (typeof window !== 'undefined') {
          // Toast ile bilgi verip yönlendir
          toast.error("Oturum Geçersiz", { description: "Lütfen tekrar giriş yapın." });
          window.location.href = '/login';
        }
      }
    }
    
    return Promise.reject(error);
  }
);

export default api;