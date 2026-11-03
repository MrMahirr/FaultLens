"use client";

import { useState, useEffect } from "react";
import { Input } from "@/shared/components/ui/Input";
import { Button } from "@/shared/components/ui/Button";
import { useAuthStore } from "@/features/auth/store/auth.store";
import { apiClient } from "@/shared/api/client";
import { Endpoints } from "@/shared/api/endpoints";
import { Save, Shield } from "lucide-react";
import toast from "react-hot-toast";
import { getApiErrorMessage } from "@/shared/api/errors";

export function AccountSettings() {
  const { user, updateUser } = useAuthStore();

  // Profile state
  const [username, setUsername] = useState(user?.username || "");
  const [email, setEmail] = useState(user?.email || "");
  const [isProfileLoading, setIsProfileLoading] = useState(true);
  const [isSaving, setIsSaving] = useState(false);

  // Password state
  const [currentPassword, setCurrentPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [isPasswordSaving, setIsPasswordSaving] = useState(false);

  // Fetch profile on mount; state is only set in promise callbacks and not after
  // unmount. If the API is unavailable the fields keep the values from the store.
  useEffect(() => {
    let active = true;
    apiClient
      .get(Endpoints.AUTH.PROFILE)
      .then((response) => {
        const profile = response.data?.data;
        if (active && profile) {
          setUsername(profile.username);
          setEmail(profile.email || "");
          updateUser({ username: profile.username, email: profile.email });
        }
      })
      .catch(() => {
        // keep store values
      })
      .finally(() => {
        if (active) setIsProfileLoading(false);
      });
    return () => {
      active = false;
    };
  }, [updateUser]);

  const handleSaveProfile = async () => {
    setIsSaving(true);
    try {
      const response = await apiClient.put(Endpoints.AUTH.PROFILE, {
        username,
        email,
      });
      const profile = response.data?.data;
      if (profile) {
        updateUser({ username: profile.username, email: profile.email });
      }
      toast.success("Profil bilgileri başarıyla kaydedildi!");
    } catch (err) {
      toast.error(getApiErrorMessage(err, "Profil güncellenirken bir hata oluştu."));
    } finally {
      setIsSaving(false);
    }
  };

  const handleSavePassword = async () => {
    setIsPasswordSaving(true);
    try {
      const response = await apiClient.put(Endpoints.AUTH.CHANGE_PASSWORD, {
        current_password: currentPassword,
        new_password: newPassword,
      });
      // Backend returns a fresh token after a password change
      const fresh = response.data?.data;
      const { user: currentUser, login } = useAuthStore.getState();
      if (fresh?.token && currentUser) {
        login(fresh.token, { ...currentUser, mustChangePassword: false });
      }
      setCurrentPassword("");
      setNewPassword("");
      setConfirmPassword("");
      toast.success("Şifre başarıyla güncellendi!");
    } catch (err) {
      toast.error(getApiErrorMessage(err, "Şifre değiştirilirken bir hata oluştu."));
    } finally {
      setIsPasswordSaving(false);
    }
  };

  return (
    <div className="space-y-6 animate-fade-in">
      <div>
        <h2 className="text-lg font-semibold text-text-primary">
          Hesap Ayarları
        </h2>
        <p className="text-sm text-text-muted mt-1">
          Profil bilgilerinizi ve şifrenizi güncelleyin.
        </p>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Profile Info */}
        <div className="bg-bg-tertiary/30 p-6 rounded-2xl border border-border-default space-y-5">
          <h3 className="text-sm font-medium text-text-secondary border-b border-border-default pb-3">
            Kişisel Bilgiler
          </h3>
          {isProfileLoading ? (
            <div className="space-y-4">
              <div className="h-10 bg-bg-tertiary rounded animate-pulse" />
              <div className="h-10 bg-bg-tertiary rounded animate-pulse" />
            </div>
          ) : (
            <>
              <Input
                label="Kullanıcı Adı"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
              />
              <Input
                label="E-posta Adresi"
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
              />
              <div className="pt-2 flex items-center gap-3">
                <Button
                  variant="primary"
                  onClick={handleSaveProfile}
                  loading={isSaving}
                  icon={<Save size={16} />}
                >
                  Bilgileri Kaydet
                </Button>
              </div>
            </>
          )}
        </div>

        {/* Change Password */}
        <div className="bg-bg-tertiary/30 p-6 rounded-2xl border border-border-default space-y-5">
          <h3 className="text-sm font-medium text-text-secondary border-b border-border-default pb-3 flex items-center gap-2">
            <Shield size={16} /> Güvenlik
          </h3>
          <Input
            label="Mevcut Şifre"
            type="password"
            value={currentPassword}
            onChange={(e) => setCurrentPassword(e.target.value)}
            placeholder="••••••••"
          />
          <Input
            label="Yeni Şifre"
            type="password"
            value={newPassword}
            onChange={(e) => setNewPassword(e.target.value)}
            placeholder="••••••••"
            error={
              newPassword && newPassword.length < 8
                ? "Yeni şifre en az 8 karakter olmalıdır"
                : undefined
            }
          />
          <Input
            label="Yeni Şifre (Tekrar)"
            type="password"
            value={confirmPassword}
            onChange={(e) => setConfirmPassword(e.target.value)}
            placeholder="••••••••"
            error={
              newPassword && confirmPassword && newPassword !== confirmPassword
                ? "Şifreler eşleşmiyor"
                : undefined
            }
          />
          <div className="pt-2 flex items-center gap-3">
            <Button
              variant="primary"
              onClick={handleSavePassword}
              loading={isPasswordSaving}
              disabled={
                !currentPassword ||
                newPassword.length < 8 ||
                newPassword !== confirmPassword
              }
            >
              Şifreyi Güncelle
            </Button>
          </div>
        </div>
      </div>
    </div>
  );
}
