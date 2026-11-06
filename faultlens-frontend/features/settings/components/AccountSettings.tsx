"use client";

import { useState, useEffect } from "react";
import { useTranslations } from "next-intl";
import toast from "react-hot-toast";
import { Save, Shield } from "lucide-react";
import { Input } from "@/shared/components/ui/Input";
import { Button } from "@/shared/components/ui/Button";
import { useAuthStore } from "@/features/auth/store/auth.store";
import { apiClient } from "@/shared/api/client";
import { Endpoints } from "@/shared/api/endpoints";
import { getApiErrorMessage } from "@/shared/api/errors";

export function AccountSettings() {
  const t = useTranslations("settings.account");
  const user = useAuthStore((state) => state.user);
  const updateUser = useAuthStore((state) => state.updateUser);

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
      const response = await apiClient.put(Endpoints.AUTH.PROFILE, { username, email });
      const profile = response.data?.data;
      const { user: currentUser, login } = useAuthStore.getState();
      if (profile?.token && currentUser) {
        // The token's subject is the username: after a rename only the fresh token is valid.
        login(profile.token, { ...currentUser, username: profile.username, email: profile.email });
      } else if (profile) {
        updateUser({ username: profile.username, email: profile.email });
      }
      toast.success(t("profileSaved"));
    } catch (err) {
      toast.error(getApiErrorMessage(err, t("profileFailed")));
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
      toast.success(t("passwordSaved"));
    } catch (err) {
      toast.error(getApiErrorMessage(err, t("passwordFailed")));
    } finally {
      setIsPasswordSaving(false);
    }
  };

  const passwordTooShort = newPassword.length > 0 && newPassword.length < 8;
  const mismatch = !!newPassword && !!confirmPassword && newPassword !== confirmPassword;

  return (
    <div className="stack gap-4">
      <header>
        <h2 className="h2">{t("title")}</h2>
        <p className="small t3 mt-1">{t("subtitle")}</p>
      </header>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
        <form
          className="card"
          aria-labelledby="profile-title"
          onSubmit={(e) => {
            e.preventDefault();
            void handleSaveProfile();
          }}
        >
          <div className="panel-h">
            <h3 id="profile-title" className="h3">
              {t("profile")}
            </h3>
          </div>
          <div className="p-4 stack gap-4">
            {isProfileLoading ? (
              <div className="stack" aria-hidden="true">
                <div className="skel h-[58px]" />
                <div className="skel h-[58px]" />
              </div>
            ) : (
              <>
                <Input
                  label={t("username")}
                  autoComplete="username"
                  value={username}
                  onChange={(e) => setUsername(e.target.value)}
                />
                <Input
                  label={t("email")}
                  type="email"
                  autoComplete="email"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                />
                <div>
                  <Button
                    type="submit"
                    variant="primary"
                    loading={isSaving}
                    icon={<Save size={15} aria-hidden="true" />}
                  >
                    {t("saveProfile")}
                  </Button>
                </div>
              </>
            )}
          </div>
        </form>

        <form
          className="card"
          aria-labelledby="security-title"
          onSubmit={(e) => {
            e.preventDefault();
            void handleSavePassword();
          }}
        >
          <div className="panel-h">
            <h3 id="security-title" className="h3 row">
              <Shield size={15} className="t3" aria-hidden="true" />
              {t("security")}
            </h3>
          </div>
          <div className="p-4 stack gap-4">
            {/* Hidden username helps password managers pair the new password with the account. */}
            <input type="text" autoComplete="username" value={username} readOnly hidden />
            <Input
              label={t("currentPassword")}
              type="password"
              autoComplete="current-password"
              value={currentPassword}
              onChange={(e) => setCurrentPassword(e.target.value)}
            />
            <Input
              label={t("newPassword")}
              type="password"
              autoComplete="new-password"
              value={newPassword}
              onChange={(e) => setNewPassword(e.target.value)}
              error={passwordTooShort ? t("passwordTooShort") : undefined}
            />
            <Input
              label={t("confirmPassword")}
              type="password"
              autoComplete="new-password"
              value={confirmPassword}
              onChange={(e) => setConfirmPassword(e.target.value)}
              error={mismatch ? t("passwordMismatch") : undefined}
            />
            <div>
              <Button
                type="submit"
                variant="primary"
                loading={isPasswordSaving}
                disabled={
                  !currentPassword || newPassword.length < 8 || newPassword !== confirmPassword
                }
              >
                {t("savePassword")}
              </Button>
            </div>
          </div>
        </form>
      </div>
    </div>
  );
}
