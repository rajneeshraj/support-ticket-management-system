import { formatDateTime } from "@/lib/format";
import type { Comment } from "@/lib/types";
import styles from "./CommentList.module.css";

/** Comments arrive oldest first from the API. */
export default function CommentList({ comments }: { comments: Comment[] }) {
  if (comments.length === 0) {
    return <p className="muted">No comments yet.</p>;
  }
  return (
    <ul className={styles.list}>
      {comments.map((comment) => (
        <li key={comment.id} className={`card ${styles.item}`}>
          <p className={styles.meta}>
            <strong>{comment.author}</strong>{" "}
            <span className="muted">
              <time dateTime={comment.createdAt}>
                {formatDateTime(comment.createdAt)}
              </time>
            </span>
          </p>
          <p className={styles.text}>{comment.text}</p>
        </li>
      ))}
    </ul>
  );
}
