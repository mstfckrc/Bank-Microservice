interface StatusBadgeProps {
  type: "currency" | "status";
  value: string | boolean;
  isActive?: boolean; // Sadece döviz rozeti için (hesap kapalıysa gri yapmak için)
}

export function StatusBadge({ type, value, isActive = true }: StatusBadgeProps) {
  
  // 1. Durum (Aktif/Kapalı) Rozeti
  if (type === "status") {
    const active = value === true || value === "Aktif";
    return (
      <span className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium ${
        active ? "bg-green-100 text-green-800" : "bg-red-100 text-red-800"
      }`}>
        {active ? "Aktif" : "Kapalı"}
      </span>
    );
  }

  // 2. Döviz (TRY/USD/EUR) Rozeti
  if (type === "currency") {
    const currency = String(value).toUpperCase();
    
    // Eğer hesap kapalıysa dümdüz gri göster
    if (!isActive) {
      return <span className="px-2 py-1 rounded text-xs font-bold bg-slate-200 text-slate-500">{currency}</span>;
    }

    // Hesap aktifse renklendir
    let colors = "bg-slate-100 text-slate-700"; // Varsayılan renk
    if (currency === "TRY") colors = "bg-red-50 text-red-700";
    else if (currency === "USD") colors = "bg-green-50 text-green-700";
    else if (currency === "EUR") colors = "bg-blue-50 text-blue-700";

    return (
      <span className={`px-2 py-1 rounded text-xs font-bold ${colors}`}>
        {currency}
      </span>
    );
  }

  return null;
}