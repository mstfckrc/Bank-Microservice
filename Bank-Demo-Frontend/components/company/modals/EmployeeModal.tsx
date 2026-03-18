'use client';

import React, { useState, useEffect } from 'react';
import { CompanyEmployeeResponse, HireEmployeeRequest, UpdateEmployeeRequest } from '@/types';

interface EmployeeModalProps {
  isOpen: boolean;
  onClose: () => void;
  selectedEmployee: CompanyEmployeeResponse | null;
  onSave: (data: HireEmployeeRequest | UpdateEmployeeRequest, isEditMode: boolean) => Promise<boolean>;
}

export default function EmployeeModal({ isOpen, onClose, selectedEmployee, onSave }: EmployeeModalProps) {
  
  const isEditMode = !!selectedEmployee; 

  const [identityNumber, setIdentityNumber] = useState('');
  const [salaryIban, setSalaryIban] = useState('');
  const [salaryAmount, setSalaryAmount] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);

  useEffect(() => {
    if (selectedEmployee) {
      setIdentityNumber(selectedEmployee.identityNumber);
      setSalaryIban(selectedEmployee.salaryIban);
      setSalaryAmount(selectedEmployee.salaryAmount.toString());
    } else {
      setIdentityNumber('');
      setSalaryIban('');
      setSalaryAmount('');
    }
  }, [selectedEmployee, isOpen]);

  if (!isOpen) return null;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsSubmitting(true);

    try {
      let success = false;
      const amount = parseFloat(salaryAmount);

      if (isEditMode) {
        const updateData: UpdateEmployeeRequest = {
          salaryIban,
          salaryAmount: amount,
        };
        success = await onSave(updateData, true);
      } else {
        const hireData: HireEmployeeRequest = {
          identityNumber,
          salaryIban,
          salaryAmount: amount,
        };
        success = await onSave(hireData, false);
      }

      if (success) onClose();
      
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    // 🚀 DÜZELTME: Arka plan bg-black/60 yapıldı, mobil için p-4 eklendi.
    <div className="fixed inset-0 bg-black/60 z-50 flex items-center justify-center p-4 backdrop-blur-sm transition-opacity">
      <div className="bg-white rounded-2xl shadow-2xl w-full max-w-md p-6 transform transition-all">
        
        {/* BAŞLIK */}
        <div className="flex justify-between items-center border-b border-gray-100 pb-3 mb-4">
          <h3 className="text-xl font-bold text-gray-800">
            {isEditMode ? 'Personel Bilgilerini Güncelle' : 'Yeni Personel Ekle'}
          </h3>
          <button onClick={onClose} disabled={isSubmitting} className="text-gray-400 hover:text-gray-600 transition-colors">
            <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>

        {/* FORM */}
        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">TC Kimlik Numarası</label>
            <input
              type="text"
              required
              disabled={isEditMode || isSubmitting}
              value={identityNumber}
              onChange={(e) => setIdentityNumber(e.target.value)}
              placeholder="11 Haneli TC Kimlik"
              className="w-full border border-gray-300 rounded-lg p-2.5 focus:ring-2 focus:ring-blue-500 focus:border-blue-500 disabled:bg-gray-100 disabled:text-gray-500 outline-none transition-all"
            />
            {isEditMode && <p className="text-xs text-gray-500 mt-1">Personelin TC Kimlik numarası değiştirilemez.</p>}
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Maaş IBAN Numarası</label>
            <input
              type="text"
              required
              disabled={isSubmitting}
              value={salaryIban}
              onChange={(e) => setSalaryIban(e.target.value)}
              placeholder="TR..."
              className="w-full border border-gray-300 rounded-lg p-2.5 focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none transition-all font-mono text-sm"
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Başlangıç Maaşı (TL)</label>
            <input
              type="number"
              required
              disabled={isSubmitting}
              min="1"
              step="0.01"
              value={salaryAmount}
              onChange={(e) => setSalaryAmount(e.target.value)}
              placeholder="Örn: 25000"
              className="w-full border border-gray-300 rounded-lg p-2.5 focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none transition-all"
            />
          </div>

          <div className="flex justify-end space-x-3 pt-4 border-t border-gray-100 mt-6">
            <button
              type="button"
              onClick={onClose}
              disabled={isSubmitting}
              className="px-4 py-2 text-gray-600 bg-gray-100 hover:bg-gray-200 rounded-lg font-medium transition-colors disabled:opacity-50"
            >
              İptal
            </button>
            <button
              type="submit"
              disabled={isSubmitting}
              className="px-4 py-2 text-white bg-blue-600 hover:bg-blue-700 rounded-lg font-medium transition-colors shadow-sm disabled:opacity-50 flex items-center"
            >
              {isSubmitting ? (
                <>
                  <svg className="animate-spin h-4 w-4 mr-2 text-white" viewBox="0 0 24 24">
                    <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" fill="none"></circle>
                    <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                  </svg>
                  İşleniyor...
                </>
              ) : isEditMode ? 'Güncelle' : 'Personeli Ekle'}
            </button>
          </div>
        </form>

      </div>
    </div>
  );
}