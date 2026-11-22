import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import StatusSelector from "@/components/StatusSelector";
import { ApiRequestError } from "@/lib/api";
import type { TicketStatus } from "@/lib/types";

const refresh = vi.fn();
vi.mock("next/navigation", () => ({
  useRouter: () => ({ refresh, push: vi.fn() }),
}));

const changeTicketStatus = vi.fn();
vi.mock("@/lib/api", async (importOriginal) => ({
  ...(await importOriginal<typeof import("@/lib/api")>()),
  changeTicketStatus: (...args: unknown[]) => changeTicketStatus(...args),
}));

function renderSelector(status: TicketStatus, allowedNextStatuses: TicketStatus[]) {
  render(
    <StatusSelector
      ticketId={7}
      status={status}
      allowedNextStatuses={allowedNextStatuses}
    />,
  );
}

describe("StatusSelector", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it.each([
    ["OPEN", ["IN_PROGRESS", "CANCELLED"], ["Move to In Progress", "Move to Cancelled"]],
    ["IN_PROGRESS", ["RESOLVED", "CANCELLED"], ["Move to Resolved", "Move to Cancelled"]],
    ["RESOLVED", ["CLOSED"], ["Move to Closed"]],
  ] as const)("offers only the allowed choices from %s", (status, allowed, labels) => {
    renderSelector(status, [...allowed]);

    expect(screen.getAllByRole("button").map((b) => b.textContent)).toEqual(labels);
  });

  it.each(["CLOSED", "CANCELLED"] as const)(
    "offers no choices for the final status %s",
    (status) => {
      renderSelector(status, []);

      expect(screen.queryByRole("button")).not.toBeInTheDocument();
      expect(screen.getByText(/can no longer change/)).toBeInTheDocument();
    },
  );

  it("sends the chosen status and refreshes the ticket", async () => {
    changeTicketStatus.mockResolvedValue({});
    renderSelector("OPEN", ["IN_PROGRESS", "CANCELLED"]);

    await userEvent.click(screen.getByRole("button", { name: "Move to In Progress" }));

    await waitFor(() => expect(refresh).toHaveBeenCalled());
    expect(changeTicketStatus).toHaveBeenCalledWith(7, "IN_PROGRESS");
    expect(screen.queryByRole("alert")).not.toBeInTheDocument();
  });

  it("shows the server's rejection message and refreshes to the real status", async () => {
    changeTicketStatus.mockRejectedValue(
      new ApiRequestError(
        409,
        {
          timestamp: "2026-01-01T00:00:00Z",
          status: 409,
          code: "INVALID_STATUS_TRANSITION",
          message: "Cannot move a ticket from CLOSED to OPEN.",
          path: "/api/v1/tickets/7/status",
        },
        "Cannot move a ticket from CLOSED to OPEN.",
      ),
    );
    renderSelector("RESOLVED", ["CLOSED"]);

    await userEvent.click(screen.getByRole("button", { name: "Move to Closed" }));

    expect(await screen.findByRole("alert")).toHaveTextContent(
      "Cannot move a ticket from CLOSED to OPEN.",
    );
    expect(refresh).toHaveBeenCalled();
  });
});
