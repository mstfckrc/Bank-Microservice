// services/currency.service.ts
import api from '../lib/axios';
import { ExchangeRateResponse } from '../types';

export const currencyService = {
  getRates: async (base: string = 'TRY'): Promise<ExchangeRateResponse> => {
    // GET /api/v1/currencies/rates?base=TRY
    const response = await api.get<ExchangeRateResponse>('/currencies/rates', {
      params: { base }
    });
    const data = response.data;
    if (!data || data.base_code !== base.toUpperCase() || !data.rates
      || typeof data.rates !== "object" || Array.isArray(data.rates)) {
      throw new Error("Kur verisi beklenen biçimde değil.");
    }
    return data;
  },
};
