import Link from "next/link";
import styles from "./Pagination.module.css";

interface PaginationProps {
  /** Zero-based current page, as returned by the API. */
  page: number;
  totalPages: number;
  /** Builds the link target for a zero-based page number. */
  hrefForPage: (page: number) => string;
}

export default function Pagination({
  page,
  totalPages,
  hrefForPage,
}: PaginationProps) {
  if (totalPages <= 1) {
    return null;
  }
  return (
    <nav className={styles.pagination} aria-label="Pagination">
      {page > 0 ? (
        <Link className="btn btn-secondary" href={hrefForPage(page - 1)}>
          Previous
        </Link>
      ) : (
        <span />
      )}
      <span className="muted">
        Page {page + 1} of {totalPages}
      </span>
      {page + 1 < totalPages ? (
        <Link className="btn btn-secondary" href={hrefForPage(page + 1)}>
          Next
        </Link>
      ) : (
        <span />
      )}
    </nav>
  );
}
