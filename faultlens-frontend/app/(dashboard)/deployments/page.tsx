"use client";

import { Suspense, useEffect, useState } from "react";
import Link from "next/link";
import { useTranslations } from "next-intl";
import toast from "react-hot-toast";
import { Plus, Rocket, Terminal, ExternalLink, Copy, Search, KeyRound } from "lucide-react";
import { useCan } from "@/features/auth/hooks/useCan";
import { useAuthStore } from "@/features/auth/store/auth.store";
import { PageHeader } from "@/shared/components/layout/PageHeader";
import { Badge } from "@/shared/components/ui/Badge";
import { Button } from "@/shared/components/ui/Button";
import { Modal } from "@/shared/components/ui/Modal";
import { Input } from "@/shared/components/ui/Input";
import { DropdownSelect } from "@/shared/components/ui/Dropdown";
import { EmptyState } from "@/shared/components/feedback/EmptyState";
import { SkeletonCard } from "@/shared/components/ui/Skeleton";
import { useDateFormat } from "@/shared/hooks/useDateFormat";
import { useUrlState } from "@/shared/hooks/useUrlState";
import { useDebounce } from "@/shared/hooks/useDebounce";
import { useDeploymentsQuery } from "@/features/deployments/api/useDeploymentQuery";
import { useCreateDeploymentMutation } from "@/features/deployments/api/useDeploymentMutation";
import { DeploymentStatus, DeploymentEnvironment } from "@/shared/types/common.types";
import { getApiErrorMessage } from "@/shared/api/errors";
import { Endpoints } from "@/shared/api/endpoints";

/**
 * Example CI step shown on the page. Mirrors what the API accepts: snake_case fields,
 * `deployed_at` is required; FAULTLENS_API_KEY is an API key with the deployments:write
 * scope (Settings → API keys), not a user token.
 */
const CI_WEBHOOK_EXAMPLE = `curl -X POST ${Endpoints.DEPLOYMENTS.CREATE} \\
  -H "Content-Type: application/json" \\
  -H "Authorization: Bearer $FAULTLENS_API_KEY" \\
  -d '{"service_name":"my-service","version":"v1.0.0","environment":"PRODUCTION","deployed_at":"'"$(date -u +%Y-%m-%dT%H:%M:%SZ)"'","deployed_by":"ci"}'`;

const STATUS_BADGE: Record<DeploymentStatus, "success" | "error" | "warning" | "info"> = {
  [DeploymentStatus.SUCCESS]: "success",
  [DeploymentStatus.FAILED]: "error",
  [DeploymentStatus.ROLLED_BACK]: "warning",
  [DeploymentStatus.IN_PROGRESS]: "info",
};

const STATUS_DOT: Record<DeploymentStatus, string> = {
  [DeploymentStatus.SUCCESS]: "var(--ok)",
  [DeploymentStatus.FAILED]: "var(--crit)",
  [DeploymentStatus.ROLLED_BACK]: "var(--warn)",
  [DeploymentStatus.IN_PROGRESS]: "var(--info)",
};

const ENVIRONMENTS = Object.values(DeploymentEnvironment);
const EMPTY_FORM = { serviceName: "", version: "", environment: "" };

function DeploymentsView() {
  const t = useTranslations("deployments");
  const tCommon = useTranslations("common");
  const dates = useDateFormat();
  const { data: deployments, isLoading } = useDeploymentsQuery();
  const createDeployment = useCreateDeploymentMutation();
  const canEdit = useCan("edit");
  const isAdmin = useCan("manageUsers");
  const username = useAuthStore((state) => state.user?.username);
  const [showModal, setShowModal] = useState(false);
  const [formData, setFormData] = useState(EMPTY_FORM);

  // Filters in the URL (?q=payments&env=PRODUCTION) — shareable.
  const { params, update } = useUrlState();
  const urlQuery = params.get("q") ?? "";
  const [searchQuery, setSearchQuery] = useState(urlQuery);
  const debouncedQuery = useDebounce(searchQuery, 300);
  useEffect(() => {
    if (debouncedQuery !== urlQuery) update({ q: debouncedQuery });
  }, [debouncedQuery, urlQuery, update]);
  const envParam = params.get("env");
  const envFilter = ENVIRONMENTS.includes(envParam as DeploymentEnvironment)
    ? (envParam as DeploymentEnvironment)
    : undefined;

  const filteredDeployments = deployments?.filter(
    (d) =>
      d.serviceName.toLowerCase().includes(searchQuery.toLowerCase()) &&
      (!envFilter || d.environment === envFilter),
  );

  const envOptions = ENVIRONMENTS.map((env) => ({ label: t(`env.${env}`), value: env }));

  const handleCreate = async () => {
    try {
      await createDeployment.mutateAsync({
        serviceName: formData.serviceName.trim(),
        version: formData.version.trim(),
        environment: formData.environment as DeploymentEnvironment,
        status: DeploymentStatus.SUCCESS,
        deployedBy: username ?? "ui",
        deployedAt: new Date().toISOString(),
      });
      toast.success(t("form.saved"));
      setShowModal(false);
      setFormData(EMPTY_FORM);
    } catch (error) {
      toast.error(getApiErrorMessage(error, t("form.saveFailed")));
    }
  };

  const copyCommand = async () => {
    try {
      await navigator.clipboard.writeText(CI_WEBHOOK_EXAMPLE);
      toast.success(t("ci.copied"));
    } catch {
      // Clipboard unavailable (insecure context); the command is selectable on the page.
    }
  };

  const hasFilters = !!searchQuery || !!envFilter;

  return (
    <>
      <PageHeader
        title={t("title")}
        description={t("subtitle")}
        action={
          canEdit && (
            <Button
              variant="primary"
              icon={<Plus size={16} aria-hidden="true" />}
              onClick={() => setShowModal(true)}
            >
              {t("add")}
            </Button>
          )
        }
      />

      <section className="card" aria-labelledby="ci-title">
        <div className="panel-h">
          <h2 id="ci-title" className="h3 row">
            <Terminal size={15} className="t3" aria-hidden="true" />
            {t("ci.title")}
          </h2>
          <div className="row gap-1">
            {isAdmin && (
              <Link href="/settings?tab=apiKeys" className="btn btn-ghost btn-sm">
                <KeyRound size={13} aria-hidden="true" />
                {t("ci.createKey")}
              </Link>
            )}
            <Button
              variant="ghost"
              size="sm"
              icon={<Copy size={13} aria-hidden="true" />}
              onClick={copyCommand}
            >
              {t("ci.copy")}
            </Button>
          </div>
        </div>
        <div className="p-4 stack gap-3">
          <p className="small t2">{t("ci.description")}</p>
          <pre className="code">{CI_WEBHOOK_EXAMPLE}</pre>
        </div>
      </section>

      <div className="flex flex-col sm:flex-row gap-2" role="search" aria-label={t("search")}>
        <label className="search grow max-w-md">
          <Search size={15} aria-hidden="true" />
          <span className="sr-only">{t("search")}</span>
          <input
            type="search"
            placeholder={t("searchPlaceholder")}
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
          />
        </label>
        <DropdownSelect
          label={t("environment")}
          value={envFilter ?? ""}
          options={[{ label: t("allEnvironments"), value: "" }, ...envOptions]}
          onChange={(v) => update({ env: v })}
          className="sm:w-56 [&_.label]:sr-only"
        />
      </div>

      {isLoading ? (
        <div className="stack" aria-busy="true">
          {Array.from({ length: 3 }, (_, i) => (
            <SkeletonCard key={i} />
          ))}
        </div>
      ) : filteredDeployments && filteredDeployments.length > 0 ? (
        <ol className="relative m-0 p-0 list-none stack gap-3" aria-label={t("timeline")}>
          {/* Timeline rail */}
          <span
            className="absolute left-[11px] top-2 bottom-2 w-px bg-border-strong"
            aria-hidden="true"
          />

          {filteredDeployments.map((deployment, index) => (
            <li key={deployment.id} className={`relative pl-9 in d${Math.min(index + 1, 8)}`}>
              <span
                className="absolute left-[6px] top-5 w-[11px] h-[11px] rounded-full border-2 border-bg-primary"
                style={{ background: STATUS_DOT[deployment.status] }}
                aria-hidden="true"
              />

              <article className="card p-4 flex flex-col sm:flex-row sm:items-start justify-between gap-4">
                <div className="min-w-0">
                  <div className="row mb-1">
                    <h3 className="h3 text-[15px]">{deployment.serviceName}</h3>
                    <span className="badge mono c-signal">{deployment.version}</span>
                  </div>
                  <p className="xs t3 row gap-3">
                    <time dateTime={deployment.deployedAt}>
                      {dates.dateTime(deployment.deployedAt)}
                    </time>
                    {deployment.deployedBy && (
                      <span>{t("by", { user: deployment.deployedBy })}</span>
                    )}
                  </p>
                  {deployment.notes && (
                    <p className="small t2 mt-2 whitespace-pre-line">{deployment.notes}</p>
                  )}
                </div>

                <div className="flex flex-col items-start sm:items-end gap-2.5 shrink-0">
                  <div className="row">
                    <Badge variant={STATUS_BADGE[deployment.status]} dot>
                      {t(`status.${deployment.status}`)}
                    </Badge>
                    <Badge variant="outline">{t(`env.${deployment.environment}`)}</Badge>
                  </div>
                  <Link
                    href={`/logs?from=${encodeURIComponent(deployment.deployedAt)}&range=all`}
                    className="btn btn-sm"
                  >
                    <ExternalLink size={13} aria-hidden="true" />
                    {t("viewLogs")}
                  </Link>
                </div>
              </article>
            </li>
          ))}
        </ol>
      ) : (
        <div className="card">
          <EmptyState
            icon={<Rocket size={18} />}
            title={hasFilters ? t("emptyFiltered") : t("emptyTitle")}
            description={hasFilters ? undefined : t("emptyDescription")}
            action={
              hasFilters
                ? {
                    label: tCommon("clearFilters"),
                    onClick: () => {
                      setSearchQuery("");
                      update({ q: undefined, env: undefined });
                    },
                  }
                : canEdit
                  ? { label: t("addFirst"), onClick: () => setShowModal(true) }
                  : undefined
            }
          />
        </div>
      )}

      <Modal
        isOpen={showModal}
        onClose={() => setShowModal(false)}
        title={t("form.title")}
        footer={
          <>
            <Button variant="secondary" onClick={() => setShowModal(false)}>
              {tCommon("cancel")}
            </Button>
            <Button
              type="submit"
              form="deployment-form"
              variant="primary"
              loading={createDeployment.isPending}
              disabled={!formData.serviceName || !formData.version || !formData.environment}
            >
              {t("form.save")}
            </Button>
          </>
        }
      >
        <form
          id="deployment-form"
          className="stack gap-4"
          onSubmit={(e) => {
            e.preventDefault();
            void handleCreate();
          }}
        >
          <Input
            label={t("form.service")}
            placeholder="payment-service"
            value={formData.serviceName}
            onChange={(e) => setFormData((prev) => ({ ...prev, serviceName: e.target.value }))}
            required
            autoFocus
          />
          <Input
            label={t("form.version")}
            placeholder="v2.3.1"
            className="mono"
            value={formData.version}
            onChange={(e) => setFormData((prev) => ({ ...prev, version: e.target.value }))}
            required
          />
          <DropdownSelect
            label={t("environment")}
            value={formData.environment}
            options={envOptions}
            onChange={(value) => setFormData((prev) => ({ ...prev, environment: value }))}
            placeholder={t("chooseEnvironment")}
          />
        </form>
      </Modal>
    </>
  );
}

export default function DeploymentsPage() {
  return (
    <Suspense>
      <DeploymentsView />
    </Suspense>
  );
}
