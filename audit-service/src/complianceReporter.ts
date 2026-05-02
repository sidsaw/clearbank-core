import type { AuditEvent } from "./audit";

export type ComplianceReport = {
  reportId: string;
  generatedAt: number;
  totalEvents: number;
  flaggedEvents: AuditEvent[];
  complianceScore: number;
};

const FLAGGED_EVENT_TYPES = [
  "LARGE_TRANSACTION",
  "FAILED_LOGIN_BURST",
  "PII_ACCESS",
  "ACCOUNT_LOCKOUT",
  "UNAUTHORIZED_ACCESS",
];

export function generateReport(
  events: AuditEvent[],
  reportId: string,
): ComplianceReport {
  const flagged = events.filter((e) =>
    FLAGGED_EVENT_TYPES.includes(e.eventType),
  );

  const score = events.length > 0
    ? Math.max(0, 100 - (flagged.length / events.length) * 100)
    : 100;

  return {
    reportId,
    generatedAt: Date.now(),
    totalEvents: events.length,
    flaggedEvents: flagged,
    complianceScore: Math.round(score),
  };
}

export function isBelowThreshold(
  report: ComplianceReport,
  threshold: number,
): boolean {
  return report.complianceScore < threshold;
}

export function summarize(report: ComplianceReport): string {
  return [
    `Report: ${report.reportId}`,
    `Events: ${report.totalEvents}`,
    `Flagged: ${report.flaggedEvents.length}`,
    `Score: ${report.complianceScore}%`,
  ].join(" | ");
}
