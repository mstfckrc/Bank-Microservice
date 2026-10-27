import { useEffect, useState, type FormEvent } from "react";
import { useAuthStore } from "@/store/useAuthStore";
import { customerService } from "@/services/customer.service";
import type { UserProfileResponse } from "@/types";
import { toast } from "sonner";

function formFor(profile: UserProfileResponse) {
  return {
    profileName: profile.profileName,
    firstName: profile.firstName ?? "",
    lastName: profile.lastName ?? "",
    email: profile.email,
  };
}

export function useSettings() {
  const user = useAuthStore((state) => state.user);
  const updateUser = useAuthStore((state) => state.updateUser);
  const [profile, setProfile] = useState<UserProfileResponse | null>(null);
  const [profileForm, setProfileForm] = useState({ profileName: "", firstName: "", lastName: "", email: "" });
  const [fetchingProfile, setFetchingProfile] = useState(true);
  const [loadingProfile, setLoadingProfile] = useState(false);
  const [loadFailed, setLoadFailed] = useState(false);
  const [reload, setReload] = useState(0);

  useEffect(() => {
    let active = true;
    if (new URLSearchParams(window.location.search).get("identity") === "unavailable") {
      toast.error("Merkezi hesap ekranı şu anda açılamıyor.");
    }
    void customerService.getProfile().then((data) => {
      if (!active) return;
      setProfile(data);
      setProfileForm(formFor(data));
      setLoadFailed(false);
      updateUser({ profileName: data.profileName, email: data.email });
    }).catch(() => { if (active) setLoadFailed(true); })
      .finally(() => { if (active) setFetchingProfile(false); });
    return () => { active = false; };
  }, [reload, updateUser]);

  const isCorporate = (profile?.role ?? user?.role) === "CORPORATE_MANAGER";
  const profileEditable = profile?.profileEditable === true;
  const nameValid = isCorporate
    ? profileForm.profileName.trim().length >= 3 && profileForm.profileName.trim().length <= 150
    : profileForm.firstName.trim().length > 0 && profileForm.firstName.length <= 100
      && profileForm.lastName.trim().length > 0 && profileForm.lastName.length <= 100;
  const email = profileForm.email.trim();
  const fieldsValid = nameValid && email.length <= 100 && /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);
  const changed = Boolean(profile && (email !== profile.email || (isCorporate
    ? profileForm.profileName.trim() !== profile.profileName
    : profileForm.firstName.trim() !== profile.firstName || profileForm.lastName.trim() !== profile.lastName)));
  const canSave = profileEditable && !fetchingProfile && !loadingProfile && !loadFailed && fieldsValid && changed;

  const handleUpdateProfile = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!canSave) return;
    try {
      setLoadingProfile(true);
      const data = await customerService.updateProfile(isCorporate
        ? { profileName: profileForm.profileName.trim(), email }
        : { firstName: profileForm.firstName.trim(), lastName: profileForm.lastName.trim(), email });
      setProfile(data);
      setProfileForm(formFor(data));
      updateUser({ profileName: data.profileName, email: data.email });
      toast.success("Profil bilgileri güncellendi.");
    } catch {
      // The shared API interceptor displays the server error; retain the entered values.
    } finally {
      setLoadingProfile(false);
    }
  };

  const reloadProfile = () => {
    setFetchingProfile(true);
    setReload((value) => value + 1);
  };

  return {
    user: profile ?? user, profileForm, setProfileForm, isCorporate, profileEditable,
    fetchingProfile, loadingProfile, loadFailed, canSave, handleUpdateProfile, reloadProfile,
  };
}
