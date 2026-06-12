export type MatchTimelineEventType =
  | "ITEM_PURCHASED"
  | "ITEM_SOLD"
  | "ITEM_DESTROYED"
  | "ITEM_UNDO"
  | "SKILL_LEVEL_UP"
  | string;

export type MatchTimelineEvent = {
  afterId?: number;
  beforeId?: number;
  itemId?: number;
  levelUpType?: string;
  participantId?: number;
  skillSlot?: number;
  timestamp: number;
  type: MatchTimelineEventType;
};
