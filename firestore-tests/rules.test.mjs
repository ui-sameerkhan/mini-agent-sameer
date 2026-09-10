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
import { doc, getDoc, setDoc, deleteDoc, collection, getDocs } from "firebase/firestore";

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
  // Not yet allocated to any project — the shape most of an untagged roster has.
  await setDoc(doc(db, "workers/W-FREE"), { id: "W-FREE", name: "Unallocated", designation: "Helper", site: null });

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

test("the roster is shared, so a transferred worker can be found and marked", async () => {
  // Workers move between projects. If the receiving site could not read the worker, the
  // transfer could not be recorded at all — see the note on /workers in firestore.rules.
  const db = as("uid-foreman-a", "fa@ktc.test");
  await assertSucceeds(getDoc(doc(db, "workers/W-A")));
  await assertSucceeds(getDoc(doc(db, "workers/W-B")));
});

test("a worker rostered elsewhere can be marked at the site they turned up to", async () => {
  const db = as("uid-foreman-a", "fa@ktc.test");
  // W-B is rostered to SITE-B; the foreman marks them at SITE-A, where they actually are.
  await assertSucceeds(setDoc(doc(db, "attendance/2026-09-12_W-B"), {
    workerId: "W-B", date: "2026-09-12", siteCode: "SITE-A",
    alignedSite: "SITE-B", siteMismatch: true,
  }));
  // ...but still not at a site they do not hold.
  await assertFails(setDoc(doc(db, "attendance/2026-09-13_W-B"), {
    workerId: "W-B", date: "2026-09-13", siteCode: "SITE-B",
  }));
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

test("the full roster may be listed, since it is shared", async () => {
  const db = as("uid-foreman-a", "fa@ktc.test");
  await assertSucceeds(getDocs(collection(db, "workers")));
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

// ---------------------------------------------------------------------------------------------
// A worker allocated to no project belongs to nobody. Hiding them left site-scoped users looking
// at an empty roster, unable to mark anyone — while another project's crew must stay hidden.
// ---------------------------------------------------------------------------------------------

test("a worker allocated to no project is still reachable", async () => {
  // An untagged roster must not leave a timekeeper unable to mark anyone.
  const db = as("uid-foreman-a", "fa@ktc.test");
  await assertSucceeds(getDoc(doc(db, "workers/W-FREE")));
});

test("sharing the roster does NOT share the attendance behind it", async () => {
  // The payroll-bearing record stays scoped even though the roster row is visible.
  const db = as("uid-foreman-a", "fa@ktc.test");
  await assertSucceeds(getDoc(doc(db, "workers/W-B")));
  await assertFails(getDoc(doc(db, "attendance/2026-09-08_W-B")));
});

// ---------------------------------------------------------------------------------------------
// transferRequests — moving a worker from one site's roster to another's.
//
// The whole point of this collection is that head office is NOT in the loop, so the rules are
// the only thing standing between "the foreman at the gate can fix the roster" and "any foreman
// can move any worker onto any project". Each test below pins one half of that.
// ---------------------------------------------------------------------------------------------

await seed(async (db) => {
  // A pending transfer pulling W-B (rostered SITE-B) towards SITE-A, and one the other way.
  await setDoc(doc(db, "transferRequests/T-INTO-A"), {
    workerId: "W-B", workerName: "Worker B", designation: "Mason",
    fromSite: "SITE-B", toSite: "SITE-A", requestedBy: "fa@ktc.test", status: "pending",
  });
  await setDoc(doc(db, "transferRequests/T-INTO-B"), {
    workerId: "W-A", workerName: "Worker A", designation: "Carpenter",
    fromSite: "SITE-A", toSite: "SITE-B", requestedBy: "tkb@ktc.test", status: "pending",
  });
});

test("a foreman raises a transfer INTO their own site", async () => {
  const db = as("uid-foreman-a", "fa@ktc.test");
  await assertSucceeds(setDoc(doc(db, "transferRequests/T-NEW-A"), {
    workerId: "W-B", workerName: "Worker B", fromSite: "SITE-B", toSite: "SITE-A",
    requestedBy: "fa@ktc.test", status: "pending",
  }));
});

test("a foreman cannot push a worker ONTO a site they do not hold", async () => {
  // The dangerous direction: moving someone else's crew, or dumping a worker on another project.
  const db = as("uid-foreman-a", "fa@ktc.test");
  await assertFails(setDoc(doc(db, "transferRequests/T-PUSH"), {
    workerId: "W-A", workerName: "Worker A", fromSite: "SITE-A", toSite: "SITE-B",
    requestedBy: "fa@ktc.test", status: "pending",
  }));
});

test("a transfer cannot be born already approved", async () => {
  // Otherwise "raise a request" would be a self-approval with extra steps.
  const db = as("uid-foreman-a", "fa@ktc.test");
  await assertFails(setDoc(doc(db, "transferRequests/T-PREAPPROVED"), {
    workerId: "W-B", workerName: "Worker B", fromSite: "SITE-B", toSite: "SITE-A",
    requestedBy: "fa@ktc.test", status: "approved",
  }));
});

test("a foreman cannot approve the transfer they raised", async () => {
  // Approving rewrites the roster, so it takes roster authority — which a foreman has not got.
  const db = as("uid-foreman-a", "fa@ktc.test");
  await assertFails(setDoc(doc(db, "transferRequests/T-INTO-A"), {
    status: "approved", approvedBy: "fa@ktc.test",
  }, { merge: true }));
});

test("the receiving site's timekeeper approves a transfer into their site", async () => {
  const db = as("uid-tk-b", "tkb@ktc.test");
  await assertSucceeds(setDoc(doc(db, "transferRequests/T-INTO-B"), {
    status: "approved", approvedBy: "tkb@ktc.test",
  }, { merge: true }));
});

test("a timekeeper cannot decide a transfer into someone else's site", async () => {
  const db = as("uid-tk-b", "tkb@ktc.test");
  await assertFails(setDoc(doc(db, "transferRequests/T-INTO-A"), {
    status: "rejected", rejectedBy: "tkb@ktc.test",
  }, { merge: true }));
});

test("approving actually lets the timekeeper move the roster row across", async () => {
  // The approval and the roster write are two operations; both have to be permitted or the
  // request would approve while the worker stayed on the site they left.
  const db = as("uid-tk-b", "tkb@ktc.test");
  await assertSucceeds(setDoc(doc(db, "workers/W-A"), {
    id: "W-A", name: "Worker A", designation: "Carpenter", site: "SITE-B",
  }));
  // ...but not the reverse: they cannot push a worker onto SITE-A's roster.
  await assertFails(setDoc(doc(db, "workers/W-B"), {
    id: "W-B", name: "Worker B", designation: "Mason", site: "SITE-A",
  }));
});

test("staff cannot raise transfers at all", async () => {
  const db = as("uid-staff", "staff@ktc.test");
  await assertFails(setDoc(doc(db, "transferRequests/T-STAFF"), {
    workerId: "W-A", workerName: "Worker A", fromSite: "SITE-B", toSite: "SITE-A",
    requestedBy: "staff@ktc.test", status: "pending",
  }));
});

test("a disabled account cannot raise or decide transfers", async () => {
  const db = as("uid-disabled", "gone@ktc.test");
  await assertFails(setDoc(doc(db, "transferRequests/T-DISABLED"), {
    workerId: "W-B", workerName: "Worker B", fromSite: "SITE-B", toSite: "SITE-A",
    requestedBy: "gone@ktc.test", status: "pending",
  }));
  await assertFails(setDoc(doc(db, "transferRequests/T-INTO-A"), { status: "approved" }, { merge: true }));
});

test("both ends of a move can see it, but nobody can list the lot", async () => {
  // Rules are not filters: an unconstrained list from a scoped account must be refused outright,
  // not quietly trimmed. Each app query is a single equality so every document it returns passes.
  const { query, where } = await import("firebase/firestore");
  const db = as("uid-foreman-a", "fa@ktc.test");

  await assertFails(getDocs(collection(db, "transferRequests")));
  // Incoming — the queue SITE-A acts on.
  await assertSucceeds(getDocs(query(collection(db, "transferRequests"), where("toSite", "==", "SITE-A"))));
  // Outgoing — SITE-A losing someone; visible, because a headcount change should not be a surprise.
  await assertSucceeds(getDocs(query(collection(db, "transferRequests"), where("fromSite", "==", "SITE-A"))));
  // A move between two other sites stays out of reach.
  await assertFails(getDocs(query(collection(db, "transferRequests"), where("toSite", "==", "SITE-B"))));
});

// ---------------------------------------------------------------------------------------------
// Retired roles.
//
// "foreman" was merged into supervisor — identical permissions, and the same job at KTC. The
// merge removed it from User Management, but profiles written before it still say "foreman".
// If the id ever stops resolving, every one of those accounts silently loses the ability to mark
// attendance, with nothing in the app to explain why. These tests exist to make that impossible.
// ---------------------------------------------------------------------------------------------

await seed(async (db) => {
  await setDoc(doc(db, "users/uid-old-foreman"), {
    name: "Old Foreman", email: "of@ktc.test", role: "foreman",
    assignedSites: ["SITE-A"], status: "active",
  });
});

test("a profile still holding the retired 'foreman' role keeps marking attendance", async () => {
  const db = as("uid-old-foreman", "of@ktc.test");
  await assertSucceeds(setDoc(doc(db, "attendance/2026-09-20_W-A"), {
    workerId: "W-A", date: "2026-09-20", siteCode: "SITE-A",
  }));
});

test("a retired 'foreman' profile stays bound to its own sites", async () => {
  // Merging the role must not widen it either.
  const db = as("uid-old-foreman", "of@ktc.test");
  await assertFails(setDoc(doc(db, "attendance/2026-09-20_W-B"), {
    workerId: "W-B", date: "2026-09-20", siteCode: "SITE-B",
  }));
});

test("a retired 'foreman' profile can raise a transfer but still cannot approve one", async () => {
  const db = as("uid-old-foreman", "of@ktc.test");
  await assertSucceeds(setDoc(doc(db, "transferRequests/T-OLD-FOREMAN"), {
    workerId: "W-B", workerName: "Worker B", fromSite: "SITE-B", toSite: "SITE-A",
    requestedBy: "of@ktc.test", status: "pending",
  }));
  await assertFails(setDoc(doc(db, "transferRequests/T-OLD-FOREMAN"), {
    status: "approved", approvedBy: "of@ktc.test",
  }, { merge: true }));
});

test("a retired 'foreman' profile gains no roster rights from the merge", async () => {
  const db = as("uid-old-foreman", "of@ktc.test");
  await assertFails(setDoc(doc(db, "workers/W-FOREMAN-MERGE"), {
    id: "W-FOREMAN-MERGE", name: "Nope", designation: "Helper", site: "SITE-A",
  }));
});

// ---------------------------------------------------------------------------------------------
// biometricChecks — the record that the ERP cross-check was run.
//
// The value of this collection is entirely in it being trustworthy: it exists so somebody can be
// shown that verification happens daily. An entry naming the wrong person, or one a supervisor
// could quietly fabricate, would make it worse than having nothing.
// ---------------------------------------------------------------------------------------------

test("a timekeeper records their own run", async () => {
  const db = as("uid-tk-b", "tkb@ktc.test");
  await assertSucceeds(setDoc(doc(db, "biometricChecks/2026-09-20__SITE-B"), {
    date: "2026-09-20", scope: "SITE-B", runBy: "tkb@ktc.test",
    runAt: "2026-09-20T05:00:00Z", agreed: 40, markedNotPunched: 2,
  }));
});

test("nobody can log a run in somebody else's name", async () => {
  // Otherwise the audit trail could be used to blame a colleague for a check they never ran.
  const db = as("uid-tk-b", "tkb@ktc.test");
  await assertFails(setDoc(doc(db, "biometricChecks/2026-09-21__SITE-B"), {
    date: "2026-09-21", scope: "SITE-B", runBy: "super@ktc.test",
    runAt: "2026-09-21T05:00:00Z", agreed: 40,
  }));
});

test("a supervisor cannot fabricate a clean check", async () => {
  // A supervisor can mark attendance but not verify it; letting them write here would let the
  // person being checked write the record saying they were checked.
  const db = as("uid-foreman-a", "fa@ktc.test");
  await assertFails(setDoc(doc(db, "biometricChecks/2026-09-22__SITE-A"), {
    date: "2026-09-22", scope: "SITE-A", runBy: "fa@ktc.test",
    runAt: "2026-09-22T05:00:00Z", agreed: 99, markedNotPunched: 0,
  }));
});

test("re-running a day overwrites its entry rather than being refused", async () => {
  const db = as("uid-tk-b", "tkb@ktc.test");
  await assertSucceeds(setDoc(doc(db, "biometricChecks/2026-09-20__SITE-B"), {
    date: "2026-09-20", scope: "SITE-B", runBy: "tkb@ktc.test",
    runAt: "2026-09-20T09:00:00Z", agreed: 41, markedNotPunched: 1,
  }));
});

test("an entry with no date is refused", async () => {
  const db = as("uid-tk-b", "tkb@ktc.test");
  await assertFails(setDoc(doc(db, "biometricChecks/junk"), {
    scope: "SITE-B", runBy: "tkb@ktc.test", runAt: "2026-09-20T05:00:00Z",
  }));
});

test("anyone active can see that the check ran, since it holds only counts", async () => {
  // The deterrent only works if the people being checked can see that checking happens.
  const db = as("uid-foreman-a", "fa@ktc.test");
  await assertSucceeds(getDoc(doc(db, "biometricChecks/2026-09-20__SITE-B")));
});

test("a run cannot be deleted by whoever ran it", async () => {
  const tk = as("uid-tk-b", "tkb@ktc.test");
  await assertFails(deleteDoc(doc(tk, "biometricChecks/2026-09-20__SITE-B")));
  const su = as("uid-super", "super@ktc.test");
  await assertSucceeds(deleteDoc(doc(su, "biometricChecks/2026-09-20__SITE-B")));
});

test("a disabled account cannot record a run", async () => {
  const db = as("uid-disabled", "gone@ktc.test");
  await assertFails(setDoc(doc(db, "biometricChecks/2026-09-23__SITE-A"), {
    date: "2026-09-23", scope: "SITE-A", runBy: "gone@ktc.test", runAt: "2026-09-23T05:00:00Z",
  }));
});

test.after(async () => {
  await testEnv.cleanup();
});
