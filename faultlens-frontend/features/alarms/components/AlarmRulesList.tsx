"use client";

import { useMemo } from "react";
import { useTranslations } from "next-intl";
import { Power, PowerOff, Trash2 } from "lucide-react";
import { useCan } from "@/features/auth/hooks/useCan";
import { DataTable, type DataTableColumnDef } from "@/shared/components/ui/Table";
import { Badge } from "@/shared/components/ui/Badge";
import { Button } from "@/shared/components/ui/Button";
import { SeverityBadge } from "@/shared/components/ui/SeverityBadge";
import { useConfirm } from "@/shared/components/ui/ConfirmDialog";
import {
  useAlarmRulesQuery,
  useDeleteAlarmRuleMutation,
  useToggleAlarmRuleMutation,
} from "../api/useAlarmRules";
import { describeRule } from "../lib/ruleForm";
import type { AlarmRule } from "../types/alarm.types";

export function AlarmRulesList() {
  const t = useTranslations("alarms");
  const tDescribe = useTranslations("alarms.rules.describe");
  const { data: rules, isLoading } = useAlarmRulesQuery();
  const toggleRule = useToggleAlarmRuleMutation();
  const deleteRule = useDeleteAlarmRuleMutation();
  const canEdit = useCan("edit");
  const { confirm, dialog } = useConfirm();
  const { mutate: toggle } = toggleRule;
  const isMutating = toggleRule.isPending || deleteRule.isPending;

  const columns = useMemo<DataTableColumnDef<AlarmRule>[]>(
    () => [
      {
        accessorKey: "name",
        header: t("rules.columns.name"),
        cell: ({ row }) => (
          <div className="font-medium text-text-primary min-w-[8rem]">{row.original.name}</div>
        ),
      },
      {
        id: "condition",
        header: t("rules.columns.condition"),
        cell: ({ row }) => (
          <code className="mono px-1.5 py-0.5 well text-text-secondary">
            {describeRule(row.original, tDescribe)}
          </code>
        ),
      },
      {
        accessorKey: "severity",
        header: t("rules.columns.severity"),
        cell: ({ row }) => <SeverityBadge severity={row.original.severity} orAbove />,
      },
      {
        id: "scope",
        header: t("rules.columns.channels"),
        cell: ({ row }) => (
          <span className="xs text-text-secondary whitespace-nowrap">
            {row.original.channels.map((c) => t(`channel.${c}`)).join(", ")}
          </span>
        ),
      },
      {
        accessorKey: "enabled",
        header: t("rules.columns.status"),
        cell: ({ row }) => (
          <Badge variant={row.original.enabled ? "success" : "default"} dot>
            {row.original.enabled ? t("rules.active") : t("rules.inactive")}
          </Badge>
        ),
      },
      {
        id: "actions",
        header: t("rules.columns.actions"),
        cell: ({ row }) => {
          const rule = row.original;
          if (!canEdit) return null;
          const toggleLabel = rule.enabled ? t("rules.deactivate") : t("rules.activate");
          return (
            <div className="flex items-center gap-1 whitespace-nowrap">
              <Button
                variant="ghost"
                size="sm"
                icon={
                  rule.enabled ? (
                    <PowerOff size={14} aria-hidden="true" />
                  ) : (
                    <Power size={14} aria-hidden="true" />
                  )
                }
                onClick={(e) => {
                  e.stopPropagation();
                  toggle({ id: rule.id, enabled: !rule.enabled });
                }}
                disabled={isMutating}
                title={toggleLabel}
                aria-label={toggleLabel}
              />
              <Button
                variant="ghost"
                size="sm"
                className="text-error"
                icon={<Trash2 size={14} aria-hidden="true" />}
                onClick={(e) => {
                  e.stopPropagation();
                  confirm({
                    title: t("rules.deleteTitle"),
                    description: t("rules.deleteDescription", { name: rule.name }),
                    onConfirm: () => deleteRule.mutateAsync(rule.id),
                  });
                }}
                disabled={isMutating}
                title={t("rules.delete")}
                aria-label={t("rules.delete")}
              />
            </div>
          );
        },
      },
    ],
    [t, tDescribe, toggle, deleteRule, confirm, isMutating, canEdit],
  );

  return (
    <>
      <DataTable
        label={t("tabs.rules")}
        data={rules ?? []}
        columns={columns}
        loading={isLoading}
        emptyMessage={t("rules.empty")}
      />
      {dialog}
    </>
  );
}
