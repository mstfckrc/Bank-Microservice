#!/usr/bin/env python3
"""Create a local Compose .env once, without printing generated secrets."""

from pathlib import Path
import os
import re
import secrets
import sys
import tempfile


ROOT = Path(__file__).resolve().parent.parent
TEMPLATE = ROOT / ".env.example"
DESTINATION = ROOT / ".env"
GENERATED_KEYS = {
    "BFF_SESSION_PASSWORD",
    "CORE_DB_PASS",
    "AUTH_DB_PASS",
    "BILL_DB_PASS",
    "CORP_DB_PASS",
    "KC_DB_PASS",
    "KC_ADMIN_PASS",
    "KEYCLOAK_CLIENT_SECRET",
    "KEYCLOAK_ADMIN_CLIENT_SECRET",
    "CORPORATE_BANK_SERVICE_CLIENT_SECRET",
    "AUTH_BANK_SERVICE_CLIENT_SECRET",
    "BILL_BANK_SERVICE_CLIENT_SECRET",
    "CORE_BANK_SERVICE_CLIENT_SECRET",
    "RMQ_PASS",
    "JWT_SECRET",
    "GRAFANA_PASS",
    "BANK_BOOTSTRAP_ADMIN_MARKER",
}
REQUIRED_KEYS = GENERATED_KEYS | {
    "BANK_BOOTSTRAP_ADMIN_ENABLED",
    "BANK_BOOTSTRAP_ADMIN_IDENTITY_NUMBER",
    "BANK_BOOTSTRAP_ADMIN_FIRST_NAME",
    "BANK_BOOTSTRAP_ADMIN_LAST_NAME",
    "BANK_BOOTSTRAP_ADMIN_EMAIL",
    "BANK_BOOTSTRAP_ADMIN_PASSWORD",
}
ASSIGNMENT = re.compile(r"^([A-Z][A-Z0-9_]*)=")


def main() -> int:
    try:
        if os.path.lexists(DESTINATION):
            raise FileExistsError
        template = TEMPLATE.read_text(encoding="utf-8")
        seen: set[str] = set()
        lines: list[str] = []
        for line in template.splitlines(keepends=True):
            match = ASSIGNMENT.match(line)
            key = match.group(1) if match else None
            if key in REQUIRED_KEYS:
                if key in seen:
                    raise ValueError(f"Tekrarlanan ayar: {key}")
                seen.add(key)
                if key == "BANK_BOOTSTRAP_ADMIN_ENABLED":
                    line = "BANK_BOOTSTRAP_ADMIN_ENABLED=true\n"
                elif key in GENERATED_KEYS:
                    # Hex works without shell or Compose interpolation/quoting and
                    # also satisfies the existing 256-bit JWT secret requirement.
                    line = f"{key}={secrets.token_hex(32)}\n"
            lines.append(line)
        missing = REQUIRED_KEYS - seen
        if missing:
            raise ValueError("Şablonda eksik ayarlar: " + ", ".join(sorted(missing)))

        # Write a restricted temporary file first; an interrupted write must
        # never leave an incomplete .env that looks like a finished setup.
        descriptor, temporary = tempfile.mkstemp(prefix=".env.prepare-", dir=ROOT)
        try:
            with os.fdopen(descriptor, "w", encoding="utf-8") as output:
                output.writelines(lines)
                output.flush()
                os.fsync(output.fileno())
            # A hard link is atomic and refuses to replace an existing .env,
            # including symlinks, even if another process created it meanwhile.
            os.link(temporary, DESTINATION)
        finally:
            os.unlink(temporary)
    except FileExistsError:
        print(".env zaten var; mevcut ayarlar değiştirilmedi.", file=sys.stderr)
        return 1
    except (OSError, ValueError) as error:
        print(f"Yerel .env hazırlanamadı: {error}", file=sys.stderr)
        return 1

    print(".env oluşturuldu; üretilen gizli değerler yalnızca dosyaya yazıldı.")
    print("Yerel demo yönetici hesabı şablondaki sabit bilgilerle hazırlanır; reCAPTCHA anahtarları ve makineye özgü ayarlar ayrıca girilmelidir.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
