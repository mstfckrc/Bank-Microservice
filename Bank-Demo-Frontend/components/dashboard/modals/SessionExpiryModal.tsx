"use client";

import { Button } from "@/components/ui/button";

interface SessionExpiryModalProps {
  isOpen: boolean;
  onLogout: () => void;
}

export default function SessionExpiryModal({ isOpen, onLogout }: SessionExpiryModalProps) {
  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-100 flex items-center justify-center bg-slate-900/40 backdrop-blur-md transition-all duration-500">
      <div className="bg-white p-8 rounded-2xl shadow-2xl max-w-md w-full text-center border border-slate-200 animate-in fade-in zoom-in duration-300">
        <div className="w-16 h-16 bg-red-100 text-red-600 rounded-full flex items-center justify-center mx-auto mb-4">
          <svg xmlns="http://www.w3.org/2000/svg" className="h-8 w-8" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z" />
          </svg>
        </div>
        
        <h2 className="text-2xl font-bold text-slate-900 mb-2">Oturum Süresi Doldu</h2>
        <p className="text-slate-600 mb-6 text-sm">
          Güvenliğiniz için oturumunuz kilitlenmiştir. İşlemlerinize devam etmek için lütfen tekrar giriş yapın.
        </p>

        <Button 
          className="w-full bg-slate-900 hover:bg-slate-800 py-6 text-base font-semibold" 
          onClick={onLogout}
        >
          Tekrar Giriş Yap
        </Button>
      </div>
    </div>
  );
}