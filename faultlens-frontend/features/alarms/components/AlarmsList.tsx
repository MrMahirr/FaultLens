"use client";

import { useMemo, useState } from "react";
import { useFormatter, useTranslations } from "next-intl";
import { Check, Hand, Trash2 } from "lucide-react";
import { useCan } from "@/features/auth/hooks/useCan";
import { DataTable, type DataTableColumnDef } from "@/shared/components/ui/Table";
import { Badge } from "@/shared/components/ui/Badge";
import { Button } from "@/shared/components/ui/Button";
import { DropdownSelect } from "@/shared/components/ui/Dropdown";
import { Pagination } from "@/shared/components/ui/Pagination";
import { SeverityBadge } from "@/shared/components/ui/SeverityBadge";
import { useConfirm } from "@/shared/components/ui/ConfirmDialog";
import { Severity } from "@/shared/types/common.types";
import { Alarm, AlarmFilters, AlarmStatus } from "../types/alarm.types";
import { AlarmDetailModal } from "./AlarmDetailModal";
import { useAlarmsQuery } from "../api/useAlarmsQuery";
import {
  useAcknowledgeAlarmMutation,
  useDeleteAlarmMutation,
  useResolveAlarmMutation,
} from "../api/useAlarmsMutation";

const PAGE_SIZE = 20;

const FILTER_SEVERITIES = [Severity.CRITICAL, Severity.ERROR, Severity.WARN, Severity.INFO];

const STATUS_BADGE: Record<AlarmStatus, "error" | "warning" | "success"> = {
  [AlarmStatus.ACTIVE]: "error",
  [AlarmStatus.ACKNOWLEDGED]: "warning",
  [AlarmStatus.RESOLVED]: "success",
};

export function AlarmsList() {
  const t = useTranslations("alarms");
  const format = useFormatter();
  const [filters, setFilters] = useState<AlarmFilters>({ page: 0, size: PAGE_SIZE });
  const [selectedAlarm, setSelectedAlarm] = useState<Alarm | null>(null);
  const { confirm, dialog } = useConfirm();

  const { data, isLoading } = useAlarmsQuery(filters);
  const acknowledgeMutation = useAcknowledgeAlarmMutation();
  const resolveMutation = useResolveAlarmMutation();
  const deleteMutation = useDeleteAlarmMutation();
  const canEdit = useCan("edit");
  const canDelete = useCan("delete");

  const alarms = data?.content ?? [];
  const isMutating =
    acknowledgeMutation.isPending || resolveMutation.isPending || deleteMutation.isPending;

  // Changing a filter always goes back to the first page.
  const updateFilter = (key: "status" | "severity", value: string) =>
    setFilters((current) => ({ ...current, [key]: value || undefined, page: 0 }));

  const columns = useMemo<DataTableColumnDef<Alarm>[]>(
    () => [
      {
        accessorKey: "ruleName",
        header: t("list.columns.name"),
        cell: ({ row }) => (
          <div className="font-medium text-text-primary min-w-[8.5rem]">
            {row.original.ruleName}
          </div>
        ),
      },
      {
        accessorKey: "severity",
        header: t("list.columns.severity"),
        cell: ({ row }) => <SeverityBadge severity={row.original.severity} />,
      },
      {
        accessorKey: "message",
        header: t("list.columns.message"),
        cell: ({ row }) => (
          // Full text is in the detail modal (row click); keep the column narrow so actions stay visible.
          <div className="text-text-muted truncate max-w-[150px] 2xl:max-w-[320px]">
            {row.original.message}
          </div>
        ),
      },
      {
        accessorKey: "status",
        header: t("list.columns.status"),
        cell: ({ row }) => (
          <Badge variant={STATUS_BADGE[row.original.status]} dot>
            {t(`status.${row.original.status}`)}
          </Badge>
        ),
      },
      {
        accessorKey: "triggeredAt",
        header: t("list.columns.triggeredAt"),
        cell: ({ row }) => (
          <span className="whitespace-nowrap mono text-text-secondary">
            {format.dateTime(new Date(row.original.triggeredAt), {
              dateStyle: "short",
              timeStyle: "short",
            })}
          </span>
        ),
      },
      {
        id: "actions",
        header: t("list.columns.actions"),
        cell: ({ row }) => {
          const alarm = row.original;
          return (
            <div className="flex items-center gap-1 whitespace-nowrap">
              {canEdit && alarm.status === AlarmStatus.ACTIVE && (
                <Button
                  variant="ghost"
                  size="sm"
                  icon={<Hand size={14} aria-hidden="true" />}
                  onClick={(e) => {
                    e.stopPropagation();
                    acknowledgeMutation.mutate(alarm.id);
                  }}
                  disabled={isMutating}
                  title={t("list.acknowledgeHint")}
                  aria-label={t("list.acknowledge")}
                />
              )}
              {canEdit && alarm.status !== AlarmStatus.RESOLVED && (
                <Button
                  variant="ghost"
                  size="sm"
                  icon={<Check size={14} aria-hidden="true" />}
                  onClick={(e) => {
                    e.stopPropagation();
                    resolveMutation.mutate(alarm.id);
                  }}
                  disabled={isMutating}
                  title={t("list.resolveHint")}
                  aria-label={t("list.resolve")}
                />
              )}
              {canDelete && (
                <Button
                  variant="ghost"
                  size="sm"
                  className="text-error"
                  icon={<Trash2 size={14} aria-hidden="true" />}
                  onClick={(e) => {
                    e.stopPropagation();
                    // Deleting is permanent (unlike resolve), so ask first.
                    confirm({
                      title: t("list.deleteTitle"),
                      description: t("list.deleteDescription"),
                      onConfirm: () => deleteMutation.mutateAsync(alarm.id),
                    });
                  }}
                  disabled={isMutating}
                  title={t("list.delete")}
                  aria-label={t("list.delete")}
                />
              )}
            </div>
          );
        },
      },
    ],
    [
      t,
      format,
      acknowledgeMutation,
      resolveMutation,
      deleteMutation,
      isMutating,
      confirm,
      canEdit,
      canDelete,
    ],
  );

  return (
    <div className="space-y-4">
      <div className="flex flex-col md:flex-row md:items-end justify-between gap-3">
        <h2 className="h2">{t("list.title")}</h2>
        <div className="grid grid-cols-2 gap-2 md:flex">
          <DropdownSelect
            label={t("list.statusFilter")}
            value={filters.status ?? ""}
            options={[
              { label: t("list.allStatuses"), value: "" },
              ...Object.values(AlarmStatus).map((status) => ({
                label: t(`status.${status}`),
                value: status,
              })),
            ]}
            onChange={(value) => updateFilter("status", value)}
            className="min-w-0 md:min-w-[160px] [&_.label]:sr-only"
          />
          <DropdownSelect
            label={t("list.severityFilter")}
            value={filters.severity ?? ""}
            options={[
              { label: t("list.allSeverities"), value: "" },
              ...FILTER_SEVERITIES.map((severity) => ({ label: severity, value: severity })),
            ]}
            onChange={(value) => updateFilter("severity", value)}
            className="min-w-0 md:min-w-[160px] [&_.label]:sr-only"
          />
        </div>
      </div>

      <DataTable
        label={t("list.title")}
        data={alarms}
        columns={columns}
        emptyMessage={t("list.empty")}
        // Sorting is server-side (newest first); client sorting would only reorder one page.
        enableSorting={false}
        loading={isLoading}
        onRowClick={(row) => setSelectedAlarm(row)}
      />

      {data && data.totalPages > 1 && (
        <div className="flex justify-center">
          <Pagination
            currentPage={data.page + 1}
            totalPages={data.totalPages}
            onPageChange={(page) => setFilters((current) => ({ ...current, page: page - 1 }))}
          />
        </div>
      )}

      <AlarmDetailModal
        isOpen={!!selectedAlarm}
        onClose={() => setSelectedAlarm(null)}
        alarm={selectedAlarm}
      />
      {dialog}
    </div>
  );
}
