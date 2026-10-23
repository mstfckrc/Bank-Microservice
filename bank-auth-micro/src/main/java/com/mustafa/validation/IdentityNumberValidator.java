package com.mustafa.validation;

/** Registration input checks only; neither an ownership check nor a KPS lookup. */
public final class IdentityNumberValidator {

    private IdentityNumberValidator() {
    }

    public static boolean isValidTckn(String identityNumber) {
        if (identityNumber == null || !identityNumber.matches("[1-9][0-9]{10}")) {
            return false;
        }

        int oddSum = 0;
        int evenSum = 0;
        for (int index = 0; index < 9; index++) {
            int digit = identityNumber.charAt(index) - '0';
            if (index % 2 == 0) {
                oddSum += digit;
            } else {
                evenSum += digit;
            }
        }

        int tenthDigit = identityNumber.charAt(9) - '0';
        int eleventhDigit = identityNumber.charAt(10) - '0';
        return Math.floorMod(7 * oddSum - evenSum, 10) == tenthDigit
                && (oddSum + evenSum + tenthDigit) % 10 == eleventhDigit;
    }

    public static boolean isValidVknFormat(String identityNumber) {
        // VKN is separate; do not apply the TCKN checksum to companies.
        return identityNumber != null && identityNumber.matches("[0-9]{10}");
    }
}
