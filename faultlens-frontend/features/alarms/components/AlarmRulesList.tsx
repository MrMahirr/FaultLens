"use client";

import { useCallback, useMemo } from "react";
import Swal from "sweetalert2";
import { Power, PowerOff, Trash2 } from "lucide-react";
import { DataTable, type DataTableColumnDef } from "@/shared/components/ui/Table";
import { Badge } from "@/shared/components/ui/Badge";
import { Button } from "@/shared/components/ui/Button";
import { Severity } from "@/shared/types/common.types";
import {
  useAlarmRulesQuery,
  useDeleteAlarmRuleMutation,
  useToggleAlarmRuleMutation,
} from "../api/useAlarmRules";
import { describeRule } from "../lib/ruleForm";
import { ALARM_CHANNEL_LABELS, type AlarmRule } from "../types/alarm.types";

function severityVariant(severity: Severity): "error" | "warning" | "info" {
  if (severity === Severity.CRITICAL || severity === Severity.ERROR) return "error";
  if (severity === Severity.WARN) return "warning";
  return "info";
}

export function AlarmRulesList() {
  const { data: rules, isLoading } = useAlarmRulesQuery();
  const toggleRule = useToggleAlarmRuleMutation();
  const deleteRule = useDeleteAlarmRuleMutation();
  const { mutate: toggle } = toggleRule;
  const { mutate: remove } = deleteRule;
  const isMutating = toggleRule.isPending || deleteRule.isPending;

  const confirmDelete = useCallback(
    async (rule: AlarmRule) => {
      const result = await Swal.fire({
        title: "Kural silinsin mi?",
        text: `"${rule.name}" silinir; ürettiği alarmlar geçmişte kalır.`,
        icon: "warning",
        showCancelButton: true,
        confirmButtonColor: "#ef4444",
        cancelButtonColor: "#3b82f6",
        confirmButtonText: "Evet, Sil",
        cancelButtonText: "İptal",
      });
      if (result.isConfirmed) remove(rule.id);
    },
    [remove],
  );

  const columns = useMemo<DataTableColumnDef<AlarmRule>[]>(
    () => [
      {
        accessorKey: "name",
        header: "Kural Adı",
        cell: ({ row }) => (
          <div className="font-medium text-text-primary min-w-[8rem]">{row.original.name}</div>
        ),
      },
      {
        id: "condition",
        header: "Koşul",
        cell: ({ row }) => (
          <code className="px-2 py-1 bg-bg-tertiary rounded text-xs text-text-secondary font-mono">
            {describeRule(row.original)}
          </code>
        ),
      },
      {
        accessorKey: "severity",
        header: "Seviye",
        cell: ({ row }) => (
          <Badge variant={severityVariant(row.original.severity)}>{row.original.severity}+</Badge>
        ),
      },
      {
        id: "scope",
        header: "Kanallar",
        cell: ({ row }) => (
          <span className="text-xs text-text-secondary whitespace-nowrap">
            {row.original.channels.map((c) => ALARM_CHANNEL_LABELS[c]).join(", ")}
          </span>
        ),
      },
      {
        accessorKey: "enabled",
        header: "Durum",
        cell: ({ row }) => (
          <Badge variant={row.original.enabled ? "success" : "default"}>
            {row.original.enabled ? "Aktif" : "Pasif"}
          </Badge>
        ),
      },
      {
        id: "actions",
        header: "Aksiyon",
        cell: ({ row }) => {
          const rule = row.original;
          return (
            <div className="flex items-center gap-1 whitespace-nowrap">
              <Button
                variant="ghost"
                size="sm"
                icon={rule.enabled ? <PowerOff size={14} /> : <Power size={14} />}
                onClick={(e) => {
                  e.stopPropagation();
                  toggle({ id: rule.id, enabled: !rule.enabled });
                }}
                disabled={isMutating}
                title={rule.enabled ? "Pasife Al" : "Aktifleştir"}
                aria-label={rule.enabled ? "Pasife Al" : "Aktifleştir"}
                className={rule.enabled ? "text-warning" : "text-success"}
              />
              <Button
                variant="ghost"
                size="sm"
                className="text-error hover:bg-error/10 hover:text-error"
                icon={<Trash2 size={14} />}
                onClick={(e) => {
                  e.stopPropagation();
                  void confirmDelete(rule);
                }}
                disabled={isMutating}
                title="Sil"
                aria-label="Sil"
              />
            </div>
          );
        },
      },
    ],
    [toggle, confirmDelete, isMutating],
  );

  return (
    <div className="space-y-4">
      <DataTable
        data={rules ?? []}
        columns={columns}
        loading={isLoading}
        emptyMessage="Henüz alarm kuralı yok. “Yeni Kural Ekle” ile oluşturabilirsiniz."
      />
    </div>
  );
}
