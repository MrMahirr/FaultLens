"use client";

import { useState } from "react";
import { Input } from "@/shared/components/ui/Input";
import { Button } from "@/shared/components/ui/Button";
import { useAuthStore } from "@/features/auth/store/auth.store";
import { Save, Shield } from "lucide-react";

export function AccountSettings() {
  const { user } = useAuthStore();
  const [username, setUsername] = useState(user?.username || "admin");
  const [email, setEmail] = useState("admin@faultlens.com");

  const [currentPassword, setCurrentPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [isSaving, setIsSaving] = useState(false);
  const [saveSuccess, setSaveSuccess] = useState(false);

  const handleSaveProfile = () => {
    setIsSaving(true);
    setTimeout(() => {
      setIsSaving(false);
      setSaveSuccess(true);
      setTimeout(() => setSaveSuccess(false), 3000);
    }, 1000);
  };

  const handleSavePassword = () => {
    // Mock save
    setCurrentPassword("");
    setNewPassword("");
    setConfirmPassword("");
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
          <div className="pt-2">
            <Button
              variant="primary"
              onClick={handleSaveProfile}
              loading={isSaving}
              icon={<Save size={16} />}
            >
              Bilgileri Kaydet
            </Button>
            {saveSuccess && (
              <span className="ml-3 text-sm text-success animate-fade-in">
                Başarıyla kaydedildi!
              </span>
            )}
          </div>
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
          <div className="pt-2">
            <Button
              variant="primary"
              onClick={handleSavePassword}
              disabled={
                !currentPassword ||
                !newPassword ||
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
