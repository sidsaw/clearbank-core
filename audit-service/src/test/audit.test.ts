import { describe, it, expect, beforeEach } from "vitest";
import { logEvent, getEvents, clearEvents } from "../audit";

describe("audit", () => {
  beforeEach(() => {
    clearEvents();
  });

  it("logEvent creates an entry with correct fields", () => {
    const event = logEvent("LOGIN", "user-1", { ip: "127.0.0.1" });
    expect(event.eventType).toBe("LOGIN");
    expect(event.userId).toBe("user-1");
    expect(event.details).toEqual({ ip: "127.0.0.1" });
    expect(event.timestamp).toBeGreaterThan(0);
  });

  it("getEvents returns events for specified user", () => {
    logEvent("LOGIN", "user-1", {});
    logEvent("LOGIN", "user-2", {});
    const events = getEvents("user-1");
    expect(events).toHaveLength(1);
    expect(events[0].userId).toBe("user-1");
  });
});
