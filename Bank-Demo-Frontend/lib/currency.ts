const currencyNames = new Intl.DisplayNames(["tr"], { type: "currency", fallback: "code" });
const nameCollator = new Intl.Collator("tr");

export const FEATURED_CURRENCIES = ["USD", "EUR", "GBP"] as const;
const priorityOrder = ["USD", "EUR", "GBP", "CHF", "JPY", "CAD", "AUD", "CNY", "SAR", "AED"];

export function getCurrencyName(code: string): string {
  return currencyNames.of(code) || code;
}

// The TRY-base response describes 1 TRY = rate units of the foreign currency.
export function toTryRate(rate: number | undefined): number | null {
  if (typeof rate !== "number" || !Number.isFinite(rate) || rate <= 0) return null;
  const value = 1 / rate;
  return Number.isFinite(value) && value > 0 ? value : null;
}

export function formatTryRate(value: number | null): string {
  if (value === null) return "—";
  return new Intl.NumberFormat("tr-TR", {
    style: "currency",
    currency: "TRY",
    minimumFractionDigits: 2,
    maximumFractionDigits: value < 0.01 ? 8 : 4,
  }).format(value);
}

export function normalizeCurrencySearch(value: string): string {
  return value.toLocaleLowerCase("tr-TR").normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "").replace(/ı/g, "i").trim();
}

export function compareCurrencies(a: { code: string; name: string }, b: { code: string; name: string }): number {
  const aRank = priorityOrder.indexOf(a.code);
  const bRank = priorityOrder.indexOf(b.code);
  const priority = (aRank < 0 ? priorityOrder.length : aRank) - (bRank < 0 ? priorityOrder.length : bRank);
  return priority || nameCollator.compare(a.name, b.name) || a.code.localeCompare(b.code);
}

export function formatRateTimestamp(seconds: number | undefined): string | null {
  if (typeof seconds !== "number" || !Number.isFinite(seconds) || seconds <= 0) return null;
  const date = new Date(seconds * 1000);
  if (!Number.isFinite(date.getTime())) return null;
  return new Intl.DateTimeFormat("tr-TR", {
    dateStyle: "short",
    timeStyle: "short",
    timeZone: "Europe/Istanbul",
  }).format(date);
}
