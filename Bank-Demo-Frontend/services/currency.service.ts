// services/currency.service.ts
import api from '../lib/axios';
import { ExchangeRateResponse } from '../types';

export const currencyService = {
  getRates: async (base: string = 'TRY'): Promise<ExchangeRateResponse> => {
    // GET /api/v1/currencies/rates?base=TRY
    const response = await api.get<ExchangeRateResponse>('/currencies/rates', {
      params: { base }
    });
    return response.data;
  },
};