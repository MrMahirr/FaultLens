"use client";

import {
  useReactTable,
  getCoreRowModel,
  getSortedRowModel,
  type ColumnDef,
  type SortingState,
  type RowSelectionState,
  flexRender,
} from "@tanstack/react-table";
import { useState, useCallback } from "react";
import { ArrowUp, ArrowDown, ArrowUpDown } from "lucide-react";
import { cn } from "@/shared/lib/utils";

import { useUIStore } from "@/shared/store/ui.store";

/* ── Types ─────────────────────────────────────────────────── */

interface DataTableProps<TData> {
  data: TData[];
  columns: ColumnDef<TData, unknown>[];
  onRowClick?: (row: TData) => void;
  enableSorting?: boolean;
  enableSelection?: boolean;
  loading?: boolean;
  emptyMessage?: string;
  className?: string;
  stickyHeader?: boolean;
}

/* ── Component ─────────────────────────────────────────────── */

function DataTable<TData>({
  data,
  columns,
  onRowClick,
  enableSorting = true,
  enableSelection = false,
  loading = false,
  emptyMessage = "Veri bulunamadı",
  className,
  stickyHeader = false,
}: DataTableProps<TData>) {
  const [sorting, setSorting] = useState<SortingState>([]);
  const [rowSelection, setRowSelection] = useState<RowSelectionState>({});
  const { compactMode } = useUIStore();

  const table = useReactTable({
    data,
    columns,
    state: {
      sorting,
      rowSelection,
    },
    onSortingChange: setSorting,
    onRowSelectionChange: setRowSelection,
    getCoreRowModel: getCoreRowModel(),
    getSortedRowModel: enableSorting ? getSortedRowModel() : undefined,
    enableRowSelection: enableSelection,
  });

  const handleRowClick = useCallback(
    (row: TData) => {
      onRowClick?.(row);
    },
    [onRowClick]
  );

  return (
    <div
      className={cn(
        "w-full overflow-x-auto rounded-xl border border-border-default",
        className
      )}
    >
      <table className="w-full text-sm">
        {/* ── Header ────────────────────────────────── */}
        <thead
          className={cn(
            "bg-bg-tertiary/50",
            stickyHeader && "sticky top-0 z-10"
          )}
        >
          {table.getHeaderGroups().map((headerGroup) => (
            <tr key={headerGroup.id}>
              {headerGroup.headers.map((header) => {
                const canSort = header.column.getCanSort();
                const sortDir = header.column.getIsSorted();

                return (
                  <th
                    key={header.id}
                    className={cn(
                      "px-4 py-3 text-left text-xs font-medium text-text-muted uppercase tracking-wider",
                      "border-b border-border-default",
                      canSort && "cursor-pointer select-none hover:text-text-secondary"
                    )}
                    onClick={
                      canSort
                        ? header.column.getToggleSortingHandler()
                        : undefined
                    }
                  >
                    <div className="flex items-center gap-1.5">
                      {header.isPlaceholder
                        ? null
                        : flexRender(
                            header.column.columnDef.header,
                            header.getContext()
                          )}
                      {canSort && (
                        <span className="text-text-muted">
                          {sortDir === "asc" ? (
                            <ArrowUp size={12} />
                          ) : sortDir === "desc" ? (
                            <ArrowDown size={12} />
                          ) : (
                            <ArrowUpDown size={12} />
                          )}
                        </span>
                      )}
                    </div>
                  </th>
                );
              })}
            </tr>
          ))}
        </thead>

        {/* ── Body ──────────────────────────────────── */}
        <tbody>
          {loading ? (
            Array.from({ length: 5 }, (_, i) => (
              <tr key={`skeleton-${i}`}>
                {columns.map((_, j) => (
                  <td
                    key={`skeleton-${i}-${j}`}
                    className="px-4 py-3 border-b border-border-default"
                  >
                    <div className="h-4 bg-bg-tertiary rounded animate-pulse" />
                  </td>
                ))}
              </tr>
            ))
          ) : table.getRowModel().rows.length === 0 ? (
            <tr>
              <td
                colSpan={columns.length}
                className="px-4 py-12 text-center text-text-muted"
              >
                {emptyMessage}
              </td>
            </tr>
          ) : (
            table.getRowModel().rows.map((row) => (
              <tr
                key={row.id}
                onClick={() => handleRowClick(row.original)}
                className={cn(
                  "border-b border-border-default transition-colors duration-150",
                  onRowClick &&
                    "cursor-pointer hover:bg-bg-tertiary/50",
                  row.getIsSelected() && "bg-accent/5"
                )}
              >
                {row.getVisibleCells().map((cell) => (
                  <td 
                    key={cell.id} 
                    className={cn("px-4", compactMode ? "py-1" : "py-3")}
                  >
                    {flexRender(cell.column.columnDef.cell, cell.getContext())}
                  </td>
                ))}
              </tr>
            ))
          )}
        </tbody>
      </table>
    </div>
  );
}

export { DataTable, type DataTableProps };
