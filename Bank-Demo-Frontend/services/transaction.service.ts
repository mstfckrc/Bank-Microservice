// services/transaction.service.ts
import api from '../lib/axios';
import { DepositRequest, TransactionResponse, TransferRequest } from '../types';

export const transactionService = {
  // 🚀 PARA TRANSFERİ (Havale / EFT)
  transfer: async (data: TransferRequest): Promise<any> => {
    // Backend'indeki POST /api/v1/transactions/transfer ucuna gidiyoruz
    const response = await api.post('/transactions/transfer', data);
    return response.data;
  },

  getAccountTransactions: async (accountNumber: string): Promise<TransactionResponse[]> => {
    const response = await api.get<TransactionResponse[]>(`/transactions/account/${accountNumber}`);
    return response.data;
  },

  deposit: async (data: DepositRequest): Promise<TransactionResponse> => {
    // POST /api/v1/transactions/deposit
    const response = await api.post<TransactionResponse>('/transactions/deposit', data);
    return response.data;
  },
};