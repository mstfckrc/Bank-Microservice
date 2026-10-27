import api from '../lib/axios';
import type { UpdateProfileRequest, UserProfileResponse } from "@/types";

export const customerService = {
  // Kullanıcının kendi profilini güncellemesi (🚀 fullName -> profileName oldu)
  updateProfile: async (data: UpdateProfileRequest): Promise<UserProfileResponse> => {
    const response = await api.put<UserProfileResponse>('/customers/profile', data);
    return response.data;
  },

  // Auth bilgisi tarayıcıdan değil, Next.js BFF oturumundan taşınır.
  getProfile: async (): Promise<UserProfileResponse> => {
    const response = await api.get<UserProfileResponse>('/customers/profile');
    return response.data;
  },

  // Reddedilen kullanıcının itiraz etmesi
  appealRejection: async () => {
    const response = await api.post('/customers/appeal', {});
    return response.data;
  }
};
