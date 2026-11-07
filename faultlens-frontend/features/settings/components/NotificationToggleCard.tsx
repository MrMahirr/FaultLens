"use client";

import React from "react";
import { Switch } from "@/shared/components/ui/Switch";

interface NotificationToggleCardProps {
  id: string;
  title: string;
  description: string;
  icon: React.ReactNode;
  enabled: boolean;
  onToggle: () => void;
  children?: React.ReactNode;
}

/** A notification channel: switch row, plus its settings while enabled. */
export function NotificationToggleCard({
  id,
  title,
  description,
  icon,
  enabled,
  onToggle,
  children,
}: NotificationToggleCardProps) {
  const hasChildren = React.Children.count(children) > 0;
  const titleId = `channel-${id}-title`;

  return (
    <section className="card" aria-labelledby={titleId}>
      <div className="between p-4">
        <div className="flex items-start gap-3 min-w-0">
          <span
            className="w-8 h-8 rounded-md well grid place-items-center t2 shrink-0"
            aria-hidden="true"
          >
            {icon}
          </span>
          <div className="min-w-0">
            <h3 id={titleId} className="h3">
              {title}
            </h3>
            <p id={`channel-${id}-desc`} className="xs t3 mt-0.5">
              {description}
            </p>
          </div>
        </div>

        <Switch
          checked={enabled}
          onCheckedChange={onToggle}
          aria-labelledby={titleId}
          aria-describedby={`channel-${id}-desc`}
        />
      </div>

      {enabled && hasChildren && (
        <div className="px-4 pb-4 pt-3 border-t border-border-default stack gap-3 swA">
          {children}
        </div>
      )}
    </section>
  );
}
