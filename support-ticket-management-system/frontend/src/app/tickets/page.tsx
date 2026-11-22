import type { Metadata } from "next";
import Link from "next/link";
import Pagination from "@/components/Pagination";
import TicketFilters from "@/components/TicketFilters";
import TicketList from "@/components/TicketList";
import { listTickets } from "@/lib/api";
import { TICKET_STATUSES, type TicketStatus } from "@/lib/types";

export const metadata: Metadata = { title: "Tickets" };

function first(value: string | string[] | undefined): string {
  return (Array.isArray(value) ? value[0] : value) ?? "";
}

function parseStatus(value: string): TicketStatus | undefined {
  return TICKET_STATUSES.find((status) => status === value);
}

function parsePage(value: string): number {
  const page = Number.parseInt(value, 10);
  return Number.isInteger(page) && page > 0 ? page - 1 : 0;
}

export default async function TicketsPage({
  searchParams,
}: PageProps<"/tickets">) {
  const params = await searchParams;
  const q = first(params.q).trim().slice(0, 100);
  const statusParam = first(params.status);
  const status = parseStatus(statusParam);
  const page = parsePage(first(params.page));

  const result = await listTickets({ q, status, page });
  const filtered = Boolean(q || status);

  function hrefForPage(target: number): string {
    const query = new URLSearchParams();
    if (q) query.set("q", q);
    if (status) query.set("status", status);
    if (target > 0) query.set("page", String(target + 1));
    const text = query.toString();
    return text ? `/tickets?${text}` : "/tickets";
  }

  return (
    <>
      <div className="page-header">
        <h1>Tickets</h1>
        <Link className="btn" href="/tickets/new">
          New ticket
        </Link>
      </div>

      <TicketFilters q={q} status={status ?? ""} />

      {result.content.length === 0 ? (
        <div className="card">
          {filtered ? (
            <>
              <p>No matching tickets.</p>
              <Link className="btn btn-secondary" href="/tickets">
                Clear filters
              </Link>
            </>
          ) : (
            <>
              <p>No tickets yet. Create the first one.</p>
              <Link className="btn" href="/tickets/new">
                New ticket
              </Link>
            </>
          )}
        </div>
      ) : (
        <>
          <p className="muted" aria-live="polite">
            {result.totalElements}{" "}
            {result.totalElements === 1 ? "ticket" : "tickets"}
          </p>
          <TicketList tickets={result.content} />
          <Pagination
            page={result.page}
            totalPages={result.totalPages}
            hrefForPage={hrefForPage}
          />
        </>
      )}
    </>
  );
}
