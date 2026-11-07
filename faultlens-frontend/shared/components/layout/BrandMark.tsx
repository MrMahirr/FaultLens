/** FaultLens mark (Instrument): a rounded frame with a rising signal line. Decorative; the
 * product name next to it carries the meaning. Used by the sidebar, identity pages and landing. */
export function BrandMark({ size = 22 }: { size?: number }) {
  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 24 24"
      fill="none"
      strokeWidth="1.8"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
    >
      <rect x="3" y="3" width="18" height="18" rx="5" stroke="currentColor" />
      <path d="M7 15l3-4 3 2 4-6" stroke="var(--signal-ink)" />
    </svg>
  );
}
