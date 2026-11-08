"use client";

import { use } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useTranslations } from "next-intl";
import { ArrowLeft, ExternalLink, FileQuestion } from "lucide-react";
import { SeverityBadge } from "@/shared/components/ui/SeverityBadge";
import { RouteLoading } from "@/shared/components/route/RouteLoading";
import { EmptyState } from "@/shared/components/feedback/EmptyState";
import { useDateFormat } from "@/shared/hooks/useDateFormat";
import { useCan } from "@/features/auth/hooks/useCan";
import {
  useLogGroupEventsQuery,
  useLogGroupQuery,
  useLogsQuery,
} from "@/features/logs/api/useLogQuery";
import { GroupStatusBadge } from "@/features/logs/components/GroupStatusBadge";
import { GroupActions } from "@/features/logs/components/GroupActions";
import { LogTable } from "@/features/logs/components/LogTable";
import { GroupAssignee } from "@/features/logs/components/GroupAssignee";
import { GroupComments } from "@/features/logs/components/GroupComments";
import { MergeGroupButton, MergedBanner } from "@/features/logs/components/GroupMerge";
import { LogGroupEventType, type LogGroupEventDto } from "@/features/logs/types/log.types";

const EVENT_DOT: Record<LogGroupEventType, string> = {
  [LogGroupEventType.CREATED]: "var(--info)",
  [LogGroupEventType.RESOLVED]: "var(--ok)",
  [LogGroupEventType.IGNORED]: "var(--fg-3)",
  [LogGroupEventType.REOPENED]: "var(--crit)",
  [LogGroupEventType.REGRESSED]: "var(--warn)",
  [LogGroupEventType.UNIGNORED]: "var(--crit)",
  [LogGroupEventType.ASSIGNED]: "var(--signal)",
  [LogGroupEventType.UNASSIGNED]: "var(--fg-3)",
  [LogGroupEventType.COMMENTED]: "var(--fg-2)",
  [LogGroupEventType.MERGED]: "var(--info)",
  [LogGroupEventType.MERGED_FROM]: "var(--info)",
  [LogGroupEventType.UNMERGED]: "var(--fg-3)",
};

export default function LogGroupPage({ params }: { params: Promise<{ id: string }> }) {
  const t = useTranslations("logs.groups.detail");
  const tGroups = useTranslations("logs.groups");
  const dates = useDateFormat();
  const router = useRouter();
  const canEdit = useCan("edit");
  const { id } = use(params);
  const groupId = parseInt(id, 10);
  const { data: group, isLoading, error } = useLogGroupQuery(groupId);
  const { data: events } = useLogGroupEventsQuery(groupId);
  const { data: entries, isLoading: entriesLoading } = useLogsQuery({ groupId, page: 0, size: 20 });

  if (isLoading) return <RouteLoading variant="detail" />;

  if (error || !group) {
    return (
      <div className="card">
        <EmptyState icon={<FileQuestion size={18} />} title={t("notFound")} />
        <div className="flex justify-center pb-8">
          <Link href="/logs/groups" className="btn btn-sm">
            <ArrowLeft size={14} aria-hidden="true" />
            {t("back")}
          </Link>
        </div>
      </div>
    );
  }

  const describe = (event: LogGroupEventDto) => {
    if (event.type === LogGroupEventType.IGNORED && event.ignoreUntil) {
      return t("events.IGNORED_UNTIL", { time: dates.dateTime(event.ignoreUntil) });
    }
    switch (event.type) {
      case LogGroupEventType.REGRESSED:
        return t("events.REGRESSED", { count: event.regressionCount ?? 1 });
      case LogGroupEventType.ASSIGNED:
        return t("events.ASSIGNED", { user: event.assignee ?? "?" });
      case LogGroupEventType.MERGED:
      case LogGroupEventType.MERGED_FROM:
      case LogGroupEventType.UNMERGED:
        return t(`events.${event.type}`, { id: event.relatedGroupId ?? "?" });
      case LogGroupEventType.IGNORED:
        return t("events.IGNORED");
      default:
        return t(`events.${event.type}`);
    }
  };

  const facts: [string, string][] = [
    [t("occurrences"), dates.number(group.count)],
    [tGroups("columns.firstSeen"), dates.dateTime(group.firstSeenAt)],
    [tGroups("columns.lastSeen"), dates.dateTime(group.lastSeenAt)],
    [t("regressions"), dates.number(group.regressionCount)],
  ];

  return (
    <div className="stack gap-5 max-w-5xl">
      <Link href="/logs/groups" className="btn btn-ghost btn-sm w-fit -ml-2">
        <ArrowLeft size={14} aria-hidden="true" />
        {t("back")}
      </Link>

      <header className="page-head flex-col sm:flex-row gap-3">
        <div className="stack gap-2 min-w-0">
          <h1 className="h2 mono break-words">{group.firstMessage}</h1>
          <div className="row gap-2 flex-wrap">
            <SeverityBadge severity={group.severity} />
            <GroupStatusBadge group={group} />
            {group.resolvedBy && group.resolvedAt && (
              <span className="xs t3">
                {t("resolvedBy", {
                  user: group.resolvedBy,
                  time: dates.relative(group.resolvedAt),
                })}
              </span>
            )}
          </div>
        </div>
        {canEdit && (
          <div className="row gap-1 flex-wrap">
            <GroupActions group={group} size="md" />
            {!group.mergedIntoId && <MergeGroupButton group={group} />}
          </div>
        )}
      </header>

      <MergedBanner group={group} />

      <GroupAssignee group={group} />

      <dl className="grid grid-cols-2 lg:grid-cols-4 gap-3 m-0" aria-label={t("summary")}>
        {facts.map(([label, value]) => (
          <div key={label} className="card p-3">
            <dt className="eyebrow">{label}</dt>
            <dd className="mono small mt-1 m-0">{value}</dd>
          </div>
        ))}
      </dl>

      <div className="grid gap-5 lg:grid-cols-[minmax(0,1fr)_320px]">
        <section className="card overflow-hidden" aria-labelledby="group-recent">
          <div className="panel-h">
            <h2 id="group-recent" className="h3">
              {t("recent")}
            </h2>
            <Link href={`/logs?group=${group.id}&range=all`} className="btn btn-ghost btn-sm">
              <ExternalLink size={13} aria-hidden="true" />
              {t("viewAll")}
            </Link>
          </div>
          <LogTable
            data={entries?.content ?? []}
            loading={entriesLoading}
            onRowClick={(log) => router.push(`/logs/${log.id}`)}
          />
        </section>

        <div className="stack gap-5">
          <GroupComments groupId={group.id} />
          <section className="card p-4 stack gap-3" aria-labelledby="group-timeline">
            <h2 id="group-timeline" className="h3">
              {t("timeline")}
            </h2>
            {events && events.length > 0 ? (
              <ol className="relative m-0 p-0 list-none stack gap-3">
                <span
                  className="absolute left-[4px] top-1 bottom-1 w-px bg-border-strong"
                  aria-hidden="true"
                />
                {events.map((event) => (
                  <li key={event.id} className="relative pl-5">
                    <span
                      className="absolute left-0 top-[5px] w-[9px] h-[9px] rounded-full"
                      style={{ background: EVENT_DOT[event.type] }}
                      aria-hidden="true"
                    />
                    <p className="small m-0">{describe(event)}</p>
                    <p className="xs t3 m-0">
                      {event.actor ?? t("system")} ·{" "}
                      <time dateTime={event.createdAt} title={dates.dateTime(event.createdAt)}>
                        {dates.relative(event.createdAt)}
                      </time>
                    </p>
                  </li>
                ))}
              </ol>
            ) : (
              <p className="small t3">{t("timelineEmpty")}</p>
            )}
          </section>
        </div>
      </div>
    </div>
  );
}
