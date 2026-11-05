import { act, renderHook } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { useWebSocket } from "./useWebSocket";

/** Minimal stand-in for the browser WebSocket; tests drive its events by hand. */
class FakeWebSocket {
  static readonly OPEN = 1;
  static instances: FakeWebSocket[] = [];

  readyState = 0;
  sent: string[] = [];
  closed = false;
  onopen: (() => void) | null = null;
  onmessage: ((event: { data: string }) => void) | null = null;
  onerror: ((event: Event) => void) | null = null;
  onclose: ((event: { code: number }) => void) | null = null;

  constructor(
    public url: string,
    public protocols?: string | string[],
  ) {
    FakeWebSocket.instances.push(this);
  }

  send(data: string) {
    this.sent.push(data);
  }

  close() {
    this.closed = true;
  }

  // ── test helpers ──
  open() {
    this.readyState = FakeWebSocket.OPEN;
    this.onopen?.();
  }

  serverClose(code = 1006) {
    this.readyState = 3;
    this.onclose?.({ code });
  }

  static last() {
    return FakeWebSocket.instances[FakeWebSocket.instances.length - 1];
  }
}

const URL = "ws://localhost:3500/ws/logs";

describe("useWebSocket", () => {
  beforeEach(() => {
    FakeWebSocket.instances = [];
    vi.stubGlobal("WebSocket", FakeWebSocket);
    vi.useFakeTimers();
  });

  afterEach(() => {
    vi.useRealTimers();
    vi.unstubAllGlobals();
  });

  it("sends the token as the bearer sub-protocol, never in the URL", () => {
    renderHook(() => useWebSocket({ url: URL, token: "jwt-abc" }));

    const ws = FakeWebSocket.last();
    expect(ws.url).toBe(URL);
    expect(ws.protocols).toEqual(["bearer", "jwt-abc"]);
  });

  it("connects without sub-protocols when there is no token", () => {
    renderHook(() => useWebSocket({ url: URL }));

    expect(FakeWebSocket.last().protocols).toBeUndefined();
  });

  it("does not connect while disabled", () => {
    renderHook(() => useWebSocket({ url: URL, token: "t", enabled: false }));

    expect(FakeWebSocket.instances).toHaveLength(0);
  });

  it("reports status and parses JSON messages", () => {
    const onMessage = vi.fn();
    const { result } = renderHook(() => useWebSocket({ url: URL, token: "t", onMessage }));
    expect(result.current.status).toBe("connecting");

    act(() => FakeWebSocket.last().open());
    expect(result.current.status).toBe("connected");

    act(() => FakeWebSocket.last().onmessage?.({ data: '{"severity":"ERROR"}' }));
    act(() => FakeWebSocket.last().onmessage?.({ data: "plain text" }));
    expect(onMessage).toHaveBeenNthCalledWith(1, { severity: "ERROR" });
    expect(onMessage).toHaveBeenNthCalledWith(2, "plain text");
  });

  it("sends objects as JSON only while open", () => {
    const { result } = renderHook(() => useWebSocket({ url: URL, token: "t" }));
    const ws = FakeWebSocket.last();

    act(() => result.current.send({ type: "ping" }));
    expect(ws.sent).toEqual([]);

    act(() => ws.open());
    act(() => result.current.send({ type: "ping" }));
    expect(ws.sent).toEqual(['{"type":"ping"}']);
  });

  it("reconnects with exponential backoff after an unexpected close", () => {
    const { result } = renderHook(() => useWebSocket({ url: URL, token: "t", baseDelay: 1000 }));

    act(() => FakeWebSocket.last().serverClose());
    expect(result.current.status).toBe("disconnected");
    expect(FakeWebSocket.instances).toHaveLength(1);

    act(() => vi.advanceTimersByTime(1000));
    expect(FakeWebSocket.instances).toHaveLength(2);

    act(() => FakeWebSocket.last().serverClose());
    act(() => vi.advanceTimersByTime(1999));
    expect(FakeWebSocket.instances).toHaveLength(2);
    act(() => vi.advanceTimersByTime(1));
    expect(FakeWebSocket.instances).toHaveLength(3);
  });

  it("does not retry when the server closes for an expired token (4401)", () => {
    renderHook(() => useWebSocket({ url: URL, token: "t" }));

    act(() => FakeWebSocket.last().serverClose(4401));
    act(() => vi.advanceTimersByTime(60_000));

    expect(FakeWebSocket.instances).toHaveLength(1);
  });

  it("reconnects with the new token when it changes", () => {
    const { rerender } = renderHook(({ token }) => useWebSocket({ url: URL, token }), {
      initialProps: { token: "old" },
    });
    const first = FakeWebSocket.last();

    rerender({ token: "new" });

    expect(first.closed).toBe(true);
    expect(FakeWebSocket.last().protocols).toEqual(["bearer", "new"]);
  });

  it("stops retrying after disconnect() and closes on unmount", () => {
    const { result, unmount } = renderHook(() => useWebSocket({ url: URL, token: "t" }));

    act(() => result.current.disconnect());
    act(() => vi.advanceTimersByTime(60_000));
    expect(FakeWebSocket.instances).toHaveLength(1);

    act(() => result.current.reconnect());
    const second = FakeWebSocket.last();
    unmount();
    expect(second.closed).toBe(true);
  });
});
