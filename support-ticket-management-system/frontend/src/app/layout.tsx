import type { Metadata, Viewport } from "next";
import Link from "next/link";
import "./globals.css";

export const metadata: Metadata = {
  title: {
    default: "Support Tickets",
    template: "%s | Support Tickets",
  },
  description: "Create, track and resolve support tickets.",
};

export const viewport: Viewport = {
  width: "device-width",
  initialScale: 1,
};

export default function RootLayout({ children }: LayoutProps<"/">) {
  return (
    <html lang="en">
      <body>
        <header className="site-header">
          <div className="page site-header-inner">
            <Link href="/tickets" className="site-title">
              Support Tickets
            </Link>
          </div>
        </header>
        <main className="page">{children}</main>
      </body>
    </html>
  );
}
