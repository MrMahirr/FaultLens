"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import toast from "react-hot-toast";
import { useQueryClient } from "@tanstack/react-query";
import { BrainCircuit, HardDrive, Languages, ShieldAlert } from "lucide-react";
import { Button } from "@/shared/components/ui/Button";
import { ErrorState } from "@/shared/components/feedback/ErrorState";
import { apiClient } from "@/shared/api/client";
import { Endpoints } from "@/shared/api/endpoints";
import { getApiErrorMessage } from "@/shared/api/errors";
import { LOCALES, isLocale } from "@/i18n/config";
import {
  systemSettingsKey,
  useSystemSettingsQuery,
  type SystemSettingsDto,
} from "@/features/settings/api/useSystemSettings";

const RETENTION_DAYS = [7, 15, 30, 90, 120, 0];

function Section({
  id,
  icon,
  title,
  hint,
  children,
}: {
  id: string;
  icon: React.ReactNode;
  title: string;
  hint: string;
  children: React.ReactNode;
}) {
  return (
    <section className="card" aria-labelledby={id}>
      <div className="panel-h">
        <h3 id={id} className="h3 row">
          <span className="t3 inline-flex" aria-hidden="true">
            {icon}
          </span>
          {title}
        </h3>
      </div>
      <div className="p-4 stack gap-3">
        <p className="xs t3">{hint}</p>
        {children}
      </div>
    </section>
  );
}

/** The editable form, seeded once from the loaded settings (remounted after a save). */
function SystemSettingsForm({ initial }: { initial: SystemSettingsDto }) {
  const t = useTranslations("settings.system");
  const tSettings = useTranslations("settings");
  const tShell = useTranslations("shell");
  const queryClient = useQueryClient();
  const [retention, setRetention] = useState(String(initial.logRetentionDays ?? 30));
  const [engine, setEngine] = useState(initial.defaultEngine ?? "ai");
  const [language, setLanguage] = useState(isLocale(initial.language) ? initial.language : "tr");
  const [isSaving, setIsSaving] = useState(false);
  const aiAvailable = Boolean(initial.aiAvailable);

  const dirty =
    retention !== String(initial.logRetentionDays) ||
    engine !== initial.defaultEngine ||
    language !== initial.language;

  const handleSave = async () => {
    setIsSaving(true);
    try {
      const response = await apiClient.put(Endpoints.SETTINGS.SYSTEM, {
        logRetentionDays: parseInt(retention, 10),
        defaultEngine: engine,
        language,
      });
      const data = response.data?.data as SystemSettingsDto | undefined;
      if (data) queryClient.setQueryData(systemSettingsKey, data);
      toast.success(t("saved"));
    } catch (err) {
      toast.error(getApiErrorMessage(err, t("saveFailed")));
    } finally {
      setIsSaving(false);
    }
  };

  return (
    <div className="stack gap-4">
      <header className="between">
        <div>
          <h2 className="h2">{t("title")}</h2>
          <p className="small t3 mt-1">{t("subtitle")}</p>
        </div>
        <Button
          variant="primary"
          size="sm"
          onClick={handleSave}
          loading={isSaving}
          disabled={!dirty}
        >
          {tSettings("saveChanges")}
        </Button>
      </header>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        <Section
          id="sys-retention"
          icon={<HardDrive size={15} />}
          title={t("retention")}
          hint={t("retentionHint")}
        >
          <label htmlFor="sys-retention-select" className="sr-only">
            {t("retention")}
          </label>
          <select
            id="sys-retention-select"
            className="input"
            value={retention}
            onChange={(e) => setRetention(e.target.value)}
          >
            {RETENTION_DAYS.map((days) => (
              <option key={days} value={days}>
                {days === 0 ? t("retentionForever") : t("retentionDays", { days })}
              </option>
            ))}
          </select>
        </Section>

        <Section
          id="sys-engine"
          icon={<BrainCircuit size={15} />}
          title={t("engine")}
          hint={t("engineHint")}
        >
          <label htmlFor="sys-engine-select" className="sr-only">
            {t("engine")}
          </label>
          <select
            id="sys-engine-select"
            className="input"
            value={engine}
            onChange={(e) => setEngine(e.target.value)}
          >
            <option value="rule">{t("engines.rule")}</option>
            <option value="ai" disabled={!aiAvailable && engine !== "ai"}>
              {t("engines.ai")}
              {aiAvailable ? "" : ` — ${t("engineUnavailable")}`}
            </option>
          </select>
          {!aiAvailable && (
            <p className="xs text-warning">
              {t("aiOff")} {engine === "ai" && t("aiOffSelected")}
            </p>
          )}
          {engine === "ai" && aiAvailable && (
            <p className="banner c-warn xs" role="note">
              <ShieldAlert size={14} aria-hidden="true" />
              <span>{t("aiPrivacy")}</span>
            </p>
          )}
        </Section>

        <Section
          id="sys-language"
          icon={<Languages size={15} />}
          title={t("language")}
          hint={t("languageHint")}
        >
          <label htmlFor="sys-language-select" className="sr-only">
            {t("language")}
          </label>
          <select
            id="sys-language-select"
            className="input"
            value={language}
            onChange={(e) => setLanguage(e.target.value as typeof language)}
          >
            {LOCALES.map((l) => (
              <option key={l} value={l} lang={l}>
                {tShell(`languages.${l}`)}
              </option>
            ))}
          </select>
        </Section>
      </div>

      {dirty && (
        <p className="xs t3" role="status">
          {tSettings("unsaved")}
        </p>
      )}
    </div>
  );
}

export function SystemSettings() {
  const { data, isError, error, refetch } = useSystemSettingsQuery();

  if (isError) return <ErrorState error={error as Error} onRetry={() => void refetch()} />;

  if (!data) {
    return (
      <div className="stack" aria-hidden="true">
        <div className="skel h-12 w-72" />
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div className="skel h-36" />
          <div className="skel h-36" />
        </div>
      </div>
    );
  }

  // Keyed on the stored values: after a save the form restarts from the saved state.
  return (
    <SystemSettingsForm
      key={`${data.logRetentionDays}-${data.defaultEngine}-${data.language}`}
      initial={data}
    />
  );
}
