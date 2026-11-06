import { cn } from "@/shared/lib/utils";

/** Stack trace with the exception lines emphasised and "at …" frames indented and dimmed. */
export function StackTrace({ trace }: { trace: string }) {
  return (
    <pre className="code whitespace-pre">
      {trace.split("\n").map((line, i) => (
        <span
          key={i}
          className={cn(
            "block",
            /Error|Exception/.test(line)
              ? "text-error"
              : line.trim().startsWith("at")
                ? "t3 pl-4"
                : "t2",
          )}
        >
          {line}
        </span>
      ))}
    </pre>
  );
}
