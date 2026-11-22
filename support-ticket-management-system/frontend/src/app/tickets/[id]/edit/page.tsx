import type { Metadata } from "next";
import Link from "next/link";
import TicketForm from "@/components/TicketForm";
import { loadTicket } from "@/lib/loadTicket";

export const metadata: Metadata = { title: "Edit ticket" };

export default async function EditTicketPage({
  params,
}: PageProps<"/tickets/[id]/edit">) {
  const { id } = await params;
  const ticket = await loadTicket(id);

  return (
    <>
      <p>
        <Link href={`/tickets/${ticket.id}`}>← Back to ticket</Link>
      </p>
      <div className="page-header">
        <h1>Edit ticket</h1>
      </div>
      {/* Keyed by version: reloading after a conflict resets the form to the latest data. */}
      <TicketForm key={ticket.version} ticket={ticket} />
    </>
  );
}
