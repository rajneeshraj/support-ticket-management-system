"use client";

import { useRouter } from "next/navigation";
import { useState, type FormEvent } from "react";
import {
  ApiRequestError,
  UNEXPECTED_ERROR_MESSAGE,
  addComment,
} from "@/lib/api";
import ErrorMessage from "./ErrorMessage";
import styles from "./CommentForm.module.css";

export default function CommentForm({ ticketId }: { ticketId: number }) {
  const router = useRouter();
  const [author, setAuthor] = useState("");
  const [text, setText] = useState("");
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [generalError, setGeneralError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSubmitting(true);
    setFieldErrors({});
    setGeneralError(null);
    try {
      await addComment(ticketId, { author, text });
      setText("");
      router.refresh();
    } catch (error) {
      // The typed text stays in the box after a failure.
      if (error instanceof ApiRequestError) {
        const errors = error.fieldErrors;
        setFieldErrors(errors);
        if (Object.keys(errors).length === 0) {
          setGeneralError(error.message);
        }
      } else {
        setGeneralError(UNEXPECTED_ERROR_MESSAGE);
      }
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <form className={`card ${styles.form}`} onSubmit={handleSubmit} noValidate>
      <h2>Add a comment</h2>
      {generalError && <ErrorMessage message={generalError} />}

      <div className="field">
        <label htmlFor="comment-author">Your name</label>
        <input
          id="comment-author"
          value={author}
          maxLength={100}
          aria-invalid={fieldErrors.author !== undefined}
          aria-describedby={fieldErrors.author ? "author-error" : undefined}
          onChange={(event) => setAuthor(event.target.value)}
        />
        {fieldErrors.author && (
          <p id="author-error" className="field-error">
            {fieldErrors.author}
          </p>
        )}
      </div>

      <div className="field">
        <label htmlFor="comment-text">Comment</label>
        <textarea
          id="comment-text"
          value={text}
          maxLength={2000}
          aria-invalid={fieldErrors.text !== undefined}
          aria-describedby={fieldErrors.text ? "text-error" : undefined}
          onChange={(event) => setText(event.target.value)}
        />
        {fieldErrors.text && (
          <p id="text-error" className="field-error">
            {fieldErrors.text}
          </p>
        )}
      </div>

      <button type="submit" className="btn" disabled={submitting}>
        {submitting ? "Posting…" : "Post comment"}
      </button>
    </form>
  );
}
