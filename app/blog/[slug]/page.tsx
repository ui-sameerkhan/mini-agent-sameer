import { notFound } from "next/navigation";
import { POSTS } from "@/lib/data";
export default async function Post({ params }: { params: Promise<{ slug: string }> }) {
  const { slug } = await params;
  const p = POSTS.find((x) => x.slug === slug); if (!p) notFound();
  return <article className="card prose"><h1>{p.title}</h1><div className="meta">{p.date}</div><p>{p.body}</p></article>;
}
