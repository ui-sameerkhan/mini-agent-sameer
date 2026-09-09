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

// ---------------------------------------------------------------------------------------------
// Invites: access prepared for a login that has not signed in yet.
// ---------------------------------------------------------------------------------------------

test("an invited account claims exactly the access it was given", async () => {
  await seed(async (db) => {
    await setDoc(doc(db, "userInvites/invited@ktc.test"), {
      email: "invited@ktc.test", name: "Invited", role: "foreman",
      assignedSites: ["SITE-A"], invitedBy: "super@ktc.test", invitedAt: "2026-09-08",
    });
  });
  const db = as("uid-invited", "invited@ktc.test");
  await assertSucceeds(getDoc(doc(db, "userInvites/invited@ktc.test")));
  await assertSucceeds(
    setDoc(doc(db, "users/uid-invited"), {
      name: "Invited", email: "invited@ktc.test", role: "foreman",
      assignedSites: ["SITE-A"], status: "active",
    }),
  );
});

test("an invited account cannot claim MORE than it was invited to", async () => {
  await seed(async (db) => {
    await setDoc(doc(db, "userInvites/greedy@ktc.test"), {
      email: "greedy@ktc.test", name: "Greedy", role: "foreman",
      assignedSites: ["SITE-A"], invitedBy: "super@ktc.test", invitedAt: "2026-09-08",
    });
  });
  const db = as("uid-greedy", "greedy@ktc.test");
  // A better role than the invite names.
  await assertFails(
    setDoc(doc(db, "users/uid-greedy"), {
      name: "Greedy", email: "greedy@ktc.test", role: "super_admin",
      assignedSites: ["SITE-A"], status: "active",
    }),
  );
  // More sites than the invite names.
  await assertFails(
    setDoc(doc(db, "users/uid-greedy"), {
      name: "Greedy", email: "greedy@ktc.test", role: "foreman",
      assignedSites: ["SITE-A", "SITE-B"], status: "active",
    }),
  );
  // Every site.
  await assertFails(
    setDoc(doc(db, "users/uid-greedy"), {
      name: "Greedy", email: "greedy@ktc.test", role: "foreman",
      assignedSites: ["ALL"], status: "active",
    }),
  );
});

test("an account with no invite cannot create a profile for itself at all", async () => {
  const db = as("uid-nobody", "nobody@ktc.test");
  await assertFails(
    setDoc(doc(db, "users/uid-nobody"), {
      name: "Nobody", email: "nobody@ktc.test", role: "foreman",
      assignedSites: ["SITE-A"], status: "active",
    }),
  );
});

test("an invite cannot be used to write someone else's profile", async () => {
  await seed(async (db) => {
    await setDoc(doc(db, "userInvites/imposter@ktc.test"), {
      email: "imposter@ktc.test", name: "Imposter", role: "foreman",
      assignedSites: ["SITE-A"], invitedBy: "super@ktc.test", invitedAt: "2026-09-08",
    });
  });
  const db = as("uid-imposter", "imposter@ktc.test");
  await assertFails(
    setDoc(doc(db, "users/uid-foreman-a"), {
      name: "Imposter", email: "imposter@ktc.test", role: "foreman",
      assignedSites: ["SITE-A"], status: "active",
    }),
  );
});

test("only super admin creates invites", async () => {
  const foreman = as("uid-foreman-a", "fa@ktc.test");
  await assertFails(
    setDoc(doc(foreman, "userInvites/fa@ktc.test"), {
      email: "fa@ktc.test", role: "super_admin", assignedSites: ["ALL"],
    }),
  );
  await assertSucceeds(
    setDoc(doc(as("uid-super", "super@ktc.test"), "userInvites/new@ktc.test"), {
      email: "new@ktc.test", name: "New", role: "timekeeper",
      assignedSites: ["SITE-B"], invitedBy: "super@ktc.test", invitedAt: "2026-09-08",
    }),
  );
});

test("a user cannot read another person's invite", async () => {
  await seed(async (db) => {
    await setDoc(doc(db, "userInvites/private@ktc.test"), {
      email: "private@ktc.test", role: "admin", assignedSites: ["SITE-A"],
    });
  });
  await assertFails(getDoc(doc(as("uid-foreman-a", "fa@ktc.test"), "userInvites/private@ktc.test")));
});

// ---------------------------------------------------------------------------------------------
// Rules are not filters. A query that isn't constrained to what the caller may read is REJECTED
// outright — it does not come back filtered. This is the behaviour the app's queries must match.
// ---------------------------------------------------------------------------------------------

test("an unconstrained collection query is REJECTED for a site-scoped user", async () => {
  const db = as("uid-foreman-a", "fa@ktc.test");
  // What the app does today: subscribe to every worker and filter on the device.
  await assertFails(getDocs(collection(db, "workers")));
});

test("the same query constrained to the user's own site SUCCEEDS", async () => {
  const { query, where } = await import("firebase/firestore");
  const db = as("uid-foreman-a", "fa@ktc.test");
  await assertSucceeds(getDocs(query(collection(db, "workers"), where("site", "==", "SITE-A"))));
});

test("a constrained query for a site the user does NOT hold is still rejected", async () => {
  const { query, where } = await import("firebase/firestore");
  const db = as("uid-foreman-a", "fa@ktc.test");
  await assertFails(getDocs(query(collection(db, "workers"), where("site", "==", "SITE-B"))));
});

test("unconstrained today-attendance query is rejected; per-site is allowed", async () => {
  const { query, where } = await import("firebase/firestore");
  const db = as("uid-foreman-a", "fa@ktc.test");
  await assertFails(getDocs(query(collection(db, "attendance"), where("date", "==", "2026-09-08"))));
  await assertSucceeds(getDocs(query(
    collection(db, "attendance"),
    where("date", "==", "2026-09-08"),
    where("siteCode", "==", "SITE-A"),
  )));
});

// ---------------------------------------------------------------------------------------------
// Lockout protection. A users/{uid} document holding only an incidental field must not read as
// a grant of access — that demoted the administrator's own account and left nobody able to
// repair user access from inside the app.
// ---------------------------------------------------------------------------------------------

test("a profile holding only a timestamp does not demote the configured admin", async () => {
  await seed(async (db) => {
    // Exactly what the buggy last-login write produced.
    await setDoc(doc(db, "users/uid-cfg-admin"), { lastLoginAt: "2026-09-09T07:00:00Z" });
  });
  const db = as("uid-cfg-admin", "admin@ktc-manpower.com");
  await assertSucceeds(getDoc(doc(db, "workers/W-A")));
  await assertSucceeds(getDoc(doc(db, "workers/W-B")));
  await assertSucceeds(getDocs(collection(db, "users")));
  await assertSucceeds(setDoc(doc(db, "users/uid-someone"), {
    name: "Someone", email: "s@ktc.test", role: "foreman",
    assignedSites: ["SITE-A"], status: "active",
  }));
});

test("the configured admin cannot be locked out by a disabled or demoted profile", async () => {
  await seed(async (db) => {
    await setDoc(doc(db, "users/uid-cfg-admin2"), {
      name: "Admin", email: "admin@ktc-manpower.com",
      role: "staff", assignedSites: [], status: "disabled",
    });
  });
  const db = as("uid-cfg-admin2", "admin@ktc-manpower.com");
  await assertSucceeds(getDoc(doc(db, "workers/W-B")));
  await assertSucceeds(getDocs(collection(db, "users")));
});

test("a partial profile does not grant a NON-admin any access it lacked", async () => {
  await seed(async (db) => {
    await setDoc(doc(db, "users/uid-partial"), { lastLoginAt: "2026-09-09T07:00:00Z" });
  });
  const db = as("uid-partial", "partial@ktc.test");
  // Falls back to legacy access, which never included user management.
  await assertFails(getDocs(collection(db, "users")));
  await assertFails(setDoc(doc(db, "users/uid-partial"), { role: "super_admin" }));
});

// ---------------------------------------------------------------------------------------------
// Timekeepers keep their own site's roster current, but cannot remove people from it.
// ---------------------------------------------------------------------------------------------

test("a timekeeper uploads roster entries for their own site", async () => {
  const db = as("uid-tk-b", "tkb@ktc.test");
  await assertSucceeds(setDoc(doc(db, "workers/W-NEW-B"), {
    id: "W-NEW-B", name: "New Hire", designation: "Helper", site: "SITE-B",
  }));
});

test("a timekeeper cannot write a roster entry for another site", async () => {
  const db = as("uid-tk-b", "tkb@ktc.test");
  await assertFails(setDoc(doc(db, "workers/W-NEW-A"), {
    id: "W-NEW-A", name: "Wrong Site", designation: "Helper", site: "SITE-A",
  }));
});

test("a timekeeper cannot delete a worker", async () => {
  const { deleteDoc } = await import("firebase/firestore");
  await assertFails(deleteDoc(doc(as("uid-tk-b", "tkb@ktc.test"), "workers/W-B")));
});

test("a foreman still cannot upload roster entries at all", async () => {
  const db = as("uid-foreman-a", "fa@ktc.test");
  await assertFails(setDoc(doc(db, "workers/W-FOREMAN-TRY"), {
    id: "W-FOREMAN-TRY", name: "Nope", designation: "Helper", site: "SITE-A",
  }));
});

test.after(async () => {
  await testEnv.cleanup();
});
