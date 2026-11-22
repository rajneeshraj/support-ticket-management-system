import Link from "next/link";
import { formatDateTime, PRIORITY_LABELS } from "@/lib/format";
import type { TicketSummary } from "@/lib/types";
import StatusBadge from "./StatusBadge";
import styles from "./TicketList.module.css";

/** A table from 640 px up; each row becomes a stacked card below that. */
export default function TicketList({ tickets }: { tickets: TicketSummary[] }) {
  return (
    <table className={styles.table}>
      <thead>
        <tr>
          <th scope="col">Title</th>
          <th scope="col">Status</th>
          <th scope="col">Priority</th>
          <th scope="col">Assignee</th>
          <th scope="col">Created</th>
        </tr>
      </thead>
      <tbody>
        {tickets.map((ticket) => (
          <tr key={ticket.id}>
            <td className={styles.title}>
              <Link href={`/tickets/${ticket.id}`}>{ticket.title}</Link>
            </td>
            <td data-label="Status">
              <StatusBadge status={ticket.status} />
            </td>
            <td data-label="Priority">{PRIORITY_LABELS[ticket.priority]}</td>
            <td data-label="Assignee">
              {ticket.assignee ?? <span className="muted">Unassigned</span>}
            </td>
            <td data-label="Created">{formatDateTime(ticket.createdAt)}</td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}
