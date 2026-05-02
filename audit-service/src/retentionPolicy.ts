import type { AuditEvent } from "./audit";

const DEFAULT_RETENTION_DAYS = 2555; // ~7 years per OCC requirements

export type RetentionConfig = {
  retentionDays: number;
  archiveAfterDays: number;
};

export function defaultConfig(): RetentionConfig {
  return {
    retentionDays: DEFAULT_RETENTION_DAYS,
    archiveAfterDays: 365,
  };
}

export function isExpired(event: AuditEvent, config: RetentionConfig): boolean {
  const ageMs = Date.now() - event.timestamp;
  const ageDays = ageMs / (1000 * 60 * 60 * 24);
  return ageDays > config.retentionDays;
}

export function shouldArchive(
  event: AuditEvent,
  config: RetentionConfig,
): boolean {
  const ageMs = Date.now() - event.timestamp;
  const ageDays = ageMs / (1000 * 60 * 60 * 24);
  return ageDays > config.archiveAfterDays && ageDays <= config.retentionDays;
}

export function partitionEvents(
  events: AuditEvent[],
  config: RetentionConfig,
): { active: AuditEvent[]; archive: AuditEvent[]; expired: AuditEvent[] } {
  const active: AuditEvent[] = [];
  const archive: AuditEvent[] = [];
  const expired: AuditEvent[] = [];

  for (const event of events) {
    if (isExpired(event, config)) {
      expired.push(event);
    } else if (shouldArchive(event, config)) {
      archive.push(event);
    } else {
      active.push(event);
    }
  }

  return { active, archive, expired };
}
