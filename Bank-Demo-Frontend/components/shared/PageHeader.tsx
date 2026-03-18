"use client";

import { useRouter } from "next/navigation";
import { Button as ShadcnButton } from "@/components/ui/button"; 
import { ArrowLeft } from "lucide-react";

interface PageHeaderProps {
  title: string;
  description?: string;
  showBackButton?: boolean;
  action?: React.ReactNode; // Sağ tarafa eklenecek butonlar için (örn: Yeni Hesap Aç)
}

export function PageHeader({ title, description, showBackButton = true, action }: PageHeaderProps) {
  const router = useRouter();

  return (
    <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 mb-6">
      <div className="flex items-center gap-4">
        {showBackButton && (
          <ShadcnButton
            variant="outline"
            size="icon"
            onClick={() => router.back()}
            className="rounded-full bg-white shadow-sm border-slate-200 hover:bg-slate-50 cursor-pointer transition-all active:scale-95"
            title="Geri Dön"
          >
            <ArrowLeft className="w-5 h-5 text-slate-600" />
          </ShadcnButton>
        )}
        <div>
          <h1 className="text-3xl font-black text-slate-900 tracking-tight">
            {title}
          </h1>
          {description && (
            <p className="text-slate-500 text-sm mt-1">{description}</p>
          )}
        </div>
      </div>

      {/* Sağ taraftaki aksiyon butonu (isteğe bağlı) */}
      {action && <div className="flex items-center gap-3">{action}</div>}
    </div>
  );
}