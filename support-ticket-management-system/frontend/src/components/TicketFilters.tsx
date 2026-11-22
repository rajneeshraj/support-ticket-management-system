"use client";

import { usePathname, useRouter } from "next/navigation";
import { useEffect, useRef, useState } from "react";
import { STATUS_LABELS } from "@/lib/format";
import { TICKET_STATUSES } from "@/lib/types";
import styles from "./TicketFilters.module.css";

const SEARCH_DEBOUNCE_MS = 300;

interface TicketFiltersProps {
  /** Current values from the URL. */
  q: string;
  status: string;
}

/** Keyword search (debounced) and status filter; state lives in the URL so results are shareable. */
export default function TicketFilters({ q, status }: TicketFiltersProps) {
  const router = useRouter();
  const pathname = usePathname();
  const [text, setText] = useState(q);
  const latest = useRef({ q, status });

  useEffect(() => {
    latest.current = { q, status };
  }, [q, status]);

  function navigate(next: { q: string; status: string }) {
    const params = new URLSearchParams();
    if (next.q) params.set("q", next.q);
    if (next.status) params.set("status", next.status);
    const query = params.toString();
    router.replace(query ? `${pathname}?${query}` : pathname);
  }

  useEffect(() => {
    const trimmed = text.trim();
    if (trimmed === latest.current.q) {
      return;
    }
    const timer = setTimeout(
      () => navigate({ q: trimmed, status: latest.current.status }),
      SEARCH_DEBOUNCE_MS,
    );
    return () => clearTimeout(timer);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [text]);

  function clear() {
    setText("");
    navigate({ q: "", status: "" });
  }

  const hasFilters = Boolean(text || status);

  return (
    <form
      className={styles.filters}
      role="search"
      onSubmit={(event) => {
        event.preventDefault();
        navigate({ q: text.trim(), status });
      }}
    >
      <div className="field">
        <label htmlFor="ticket-search">Search</label>
        <input
          id="ticket-search"
          type="search"
          value={text}
          maxLength={100}
          placeholder="Search title or description"
          onChange={(event) => setText(event.target.value)}
        />
      </div>
      <div className="field">
        <label htmlFor="ticket-status">Status</label>
        <select
          id="ticket-status"
          value={status}
          onChange={(event) =>
            navigate({ q: text.trim(), status: event.target.value })
          }
        >
          <option value="">All statuses</option>
          {TICKET_STATUSES.map((value) => (
            <option key={value} value={value}>
              {STATUS_LABELS[value]}
            </option>
          ))}
        </select>
      </div>
      {hasFilters && (
        <button type="button" className="btn btn-secondary" onClick={clear}>
          Clear
        </button>
      )}
    </form>
  );
}
