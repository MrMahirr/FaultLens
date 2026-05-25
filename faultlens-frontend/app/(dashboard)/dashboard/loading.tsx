import { SkeletonCard } from "@/shared/components/ui/Skeleton";

export default function DashboardLoading() {
  return (
    <div className="space-y-6 p-4 sm:p-6 lg:p-8">
      {/* Header skeleton */}
      <div className="space-y-2">
        <div className="h-8 w-48 bg-bg-tertiary rounded animate-pulse" />
        <div className="h-4 w-72 bg-bg-tertiary rounded animate-pulse" />
      </div>

      {/* Stats Grid skeleton */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        {Array.from({ length: 4 }, (_, i) => (
          <SkeletonCard key={i} />
        ))}
      </div>

      {/* Chart + Feed skeleton */}
      <div className="grid grid-cols-1 lg:grid-cols-5 gap-6">
        <div className="lg:col-span-3 h-[360px] bg-bg-secondary border border-border-default rounded-xl animate-pulse" />
        <div className="lg:col-span-2 h-[360px] bg-bg-secondary border border-border-default rounded-xl animate-pulse" />
      </div>
    </div>
  );
}
