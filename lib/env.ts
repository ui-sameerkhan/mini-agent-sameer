/** True in `next dev`, or when SHUGHL_DEMO=1: demo seed data and on-screen OTP codes. Never set in real production. */
export const DEMO = process.env.NODE_ENV !== "production" || process.env.SHUGHL_DEMO === "1";
