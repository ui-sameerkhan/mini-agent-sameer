// Runs the web-app test files. loadlib.test.mjs pulls loadLib straight out of the shipped
// index.html so it cannot drift from the real code; lib-urls.test.mjs checks every library
// source the app can load actually resolves.
import { readFileSync, writeFileSync, mkdtempSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join, dirname } from 'node:path';
import { fileURLToPath } from 'node:url';

const here = dirname(fileURLToPath(import.meta.url));
const html = readFileSync(join(here, '..', 'web', 'index.html'), 'utf8');
const m = html.match(/const _libPromises[\s\S]*?\n}\n/);
if (!m) { console.error('loadLib not found in web/index.html'); process.exit(1); }

console.log('loadLib behaviour');
const dir = mkdtempSync(join(tmpdir(), 'sitepulse-webtest-'));
writeFileSync(join(dir, 'loadlib.mjs'), m[0]);
writeFileSync(join(dir, 'test.mjs'), readFileSync(join(here, 'loadlib.test.mjs'), 'utf8'));
const cwd = process.cwd();
process.chdir(dir);
await import(join(dir, 'test.mjs'));
process.chdir(cwd);

console.log('\nlibrary URLs');
await import(join(here, 'lib-urls.test.mjs'));
