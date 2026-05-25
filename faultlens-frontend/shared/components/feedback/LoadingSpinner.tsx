"use client";

import { motion } from "framer-motion";
import { cn } from "@/shared/lib/utils";

/* ── Types ─────────────────────────────────────────────────── */

interface LoadingSpinnerProps {
  size?: "sm" | "md" | "lg";
  label?: string;
  fullScreen?: boolean;
  className?: string;
}

/* ── Size Map ──────────────────────────────────────────────── */

const sizeMap = {
  sm: { container: 24, stroke: 2 },
  md: { container: 40, stroke: 3 },
  lg: { container: 56, stroke: 3 },
};

/* ── Component ─────────────────────────────────────────────── */

function LoadingSpinner({
  size = "md",
  label,
  fullScreen = false,
  className,
}: LoadingSpinnerProps) {
  const { container, stroke } = sizeMap[size];
  const radius = (container - stroke * 2) / 2;
  const circumference = 2 * Math.PI * radius;

  const spinner = (
    <div className={cn("flex flex-col items-center gap-3", className)}>
      <motion.svg
        width={container}
        height={container}
        viewBox={`0 0 ${container} ${container}`}
        animate={{ rotate: 360 }}
        transition={{
          duration: 1,
          repeat: Infinity,
          ease: "linear",
        }}
      >
        {/* Track */}
        <circle
          cx={container / 2}
          cy={container / 2}
          r={radius}
          fill="none"
          stroke="var(--bg-tertiary)"
          strokeWidth={stroke}
        />
        {/* Progress */}
        <motion.circle
          cx={container / 2}
          cy={container / 2}
          r={radius}
          fill="none"
          stroke="var(--accent)"
          strokeWidth={stroke}
          strokeLinecap="round"
          strokeDasharray={circumference}
          initial={{ strokeDashoffset: circumference * 0.75 }}
          animate={{
            strokeDashoffset: [
              circumference * 0.75,
              circumference * 0.25,
              circumference * 0.75,
            ],
          }}
          transition={{
            duration: 1.5,
            repeat: Infinity,
            ease: "easeInOut",
          }}
        />
      </motion.svg>

      {label && (
        <p className="text-sm text-text-secondary font-medium">{label}</p>
      )}
    </div>
  );

  if (fullScreen) {
    return (
      <div className="fixed inset-0 z-50 flex items-center justify-center bg-bg-primary/80 backdrop-blur-sm">
        {spinner}
      </div>
    );
  }

  return spinner;
}

export { LoadingSpinner, type LoadingSpinnerProps };
