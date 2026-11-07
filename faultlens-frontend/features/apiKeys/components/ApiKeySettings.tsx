"use client";

import { useMemo, useState } from "react";
import { useTranslations } from "next-intl";
import toast from "react-hot-toast";
import { AlertTriangle, Copy, KeyRound, Plus, Trash2 } from "lucide-react";
import { Badge } from "@/shared/components/ui/Badge";
import { Button } from "@/shared/components/ui/Button";
import { Input } from "@/shared/components/ui/Input";
import { Modal } from "@/shared/components/ui/Modal";
import { useConfirm } from "@/shared/components/ui/ConfirmDialog";
import { DataTable, type DataTableColumnDef } from "@/shared/components/ui/Table";
import { useDateFormat } from "@/shared/hooks/useDateFormat";
import { useValidationMessage } from "@/shared/hooks/useValidationMessage";
import { Endpoints } from "@/shared/api/endpoints";
import {
  useApiKeysQuery,
  useCreateApiKeyMutation,
  useRevokeApiKeyMutation,
} from "../api/useApiKeys";
import {
  API_KEY_SCOPES,
  type ApiKey,
  type ApiKeyScope,
  type CreatedApiKey,
} from "../types/apiKey.types";

const EXPIRY_OPTIONS = ["30", "90", "365", "never"] as const;
type Expiry = (typeof EXPIRY_OPTIONS)[number];

/* ── Create dialog ─────────────────────────────────────────── */

function CreateKeyDialog({
  isOpen,
  onClose,
  onCreated,
}: {
  isOpen: boolean;
  onClose: () => void;
  onCreated: (created: CreatedApiKey) => void;
}) {
  const t = useTranslations("settings.apiKeys");
  const tCommon = useTranslations("common");
  const v = useValidationMessage();
  const create = useCreateApiKeyMutation();
  const [name, setName] = useState("");
  const [scopes, setScopes] = useState<ApiKeyScope[]>(["deployments:write"]);
  const [expiry, setExpiry] = useState<Expiry>("365");
  const [touched, setTouched] = useState(false);

  const reset = () => {
    setName("");
    setScopes(["deployments:write"]);
    setExpiry("365");
    setTouched(false);
  };

  const close = () => {
    reset();
    onClose();
  };

  const submit = () => {
    setTouched(true);
    if (!name.trim() || scopes.length === 0) return;
    create.mutate(
      {
        name: name.trim(),
        scopes,
        expiresInDays: expiry === "never" ? undefined : Number(expiry),
      },
      {
        onSuccess: (created) => {
          reset();
          onCreated(created);
        },
      },
    );
  };

  return (
    <Modal
      isOpen={isOpen}
      onClose={close}
      title={t("form.title")}
      footer={
        <>
          <Button variant="secondary" onClick={close}>
            {tCommon("cancel")}
          </Button>
          <Button
            type="submit"
            form="create-api-key-form"
            variant="primary"
            loading={create.isPending}
          >
            {t("form.submit")}
          </Button>
        </>
      }
    >
      <form
        id="create-api-key-form"
        className="stack gap-4"
        noValidate
        onSubmit={(e) => {
          e.preventDefault();
          submit();
        }}
      >
        <Input
          label={t("form.name")}
          placeholder={t("form.namePlaceholder")}
          value={name}
          onChange={(e) => setName(e.target.value)}
          maxLength={100}
          error={touched && !name.trim() ? v("required") : undefined}
          required
          autoFocus
        />

        <fieldset className="field">
          <legend className="label mb-1.5">{t("form.scopes")}</legend>
          <div className="stack gap-2">
            {API_KEY_SCOPES.map((scope) => (
              <label key={scope} className="flex items-start gap-2.5 small">
                <input
                  type="checkbox"
                  className="check mt-0.5"
                  checked={scopes.includes(scope)}
                  onChange={(e) =>
                    setScopes((current) =>
                      e.target.checked ? [...current, scope] : current.filter((s) => s !== scope),
                    )
                  }
                />
                <span>
                  <span className="mono">{scope}</span>
                  <span className="block xs t3">{t(`scopeHelp.${scope}`)}</span>
                </span>
              </label>
            ))}
          </div>
          {touched && scopes.length === 0 && (
            <p className="err" role="alert">
              {t("form.scopeRequired")}
            </p>
          )}
        </fieldset>

        <div className="field">
          <label htmlFor="api-key-expiry" className="label">
            {t("form.expiry")}
          </label>
          <select
            id="api-key-expiry"
            className="input"
            value={expiry}
            onChange={(e) => setExpiry(e.target.value as Expiry)}
          >
            {EXPIRY_OPTIONS.map((option) => (
              <option key={option} value={option}>
                {t(`form.expiryOptions.${option}`)}
              </option>
            ))}
          </select>
        </div>
      </form>
    </Modal>
  );
}

/* ── One-time token dialog ─────────────────────────────────── */

function CreatedKeyDialog({
  created,
  onClose,
}: {
  created: CreatedApiKey | null;
  onClose: () => void;
}) {
  const t = useTranslations("settings.apiKeys.created");

  const copy = async () => {
    if (!created) return;
    try {
      await navigator.clipboard.writeText(created.token);
      toast.success(t("copied"));
    } catch {
      // Clipboard unavailable (insecure context): the key stays selectable in the dialog.
    }
  };

  const example = created
    ? `curl -X POST ${Endpoints.DEPLOYMENTS.CREATE} \\\n  -H "Authorization: Bearer $FAULTLENS_API_KEY" \\\n  -H "Content-Type: application/json" \\\n  -d '{"service_name":"my-service","version":"v1.0.0","environment":"PRODUCTION","deployed_at":"2026-01-01T00:00:00Z"}'`
    : "";

  return (
    <Modal
      isOpen={created !== null}
      onClose={onClose}
      title={t("title")}
      description={created?.key.name}
      closeOnBackdrop={false}
      footer={
        <Button variant="primary" onClick={onClose}>
          {t("done")}
        </Button>
      }
    >
      <div className="banner c-warn" role="alert">
        <AlertTriangle size={14} aria-hidden="true" />
        <p className="small">{t("warning")}</p>
      </div>

      <div className="flex items-stretch gap-2">
        <code className="code flex-1 whitespace-pre-wrap break-all" data-testid="api-key-token">
          {created?.token}
        </code>
        <Button variant="secondary" icon={<Copy size={14} aria-hidden="true" />} onClick={copy}>
          {t("copy")}
        </Button>
      </div>

      {created?.key.scopes.includes("deployments:write") && (
        <section className="field">
          <h3 className="eyebrow">{t("usage")}</h3>
          <pre className="code">{example}</pre>
        </section>
      )}
    </Modal>
  );
}

/* ── Section ───────────────────────────────────────────────── */

export function ApiKeySettings() {
  const t = useTranslations("settings.apiKeys");
  const dates = useDateFormat();
  const { data: keys, isLoading } = useApiKeysQuery();
  const revoke = useRevokeApiKeyMutation();
  const { confirm, dialog } = useConfirm();
  const [showCreate, setShowCreate] = useState(false);
  const [created, setCreated] = useState<CreatedApiKey | null>(null);

  const columns = useMemo<DataTableColumnDef<ApiKey>[]>(
    () => [
      {
        accessorKey: "name",
        header: t("columns.name"),
        cell: ({ row }) => (
          <div className="min-w-[9rem]">
            <div className="font-medium">{row.original.name}</div>
            {row.original.createdBy && <div className="xs t3">{row.original.createdBy}</div>}
          </div>
        ),
      },
      {
        accessorKey: "displayKey",
        header: t("columns.key"),
        cell: ({ row }) => <code className="mono t2">{row.original.displayKey}</code>,
      },
      {
        id: "scopes",
        header: t("columns.scopes"),
        cell: ({ row }) => (
          <div className="row gap-1">
            {row.original.scopes.map((scope) => (
              <Badge key={scope} variant="outline" mono>
                {scope}
              </Badge>
            ))}
          </div>
        ),
      },
      {
        accessorKey: "lastUsedAt",
        header: t("columns.lastUsed"),
        cell: ({ row }) =>
          row.original.lastUsedAt ? (
            <span className="xs" title={row.original.lastUsedIp ?? undefined}>
              {dates.relative(row.original.lastUsedAt)}
            </span>
          ) : (
            <span className="xs t3">{t("never")}</span>
          ),
      },
      {
        id: "status",
        header: t("columns.status"),
        cell: ({ row }) => {
          const key = row.original;
          if (key.revokedAt) return <Badge dot>{t("revoked")}</Badge>;
          if (!key.active)
            return (
              <Badge variant="warning" dot>
                {t("expired")}
              </Badge>
            );
          return (
            <div className="stack gap-0.5">
              <Badge variant="success" dot>
                {t("active")}
              </Badge>
              <span className="xs t3">
                {key.expiresAt ? t("expires", { date: dates.date(key.expiresAt) }) : t("noExpiry")}
              </span>
            </div>
          );
        },
      },
      {
        id: "actions",
        header: t("columns.actions"),
        cell: ({ row }) =>
          row.original.active ? (
            <Button
              variant="ghost"
              size="sm"
              className="text-error"
              icon={<Trash2 size={14} aria-hidden="true" />}
              aria-label={t("revoke")}
              title={t("revoke")}
              onClick={() =>
                confirm({
                  title: t("revokeTitle", { name: row.original.name }),
                  description: t("revokeDescription"),
                  confirmLabel: t("revokeConfirm"),
                  onConfirm: () => revoke.mutateAsync(row.original.id),
                })
              }
            />
          ) : null,
      },
    ],
    [t, dates, confirm, revoke],
  );

  return (
    <div className="stack gap-4">
      <header className="between">
        <div>
          <h2 className="h2">{t("title")}</h2>
          <p className="small t3 mt-1 max-w-2xl">{t("subtitle")}</p>
        </div>
        <Button
          variant="primary"
          size="sm"
          icon={<Plus size={14} aria-hidden="true" />}
          onClick={() => setShowCreate(true)}
        >
          {t("create")}
        </Button>
      </header>

      {!isLoading && (keys ?? []).length === 0 ? (
        <div className="card empty">
          <KeyRound size={18} aria-hidden="true" />
          <p className="h3 text-text-primary">{t("empty")}</p>
          <p className="small">{t("emptyHint")}</p>
        </div>
      ) : (
        <DataTable
          label={t("list")}
          data={keys ?? []}
          columns={columns}
          loading={isLoading}
          enableSorting={false}
        />
      )}

      <CreateKeyDialog
        isOpen={showCreate}
        onClose={() => setShowCreate(false)}
        onCreated={(result) => {
          setShowCreate(false);
          setCreated(result);
        }}
      />
      <CreatedKeyDialog created={created} onClose={() => setCreated(null)} />
      {dialog}
    </div>
  );
}
