import React from "react";

interface NotificationToggleCardProps {
  id: string;
  title: string;
  description: string;
  icon: React.ReactNode;
  enabled: boolean;
  onToggle: () => void;
  children?: React.ReactNode;
}

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

  return (
    <div className="flex flex-col">
      <div
        className={`flex items-center justify-between p-5 rounded-2xl border border-border-default bg-bg-tertiary/30 hover:bg-bg-tertiary/50 transition-colors ${
          enabled && hasChildren ? "rounded-b-none border-b-0" : ""
        }`}
      >
        <div className="flex items-start gap-4">
          <div className="p-3 bg-bg-primary rounded-xl shadow-sm border border-border-default">
            {icon}
          </div>
          <div>
            <h3 className="font-medium text-text-primary">{title}</h3>
            <p className="text-sm text-text-muted mt-0.5">{description}</p>
          </div>
        </div>

        <button
          onClick={onToggle}
          className={`relative inline-flex h-6 w-11 flex-shrink-0 cursor-pointer rounded-full border-2 border-transparent transition-colors duration-200 ease-in-out focus:outline-none ${
            enabled ? "bg-accent" : "bg-bg-secondary border-border-default"
          }`}
          role="switch"
          aria-checked={enabled}
        >
          <span
            aria-hidden="true"
            className={`pointer-events-none inline-block h-5 w-5 transform rounded-full bg-white shadow ring-0 transition duration-200 ease-in-out ${
              enabled ? "translate-x-5" : "translate-x-0"
            }`}
          />
        </button>
      </div>

      {enabled && hasChildren && (
        <div className="pl-16 pr-5 pb-5 bg-bg-tertiary/20 rounded-b-2xl border-x border-b border-border-default animate-fade-in flex flex-col gap-4">
          {children}
        </div>
      )}
    </div>
  );
}
