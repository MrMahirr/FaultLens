import { redirect } from "next/navigation";

/**
 * "/" has no page of its own: proxy.ts already sends signed-in users to /dashboard and
 * everyone else to /login. This covers requests that bypass the proxy.
 */
export default function RootPage() {
  redirect("/login");
}
