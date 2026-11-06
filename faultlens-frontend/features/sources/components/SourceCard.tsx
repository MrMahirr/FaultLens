"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import toast from "react-hot-toast";
import { MoreVertical, Wifi, WifiOff, TestTube, Trash2 } from "lucide-react";
import { Badge } from "@/shared/components/ui/Badge";
import { Button } from "@/shared/components/ui/Button";
import { Dropdown, type DropdownItem } from "@/shared/components/ui/Dropdown";
import { useConfirm } from "@/shared/components/ui/ConfirmDialog";
import { useDateFormat } from "@/shared/hooks/useDateFormat";
import type { LogSourceDto } from "@/features/sources/types/source.types";
import {
  useDeleteSourceMutation,
  useTestConnectionMutation,
  useToggleSourceMutation,
} from "@/features/sources/api/useSourceMutation";
import { useCan } from "@/features/auth/hooks/useCan";
import { SourceDetailModal } from "./SourceDetailModal";
import { SourceStatus } from "./SourceStatus";

/* ── Props ─────────────────────────────────────────────────── */

interface SourceCardProps {
  source: LogSourceDto;
  /** Position in the grid (staggered entry animation). */
  index: number;
}

function parseConfig(raw?: string): Record<string, string> {
  try {
    return raw ? JSON.parse(raw) : {};
  } catch {
    return {};
  }
}

/* ── Component ─────────────────────────────────────────────── */

function SourceCard({ source, index }: SourceCardProps) {
  const t = useTranslations("sources");
  const tToast = useTranslations("sources.toast");
  const dates = useDateFormat();
  const [isModalOpen, setIsModalOpen] = useState(false);
  const testConnection = useTestConnectionMutation();
  const toggleSource = useToggleSourceMutation();
  const deleteSource = useDeleteSourceMutation();
  const canEdit = useCan("edit");
  const canDelete = useCan("delete");
  const { confirm, dialog } = useConfirm();
  const config = parseConfig(source.config);

  // The API enforces the same rules (EDITOR: toggle/test, ADMIN: delete); hiding avoids 403s.
  const menuItems: DropdownItem[] = [];
  if (canEdit) {
    menuItems.push({
      label: source.enabled ? t("disable") : t("enable"),
      value: "toggle",
      icon: source.enabled ? <WifiOff size={14} /> : <Wifi size={14} />,
    });
  }
  if (canDelete) {
    menuItems.push({
      label: t("delete"),
      value: "delete",
      danger: true,
      icon: <Trash2 size={14} />,
    });
  }

  const handleMenu = (value: string) => {
    if (value === "toggle") {
      toggleSource.mutate({ id: source.id, enabled: !source.enabled });
    } else if (value === "delete") {
      confirm({
        title: t("deleteTitle", { name: source.name }),
        description: t("deleteDescription"),
        confirmLabel: t("deleteConfirm"),
        onConfirm: () => deleteSource.mutateAsync(source.id),
      });
    }
  };

  const failing = source.enabled && source.health === "ERROR" && source.lastError;

  return (
    <li className={`list-none in d${Math.min(index + 1, 8)}`}>
      <article
        className="card hov p-4 h-full flex flex-col gap-3"
        style={failing ? { boxShadow: "inset 2px 0 0 var(--crit)" } : undefined}
        aria-labelledby={`source-${source.id}`}
      >
        <div className="between">
          <SourceStatus status={source.status} />
          {menuItems.length > 0 && (
            <Dropdown
              label={t("actions")}
              trigger={
                <button
                  type="button"
                  className="btn btn-ghost btn-sm btn-icon"
                  aria-label={t("actions")}
                >
                  <MoreVertical size={16} aria-hidden="true" />
                </button>
              }
              items={menuItems}
              onSelect={handleMenu}
            />
          )}
        </div>

        <div>
          {/* The name opens the detail dialog; the card itself is not a button (nested controls). */}
          <h3 id={`source-${source.id}`} className="h3 text-[15px]">
            <button
              type="button"
              className="text-left hover:underline underline-offset-4"
              onClick={() => setIsModalOpen(true)}
            >
              {source.name}
            </button>
          </h3>
          <div className="row mt-1.5">
            <Badge variant="outline">{t(`types.${source.type}`)}</Badge>
            {!source.enabled && <Badge>{t("disabledBadge")}</Badge>}
          </div>
        </div>

        <dl className="stack gap-1 xs t3 m-0">
          {config.namespace && (
            <div>
              <dt className="inline">{t("namespace")}: </dt>
              <dd className="inline mono t2 m-0">{config.namespace}</dd>
            </div>
          )}
          {config.host && (
            <div>
              <dt className="inline">{t("host")}: </dt>
              <dd className="inline mono t2 m-0">{config.host}</dd>
            </div>
          )}
          <div>{t("lastLog", { time: dates.relative(source.lastSeenAt) })}</div>
          {failing && (
            <p
              className="text-error break-words"
              title={source.lastErrorAt ? dates.dateTimeLong(source.lastErrorAt) : undefined}
              data-testid="source-last-error"
              role="status"
            >
              {source.lastErrorAt
                ? t("lastError", {
                    time: dates.relative(source.lastErrorAt),
                    error: source.lastError!,
                  })
                : t("lastErrorNoTime", { error: source.lastError! })}
            </p>
          )}
          {source.enabled && source.collectorInstance && (
            <p className="ellipsis" title={source.collectorInstance}>
              {t.rich("collector", {
                instance: source.collectorInstance,
                m: (chunks) => <span className="mono">{chunks}</span>,
              })}
            </p>
          )}
        </dl>

        {canEdit && (
          <div className="mt-auto pt-1">
            <Button
              variant="secondary"
              size="sm"
              fullWidth
              icon={<TestTube size={14} aria-hidden="true" />}
              loading={testConnection.isPending}
              onClick={() =>
                testConnection.mutate(source.id, {
                  onSuccess: () => toast.success(tToast("testOk")),
                  onError: (error) => toast.error(error.message || tToast("connectFailed")),
                })
              }
              title={t("testHint")}
            >
              {t("test")}
            </Button>
          </div>
        )}
      </article>

      <SourceDetailModal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        source={source}
      />
      {dialog}
    </li>
  );
}

export { SourceCard };
