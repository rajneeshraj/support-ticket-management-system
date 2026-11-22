import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import ErrorMessage from "@/components/ErrorMessage";

describe("ErrorMessage", () => {
  it("announces the server message as an alert", () => {
    render(<ErrorMessage message="Title is required and must not be blank." />);

    expect(screen.getByRole("alert")).toHaveTextContent(
      "Title is required and must not be blank.",
    );
    expect(screen.queryByRole("button")).not.toBeInTheDocument();
  });

  it("offers a retry action when one is provided", async () => {
    const onRetry = vi.fn();
    render(<ErrorMessage message="Could not reach the server." onRetry={onRetry} />);

    await userEvent.click(screen.getByRole("button", { name: "Try again" }));

    expect(onRetry).toHaveBeenCalledOnce();
  });
});
