/**
 * Security-rule tests for SitePulse.
 *
 * These exist because the role system is the security boundary: a mistake here silently exposes
 * one project's payroll data to another project's foreman. Each test states the access it
 * expects and, just as importantly, the access it expects to be refused.
 *
 * Run with:  node --test firestore-tests/rules.test.mjs   (inside `firebase emulators:exec`)
 */
import { readFileSync } from "node:fs";
import { fileURLToPath } from "node:url";
import { dirname, join } from "node:path";
import test from "node:test";
import assert from "node:assert/strict";
import {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} from "@firebase/rules-unit-testing";
import { doc, getDoc, setDoc, collection, getDocs } from "firebase/firestore";

const PROJECT_ID = "sitepulse-rules-test";
// Resolved relative to this file so the suite runs from any working directory.
const RULES_PATH = join(dirname(fileURLToPath(import.meta.url)), "..", "firestore.rules");

const testEnv = await initializeTestEnvironment({
  projectId: PROJECT_ID,
  firestore: {
    rules: readFileSync(RULES_PATH, "utf8"),
    host: "127.0.0.1",
    port: 8080,
  },
});

/** Seeds documents with rules bypassed, so a test's setup can't be blocked by the rules it tests. */
async function seed(fn) {
  await testEnv.withSecurityRulesDisabled(async (ctx) => fn(ctx.firestore()));
}

function as(uid, email) {
  return testEnv.authenticatedContext(uid, { email, email_verified: true }).firestore();
}

// ---------------------------------------------------------------------------------------------
// Fixtures: two sites, a worker on each, and one user per role.
// ---------------------------------------------------------------------------------------------
await seed(async (db) => {
  await setDoc(doc(db, "sites/SITE-A"), { code: "SITE-A", name: "JVC", lat: 25, lng: 55, radius: 500 });
  await setDoc(doc(db, "sites/SITE-B"), { code: "SITE-B", name: "Deira", lat: 25.1, lng: 55.1, radius: 500 });

  await setDoc(doc(db, "workers/W-A"), { id: "W-A", name: "Worker A", designation: "Carpenter", site: "SITE-A" });
  await setDoc(doc(db, "workers/W-B"), { id: "W-B", name: "Worker B", designation: "Mason", site: "SITE-B" });

  await setDoc(doc(db, "attendance/2026-09-08_W-A"), { workerId: "W-A", date: "2026-09-08", siteCode: "SITE-A" });
  await setDoc(doc(db, "attendance/2026-09-08_W-B"), { workerId: "W-B", date: "2026-09-08", siteCode: "SITE-B" });

  await setDoc(doc(db, "users/uid-super"), { name: "Super", email: "super@ktc.test", role: "super_admin", assignedSites: ["ALL"], status: "active" });
  await setDoc(doc(db, "users/uid-foreman-a"), { name: "Foreman A", email: "fa@ktc.test", role: "foreman", assignedSites: ["SITE-A"], status: "active" });
  await setDoc(doc(db, "users/uid-tk-b"), { name: "TK B", email: "tkb@ktc.test", role: "timekeeper", assignedSites: ["SITE-B"], status: "active" });
  await setDoc(doc(db, "users/uid-staff"), { name: "Staff", email: "staff@ktc.test", role: "staff", assignedSites: ["SITE-A"], status: "active", employeeId: "W-A" });
  await setDoc(doc(db, "users/uid-disabled"), { name: "Gone", email: "gone@ktc.test", role: "admin", assignedSites: ["SITE-A"], status: "disabled" });
});

test("super admin reads every site's workers", async () => {
  const db = as("uid-super", "super@ktc.test");
  await assertSucceeds(getDoc(doc(db, "workers/W-A")));
  await assertSucceeds(getDoc(doc(db, "workers/W-B")));
});

test("foreman reads only their assigned site's worker", async () => {
  const db = as("uid-foreman-a", "fa@ktc.test");
  await assertSucceeds(getDoc(doc(db, "workers/W-A")));
  await assertFails(getDoc(doc(db, "workers/W-B")));
});

test("foreman marks attendance at their site but not another", async () => {
  const db = as("uid-foreman-a", "fa@ktc.test");
  await assertSucceeds(
    setDoc(doc(db, "attendance/2026-09-09_W-A"), { workerId: "W-A", date: "2026-09-09", siteCode: "SITE-A" }),
  );
  await assertFails(
    setDoc(doc(db, "attendance/2026-09-09_W-B"), { workerId: "W-B", date: "2026-09-09", siteCode: "SITE-B" }),
  );
});

test("timekeeper is scoped to their own site's attendance", async () => {
  const db = as("uid-tk-b", "tkb@ktc.test");
  await assertSucceeds(getDoc(doc(db, "attendance/2026-09-08_W-B")));
  await assertFails(getDoc(doc(db, "attendance/2026-09-08_W-A")));
});

test("foreman cannot create or edit workers", async () => {
  const db = as("uid-foreman-a", "fa@ktc.test");
  await assertFails(
    setDoc(doc(db, "workers/W-NEW"), { id: "W-NEW", name: "New", designation: "Helper", site: "SITE-A" }),
  );
});

test("staff read only their own attendance, never a colleague's", async () => {
  const db = as("uid-staff", "staff@ktc.test");
  await assertSucceeds(getDoc(doc(db, "attendance/2026-09-08_W-A")));
  await assertFails(getDoc(doc(db, "attendance/2026-09-08_W-B")));
});

test("staff cannot mark attendance for another employee", async () => {
  const db = as("uid-staff", "staff@ktc.test");
  await assertFails(
    setDoc(doc(db, "attendance/2026-09-10_W-B"), { workerId: "W-B", date: "2026-09-10", siteCode: "SITE-A" }),
  );
});

test("staff can self check in", async () => {
  const db = as("uid-staff", "staff@ktc.test");
  await assertSucceeds(
    setDoc(doc(db, "attendance/2026-09-10_W-A"), { workerId: "W-A", date: "2026-09-10", siteCode: "SITE-A" }),
  );
});

test("nobody can escalate their own role", async () => {
  const foreman = as("uid-foreman-a", "fa@ktc.test");
  await assertFails(setDoc(doc(foreman, "users/uid-foreman-a"), { role: "super_admin", assignedSites: ["ALL"], status: "active" }));
  // Nor grant themselves another site.
  await assertFails(setDoc(doc(foreman, "users/uid-foreman-a"), { assignedSites: ["SITE-A", "SITE-B"] }));
});

test("a user reads their own profile but not anyone else's", async () => {
  const db = as("uid-foreman-a", "fa@ktc.test");
  await assertSucceeds(getDoc(doc(db, "users/uid-foreman-a")));
  await assertFails(getDoc(doc(db, "users/uid-tk-b")));
});

test("only super admin lists all users", async () => {
  await assertSucceeds(getDocs(collection(as("uid-super", "super@ktc.test"), "users")));
  await assertFails(getDocs(collection(as("uid-foreman-a", "fa@ktc.test"), "users")));
});

test("a disabled account loses all access", async () => {
  const db = as("uid-disabled", "gone@ktc.test");
  await assertFails(getDoc(doc(db, "workers/W-A")));
  await assertFails(getDoc(doc(db, "attendance/2026-09-08_W-A")));
  await assertFails(setDoc(doc(db, "attendance/2026-09-11_W-A"), { workerId: "W-A", date: "2026-09-11", siteCode: "SITE-A" }));
});

test("an unauthenticated request is refused everywhere", async () => {
  const db = testEnv.unauthenticatedContext().firestore();
  await assertFails(getDoc(doc(db, "workers/W-A")));
  await assertFails(getDoc(doc(db, "sites/SITE-A")));
  await assertFails(setDoc(doc(db, "attendance/x_y"), { workerId: "x", siteCode: "SITE-A" }));
});

test("the configured admin email is super admin even with no profile document", async () => {
  // This is the bootstrap path: without it nobody could create the first profile.
  const db = as("uid-legacy-admin", "admin@ktc-manpower.com");
  await assertSucceeds(getDoc(doc(db, "workers/W-A")));
  await assertSucceeds(getDoc(doc(db, "workers/W-B")));
  await assertSucceeds(setDoc(doc(db, "users/uid-new"), { name: "New", email: "n@ktc.test", role: "foreman", assignedSites: ["SITE-A"], status: "active" }));
});

test("a legacy account keeps the access it had before profiles existed", async () => {
  // Migration safety: an existing login with no profile must not be locked out.
  const db = as("uid-legacy", "someone@ktc.test");
  await assertSucceeds(getDoc(doc(db, "workers/W-A")));
  // ...but still cannot touch user management.
  await assertFails(setDoc(doc(db, "users/uid-legacy"), { role: "super_admin" }));
});

test("attendance cannot be deleted by anyone below super admin", async () => {
  const { deleteDoc } = await import("firebase/firestore");
  await assertFails(deleteDoc(doc(as("uid-tk-b", "tkb@ktc.test"), "attendance/2026-09-08_W-B")));
  await assertFails(deleteDoc(doc(as("uid-foreman-a", "fa@ktc.test"), "attendance/2026-09-08_W-A")));
});

test.after(async () => {
  await testEnv.cleanup();
});
