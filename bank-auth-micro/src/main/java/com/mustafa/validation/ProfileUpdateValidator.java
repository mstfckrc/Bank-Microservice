package com.mustafa.validation;

import com.mustafa.dto.request.UpdateProfileRequest;
import com.mustafa.entity.AppUser;
import com.mustafa.exception.BankOperationException;

public final class ProfileUpdateValidator {
    private ProfileUpdateValidator() {
    }

    public static void validate(UpdateProfileRequest request, AppUser.Role role) {
        if (request.getEmail() != null
                && (request.getEmail().isBlank() || request.getEmail().length() > 100)) {
            throw new BankOperationException("İletişim e-postası boş olamaz ve en fazla 100 karakter olabilir.");
        }
        if (role == AppUser.Role.CORPORATE_MANAGER) {
            if (request.getFirstName() != null || request.getLastName() != null) {
                throw new BankOperationException("Kurumsal profilde kişi adı yerine şirket unvanını kullanın.");
            }
            if (request.getProfileName() != null
                    && (request.getProfileName().trim().length() < 3 || request.getProfileName().trim().length() > 150)) {
                throw new BankOperationException("Şirket unvanı 3 ile 150 karakter arasında olmalıdır.");
            }
        } else {
            if (request.getProfileName() != null) {
                throw new BankOperationException("Ad ve soyadı ayrı alanlarda gönderin.");
            }
            if (request.getFirstName() != null || request.getLastName() != null) {
                if (request.getFirstName() == null || request.getFirstName().isBlank()
                        || request.getFirstName().length() > 100 || request.getLastName() == null
                        || request.getLastName().isBlank() || request.getLastName().length() > 100) {
                    throw new BankOperationException("Ad ve soyad zorunludur; her biri en fazla 100 karakter olabilir.");
                }
            }
        }
        if (request.getEmail() == null && request.getProfileName() == null
                && request.getFirstName() == null && request.getLastName() == null) {
            throw new BankOperationException("Güncellenecek profil bilgisi gönderilmedi.");
        }
    }
}
