const { test } = require("node:test");
const assert = require("node:assert/strict");
const { readFileSync } = require("node:fs");
const { join } = require("node:path");
const D = require("./data.js");
const html = readFileSync(join(__dirname, "index.html"), "utf8");
const coreSource = html.match(
  /<script id="unify-core">([\s\S]*?)<\/script>/,
)?.[1];
assert.ok(coreSource, "The HTML must contain the preview logic");
const coreModule = { exports: {} };
new Function("module", coreSource)(coreModule);
const C = coreModule.exports;

test("sample left joins preserve holdings and explicit missing-reference lineage", () => {
  const r = C.preview(D.sql, 0, D);
  assert.equal(r.ok, true);
  assert.equal(r.input, 12);
  assert.equal(r.rows.length, 12);
  assert.equal(r.unmatched, 2);
  assert.deepEqual(
    r.columns.map((c) => c.name),
    [
      "portfolio",
      "isin",
      "instrument",
      "assetClass",
      "currency",
      "units",
      "marketValue",
    ],
  );
  const missing = r.rows.filter((row) => row.instrument === null);
  assert.equal(missing.length, 2);
  assert.ok(
    missing.every(
      (row) => row._lineage.Instruments === null && row._lineage.Holdings > 0,
    ),
  );
  const holding = D.holdings.find((h) => h.currency === "GBP");
  const row = r.rows.find((x) => x.isin === holding.isin);
  assert.equal(row.marketValue, holding.units * holding.price * 1.26);
});

test("parameter and inner-join changes produce different observable results", () => {
  assert.equal(C.preview(D.sql, 100, D).rows.length, 10);
  const inner = C.preview(
    D.sql.replace("LEFT JOIN Instruments", "INNER JOIN Instruments"),
    0,
    D,
  );
  assert.equal(inner.rows.length, 10);
  assert.equal(inner.unmatched, 0);
  assert.ok(inner.rows.every((row) => row.instrument !== null));
  assert.equal(
    inner.columns.find((c) => c.name === "instrument").nullable,
    false,
  );
  assert.equal(C.preview(D.sql, -1, D).ok, false);
  assert.equal(C.preview(D.sql, 1.5, D).ok, false);
});

test("source-only queries report their own input and lineage without phantom joins", () => {
  for (const [sql, count, lineage] of [
    ["SELECT * FROM Instruments AS i;", 10, "Instruments"],
    ["SELECT * FROM FxRates AS fx;", 6, "FxRates"],
    ["SELECT * FROM Holdings AS h;", 12, "Holdings"],
  ]) {
    const r = C.preview(sql, 0, D);
    assert.equal(r.ok, true);
    assert.equal(r.input, count);
    assert.equal(r.rows.length, count);
    assert.equal(r.unmatched, 0);
    assert.deepEqual(Object.keys(r.rows[0]._lineage), [lineage]);
    assert.ok(Object.values(r.rows[0]).some((value) => value !== null));
  }
  assert.equal(
    C.preview("SELECT name FROM Instruments;", 0, D).rows[0].name,
    D.instruments[0].name,
  );
});

test("query failures are explicit rather than silently producing plausible data", () => {
  const invalid = [
    "SELECT * FROM MissingData;",
    "SELECT h.unknown FROM Holdings AS h;",
    "SELECT h.isin, h.isin FROM Holdings AS h;",
    "SELECT h.isin FROM Holdings AS h ORDER BY missing;",
    "SELECT h.isin FROM Holdings AS h WHERE h.units >= ? AND h.units < 100;",
    "SELECT i.name FROM Holdings AS h;",
    "SELECT * FROM Holdings AS h RIGHT JOIN Instruments AS i ON h.isin=i.isin;",
    "SELECT * FROM Holdings AS h LEFT JOIN Instruments AS i ON h.currency=i.isin;",
    "SELECT * FROM Holdings AS other;",
    "SELECT COUNT(*) FROM Holdings;",
    "SELECT * FROM Holdings; DROP TABLE Holdings;",
    "DELETE FROM Holdings;",
  ];
  for (const sql of invalid) {
    const r = C.preview(sql, 0, D);
    assert.equal(r.ok, false, sql);
    assert.ok(r.issues[0].message.length > 10);
  }
});

test("selected-column sorting, limits, and fixture immutability", () => {
  const before = JSON.stringify(D);
  const r = C.preview(
    "SELECT h.isin, h.units FROM Holdings AS h ORDER BY units DESC LIMIT 3;",
    0,
    D,
  );
  assert.equal(r.ok, true);
  assert.deepEqual(
    r.rows.map((row) => row.units),
    [2100, 1500, 1250],
  );
  assert.equal(JSON.stringify(D), before);
  assert.equal(C.compare(null, 0), -1);
  assert.equal(C.compare(2, 10), -8);
});

test("CSV distinguishes null from literal null-token text and escapes separators", () => {
  const result = {
    columns: [{ name: "value" }],
    rows: [{ value: null }, { value: "\\N" }, { value: 'a,"b"\nc' }],
  };
  assert.equal(C.csv(result), '"value"\r\n\\N\r\n"\\N"\r\n"a,""b""\nc"');
});

test("HTML output is escaped, expression lists respect nesting, stage names are unique", () => {
  assert.equal(
    C.escapeHtml('<img src=x onerror="x">'),
    "&lt;img src=x onerror=&quot;x&quot;&gt;",
  );
  assert.deepEqual(C.splitExpressions("a, COALESCE(b, 'c,d'), 'it''s,e'"), [
    "a",
    "COALESCE(b, 'c,d')",
    "'it''s,e'",
  ]);
  assert.equal(
    C.validateStageName("  ", D.stages, "query"),
    "Give the stage a name.",
  );
  assert.equal(
    C.validateStageName("Join instruments", D.stages, "query"),
    "A stage already uses that name.",
  );
  assert.equal(C.validateStageName("Join instruments", D.stages, "join"), "");
});

test("the optional local server serves only prototype assets and read-only requests", async (t) => {
  const { createPreviewServer } = await import("./serve.mjs");
  const server = createPreviewServer();
  await new Promise((resolve) => server.listen(0, "127.0.0.1", resolve));
  t.after(() => new Promise((resolve) => server.close(resolve)));
  const origin = `http://127.0.0.1:${server.address().port}`;
  const page = await fetch(origin);
  assert.equal(page.status, 200);
  assert.match(await page.text(), /Unify — Data workspace/);
  const script = await fetch(origin + "/data.js");
  assert.equal(script.status, 200);
  assert.match(script.headers.get("content-type"), /javascript/);
  for (const path of [
    "/package.json",
    "/app.js",
    "/core.js",
    "/styles.css",
    "/node_modules/jsdom/package.json",
    "/%2e%2e%5cTECHNICAL_DESIGN.md",
    "/%ZZ",
  ])
    assert.equal((await fetch(origin + path)).status, 404);
  assert.equal((await fetch(origin, { method: "POST" })).status, 405);
});
