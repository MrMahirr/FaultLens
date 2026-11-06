"use client";

import { useTranslations } from "next-intl";
import { ArrowLeft, ArrowRight } from "lucide-react";
import { Input } from "@/shared/components/ui/Input";
import { Button } from "@/shared/components/ui/Button";
import { LogSourceType } from "@/shared/types/common.types";

interface SourceConfigFormStepProps {
  selectedType: LogSourceType | null;
  name: string;
  setName: (v: string) => void;
  host: string;
  setHost: (v: string) => void;
  namespace: string;
  setNamespace: (v: string) => void;
  username: string;
  setUsername: (v: string) => void;
  password: string;
  setPassword: (v: string) => void;
  logFilePath: string;
  setLogFilePath: (v: string) => void;
  onBack: () => void;
  onNext: () => void;
}

export function SourceConfigFormStep({
  selectedType,
  name,
  setName,
  host,
  setHost,
  namespace,
  setNamespace,
  username,
  setUsername,
  password,
  setPassword,
  logFilePath,
  setLogFilePath,
  onBack,
  onNext,
}: SourceConfigFormStepProps) {
  const t = useTranslations("sources.wizard");

  return (
    <form
      className="stack gap-4 swA"
      onSubmit={(e) => {
        e.preventDefault();
        if (name) onNext();
      }}
    >
      <Input
        label={t("name")}
        placeholder={t("namePlaceholder")}
        value={name}
        onChange={(e) => setName(e.target.value)}
        required
        autoFocus
      />

      {selectedType === LogSourceType.KUBERNETES && (
        <Input
          label={t("namespace")}
          placeholder="default"
          value={namespace}
          onChange={(e) => setNamespace(e.target.value)}
          className="mono"
        />
      )}

      {selectedType === LogSourceType.SSH && (
        <>
          <Input
            label={t("host")}
            placeholder="127.0.0.1:22"
            value={host}
            onChange={(e) => setHost(e.target.value)}
            className="mono"
          />
          <Input
            label={t("username")}
            placeholder="root"
            autoComplete="off"
            value={username}
            onChange={(e) => setUsername(e.target.value)}
          />
          <Input
            label={t("password")}
            type="password"
            autoComplete="new-password"
            hint={t("passwordHint")}
            value={password}
            onChange={(e) => setPassword(e.target.value)}
          />
          <Input
            label={t("logFilePath")}
            placeholder="/var/log/syslog"
            value={logFilePath}
            onChange={(e) => setLogFilePath(e.target.value)}
            className="mono"
          />
        </>
      )}

      {selectedType === LogSourceType.DOCKER && (
        <>
          <Input
            label={t("container")}
            placeholder="faultlens-frontend"
            value={logFilePath}
            onChange={(e) => setLogFilePath(e.target.value)}
            className="mono"
          />
          <Input
            label={t("dockerHost")}
            placeholder={t("dockerHostPlaceholder")}
            value={host}
            onChange={(e) => setHost(e.target.value)}
            className="mono"
          />
        </>
      )}

      {selectedType === LogSourceType.LOCAL_FILE && (
        <Input
          label={t("localPath")}
          placeholder={t("localPathPlaceholder")}
          value={logFilePath}
          onChange={(e) => setLogFilePath(e.target.value)}
          className="mono"
        />
      )}

      <div className="flex gap-2 pt-2">
        <Button
          variant="secondary"
          icon={<ArrowLeft size={14} aria-hidden="true" />}
          onClick={onBack}
        >
          {t("back")}
        </Button>
        <Button
          type="submit"
          variant="primary"
          icon={<ArrowRight size={14} aria-hidden="true" />}
          iconPosition="right"
          disabled={!name}
          fullWidth
        >
          {t("next")}
        </Button>
      </div>
    </form>
  );
}
