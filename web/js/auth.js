import { auth, onAuthStateChanged, signInWithEmailAndPassword, sendPasswordResetEmail, signOut } from "./firebase-init.js";
import { ADMIN_EMAILS, OFFICE_STAFF_EMAIL_DOMAIN } from "./constants.js";

// Mirrors app/src/main/java/com/ktc/sitepulse/data/repo/AuthRepository.kt's SessionState.
export function sessionFromUser(user) {
  const email = (user?.email || "").toLowerCase();
  const isAdmin = ADMIN_EMAILS.has(email);
  const isOfficeStaff = !isAdmin && email.endsWith(`@${OFFICE_STAFF_EMAIL_DOMAIN}`);
  return {
    uid: user?.uid || null,
    email,
    isLoggedIn: !!user,
    isAdmin,
    isOfficeStaff,
    isSupervisor: !!user && !isAdmin && !isOfficeStaff,
  };
}

export function watchSession(callback) {
  return onAuthStateChanged(auth, (user) => callback(sessionFromUser(user)));
}

export async function login(email, password) {
  await signInWithEmailAndPassword(auth, email.trim(), password);
}

export async function sendPasswordReset(email) {
  await sendPasswordResetEmail(auth, email.trim());
}

export function logout() {
  return signOut(auth);
}

export function currentEmail() {
  return (auth.currentUser?.email || "").toLowerCase();
}
