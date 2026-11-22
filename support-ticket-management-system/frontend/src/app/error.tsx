"use client";

import { useEffect } from "react";
import ErrorMessage from "@/components/ErrorMessage";

export default function AppError({
  error,
  retry,
}: {
  error: unknown;
  retry: () => void;
}) {
  useEffect(() => {
    console.error(error);
  }, [error]);

  return (
    <>
      <h1>Something went wrong</h1>
      {/* Raw error text is never shown; it can contain technical detail. */}
      <ErrorMessage
        message="We could not load this page. Check your connection and try again."
        onRetry={retry}
      />
    </>
  );
}
