// Small shared UI helpers used by every feature view — toasts, confirm/modal scaffolding, escaping.
export function toast(message, kind = "info") {
  const host = document.getElementById("toastHost");
  const el = document.createElement("div");
  el.className = `toast ${kind === "error" ? "error" : kind === "success" ? "success" : ""}`;
  el.textContent = message;
  host.appendChild(el);
  setTimeout(() => el.remove(), 4200);
}

export function esc(s) {
  return String(s ?? "").replace(/[&<>"']/g, (c) => ({
    "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;",
  }[c]));
}

export function openModal(innerHtml, onMount) {
  const backdrop = document.createElement("div");
  backdrop.className = "modal-backdrop";
  backdrop.innerHTML = `<div class="modal">${innerHtml}</div>`;
  document.body.appendChild(backdrop);
  backdrop.addEventListener("click", (e) => { if (e.target === backdrop) backdrop.remove(); });
  const close = () => backdrop.remove();
  if (onMount) onMount(backdrop.querySelector(".modal"), close);
  return { el: backdrop.querySelector(".modal"), close };
}

export function confirmDialog(message, onConfirm) {
  openModal(
    `<h3>Please confirm</h3><p>${esc(message)}</p>
     <div class="modal-actions">
       <button class="btn btn-outline" data-act="cancel">Cancel</button>
       <button class="btn btn-red" data-act="ok">Confirm</button>
     </div>`,
    (modal, close) => {
      modal.querySelector('[data-act="cancel"]').onclick = close;
      modal.querySelector('[data-act="ok"]').onclick = () => { close(); onConfirm(); };
    }
  );
}

export function downloadBlob(blob, filename) {
  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url; a.download = filename;
  document.body.appendChild(a); a.click(); a.remove();
  setTimeout(() => URL.revokeObjectURL(url), 4000);
}
