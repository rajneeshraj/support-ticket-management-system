import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import TicketForm from "@/components/TicketForm";
import { ApiRequestError } from "@/lib/api";
import type { ApiError, TicketDetail } from "@/lib/types";

const push = vi.fn();
const refresh = vi.fn();
vi.mock("next/navigation", () => ({
  useRouter: () => ({ push, refresh }),
}));

const createTicket = vi.fn();
const updateTicket = vi.fn();
vi.mock("@/lib/api", async (importOriginal) => ({
  ...(await importOriginal<typeof import("@/lib/api")>()),
  createTicket: (...args: unknown[]) => createTicket(...args),
  updateTicket: (...args: unknown[]) => updateTicket(...args),
}));

function apiError(overrides: Partial<ApiError>): ApiRequestError {
  const body: ApiError = {
    timestamp: "2026-01-01T00:00:00Z",
    status: 400,
    code: "VALIDATION_ERROR",
    message: "Validation failed.",
    path: "/api/v1/tickets",
    ...overrides,
  };
  return new ApiRequestError(body.status, body, body.message);
}

const existing: TicketDetail = {
  id: 5,
  title: "Printer down",
  description: "Floor 2",
  status: "OPEN",
  priority: "HIGH",
  assignee: null,
  createdAt: "2026-01-01T00:00:00Z",
  updatedAt: "2026-01-01T00:00:00Z",
  version: 3,
  allowedNextStatuses: ["IN_PROGRESS", "CANCELLED"],
  comments: [],
};

describe("TicketForm", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("shows each field error next to its field and keeps the entered text", async () => {
    createTicket.mockRejectedValue(
      apiError({
        fieldErrors: [
          { field: "title", message: "Title is required and must not be blank." },
          { field: "description", message: "Description must be at most 5,000 characters." },
        ],
      }),
    );
    render(<TicketForm />);

    await userEvent.type(screen.getByLabelText("Description"), "Some details");
    await userEvent.click(screen.getByRole("button", { name: "Create ticket" }));

    const title = screen.getByLabelText("Title");
    expect(await screen.findByText("Title is required and must not be blank.")).toBeInTheDocument();
    expect(title).toHaveAttribute("aria-invalid", "true");
    expect(title).toHaveAccessibleDescription("Title is required and must not be blank.");
    expect(screen.getByLabelText("Description")).toHaveAccessibleDescription(
      "Description must be at most 5,000 characters.",
    );
    expect(screen.getByLabelText("Description")).toHaveValue("Some details");
    // Field problems are not repeated in a form-level alert.
    expect(screen.queryByRole("alert")).not.toBeInTheDocument();
    expect(push).not.toHaveBeenCalled();
  });

  it("sends the details and opens the new ticket", async () => {
    createTicket.mockResolvedValue({ ...existing, id: 42 });
    render(<TicketForm />);

    await userEvent.type(screen.getByLabelText("Title"), "New issue");
    await userEvent.type(screen.getByLabelText("Description"), "Details");
    await userEvent.selectOptions(screen.getByLabelText("Priority"), "CRITICAL");
    await userEvent.click(screen.getByRole("button", { name: "Create ticket" }));

    await waitFor(() => expect(push).toHaveBeenCalledWith("/tickets/42"));
    expect(createTicket).toHaveBeenCalledWith({
      title: "New issue",
      description: "Details",
      priority: "CRITICAL",
    });
  });

  it("shows a form-level message for errors that are not about a field", async () => {
    createTicket.mockRejectedValue(new ApiRequestError(0, null, "Could not reach the server."));
    render(<TicketForm />);

    await userEvent.click(screen.getByRole("button", { name: "Create ticket" }));

    expect(await screen.findByRole("alert")).toHaveTextContent("Could not reach the server.");
  });

  it("prefills in edit mode and sends the loaded version", async () => {
    updateTicket.mockResolvedValue(existing);
    render(<TicketForm ticket={existing} />);

    expect(screen.getByLabelText("Title")).toHaveValue("Printer down");
    await userEvent.type(screen.getByLabelText("Assignee"), "Asha");
    await userEvent.click(screen.getByRole("button", { name: "Save changes" }));

    await waitFor(() => expect(push).toHaveBeenCalledWith("/tickets/5"));
    expect(updateTicket).toHaveBeenCalledWith(5, {
      title: "Printer down",
      description: "Floor 2",
      priority: "HIGH",
      assignee: "Asha",
      version: 3,
    });
  });

  it("offers to reload the ticket after a concurrent modification", async () => {
    updateTicket.mockRejectedValue(
      apiError({
        status: 409,
        code: "CONCURRENT_MODIFICATION",
        message: "This ticket was changed by someone else. Reload it and try again.",
      }),
    );
    render(<TicketForm ticket={existing} />);

    await userEvent.click(screen.getByRole("button", { name: "Save changes" }));
    expect(await screen.findByRole("alert")).toHaveTextContent("changed by someone else");
    await userEvent.click(screen.getByRole("button", { name: "Reload ticket" }));

    expect(refresh).toHaveBeenCalled();
  });
});
