import { useState, useEffect } from "react";
import { useAuthStore } from "@/store/useAuthStore";
import { customerService } from "@/services/customer.service";
import { adminService } from "@/services/admin.service";
import { toast } from "sonner";

export function useSettings(isAdmin: boolean = false) {
  const { user, updateUser } = useAuthStore();
  
  const [loadingProfile, setLoadingProfile] = useState(false);
  const [loadingPass, setLoadingPass] = useState(false);

  // 🚀 V2: fullName yerine profileName kullanıyoruz
  const [profileForm, setProfileForm] = useState({ profileName: "", email: "" });
  const [passForm, setPassForm] = useState({ oldPassword: "", newPassword: "" });

  useEffect(() => {
    if (user) {
      setProfileForm({
        profileName: user.profileName || "",
        email: user.email || "",
      });
    }
  }, [user]);

  const handleUpdateProfile = async () => {
    if (!user?.identityNumber) return; // 🚀 V2: tcNo kontrolü yerine identityNumber
    try {
      setLoadingProfile(true);
      
      const updatedData = isAdmin 
        ? await adminService.updateCustomer(user.identityNumber, profileForm)
        : await customerService.updateProfile(profileForm);
      
      // Store'u güncelle
      updateUser({
        profileName: updatedData.profileName,
        email: updatedData.email
      });
      
      toast.success("Profil bilgileri başarıyla güncellendi.");
    } finally {
      setLoadingProfile(false);
    }
  };

  const handleChangePassword = async () => {
    try {
      setLoadingPass(true);
      await customerService.changePassword(passForm);
      toast.success("Şifreniz başarıyla değiştirildi.");
      setPassForm({ oldPassword: "", newPassword: "" }); 
    } finally {
      setLoadingPass(false);
    }
  };

  return {
    user,
    profileForm,
    setProfileForm,
    passForm,
    setPassForm,
    loadingProfile,
    loadingPass,
    handleUpdateProfile,
    handleChangePassword
  };
}