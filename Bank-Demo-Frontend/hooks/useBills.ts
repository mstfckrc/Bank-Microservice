import { useState, useCallback } from 'react';
import { BillInstructionRequest, BillInstructionResponse } from '@/types';
import { toast } from 'sonner';
import { billService } from '@/services/bill.services';

export const useBills = () => {
  const [instructions, setInstructions] = useState<BillInstructionResponse[]>([]);
  const [loading, setLoading] = useState(false);
  const [isProcessing, setIsProcessing] = useState(false);

  // Talimatları Çek
  const fetchInstructions = useCallback(async () => {
    setLoading(true);
    try {
      const data = await billService.getMyInstructions();
      setInstructions(data);
    } catch (err: any) {
      toast.error('Hata', { description: err.response?.data?.message || 'Fatura talimatları yüklenemedi.' });
    } finally {
      setLoading(false);
    }
  }, []);

  // Yeni Talimat Ekle
  const createInstruction = async (data: BillInstructionRequest) => {
    setIsProcessing(true);
    try {
      const newInstruction = await billService.createInstruction(data);
      setInstructions((prev) => [...prev, newInstruction]);
      toast.success('Başarılı', { description: 'Fatura ödeme talimatı oluşturuldu.' });
      return true;
    } catch (err: any) {
      toast.error('Talimat Oluşturulamadı', { description: err.response?.data?.message || 'Bir hata oluştu.' });
      return false;
    } finally {
      setIsProcessing(false);
    }
  };

  // Talimat Sil
  const deleteInstruction = async (id: number) => {
    setIsProcessing(true);
    try {
      const res = await billService.deleteInstruction(id);
      setInstructions((prev) => prev.filter((inst) => inst.id !== id));
      toast.success('İptal Edildi', { description: res.message });
      return true;
    } catch (err: any) {
      toast.error('Hata', { description: err.response?.data?.message || 'Talimat iptal edilemedi.' });
      return false;
    } finally {
      setIsProcessing(false);
    }
  };

  return {
    instructions,
    loading,
    isProcessing,
    fetchInstructions,
    createInstruction,
    deleteInstruction
  };
};