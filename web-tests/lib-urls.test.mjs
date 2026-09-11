/**
 * Checks that every library URL the web app can load actually resolves.
 *
 * This exists because it did not. The QR library was pinned to
 * qrcode@1.5.3/build/qrcode.min.js — a path that version does not publish — so every CDN
 * answered 404 and QR badges could never be displayed, on any network. Adding "fallback" CDNs
 * made it worse: three URLs, all wrong, and an error message blaming the user's connection.
 *
 * Local vendor copies are checked on disk; CDN fallbacks are checked over the network, and are
 * skipped (not failed) when this machine has no route out, so the suite stays useful offline.
 *
 *     node web-tests/run.mjs
 */
import { readFileSync, existsSync, statSync } from 'node:fs';
import { join, dirname } from 'node:path';
import { fileURLToPath } from 'node:url';

const here = dirname(fileURLToPath(import.meta.url));
const webDir = join(here, '..', 'web');
const html = readFileSync(join(webDir, 'index.html'), 'utf8');

let pass = 0, fail = 0, skip = 0;
const ok = (m) => { pass++; console.log('  ok    ' + m); };
const bad = (m) => { fail++; console.log('  FAIL  ' + m); };
const skipped = (m) => { skip++; console.log('  skip  ' + m); };

// Every string passed to loadLib(), in order.
const urls = [...html.matchAll(/loadLib\(\s*'([^']+)'\s*,\s*\[([\s\S]*?)\]/g)].map(
  ([, global, list]) => ({
    global,
    urls: [...list.matchAll(/'([^']+)'/g)].map((m) => m[1]),
  })
);

if (!urls.length) bad('no loadLib() calls found — has the loader been renamed?');

for (const { global, urls: list } of urls) {
  if (!list.length) { bad(`${global}: no URLs at all`); continue; }
  if (!list[0].startsWith('./')) {
    bad(`${global}: first source is ${list[0]} — a local copy should come first`);
  }

  for (const url of list) {
    if (url.startsWith('./')) {
      const file = join(webDir, url.slice(2));
      if (!existsSync(file)) { bad(`${global}: ${url} is missing from the build`); continue; }
      const size = statSync(file).size;
      const head = readFileSync(file, 'utf8').slice(0, 200);
      if (size < 1000) bad(`${global}: ${url} is only ${size} bytes`);
      else if (/^\s*<(!doctype|html)/i.test(head)) bad(`${global}: ${url} is HTML, not JavaScript`);
      else ok(`${global}: ${url} present (${Math.round(size / 1024)}KB)`);
      continue;
    }

    try {
      const res = await fetch(url, { method: 'GET', redirect: 'follow' });
      if (res.ok) ok(`${global}: ${url} -> ${res.status}`);
      else bad(`${global}: ${url} -> ${res.status}`);
    } catch (e) {
      skipped(`${global}: ${url} (no network from here: ${e.message})`);
    }
  }
}

console.log(`\n${pass} passed, ${fail} failed, ${skip} skipped`);
if (fail) process.exit(1);
