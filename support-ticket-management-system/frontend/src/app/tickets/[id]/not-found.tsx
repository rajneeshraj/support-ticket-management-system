import Link from "next/link";

export default function TicketNotFound() {
  return (
    <div className="card">
      <h1>Ticket not found</h1>
      <p>This ticket does not exist or may have been removed.</p>
      <Link className="btn" href="/tickets">
        Back to tickets
      </Link>
    </div>
  );
}
