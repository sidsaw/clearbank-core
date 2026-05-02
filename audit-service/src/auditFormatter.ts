import type { AuditEvent } from "./audit";

export function toJson(event: AuditEvent): string {
  return JSON.stringify({
    type: event.eventType,
    user: event.userId,
    details: event.details,
    ts: event.timestamp,
  });
}

export function toLogLine(event: AuditEvent): string {
  const ts = new Date(event.timestamp).toISOString();
  return `[${ts}] ${event.eventType} user=${event.userId} ${JSON.stringify(event.details)}`;
}

export function toCsv(events: AuditEvent[]): string {
  const header = "eventType,userId,timestamp,details";
  const rows = events.map(
    (e) =>
      `${e.eventType},${e.userId},${e.timestamp},"${JSON.stringify(e.details).replace(/"/g, '""')}"`,
  );
  return [header, ...rows].join("\n");
}

export function filterByTimeRange(
  events: AuditEvent[],
  startMs: number,
  endMs: number,
): AuditEvent[] {
  return events.filter((e) => e.timestamp >= startMs && e.timestamp <= endMs);
}
