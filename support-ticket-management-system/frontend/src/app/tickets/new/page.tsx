import type { Metadata } from "next";
import TicketForm from "@/components/TicketForm";

export const metadata: Metadata = { title: "New ticket" };

export default function NewTicketPage() {
  return (
    <>
      <div className="page-header">
        <h1>New ticket</h1>
      </div>
      <TicketForm />
    </>
  );
}
