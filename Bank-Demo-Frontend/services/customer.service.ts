import api from '../lib/axios';

export const customerService = {
  // Kullanıcının kendi profilini güncellemesi (🚀 fullName -> profileName oldu)
  updateProfile: async (data: { profileName: string; email: string }) => {
    const response = await api.put('/customers/profile', data);
    return response.data;
  },

  // Kullanıcının kendi şifresini değiştirmesi
  changePassword: async (data: { oldPassword: string; newPassword: string }) => {
    const response = await api.put('/customers/password', data);
    return response.data;
  },

  // Token ile kullanıcının kendi profilini çekmesi
  getProfile: async (token: string) => {
    const response = await api.get('/customers/profile', {
      headers: {
        Authorization: `Bearer ${token}`
      }
    });
    return response.data;
  },

  // Reddedilen kullanıcının itiraz etmesi
  appealRejection: async () => {
    const response = await api.post('/customers/appeal');
    return response.data;
  }
};