export type StoredEvent = {
  id: string;
  eventType: string;
  userId: string;
  details: Record<string, unknown>;
  timestamp: number;
  hash: string;
};

const store: StoredEvent[] = [];
let nextId = 1;

export function persist(
  eventType: string,
  userId: string,
  details: Record<string, unknown>,
): StoredEvent {
  const id = `EVT-${String(nextId++).padStart(6, "0")}`;
  const timestamp = Date.now();
  const hash = simpleHash(`${id}:${eventType}:${userId}:${timestamp}`);
  const event: StoredEvent = { id, eventType, userId, details, timestamp, hash };
  store.push(event);
  return event;
}

export function findById(id: string): StoredEvent | undefined {
  return store.find((e) => e.id === id);
}

export function findByUser(userId: string): StoredEvent[] {
  return store.filter((e) => e.userId === userId);
}

export function count(): number {
  return store.length;
}

export function clearStore(): void {
  store.length = 0;
  nextId = 1;
}

function simpleHash(input: string): string {
  let hash = 0;
  for (let i = 0; i < input.length; i++) {
    const char = input.charCodeAt(i);
    hash = (hash << 5) - hash + char;
    hash |= 0;
  }
  return Math.abs(hash).toString(16);
}
