import api from '../lib/axios'; // axios instance'ının yolunu kendi projene göre ayarla
import { BillInstructionRequest, BillInstructionResponse } from '../types';

const BASE_URL = '/bills/instructions';

export const billService = {
  // Tüm talimatları getir
  getMyInstructions: async (): Promise<BillInstructionResponse[]> => {
    const response = await api.get(BASE_URL);
    return response.data;
  },

  // Yeni talimat oluştur
  createInstruction: async (data: BillInstructionRequest): Promise<BillInstructionResponse> => {
    const response = await api.post(BASE_URL, data);
    return response.data;
  },

  // Talimat iptal et (Sil)
  deleteInstruction: async (id: number): Promise<{ message: string }> => {
    const response = await api.delete(`${BASE_URL}/${id}`);
    return response.data;
  }
};