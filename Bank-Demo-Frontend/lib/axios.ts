import axios from 'axios';
import Cookies from 'js-cookie';
import { toast } from 'sonner';
import { useAuthStore } from '@/store/useAuthStore';

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

// Parallel dashboard requests must not start several logout redirects.
let logoutInProgress = false;

// 3. YANIT (RESPONSE) INTERCEPTOR
api.interceptors.response.use(
  (response) => response, 
  (error) => {
    const isLoginRequest = error.config?.url?.includes('/login');
    const status = error.response?.status;
    const message = error.response?.data?.message || "Beklenmedik bir hata oluştu.";

    if (status === 403) {
      // A permission denial does not invalidate the authenticated session.
      toast.error('İşlem için yetkiniz yok', { description: message });
    } else if (status !== 401) {
      toast.error("İşlem Başarısız", {
        description: message,
      });
    }

    if (status === 401 && !isLoginRequest && typeof window !== 'undefined' && !logoutInProgress) {
      logoutInProgress = true;
      const idToken = Cookies.get('id_token');
      useAuthStore.getState().logout();
      Cookies.remove('id_token', { path: '/' });
      toast.error('Oturum Geçersiz', { description: 'Lütfen tekrar giriş yapın.' });

      const keycloakUrl = process.env.NEXT_PUBLIC_KEYCLOAK_URL || 'http://localhost:9090';
      const frontendUrl = process.env.NEXT_PUBLIC_FRONTEND_URL || window.location.origin;
      const logoutUrl = new URL(`${keycloakUrl.replace(/\/$/, '')}/realms/bank-realm/protocol/openid-connect/logout`);
      logoutUrl.searchParams.set('post_logout_redirect_uri', new URL('/', frontendUrl).href);
      logoutUrl.searchParams.set('client_id', 'bank-auth-client');
      if (idToken) logoutUrl.searchParams.set('id_token_hint', idToken);
      window.location.assign(logoutUrl.href);
    }
    
    return Promise.reject(error);
  }
);

export default api;
