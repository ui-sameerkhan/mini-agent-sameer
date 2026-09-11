// Exercises loadLib against a fake DOM: CDN fallback order, the "200 but defines nothing"
// case a captive portal produces, and whether a failure is cached (it must not be).
import { readFileSync } from 'node:fs';

const scripts = [];
let behaviour = {}, want = null;
global.window = {};
global.URL = URL;
global.document = {
  head: { appendChild(s){ scripts.push(s); queueMicrotask(() => {
    const b = behaviour[s.src];
    if(b === 'ok'){ window[want] = {}; s.onload(); }
    else if(b === 'empty'){ s.onload(); }   // 200, but the global never appears
    else s.onerror();                        // unreachable host
  }); } },
  createElement(){ return {}; },
};

const code = readFileSync('./loadlib.mjs', 'utf8');
const mod = await import('data:text/javascript,' +
  encodeURIComponent(code + '\nexport { loadLib };'));
const { loadLib } = mod;

let pass = 0, fail = 0;
const check = (name, cond) => { cond ? (pass++, console.log('  ok   ' + name))
                                     : (fail++, console.log('  FAIL ' + name)); };

want = 'LibA';
behaviour = { 'https://a.test/x.js': 'down', 'https://b.test/x.js': 'ok' };
await loadLib('LibA', ['https://a.test/x.js', 'https://b.test/x.js'], 'A');
check('falls through to the second CDN when the first is unreachable', !!window.LibA);

want = 'LibB';
behaviour = { 'https://a.test/y.js': 'empty', 'https://b.test/y.js': 'ok' };
await loadLib('LibB', ['https://a.test/y.js', 'https://b.test/y.js'], 'B');
check('treats a 200 that defines nothing as a failure and falls back', !!window.LibB);

want = 'LibC';
behaviour = { 'https://a.test/z.js': 'down', 'https://b.test/z.js': 'down' };
let threw = null;
try { await loadLib('LibC', ['https://a.test/z.js', 'https://b.test/z.js'], 'C'); }
catch(e){ threw = e; }
check('rejects when every CDN is down', !!threw);
check('the error names the hosts IT would need to allow', /jsdelivr|unpkg|cdnjs/.test(threw.message));

behaviour = { 'https://a.test/z.js': 'ok' };
await loadLib('LibC', ['https://a.test/z.js', 'https://b.test/z.js'], 'C');
check('a retry after total failure really retries', !!window.LibC);

want = 'LibD';
window.LibD = {};
const before = scripts.length;
await loadLib('LibD', ['https://a.test/w.js'], 'D');
check('an already-loaded library fetches nothing', scripts.length === before);

console.log(`\n${pass} passed, ${fail} failed`);
process.exit(fail ? 1 : 0);
