"use client";

import { Component, type ReactNode, type ErrorInfo } from "react";
import { ErrorState } from "@/shared/components/feedback/ErrorState";

/* ── Types ─────────────────────────────────────────────────── */

interface ErrorBoundaryProps {
  children: ReactNode;
  fallback?: ReactNode;
  onError?: (error: Error, errorInfo: ErrorInfo) => void;
}

interface ErrorBoundaryState {
  hasError: boolean;
  error: Error | null;
}

/* ── Component ─────────────────────────────────────────────── */

class ErrorBoundary extends Component<ErrorBoundaryProps, ErrorBoundaryState> {
  constructor(props: ErrorBoundaryProps) {
    super(props);
    this.state = { hasError: false, error: null };
  }

  static getDerivedStateFromError(error: Error): ErrorBoundaryState {
    return { hasError: true, error };
  }

  componentDidCatch(error: Error, errorInfo: ErrorInfo): void {
    this.props.onError?.(error, errorInfo);
  }

  handleReset = () => {
    this.setState({ hasError: false, error: null });
  };

  render() {
    if (this.state.hasError) {
      return (
        this.props.fallback ?? <ErrorState error={this.state.error} onRetry={this.handleReset} />
      );
    }
    return this.props.children;
  }
}

export { ErrorBoundary, type ErrorBoundaryProps };
