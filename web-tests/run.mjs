// Pulls loadLib out of the shipped index.html so the test cannot drift from the real code.
import { readFileSync, writeFileSync, mkdtempSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join, dirname } from 'node:path';
import { fileURLToPath } from 'node:url';

const here = dirname(fileURLToPath(import.meta.url));
const html = readFileSync(join(here, '..', 'web', 'index.html'), 'utf8');
const m = html.match(/const _libPromises[\s\S]*?\n}\n/);
if (!m) { console.error('loadLib not found in web/index.html'); process.exit(1); }

const dir = mkdtempSync(join(tmpdir(), 'sitepulse-webtest-'));
writeFileSync(join(dir, 'loadlib.mjs'), m[0]);
writeFileSync(join(dir, 'test.mjs'), readFileSync(join(here, 'loadlib.test.mjs'), 'utf8'));
process.chdir(dir);
await import(join(dir, 'test.mjs'));
