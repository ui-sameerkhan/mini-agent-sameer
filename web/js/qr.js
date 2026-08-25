// QR scan (camera + jsQR) and generation (qrcode.js + jsPDF for bulk badges) — entirely local,
// no Firebase calls, mirrors the Android app's QrCodeUtil.kt / CameraX+ZXing scan flow.

/** Opens the device camera in a modal and resolves with the decoded string, or null if cancelled. */
export function scanQr() {
  return new Promise((resolve) => {
    const backdrop = document.createElement("div");
    backdrop.className = "modal-backdrop";
    backdrop.innerHTML = `
      <div class="modal">
        <h3>📷 Scan QR / Barcode</h3>
        <div class="qr-video-wrap"><video id="qrVideo" playsinline muted></video></div>
        <canvas id="qrCanvas" class="hidden"></canvas>
        <p class="muted" style="margin-top:8px">Point the camera at an employee ID badge.</p>
        <div class="modal-actions"><button class="btn btn-outline" id="qrCancel">Cancel</button></div>
      </div>`;
    document.body.appendChild(backdrop);

    const video = backdrop.querySelector("#qrVideo");
    const canvas = backdrop.querySelector("#qrCanvas");
    const ctx = canvas.getContext("2d");
    let stream = null, raf = null, done = false;

    function stop(result) {
      if (done) return;
      done = true;
      if (raf) cancelAnimationFrame(raf);
      if (stream) stream.getTracks().forEach((t) => t.stop());
      backdrop.remove();
      resolve(result);
    }

    backdrop.querySelector("#qrCancel").onclick = () => stop(null);
    backdrop.addEventListener("click", (e) => { if (e.target === backdrop) stop(null); });

    navigator.mediaDevices.getUserMedia({ video: { facingMode: "environment" } })
      .then((s) => {
        stream = s;
        video.srcObject = s;
        video.play();
        const tick = () => {
          if (done) return;
          if (video.readyState === video.HAVE_ENOUGH_DATA) {
            canvas.width = video.videoWidth;
            canvas.height = video.videoHeight;
            ctx.drawImage(video, 0, 0, canvas.width, canvas.height);
            const img = ctx.getImageData(0, 0, canvas.width, canvas.height);
            const code = jsQR(img.data, img.width, img.height);
            if (code?.data) { stop(code.data.trim()); return; }
          }
          raf = requestAnimationFrame(tick);
        };
        raf = requestAnimationFrame(tick);
      })
      .catch((e) => {
        alert(`Camera access failed: ${e.message}`);
        stop(null);
      });
  });
}

/** Returns a data: URL PNG of the QR code for the given content. */
export async function generateQrDataUrl(content, size = 300) {
  return QRCode.toDataURL(content, { width: size, margin: 1 });
}

/** Builds one printable PDF (3x4 grid per A4 page) of QR badges for the given workers. */
export async function generateBulkQrPdf(workers) {
  const { jsPDF } = window.jspdf;
  const pdf = new jsPDF({ unit: "pt", format: "a4" });
  const pageW = 595, pageH = 842, margin = 24, cols = 3, rows = 4;
  const cellW = (pageW - margin * 2) / cols, cellH = (pageH - margin * 2) / rows;
  const qrSize = Math.max(40, Math.min(cellW, cellH) - 46);
  const perPage = cols * rows;

  for (let i = 0; i < workers.length; i++) {
    const w = workers[i];
    const posInPage = i % perPage;
    if (posInPage === 0 && i > 0) pdf.addPage();
    const col = posInPage % cols, row = Math.floor(posInPage / cols);
    const cellLeft = margin + col * cellW, cellTop = margin + row * cellH;
    const qrLeft = cellLeft + (cellW - qrSize) / 2, qrTop = cellTop + 8;

    const dataUrl = await generateQrDataUrl(w.id, 300);
    pdf.addImage(dataUrl, "PNG", qrLeft, qrTop, qrSize, qrSize);
    pdf.setFontSize(10);
    pdf.text(String(w.name).slice(0, 24), cellLeft + cellW / 2, qrTop + qrSize + 15, { align: "center" });
    pdf.setFontSize(9);
    pdf.setTextColor(90, 90, 90);
    pdf.text(`ID ${w.id}`, cellLeft + cellW / 2, qrTop + qrSize + 29, { align: "center" });
    pdf.setTextColor(0, 0, 0);
  }
  return pdf.output("blob");
}
