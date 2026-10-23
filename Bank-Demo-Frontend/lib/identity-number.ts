// UX validation only. Auth repeats these checks before creating a Keycloak user.
export function isValidTckn(identityNumber: string): boolean {
  if (!/^[1-9][0-9]{10}$/.test(identityNumber)) return false;

  const digits = Array.from(identityNumber, Number);
  let oddSum = 0;
  let evenSum = 0;
  for (let index = 0; index < 9; index++) {
    if (index % 2 === 0) oddSum += digits[index];
    else evenSum += digits[index];
  }

  const tenthDigit = ((7 * oddSum - evenSum) % 10 + 10) % 10;
  return tenthDigit === digits[9]
    && (oddSum + evenSum + digits[9]) % 10 === digits[10];
}

export function isValidVknFormat(identityNumber: string): boolean {
  return /^[0-9]{10}$/.test(identityNumber);
}
