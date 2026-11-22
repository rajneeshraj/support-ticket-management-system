"use client";

import { useRouter } from "next/navigation";
import { useState, type FormEvent } from "react";
import {
  ApiRequestError,
  UNEXPECTED_ERROR_MESSAGE,
  createTicket,
  updateTicket,
} from "@/lib/api";
import { PRIORITY_LABELS } from "@/lib/format";
import {
  TICKET_PRIORITIES,
  type TicketDetail,
  type TicketPriority,
} from "@/lib/types";
import ErrorMessage from "./ErrorMessage";
import styles from "./TicketForm.module.css";

interface TicketFormProps {
  /** When given, the form edits this ticket; otherwise it creates a new one. */
  ticket?: TicketDetail;
}

interface FormProblem {
  message: string;
  fieldErrors: Record<string, string>;
  conflict: boolean;
}

const FIELD_NAMES = ["title", "description", "priority", "assignee"];

function toProblem(error: unknown): FormProblem {
  if (error instanceof ApiRequestError) {
    const fieldErrors = error.fieldErrors;
    // Field problems are shown beside the inputs; show a form-level message only for the rest.
    const onlyFieldProblems =
      Object.keys(fieldErrors).length > 0 &&
      Object.keys(fieldErrors).every((field) => FIELD_NAMES.includes(field));
    return {
      message: onlyFieldProblems ? "" : error.message,
      fieldErrors,
      conflict: error.code === "CONCURRENT_MODIFICATION",
    };
  }
  return { message: UNEXPECTED_ERROR_MESSAGE, fieldErrors: {}, conflict: false };
}

export default function TicketForm({ ticket }: TicketFormProps) {
  const router = useRouter();
  const editing = ticket !== undefined;
  const [title, setTitle] = useState(ticket?.title ?? "");
  const [description, setDescription] = useState(ticket?.description ?? "");
  const [priority, setPriority] = useState<TicketPriority>(
    ticket?.priority ?? "MEDIUM",
  );
  const [assignee, setAssignee] = useState(ticket?.assignee ?? "");
  const [problem, setProblem] = useState<FormProblem | null>(null);
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSubmitting(true);
    setProblem(null);
    try {
      let saved: TicketDetail;
      if (ticket) {
        saved = await updateTicket(ticket.id, {
          title,
          description,
          priority,
          assignee: assignee.trim() === "" ? null : assignee,
          version: ticket.version,
        });
      } else {
        saved = await createTicket({ title, description, priority });
      }
      router.push(`/tickets/${saved.id}`);
      router.refresh();
    } catch (error) {
      // Entered values stay in state, so the user never retypes after a failure.
      setProblem(toProblem(error));
      setSubmitting(false);
    }
  }

  function fieldError(name: string) {
    const message = problem?.fieldErrors[name];
    return {
      invalid: message !== undefined,
      error: message ? (
        <p id={`${name}-error`} className="field-error">
          {message}
        </p>
      ) : null,
      describedBy: message ? `${name}-error` : undefined,
    };
  }

  const titleState = fieldError("title");
  const descriptionState = fieldError("description");
  const priorityState = fieldError("priority");
  const assigneeState = fieldError("assignee");

  return (
    <form className={`card ${styles.form}`} onSubmit={handleSubmit} noValidate>
      {problem?.message && (
        <ErrorMessage
          message={problem.message}
          onRetry={problem.conflict ? () => router.refresh() : undefined}
          retryLabel="Reload ticket"
        />
      )}

      <div className="field">
        <label htmlFor="title">Title</label>
        <input
          id="title"
          name="title"
          value={title}
          maxLength={150}
          aria-invalid={titleState.invalid}
          aria-describedby={titleState.describedBy}
          onChange={(event) => setTitle(event.target.value)}
        />
        {titleState.error}
      </div>

      <div className="field">
        <label htmlFor="description">Description</label>
        <textarea
          id="description"
          name="description"
          value={description}
          maxLength={5000}
          aria-invalid={descriptionState.invalid}
          aria-describedby={descriptionState.describedBy}
          onChange={(event) => setDescription(event.target.value)}
        />
        {descriptionState.error}
      </div>

      <div className="field">
        <label htmlFor="priority">Priority</label>
        <select
          id="priority"
          name="priority"
          value={priority}
          aria-invalid={priorityState.invalid}
          aria-describedby={priorityState.describedBy}
          onChange={(event) =>
            setPriority(event.target.value as TicketPriority)
          }
        >
          {TICKET_PRIORITIES.map((value) => (
            <option key={value} value={value}>
              {PRIORITY_LABELS[value]}
            </option>
          ))}
        </select>
        {priorityState.error}
      </div>

      {editing && (
        <div className="field">
          <label htmlFor="assignee">Assignee</label>
          <input
            id="assignee"
            name="assignee"
            value={assignee}
            maxLength={100}
            aria-invalid={assigneeState.invalid}
            aria-describedby={assigneeState.describedBy}
            onChange={(event) => setAssignee(event.target.value)}
          />
          {assigneeState.error}
        </div>
      )}

      <div className="actions">
        <button type="submit" className="btn" disabled={submitting}>
          {submitting ? "Saving…" : editing ? "Save changes" : "Create ticket"}
        </button>
        <button
          type="button"
          className="btn btn-secondary"
          disabled={submitting}
          onClick={() =>
            router.push(ticket ? `/tickets/${ticket.id}` : "/tickets")
          }
        >
          Cancel
        </button>
      </div>
    </form>
  );
}
