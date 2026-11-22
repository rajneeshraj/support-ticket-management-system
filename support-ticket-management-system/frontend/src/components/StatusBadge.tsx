import { STATUS_LABELS } from "@/lib/format";
import type { TicketStatus } from "@/lib/types";
import styles from "./StatusBadge.module.css";

export default function StatusBadge({ status }: { status: TicketStatus }) {
  return (
    <span className={`${styles.badge} ${styles[status]}`}>
      {STATUS_LABELS[status]}
    </span>
  );
}
