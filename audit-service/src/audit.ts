export type AuditEvent = {
  eventType: string;
  userId: string;
  details: Record<string, unknown>;
  timestamp: number;
};

const events: AuditEvent[] = [];

export function logEvent(
  eventType: string,
  userId: string,
  details: Record<string, unknown>,
): AuditEvent {
  const entry: AuditEvent = {
    eventType,
    userId,
    details,
    timestamp: Date.now(),
  };
  events.push(entry);
  return entry;
}

export function getEvents(userId: string): AuditEvent[] {
  return events.filter((e) => e.userId === userId);
}

export function getEventsByType(eventType: string): AuditEvent[] {
  return events.filter((e) => e.eventType === eventType);
}

export function clearEvents(): void {
  events.length = 0;
}
