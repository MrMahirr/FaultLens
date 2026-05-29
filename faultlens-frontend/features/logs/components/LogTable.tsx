"use client";

import { useMemo } from "react";
import { type ColumnDef } from "@tanstack/react-table";
import { DataTable } from "@/shared/components/ui/Table";
import { SeverityBadge } from "@/features/logs/components/SeverityBadge";
import { Badge } from "@/shared/components/ui/Badge";
import { Tooltip } from "@/shared/components/ui/Tooltip";
import type { LogEntryDto } from "@/features/logs/types/log.types";
import { formatDate, truncate } from "@/shared/lib/utils";

/* ── Props ─────────────────────────────────────────────────── */

interface LogTableProps {
  data: LogEntryDto[];
  loading?: boolean;
  onRowClick?: (log: LogEntryDto) => void;
}

/* ── Component ─────────────────────────────────────────────── */

function LogTable({ data, loading = false, onRowClick }: LogTableProps) {
  const columns = useMemo<ColumnDef<LogEntryDto, unknown>[]>(
    () => [
      {
        accessorKey: "timestamp",
        header: "Zaman",
        cell: ({ getValue }) => (
          <span className="text-xs font-mono text-text-secondary whitespace-nowrap">
            {formatDate(getValue() as string, {
              year: undefined,
              month: undefined,
              day: undefined,
            })}
          </span>
        ),
        size: 100,
      },
      {
        accessorKey: "severity",
        header: "Severity",
        cell: ({ getValue }) => (
          <SeverityBadge severity={getValue() as LogEntryDto["severity"]} />
        ),
        size: 90,
      },
      {
        accessorKey: "sourceId",
        header: "Kaynak",
        cell: ({ row }) => (
          <span className="text-xs font-mono text-text-secondary">
            {row.original.podName ?? row.original.serviceName ?? `Source #${row.original.sourceId}`}
          </span>
        ),
        size: 160,
      },
      {
        accessorKey: "message",
        header: "Mesaj",
        cell: ({ getValue }) => {
          const message = getValue() as string;
          return (
            <Tooltip content={message} position="bottom">
              <span className="text-sm text-text-primary">
                {truncate(message, 80)}
              </span>
            </Tooltip>
          );
        },
      },
      {
        accessorKey: "namespace",
        header: "Namespace",
        cell: ({ getValue }) => (
          <span className="text-xs text-text-muted">{getValue() as string}</span>
        ),
        size: 100,
      },
      {
        accessorKey: "groupId",
        header: "Grup",
        cell: ({ getValue }) => {
          const groupId = getValue() as number | undefined;
          return groupId ? (
            <Badge variant="accent" size="sm">
              #{groupId}
            </Badge>
          ) : (
            <span className="text-xs text-text-muted">—</span>
          );
        },
        size: 70,
      },
    ],
    []
  );

  return (
    <DataTable
      data={data}
      columns={columns}
      loading={loading}
      onRowClick={onRowClick}
      emptyMessage="Bu filtrelere uygun log bulunamadı"
      stickyHeader
    />
  );
}

export { LogTable };
