import { notFound } from "next/navigation";
import { ApiRequestError, getTicket } from "./api";
import type { TicketDetail } from "./types";

/** Loads a ticket for a page, turning "no such ticket" (and malformed ids) into the not-found page. */
export async function loadTicket(id: string): Promise<TicketDetail> {
  if (!/^\d{1,18}$/.test(id)) {
    notFound();
  }
  try {
    return await getTicket(id);
  } catch (error) {
    if (error instanceof ApiRequestError && error.status === 404) {
      notFound();
    }
    throw error;
  }
}
