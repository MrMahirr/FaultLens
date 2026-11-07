"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import toast from "react-hot-toast";
import { AlertTriangle, Copy, KeyRound } from "lucide-react";
import { Button } from "@/shared/components/ui/Button";
import { Endpoints } from "@/shared/api/endpoints";
import { useCan } from "@/features/auth/hooks/useCan";
import { useCreateApiKeyMutation } from "@/features/apiKeys/api/useApiKeys";

interface IngestSetupProps {
  sourceId: number;
  sourceName: string;
}

function CopyBlock({ text, label }: { text: string; label: string }) {
  const tCommon = useTranslations("common");
  const copy = async () => {
    try {
      await navigator.clipboard.writeText(text);
      toast.success(tCommon("copied"));
    } catch {
      // Clipboard unavailable (insecure context): the text stays selectable.
    }
  };
  return (
    <div className="relative">
      <pre className="code pr-12" aria-label={label}>
        {text}
      </pre>
      <Button
        variant="ghost"
        size="sm"
        className="absolute top-1.5 right-1.5"
        icon={<Copy size={13} aria-hidden="true" />}
        aria-label={`${tCommon("copy")}: ${label}`}
        onClick={copy}
      />
    </div>
  );
}

/**
 * How to push logs to an HTTP source (P10 / 1.2): creates the source-bound logs:ingest key
 * (admins) and shows ready-to-copy curl and OpenTelemetry Collector snippets.
 */
export function IngestSetup({ sourceId, sourceName }: IngestSetupProps) {
  const t = useTranslations("sources.ingest");
  const isAdmin = useCan("manageUsers");
  const createKey = useCreateApiKeyMutation();
  const [token, setToken] = useState<string | null>(null);

  const keyValue = token ?? "$FAULTLENS_API_KEY";
  const curl = `curl -fsS -X POST ${Endpoints.INGEST.JSON} \\
  -H "Authorization: Bearer ${keyValue}" \\
  -H "Content-Type: application/json" \\
  -d '[{"severity":"ERROR","message":"Payment failed","service_name":"orders","timestamp":"2026-11-07T10:00:00Z"}]'`;
  const otel = `exporters:
  otlphttp/faultlens:
    endpoint: ${Endpoints.INGEST.OTLP_BASE}
    headers:
      Authorization: "Bearer \${env:FAULTLENS_API_KEY}"

service:
  pipelines:
    logs:
      receivers: [otlp]
      exporters: [otlphttp/faultlens]`;

  const create = () =>
    createKey.mutate(
      { name: `${sourceName}-ingest`, scopes: ["logs:ingest"], sourceId, expiresInDays: 365 },
      { onSuccess: (created) => setToken(created.token) },
    );

  return (
    <div className="stack gap-4 swA">
      <div>
        <h3 className="h3">{t("title")}</h3>
        <p className="small t2 mt-1">{t("intro")}</p>
      </div>

      {token ? (
        <div className="stack gap-2">
          <div className="banner c-warn" role="alert">
            <AlertTriangle size={14} aria-hidden="true" />
            <p className="small">{t("keyCreated")}</p>
          </div>
          <CopyBlock text={token} label="API key" />
        </div>
      ) : isAdmin ? (
        <div>
          <Button
            variant="primary"
            icon={<KeyRound size={14} aria-hidden="true" />}
            loading={createKey.isPending}
            onClick={create}
          >
            {t("createKey")}
          </Button>
        </div>
      ) : (
        <p className="banner c-info small">{t("adminRequired")}</p>
      )}

      <section className="field">
        <h4 className="eyebrow">{t("jsonTitle")}</h4>
        <CopyBlock text={curl} label={t("jsonTitle")} />
      </section>

      <section className="field">
        <h4 className="eyebrow">{t("otelTitle")}</h4>
        <p className="help">{t("otelHint")}</p>
        <CopyBlock text={otel} label={t("otelTitle")} />
      </section>

      <p className="xs t3">{t("limits")}</p>
    </div>
  );
}
