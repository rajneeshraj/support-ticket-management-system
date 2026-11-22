import type {
  ApiError,
  Comment,
  CreateCommentInput,
  CreateTicketInput,
  ListTicketsParams,
  TicketDetail,
  TicketPage,
  TicketStatus,
  UpdateTicketInput,
} from "./types";

const DEFAULT_BACKEND_URL = "http://localhost:8080";

export const NETWORK_ERROR_MESSAGE =
  "Could not reach the server. Check your connection and try again.";
export const UNEXPECTED_ERROR_MESSAGE =
  "Something went wrong. Please try again.";

/** Server code calls the backend directly; the browser uses the same-origin rewrite. */
function baseUrl(): string {
  if (typeof window === "undefined") {
    return `${process.env.BACKEND_URL ?? DEFAULT_BACKEND_URL}/api/v1`;
  }
  return "/api/v1";
}

/** A failed API call. `message` is always safe to show to the user. */
export class ApiRequestError extends Error {
  readonly status: number;
  readonly code: string;
  readonly apiError: ApiError | null;

  constructor(status: number, apiError: ApiError | null, message: string) {
    super(message);
    this.name = "ApiRequestError";
    this.status = status;
    this.code = apiError?.code ?? (status === 0 ? "NETWORK_ERROR" : "UNKNOWN");
    this.apiError = apiError;
  }

  /** Field name to message, for showing errors next to the inputs. */
  get fieldErrors(): Record<string, string> {
    const result: Record<string, string> = {};
    for (const fieldError of this.apiError?.fieldErrors ?? []) {
      if (!(fieldError.field in result)) {
        result[fieldError.field] = fieldError.message;
      }
    }
    return result;
  }
}

function isApiError(value: unknown): value is ApiError {
  return (
    typeof value === "object" &&
    value !== null &&
    typeof (value as ApiError).code === "string" &&
    typeof (value as ApiError).message === "string"
  );
}

/** Turns an API failure into a friendly message; never raw technical text. */
function friendlyMessage(status: number, apiError: ApiError | null): string {
  // The backend writes 4xx messages for people; anything else gets the generic text.
  return apiError && status < 500 ? apiError.message : UNEXPECTED_ERROR_MESSAGE;
}

async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  let response: Response;
  try {
    response = await fetch(`${baseUrl()}${path}`, {
      ...init,
      cache: "no-store",
      headers: {
        Accept: "application/json",
        ...(init.body ? { "Content-Type": "application/json" } : {}),
        ...init.headers,
      },
    });
  } catch {
    throw new ApiRequestError(0, null, NETWORK_ERROR_MESSAGE);
  }

  if (!response.ok) {
    let apiError: ApiError | null = null;
    try {
      const body: unknown = await response.json();
      apiError = isApiError(body) ? body : null;
    } catch {
      apiError = null;
    }
    throw new ApiRequestError(
      response.status,
      apiError,
      friendlyMessage(response.status, apiError),
    );
  }

  return (await response.json()) as T;
}

function send<T>(method: string, path: string, body: unknown): Promise<T> {
  return request<T>(path, { method, body: JSON.stringify(body) });
}

export function listTickets(params: ListTicketsParams = {}): Promise<TicketPage> {
  const query = new URLSearchParams();
  if (params.status) query.set("status", params.status);
  if (params.q) query.set("q", params.q);
  if (params.page) query.set("page", String(params.page));
  if (params.size) query.set("size", String(params.size));
  const suffix = query.size > 0 ? `?${query}` : "";
  return request<TicketPage>(`/tickets${suffix}`);
}

export function getTicket(id: number | string): Promise<TicketDetail> {
  return request<TicketDetail>(`/tickets/${encodeURIComponent(String(id))}`);
}

export function createTicket(input: CreateTicketInput): Promise<TicketDetail> {
  return send<TicketDetail>("POST", "/tickets", input);
}

export function updateTicket(
  id: number,
  input: UpdateTicketInput,
): Promise<TicketDetail> {
  return send<TicketDetail>("PUT", `/tickets/${id}`, input);
}

export function changeTicketStatus(
  id: number,
  status: TicketStatus,
): Promise<TicketDetail> {
  return send<TicketDetail>("PATCH", `/tickets/${id}/status`, { status });
}

export function addComment(
  ticketId: number,
  input: CreateCommentInput,
): Promise<Comment> {
  return send<Comment>("POST", `/tickets/${ticketId}/comments`, input);
}
