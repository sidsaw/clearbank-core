import type { StoredEvent } from "./eventStore";

export type IntegrityResult = {
  valid: boolean;
  checkedCount: number;
  errors: string[];
};

export function verifyChain(events: StoredEvent[]): IntegrityResult {
  const errors: string[] = [];

  for (let i = 0; i < events.length; i++) {
    const event = events[i];

    if (!event.id || !event.hash) {
      errors.push(`Event at index ${i} missing id or hash`);
      continue;
    }

    if (!event.eventType || event.eventType.trim() === "") {
      errors.push(`Event ${event.id} has empty eventType`);
    }

    if (!event.userId || event.userId.trim() === "") {
      errors.push(`Event ${event.id} has empty userId`);
    }

    if (event.timestamp <= 0) {
      errors.push(`Event ${event.id} has invalid timestamp`);
    }

    if (i > 0 && event.timestamp < events[i - 1].timestamp) {
      errors.push(
        `Event ${event.id} timestamp precedes previous event ${events[i - 1].id}`,
      );
    }
  }

  return {
    valid: errors.length === 0,
    checkedCount: events.length,
    errors,
  };
}

export function detectGaps(events: StoredEvent[]): string[] {
  const gaps: string[] = [];
  for (let i = 1; i < events.length; i++) {
    const prevNum = parseInt(events[i - 1].id.replace("EVT-", ""), 10);
    const currNum = parseInt(events[i].id.replace("EVT-", ""), 10);
    if (currNum !== prevNum + 1) {
      gaps.push(`Gap between ${events[i - 1].id} and ${events[i].id}`);
    }
  }
  return gaps;
}
