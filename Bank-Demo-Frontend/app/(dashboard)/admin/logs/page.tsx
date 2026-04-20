"use client";

import { useEffect, useState } from "react";
import { adminService } from "@/services/admin.service";
import { Card, CardContent, CardHeader } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { toast } from "sonner";
import { 
  Terminal, 
  Search, 
  AlertTriangle, 
  Info, 
  XCircle, 
  Activity,
  ShieldAlert,
  Loader2
} from "lucide-react";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { PageHeader } from "@/components/shared/PageHeader";

export default function SystemLogsPage() {
  const [logs, setLogs] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [limit, setLimit] = useState<number>(100);
  const [level, setLevel] = useState<string>("ALL");

  useEffect(() => {
    fetchLogs();
  }, [filterChangedTrigger()]); // Limit veya level değiştiğinde tetiklenir

  // Bağımlılıkları kolay yönetmek için yardımcı fonksiyon
  function filterChangedTrigger() {
    return `${limit}-${level}`;
  }

  const fetchLogs = async () => {
    try {
      setLoading(true);
      const data = await adminService.getSystemLogs(limit, level);
      setLogs(data);
    } catch (error) {
      toast.error("İstihbarat ağına ulaşılamadı.", { description: "Loglar çekilirken bir hata oluştu." });
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="space-y-6 max-w-7xl mx-auto p-4 md:p-0 relative">
      <PageHeader
        title="İstihbarat Merkezi (Sistem Logları)"
        description="Elasticsearch üzerinden akan tüm mikroservis aktivitelerini, uyarıları ve hataları canlı olarak izleyin."
        action={<Button onClick={fetchLogs} variant="secondary" size="sm">Listeyi Yenile</Button>}
      />

      <Card className="border-none shadow-xl">
        <CardHeader className="border-b bg-slate-50/50 pb-4">
          <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
            
            {/* 🎛️ SEVİYE FİLTRELERİ */}
            <div className="flex flex-wrap gap-2">
              <Button 
                variant={level === "ALL" ? "default" : "outline"} 
                onClick={() => setLevel("ALL")}
              >
                <Search className="w-4 h-4 mr-2" /> Tümü
              </Button>
              <Button 
                variant={level === "INFO" ? "default" : "outline"} 
                onClick={() => setLevel("INFO")} 
                className={level === "INFO" ? "bg-blue-600 hover:bg-blue-700" : ""}
              >
                <Info className="w-4 h-4 mr-2" /> Sadece Bilgi
              </Button>
              <Button 
                variant={level === "WARN" ? "default" : "outline"} 
                onClick={() => setLevel("WARN")} 
                className={level === "WARN" ? "bg-amber-500 hover:bg-amber-600" : ""}
              >
                <AlertTriangle className="w-4 h-4 mr-2" /> Uyarılar
              </Button>
              <Button 
                variant={level === "ERROR" ? "default" : "outline"} 
                onClick={() => setLevel("ERROR")} 
                className={level === "ERROR" ? "bg-red-600 hover:bg-red-700" : ""}
              >
                <XCircle className="w-4 h-4 mr-2" /> Hatalar
              </Button>
            </div>

            {/* 📏 LİMİT FİLTRELERİ */}
            <div className="flex flex-wrap items-center gap-2 border-t md:border-t-0 md:border-l pt-3 md:pt-0 md:pl-4 border-slate-200">
              <span className="text-xs font-semibold text-slate-500 uppercase mr-1">Kayıt Sayısı:</span>
              {[100, 200, 500].map((n) => (
                <Button 
                  key={n} 
                  variant={limit === n ? "secondary" : "ghost"} 
                  size="sm" 
                  onClick={() => setLimit(n)}
                  className="h-8 text-xs"
                >
                  Son {n}
                </Button>
              ))}
            </div>

          </div>
        </CardHeader>
        <CardContent className="p-0">
          {loading ? (
            <div className="flex justify-center py-20">
              <Loader2 className="h-8 w-8 animate-spin text-slate-500" />
            </div>
          ) : logs.length === 0 ? (
            <div className="p-16 text-center text-slate-400 flex flex-col items-center">
              <ShieldAlert className="w-12 h-12 mb-3 opacity-20" />
              <p>Seçilen kriterlerde log bulunamadı.</p>
            </div>
          ) : (
            <div className="overflow-x-auto max-h-150 overflow-y-auto">
              <Table>
                <TableHeader className="sticky top-0 bg-slate-50 border-b z-10 shadow-sm">
                  <TableRow>
                    <TableHead className="font-bold w-45">Tarih / Saat</TableHead>
                    <TableHead className="font-bold w-45">Servis Adı</TableHead>
                    <TableHead className="font-bold w-25 text-center">Seviye</TableHead>
                    <TableHead className="font-bold">Sistem Mesajı</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {logs.map((log, index) => {
                    const isError = log.level === "ERROR";
                    const isWarn = log.level === "WARN";

                    return (
                      <TableRow 
                        key={index} 
                        className={`border-b transition-colors ${
                          isError ? "bg-red-50/30 hover:bg-red-50" : 
                          isWarn ? "bg-amber-50/30 hover:bg-amber-50" : 
                          "hover:bg-slate-50"
                        }`}
                      >
                        <TableCell className="font-mono text-xs text-slate-500 whitespace-nowrap">
                          {log.timestamp ? new Date(log.timestamp).toLocaleString('tr-TR') : "-"}
                        </TableCell>
                        
                        <TableCell>
                          <div className="flex items-center gap-1.5">
                            <Activity className="w-3 h-3 text-slate-400" />
                            <span className="font-mono text-xs font-semibold text-slate-700">
                              {log.appName || "unknown"}
                            </span>
                          </div>
                        </TableCell>
                        
                        <TableCell className="text-center">
                          {isError && <span className="px-2 py-1 bg-red-100 text-red-700 rounded text-[10px] font-bold border border-red-200">ERROR</span>}
                          {isWarn && <span className="px-2 py-1 bg-amber-100 text-amber-700 rounded text-[10px] font-bold border border-amber-200">WARN</span>}
                          {log.level === "INFO" && <span className="px-2 py-1 bg-blue-100 text-blue-700 rounded text-[10px] font-bold border border-blue-200">INFO</span>}
                          {!["ERROR", "WARN", "INFO"].includes(log.level) && <span className="px-2 py-1 bg-slate-100 text-slate-700 rounded text-[10px] font-bold border border-slate-200">{log.level || "-"}</span>}
                        </TableCell>
                        
                        <TableCell className="text-sm text-slate-800 break-all font-mono">
                          {log.message}
                        </TableCell>
                      </TableRow>
                    );
                  })}
                </TableBody>
              </Table>
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  );
}