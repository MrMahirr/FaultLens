"use client";

import { Suspense, useMemo } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { useTranslations } from "next-intl";
import { Layers, UserRound } from "lucide-react";
import { useAuthStore } from "@/features/auth/store/auth.store";
import { PageHeader } from "@/shared/components/layout/PageHeader";
import { DataTable, type DataTableColumnDef } from "@/shared/components/ui/Table";
import { Tabs } from "@/shared/components/ui/Tabs";
import { Pagination } from "@/shared/components/ui/Pagination";
import { SeverityBadge } from "@/shared/components/ui/SeverityBadge";
import { EmptyState } from "@/shared/components/feedback/EmptyState";
import { useDateFormat } from "@/shared/hooks/useDateFormat";
import { useUrlState } from "@/shared/hooks/useUrlState";
import { useCan } from "@/features/auth/hooks/useCan";
import { useLogGroupsQuery } from "@/features/logs/api/useLogQuery";
import { LogsNav } from "@/features/logs/components/LogsNav";
import { GroupStatusBadge } from "@/features/logs/components/GroupStatusBadge";
import { GroupActions } from "@/features/logs/components/GroupActions";
import { GroupStatus, type LogGroupDto } from "@/features/logs/types/log.types";

const PAGE_SIZE = 25;
type StatusTab = GroupStatus | "all";
const TABS: StatusTab[] = [
  GroupStatus.UNRESOLVED,
  GroupStatus.RESOLVED,
  GroupStatus.IGNORED,
  "all",
];
const ALL_STATUSES = Object.values(GroupStatus);

/** `?status=unresolved` (default) | resolved | ignored | all */
function parseTab(value: string | null): StatusTab {
  const upper = value?.toUpperCase();
  if (upper === "ALL") return "all";
  return ALL_STATUSES.includes(upper as GroupStatus)
    ? (upper as GroupStatus)
    : GroupStatus.UNRESOLVED;
}

function LogGroupsView() {
  const t = useTranslations("logs.groups");
  const dates = useDateFormat();
  const router = useRouter();
  const canEdit = useCan("edit");
  const { params, update } = useUrlState();
  const tab = parseTab(params.get("status"));
  const page = Math.max(0, (Number(params.get("page")) || 1) - 1);
  const me = useAuthStore((state) => state.user?.username);
  // ?mine=1 — "assigned to me" (2.2); shareable like the status tab.
  const mine = params.get("mine") === "1" && !!me;

  const { data, isLoading } = useLogGroupsQuery({
    statuses: tab === "all" ? ALL_STATUSES : [tab],
    assignee: mine ? me : undefined,
    page,
    size: PAGE_SIZE,
  });
  const groups = data?.content ?? [];
  const totalPages = data?.totalPages ?? 0;

  const columns = useMemo<DataTableColumnDef<LogGroupDto>[]>(
    () => [
      {
        accessorKey: "firstMessage",
        header: t("columns.message"),
        cell: ({ row }) => (
          <Link
            href={`/logs/groups/${row.original.id}`}
            className="block min-w-[16rem] max-w-[40rem] truncate mono small text-text-primary hover:underline"
            title={row.original.firstMessage}
            onClick={(e) => e.stopPropagation()}
          >
            {row.original.firstMessage}
          </Link>
        ),
      },
      {
        accessorKey: "severity",
        header: t("columns.severity"),
        cell: ({ row }) => <SeverityBadge severity={row.original.severity} />,
      },
      {
        accessorKey: "status",
        header: t("columns.status"),
        cell: ({ row }) => <GroupStatusBadge group={row.original} />,
      },
      {
        accessorKey: "assignee",
        header: t("columns.assignee"),
        cell: ({ row }) =>
          row.original.assignee ? (
            <span className="row gap-1 small whitespace-nowrap">
              <UserRound size={12} className="t3" aria-hidden="true" />
              {row.original.assignee}
            </span>
          ) : (
            <span className="xs t3">{t("unassigned")}</span>
          ),
      },
      {
        accessorKey: "count",
        header: t("columns.count"),
        cell: ({ row }) => <span className="mono small">{dates.number(row.original.count)}</span>,
      },
      {
        accessorKey: "firstSeenAt",
        header: t("columns.firstSeen"),
        cell: ({ row }) => (
          <time dateTime={row.original.firstSeenAt} className="xs t3 whitespace-nowrap">
            {dates.relative(row.original.firstSeenAt)}
          </time>
        ),
      },
      {
        accessorKey: "lastSeenAt",
        header: t("columns.lastSeen"),
        cell: ({ row }) => (
          <time
            dateTime={row.original.lastSeenAt}
            title={dates.dateTime(row.original.lastSeenAt)}
            className="xs t2 whitespace-nowrap"
          >
            {dates.relative(row.original.lastSeenAt)}
          </time>
        ),
      },
      ...(canEdit
        ? [
            {
              id: "actions",
              header: t("columns.actions"),
              cell: ({ row }: { row: { original: LogGroupDto } }) => (
                <GroupActions group={row.original} />
              ),
            } as DataTableColumnDef<LogGroupDto>,
          ]
        : []),
    ],
    [t, dates, canEdit],
  );

  return (
    <>
      <PageHeader
        title={t("title")}
        description={t("subtitle")}
        action={
          data && (
            <span className="xs t3 mono" aria-live="polite">
              {t("resultCount", { count: data.totalElements })}
            </span>
          )
        }
      />
      <LogsNav />

      <Tabs
        label={t("statusFilter")}
        idPrefix="group-status"
        value={tab}
        onChange={(value) =>
          update({
            status: value === GroupStatus.UNRESOLVED ? undefined : value.toLowerCase(),
            page: undefined,
          })
        }
        items={TABS.map((value) => ({ value, label: t(`statusTabs.${value}`) }))}
      />

      <label className="row gap-2 small w-fit cursor-pointer">
        <input
          type="checkbox"
          checked={mine}
          onChange={(e) => update({ mine: e.target.checked ? "1" : undefined, page: undefined })}
        />
        {t("assignedToMe")}
      </label>

      <div
        role="tabpanel"
        id={`group-status-panel-${tab}`}
        aria-labelledby={`group-status-tab-${tab}`}
      >
        {!isLoading && groups.length === 0 ? (
          <div className="card">
            <EmptyState
              icon={<Layers size={18} />}
              title={t(`empty.${tab}`)}
              description={t("emptyHint")}
            />
          </div>
        ) : (
          <div className="card overflow-hidden">
            <DataTable
              data={groups}
              columns={columns}
              loading={isLoading}
              enableSorting={false}
              onRowClick={(group) => router.push(`/logs/groups/${group.id}`)}
            />
          </div>
        )}
      </div>

      {totalPages > 1 && (
        <div className="flex justify-center">
          <Pagination
            currentPage={page + 1}
            totalPages={totalPages}
            onPageChange={(p) => update({ page: p > 1 ? p : undefined })}
          />
        </div>
      )}
    </>
  );
}

export default function LogGroupsPage() {
  return (
    <Suspense>
      <LogGroupsView />
    </Suspense>
  );
}
