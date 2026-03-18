import { useState, useCallback } from 'react';
import { companyService } from '../services/company.service';
import { CompanyEmployeeResponse, HireEmployeeRequest, TransactionResponse, UpdateEmployeeRequest } from '../types';

export const useCompanyEmployees = () => {
  const [employees, setEmployees] = useState<CompanyEmployeeResponse[]>([]);
  const [loading, setLoading] = useState<boolean>(false); // Sayfa ilk açıldığındaki yükleme
  const [isProcessing, setIsProcessing] = useState<boolean>(false); // Butona basıldığındaki (ekleme/silme) yükleme
  const [error, setError] = useState<string | null>(null);

  // 1. Çalışanları Listele
  const fetchEmployees = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await companyService.getMyEmployees();
      setEmployees(data);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Çalışan listesi çekilemedi.');
    } finally {
      setLoading(false);
    }
  }, []);

  // 2. Yeni Personel Ekle
  const hireEmployee = async (requestData: HireEmployeeRequest): Promise<boolean> => {
    setIsProcessing(true);
    setError(null);
    try {
      const newEmployee = await companyService.hireEmployee(requestData);
      // Backend'e tekrar istek atmadan, yeni personeli anında ekrana (state'e) ekliyoruz
      setEmployees((prev) => [...prev, newEmployee]);
      return true; // İşlem başarılı
    } catch (err: any) {
      setError(err.response?.data?.message || 'Personel işe alınırken bir hata oluştu.');
      return false; // İşlem başarısız
    } finally {
      setIsProcessing(false);
    }
  };

  // 3. Maaş / IBAN Güncelle
  const updateEmployee = async (
    employeeIdentityNumber: string,
    requestData: UpdateEmployeeRequest
  ): Promise<boolean> => {
    setIsProcessing(true);
    setError(null);
    try {
      const updatedEmployee = await companyService.updateEmployee(employeeIdentityNumber, requestData);
      // Sadece güncellenen personeli bulup state içinde değiştiriyoruz
      setEmployees((prev) =>
        prev.map((emp) => (emp.identityNumber === employeeIdentityNumber ? updatedEmployee : emp))
      );
      return true;
    } catch (err: any) {
      setError(err.response?.data?.message || 'Personel bilgileri güncellenemedi.');
      return false;
    } finally {
      setIsProcessing(false);
    }
  };

  // 4. Personeli İşten Çıkar
  const removeEmployee = async (employeeIdentityNumber: string): Promise<boolean> => {
    setIsProcessing(true);
    setError(null);
    try {
      await companyService.removeEmployee(employeeIdentityNumber);
      // Silinen personeli ekrandan anında uçuruyoruz
      setEmployees((prev) => prev.filter((emp) => emp.identityNumber !== employeeIdentityNumber));
      return true;
    } catch (err: any) {
      setError(err.response?.data?.message || 'Personel silinirken bir hata oluştu.');
      return false;
    } finally {
      setIsProcessing(false);
    }
  };

  // 🚀 5. TOPLU MAAŞ DAĞITIMI MANTIĞI
  const paySalaries = async (senderIban: string): Promise<TransactionResponse[] | null> => {
    setIsProcessing(true);
    setError(null);
    try {
      // Servise gidip tüm işlemi hallediyor ve bize dekont listesini dönüyor
      const results = await companyService.paySalaries(senderIban);
      return results; // Başarılıysa listeyi döndür (Modal'da fiş basmak için lazım olacak)
    } catch (err: any) {
      setError(err.response?.data?.message || 'Maaş ödemesi sırasında bir hata oluştu.');
      return null; // Başarısızsa null dön
    } finally {
      setIsProcessing(false);
    }
  };

  return {
    employees,
    loading,
    isProcessing,
    error,
    fetchEmployees,
    hireEmployee,
    updateEmployee,
    removeEmployee,
    paySalaries,
  };
};