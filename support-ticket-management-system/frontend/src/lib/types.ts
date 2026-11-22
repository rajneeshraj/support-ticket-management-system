export const TICKET_STATUSES = [
  "OPEN",
  "IN_PROGRESS",
  "RESOLVED",
  "CLOSED",
  "CANCELLED",
] as const;
export type TicketStatus = (typeof TICKET_STATUSES)[number];

export const TICKET_PRIORITIES = ["LOW", "MEDIUM", "HIGH", "CRITICAL"] as const;
export type TicketPriority = (typeof TICKET_PRIORITIES)[number];

export interface TicketSummary {
  id: number;
  title: string;
  status: TicketStatus;
  priority: TicketPriority;
  assignee: string | null;
  createdAt: string;
}

export interface Comment {
  id: number;
  author: string;
  text: string;
  createdAt: string;
}

export interface TicketDetail {
  id: number;
  title: string;
  description: string;
  status: TicketStatus;
  priority: TicketPriority;
  assignee: string | null;
  createdAt: string;
  updatedAt: string;
  version: number;
  allowedNextStatuses: TicketStatus[];
  comments: Comment[];
}

export interface TicketPage {
  content: TicketSummary[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface FieldError {
  field: string;
  message: string;
}

export interface ApiError {
  timestamp: string;
  status: number;
  code: string;
  message: string;
  path: string;
  fieldErrors?: FieldError[];
  currentStatus?: TicketStatus;
  requestedStatus?: TicketStatus;
  allowedNextStatuses?: TicketStatus[];
}

export interface CreateTicketInput {
  title: string;
  description: string;
  priority: TicketPriority;
}

export interface UpdateTicketInput {
  title: string;
  description: string;
  priority: TicketPriority;
  assignee: string | null;
  version: number;
}

export interface CreateCommentInput {
  author: string;
  text: string;
}

export interface ListTicketsParams {
  status?: TicketStatus;
  q?: string;
  page?: number;
  size?: number;
}
