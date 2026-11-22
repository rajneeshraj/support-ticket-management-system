"use client";

import { useRouter } from "next/navigation";
import { useState } from "react";
import {
  ApiRequestError,
  UNEXPECTED_ERROR_MESSAGE,
  changeTicketStatus,
} from "@/lib/api";
import { STATUS_LABELS } from "@/lib/format";
import type { TicketStatus } from "@/lib/types";
import ErrorMessage from "./ErrorMessage";
import styles from "./StatusSelector.module.css";

interface StatusSelectorProps {
  ticketId: number;
  status: TicketStatus;
  /** Exactly what the backend allows from the current status; the UI never decides this itself. */
  allowedNextStatuses: TicketStatus[];
}

export default function StatusSelector({
  ticketId,
  status,
  allowedNextStatuses,
}: StatusSelectorProps) {
  const router = useRouter();
  const [pending, setPending] = useState<TicketStatus | null>(null);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  async function move(target: TicketStatus) {
    setPending(target);
    setErrorMessage(null);
    try {
      await changeTicketStatus(ticketId, target);
    } catch (error) {
      setErrorMessage(
        error instanceof ApiRequestError
          ? error.message
          : UNEXPECTED_ERROR_MESSAGE,
      );
    } finally {
      setPending(null);
      // Success or rejection, show the ticket's real current status and choices.
      router.refresh();
    }
  }

  return (
    <section className={styles.selector} aria-labelledby="status-heading">
      <h2 id="status-heading">Change status</h2>
      {errorMessage && <ErrorMessage message={errorMessage} />}
      {allowedNextStatuses.length === 0 ? (
        <p className="muted">
          This ticket is {STATUS_LABELS[status]} and its status can no longer
          change.
        </p>
      ) : (
        <div className="actions">
          {allowedNextStatuses.map((target) => (
            <button
              key={target}
              type="button"
              className="btn btn-secondary"
              disabled={pending !== null}
              onClick={() => move(target)}
            >
              {pending === target
                ? "Updating…"
                : `Move to ${STATUS_LABELS[target]}`}
            </button>
          ))}
        </div>
      )}
    </section>
  );
}
