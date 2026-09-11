# Web app tests

`loadlib.test.mjs` exercises the lazy library loader in `web/index.html` against a fake DOM —
CDN fallback, the "200 but defines nothing" case a captive portal produces, and whether a
failed load is cached (it must not be, or "try again" can never work).

It extracts `loadLib` straight out of `index.html`, so it tests the shipped code rather than a
copy that can drift.

    node web-tests/run.mjs
