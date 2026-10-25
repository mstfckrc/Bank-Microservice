// services/auth.service.ts
import api from '../lib/axios';
import { RegisterRequest } from '../types';

export const authService = {
  // Giriş Keycloak OIDC authorization-code flow ile; yalnız kayıt BFF API'sine gider.
  register: async (data: RegisterRequest): Promise<void> => {
    await api.post('/auth/register', data);
  }
};
