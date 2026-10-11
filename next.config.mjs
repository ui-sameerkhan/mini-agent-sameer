/** @type {import('next').NextConfig} */
export default {
  serverExternalPackages: ["better-sqlite3"],
  experimental: { serverActions: { bodySizeLimit: "8mb" } },
};
