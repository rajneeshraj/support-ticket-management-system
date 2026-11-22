import type { Metadata } from "next";
import Link from "next/link";
import CommentForm from "@/components/CommentForm";
import CommentList from "@/components/CommentList";
import StatusBadge from "@/components/StatusBadge";
import StatusSelector from "@/components/StatusSelector";
import { formatDateTime, PRIORITY_LABELS } from "@/lib/format";
import { loadTicket } from "@/lib/loadTicket";
import styles from "./page.module.css";

export async function generateMetadata({
  params,
}: PageProps<"/tickets/[id]">): Promise<Metadata> {
  const { id } = await params;
  return { title: `Ticket #${id}` };
}

export default async function TicketDetailPage({
  params,
}: PageProps<"/tickets/[id]">) {
  const { id } = await params;
  const ticket = await loadTicket(id);

  return (
    <>
      <p>
        <Link href="/tickets">← All tickets</Link>
      </p>

      <div className="page-header">
        <h1>{ticket.title}</h1>
        <Link className="btn btn-secondary" href={`/tickets/${ticket.id}/edit`}>
          Edit
        </Link>
      </div>

      <section className="card" aria-label="Ticket details">
        <dl className={styles.details}>
          <div>
            <dt>Status</dt>
            <dd>
              <StatusBadge status={ticket.status} />
            </dd>
          </div>
          <div>
            <dt>Priority</dt>
            <dd>{PRIORITY_LABELS[ticket.priority]}</dd>
          </div>
          <div>
            <dt>Assignee</dt>
            <dd>{ticket.assignee ?? <span className="muted">Unassigned</span>}</dd>
          </div>
          <div>
            <dt>Created</dt>
            <dd>{formatDateTime(ticket.createdAt)}</dd>
          </div>
          <div>
            <dt>Last updated</dt>
            <dd>{formatDateTime(ticket.updatedAt)}</dd>
          </div>
        </dl>
        <h2>Description</h2>
        <p className={styles.description}>{ticket.description}</p>
      </section>

      <StatusSelector
        ticketId={ticket.id}
        status={ticket.status}
        allowedNextStatuses={ticket.allowedNextStatuses}
      />

      <section className={styles.comments} aria-labelledby="comments-heading">
        <h2 id="comments-heading">Comments ({ticket.comments.length})</h2>
        <CommentList comments={ticket.comments} />
        <CommentForm ticketId={ticket.id} />
      </section>
    </>
  );
}
