import Link from "next/link";
import { POSTS } from "@/lib/data";
export const metadata = { title: "Career tips" };
export default function Blog() {
  return <div className="stack"><h1>Career tips</h1>{POSTS.map((p) => <Link key={p.slug} href={`/blog/${p.slug}`} className="card job"><h3>{p.title}</h3><div className="meta">{p.date}</div></Link>)}</div>;
}
