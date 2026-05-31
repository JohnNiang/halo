export interface Notification {
  id: number;
  recipient: string;
  category: string;
  messageKey: string;
  messageArgs: Record<string, string>;
  subjectUrl: string | null;
  unread: boolean;
  createdAt: string;
  readAt: string | null;
}

export interface NotificationListResponse {
  items: Notification[];
  total: number;
  page: number;
  size: number;
  totalPages: number;
}

export interface UnreadCountResponse {
  count: number;
}

export interface NotificationPreferencesResponse {
  categories: NotificationCategory[];
  notifiers: string[];
  preferences: Record<string, string[]>;
}

export interface NotificationCategory {
  name: string;
  displayName: string;
  description: string;
}
