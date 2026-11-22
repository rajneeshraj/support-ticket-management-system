import styles from "./ErrorMessage.module.css";

interface ErrorMessageProps {
  message: string;
  onRetry?: () => void;
  retryLabel?: string;
}

/** Announces a problem to the user, with an optional retry action. */
export default function ErrorMessage({
  message,
  onRetry,
  retryLabel = "Try again",
}: ErrorMessageProps) {
  return (
    <div role="alert" className={styles.error}>
      <p className={styles.message}>{message}</p>
      {onRetry && (
        <button type="button" className="btn btn-secondary" onClick={onRetry}>
          {retryLabel}
        </button>
      )}
    </div>
  );
}
