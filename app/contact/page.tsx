export const metadata = { title: "Contact" };
export default async function Contact({ searchParams }: { searchParams: Promise<{ reported?: string }> }) {
  const { reported } = await searchParams;
  return (
    <article className="card prose stack"><h1>Contact us</h1>
      {reported && <div className="okmsg">Thank you — our team will review the report you sent.</div>}
      <p>Email: <a href="mailto:support@shughl.example">support@shughl.example</a> (replace with your real address)</p>
      <p>To report a scam, use the “Report this job” button on the job page.</p></article>
  );
}
