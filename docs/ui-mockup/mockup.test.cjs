/*
© Mark Andrew Ray-Smith Cityline Ltd 2025
Version 1.3.3

Single automated-test companion. From the repository root:
  npm --prefix scripts/ui-mockup ci --ignore-scripts
  npm --prefix scripts/ui-mockup test
Test dependencies live outside this mockup folder.
*/
const { createRequire } = require("node:module");
const testToolingRequire = createRequire(
  require("node:path").resolve(
    __dirname,
    "../../scripts/ui-mockup/package.json",
  ),
);

// Core and optional server
(() => {
  const { test } = require("node:test");
  const assert = require("node:assert/strict");
  const { readFileSync } = require("node:fs");
  const { join } = require("node:path");
  const html = readFileSync(join(__dirname, "index.html"), "utf8");
  const D = require("./data.js");
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
    const { createPreviewServer } = await import(
      "../../scripts/ui-mockup/serve.mjs"
    );
    const server = createPreviewServer();
    await new Promise((resolve) => server.listen(0, "127.0.0.1", resolve));
    t.after(() => new Promise((resolve) => server.close(resolve)));
    const origin = `http://127.0.0.1:${server.address().port}`;
    const page = await fetch(origin);
    assert.equal(page.status, 200);
    assert.match(await page.text(), /Unify — Data workspace/);
    const dataResponse = await fetch(origin + "/data.js");
    assert.equal(dataResponse.status, 200);
    assert.match(dataResponse.headers.get("content-type"), /javascript/);
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
})();

// CSV parsing and mapping
(() => {
  const { test } = require("node:test");
  const assert = require("node:assert/strict");
  const { readFileSync } = require("node:fs");
  const { join } = require("node:path");
  const html = readFileSync(join(__dirname, "index.html"), "utf8");
  const source = html.match(/<script id="unify-csv">([\s\S]*?)<\/script>/)?.[1];
  assert.ok(source, "CSV parsing and mapping must be embedded in the HTML");
  const api = { exports: {} };
  new Function("module", source)(api);
  const CSV = api.exports;
  const defaults = {
    delimiter: ",",
    quote: '"',
    hasHeader: true,
    headerRecord: 1,
    firstRecord: 2,
    lastRecord: "",
    blankRecords: "error",
    matchBy: "name",
    nullToken: "",
  };
  const selection = (text, options = {}) =>
    CSV.select(CSV.parse(text, { ...defaults, ...options }), {
      ...defaults,
      ...options,
    });

  test("CSV parsing preserves quoted delimiters, doubled quotes, CRLF, newlines, and trailing fields", () => {
    const records = CSV.parse(
      'Name,Note,Empty\r\n"A, B","line 1\r\nline ""2""",\r\n',
    );
    assert.equal(records.length, 2);
    assert.deepEqual(
      records[1].cells.map((c) => c.value),
      ["A, B", 'line 1\r\nline "2"', ""],
    );
    assert.equal(records[1].ordinal, 2);
    assert.equal(CSV.parse("a,b\n\n").length, 2);
    assert.throws(() => CSV.parse('a\n"unfinished'), /Unterminated/);
    assert.throws(() => CSV.parse('a\n"closed"text'), /Unexpected text/);
  });

  test("header and record selection exclude explicit preambles and trailers", () => {
    const sample = "Daily export\nName;Amount\nAlpha;12\nBeta;15\nTrailer\n";
    const selected = selection(sample, {
      delimiter: ";",
      headerRecord: 2,
      firstRecord: 3,
      lastRecord: 4,
    });
    assert.deepEqual(
      selected.columns.map((c) => c.name),
      ["Name", "Amount"],
    );
    assert.deepEqual(
      selected.rows.map((r) => r.ordinal),
      [3, 4],
    );
    assert.throws(
      () =>
        selection(sample, { delimiter: ";", headerRecord: 2, firstRecord: 2 }),
      /after the header/,
    );
    assert.throws(() => selection("a,b\n1,2\n3"), /Expected 2 fields/);
    assert.throws(() => selection("a,a\n1,2"), /duplicate/);
    assert.throws(() => selection("a,\n1,2"), /empty column/);
  });

  test("headerless positional mappings preserve identifiers and follow output order", () => {
    const options = {
      ...defaults,
      hasHeader: false,
      firstRecord: 1,
      matchBy: "position",
    };
    const selected = selection("00123,7,ignored\n00456,8,ignored", options);
    const mappings = CSV.mappingsFor(selected.columns);
    mappings[0].outputName = "customerId";
    mappings[1].outputName = "quantity";
    mappings[1].type = "INT32";
    mappings[2].include = false;
    const result = CSV.preview(
      selected,
      [mappings[1], mappings[0], mappings[2]],
      options,
    );
    assert.equal(result.ok, true);
    assert.deepEqual(
      result.columns.map((c) => c.name),
      ["quantity", "customerId"],
    );
    assert.equal(result.rows[0].quantity, 7);
    assert.equal(result.rows[0].customerId, "00123");
  });

  test("mapped previews trim and convert only selected fields, with located errors", () => {
    const selected = selection(
      'Account,Units,Price\n" Fund ",12,3.5\nIncome,bad,4.0',
    );
    const mapping = CSV.mappingsFor(selected.columns);
    mapping[0].trim = true;
    mapping[0].outputName = "portfolio";
    mapping[1].type = "INT32";
    mapping[2].type = "DOUBLE";
    const result = CSV.preview(selected, mapping, {
      ...defaults,
      fileName: "positions.csv",
    });
    assert.equal(result.ok, false);
    assert.equal(result.rows.length, 1);
    assert.equal(result.invalidRecords, 1);
    assert.equal(result.rows[0].portfolio, "Fund");
    assert.equal(result.rows[0].Units, 12);
    assert.equal(result.rows[0].Price, 3.5);
    assert.deepEqual(result.rows[0]._lineage, { "positions.csv": 2 });
    assert.equal(result.issues[0].record, 3);
    assert.equal(result.issues[0].field, "Units");
    assert.throws(() => CSV.convert("2147483648", { type: "INT32" }), /range/);
    assert.throws(() => CSV.convert("Infinity", { type: "DOUBLE" }), /finite/);
  });

  test("null tokens, quoted token literals, empty text, and empty-to-null remain distinct", () => {
    const selected = selection('Value\n\\N\n"\\N"\n""');
    const mapping = CSV.mappingsFor(selected.columns);
    mapping[0].nullable = true;
    let result = CSV.preview(selected, mapping, {
      ...defaults,
      nullToken: "\\N",
    });
    assert.deepEqual(
      result.rows.map((r) => r.Value),
      [null, "\\N", ""],
    );
    mapping[0].emptyAsNull = true;
    result = CSV.preview(selected, mapping, { ...defaults, nullToken: "\\N" });
    assert.deepEqual(
      result.rows.map((r) => r.Value),
      [null, "\\N", null],
    );
    mapping[0].nullable = false;
    assert.equal(
      CSV.preview(selected, mapping, { ...defaults, nullToken: "\\N" }).ok,
      false,
    );
  });

  test("saved name-based mappings follow headers when source column order changes", () => {
    const original = selection("Identifier,Units\n001,5");
    const mappings = CSV.mappingsFor(original.columns);
    mappings[1].type = "INT32";
    const reordered = selection("Units,Identifier\n6,002");
    const result = CSV.preview(reordered, mappings, defaults);
    assert.equal(result.rows[0].Identifier, "002");
    assert.equal(result.rows[0].Units, 6);
    assert.throws(
      () => CSV.preview(selection("Other,Units\n002,6"), mappings, defaults),
      /Missing source column/,
    );
    mappings[1].outputName = "Identifier";
    assert.throws(
      () => CSV.preview(original, mappings, defaults),
      /Duplicate output/,
    );
  });

  test("blank records and sample limits fail explicitly instead of silently discarding data", () => {
    assert.throws(() => selection("Name\nAlpha\n\nBeta"), /Blank data record/);
    const selected = selection("Name\nAlpha\n\nBeta", { blankRecords: "skip" });
    assert.equal(selected.skippedBlank, 1);
    assert.equal(selected.rows.length, 2);
    assert.throws(() => CSV.parse("x".repeat(1048577)), /1 MiB/);
    assert.throws(() => CSV.parse("x\n".repeat(1001)), /1,000/);
  });

  test("date, time, and boolean conversions reject invalid representations", () => {
    assert.equal(CSV.convert("2024-02-29", { type: "DATE" }), "2024-02-29");
    assert.throws(() => CSV.convert("2025-02-29", { type: "DATE" }), /valid/);
    assert.equal(CSV.convert("23:59:59.12", { type: "TIME" }), "23:59:59.12");
    assert.throws(() => CSV.convert("24:00:00", { type: "TIME" }), /HH/);
    assert.equal(CSV.convert("false", { type: "BOOLEAN" }), false);
    assert.throws(
      () => CSV.convert("yes", { type: "BOOLEAN" }),
      /true or false/,
    );
  });
})();

// Configuration processing
(() => {
  const { test } = require("node:test");
  const assert = require("node:assert/strict");
  const { readFileSync } = require("node:fs");
  const { join } = require("node:path");
  const { JSDOM } = testToolingRequire("jsdom");
  const html = readFileSync(join(__dirname, "index.html"), "utf8");
  const root = { DOMParser: new JSDOM("").window.DOMParser };
  for (const id of ["unify-csv", "unify-config", "unify-config-query"]) {
    const source = html.match(
      new RegExp('<script id="' + id + '">([\\s\\S]*?)<\\/script>'),
    )[1];
    new Function("globalThis", source)(root);
  }
  const C = root.UnifyConfig;
  const columns = [
    { name: "key", type: "STRING", nullable: false },
    { name: "amount", type: "INT32", nullable: true },
  ];
  const table = (rows) => C.good(columns, rows);

  test("aggregate expressions reject every ungrouped reference, including wrapped and conditional fields", () => {
    const input = table([
      { key: "A", amount: 1 },
      { key: "B", amount: 2 },
    ]);
    const resolve = () => input;
    for (const sql of [
      "SELECT key || SUM(amount) AS total FROM Sales",
      "SELECT UPPER(key) || SUM(amount) AS total FROM Sales",
      "SELECT CAST(SUM(amount) AS INT32) + LENGTH(key) AS total FROM Sales",
      "SELECT CASE WHEN key = 'A' THEN SUM(amount) ELSE 0 END AS total FROM Sales",
      "SELECT SUM(amount) AS total FROM Sales HAVING key = 'A'",
      "SELECT SUM(amount) AS total FROM Sales ORDER BY key",
    ])
      assert.throws(() => C.query(sql, resolve), /GROUP BY/);
    assert.throws(
      () => C.query("SELECT SUM(MAX(amount)) AS total FROM Sales", resolve),
      /Nested aggregate/,
    );
    assert.throws(
      () =>
        C.query(
          "SELECT key, SUM(amount) AS total FROM Sales GROUP BY LOWER(key)",
          resolve,
        ),
      /GROUP BY/,
    );
    const grouped = C.query(
      "SELECT s.key || SUM(s.amount) AS total FROM Sales s GROUP BY key ORDER BY total",
      resolve,
    );
    assert.deepEqual(
      grouped.rows.map((row) => row.total),
      ["A1", "B2"],
    );
    assert.equal(
      C.query(
        "SELECT CAST(SUM(amount) AS INT32) + 1 AS total FROM Sales",
        resolve,
      ).rows[0].total,
      4,
    );
    assert.equal(
      C.query("SELECT SUM(amount) IS NULL AS empty FROM Sales", () => table([]))
        .rows[0].empty,
      true,
    );
    assert.deepEqual(
      C.query(
        "SELECT key, SUM(amount) AS total FROM Sales GROUP BY key HAVING total > 1 ORDER BY total DESC",
        resolve,
      ).rows.map((row) => [row.key, row.total]),
      [["B", 2]],
    );
  });

  test("CSV null tokens cannot introduce extra fields or records and safe custom tokens round-trip", () => {
    const input = C.good(
      [
        { name: "missing", type: "STRING", nullable: true },
        { name: "literal", type: "STRING", nullable: false },
      ],
      [{ missing: null, literal: "NULL" }],
    );
    for (const token of ["missing,value", 'a"b', "a\nb", "a\rb"])
      assert.throws(
        () => C.serialize(input, { format: "CSV", nullToken: token }),
        /null token.*delimiter.*quote.*line break/i,
      );
    assert.throws(
      () =>
        C.serialize(input, {
          format: "CSV",
          delimiter: ";",
          quote: "'",
          nullToken: "a;b",
        }),
      /null token/i,
    );
    assert.throws(
      () =>
        C.serialize(input, {
          format: "CSV",
          delimiter: ";",
          quote: "'",
          nullToken: "a'b",
        }),
      /null token/i,
    );
    const text = C.serialize(input, {
      format: "CSV",
      nullToken: "NULL",
      header: true,
    });
    const options = {
      hasHeader: true,
      headerRecord: 1,
      firstRecord: 2,
      lastRecord: "",
      blankRecords: "error",
      matchBy: "name",
      nullToken: "NULL",
    };
    const selection = root.UnifyCsv.select(
      root.UnifyCsv.parse(text, options),
      options,
    );
    const result = root.UnifyCsv.preview(
      selection,
      root.UnifyCsv.mappingsFor(selection.columns).map((column) => ({
        ...column,
        nullable: true,
      })),
      options,
    );
    assert.equal(result.rows.length, 1);
    assert.equal(result.rows[0].missing, null);
    assert.equal(result.rows[0].literal, "NULL");
  });
  test("source readers preserve scalar types and reject duplicate, nested and malformed records", () => {
    const json = C.json(
      '[{"id":"001","ok":true,"n":null,"nested":{"a":1}},{"id":"002","ok":false}]',
    );
    assert.equal(json.rows[0].id, "001");
    assert.equal(json.rows[0].ok, true);
    assert.equal(json.columns.find((c) => c.name === "nested").nested, true);
    assert.throws(() => C.json('[{"id":1,"id":2}]'), /Duplicate/);
    assert.throws(() => C.json('{"items":[]}'), /array/);
    assert.equal(C.json('{"items":[]}', "items").rows.length, 0);
    const nested = C.map(json, [
      { source: "nested", name: "nested", type: "STRING", nullable: true },
    ]);
    assert.equal(nested.ok, false);
    const xml = C.xml(
      '<records xmlns="urn:data" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"><record><key>A&amp;B</key><amount xsi:nil="true"/></record></records>',
      { rootName: "records", rowName: "record", namespaceURI: "urn:data" },
    );
    assert.equal(xml.rows[0].key, "A&B");
    assert.equal(xml.rows[0].amount, null);
    assert.throws(
      () =>
        C.xml("<!DOCTYPE records><records/>", {
          rootName: "records",
          rowName: "record",
        }),
      /DTD/,
    );
    assert.throws(
      () => C.xml("<records>", { rootName: "records", rowName: "record" }),
      /well formed/,
    );
  });
  test("JDBC and HTTP source previews validate transport configuration without claiming live access", () => {
    assert.equal(
      C.source(
        {
          format: "JDBC",
          readMode: "query",
          query: "SELECT * FROM positions WHERE units >= ?",
          parameters: [
            { name: "minimum", type: "INT32", value: "10", required: true },
          ],
        },
        '[{"units":20}]',
      ).rows[0].units,
      20,
    );
    assert.throws(
      () =>
        C.source(
          {
            format: "JDBC",
            readMode: "query",
            query: "DELETE FROM positions",
            parameters: [],
          },
          "[]",
        ),
      /SELECT|read-only/i,
    );
    assert.throws(
      () =>
        C.source(
          {
            format: "JDBC",
            readMode: "query",
            query: "SELECT * FROM positions WHERE units > ?",
            parameters: [],
          },
          "[]",
        ),
      /parameter/i,
    );
    const request = {
      endpoint: "https://example.invalid/data",
      connectTimeout: 1000,
      readTimeout: 1000,
      queryParams: [{ name: "date", value: "2026 11" }],
      headers: [{ name: "Accept", value: "application/json" }],
    };
    assert.match(C.request(request), /date=2026\+11|date=2026%2011/);
    assert.throws(
      () =>
        C.request({
          ...request,
          headers: [{ name: "Authorization", value: "secret" }],
        }),
      /header|credential/i,
    );
    assert.throws(
      () =>
        C.source(
          {
            ...request,
            format: "HTTP JSON",
            status: 503,
            contentType: "application/json",
          },
          "[]",
        ),
      /status|503/i,
    );
    assert.throws(
      () =>
        C.source(
          {
            ...request,
            format: "HTTP JSON",
            status: 200,
            contentType: "text/html",
          },
          "[]",
        ),
      /content type|JSON/i,
    );
  });
  test("schemas and mappings enforce missing versus null, constraints, types and whole-record rejection budgets", () => {
    assert.equal(
      root.UnifyCsv.convert("TRUE", { type: "BOOLEAN", nullable: false }),
      true,
    );
    assert.equal(
      C.convert(undefined, {
        type: "INT32",
        nullable: false,
        defaultEnabled: true,
        defaultValue: "4",
      }),
      4,
    );
    assert.throws(
      () =>
        C.convert(null, {
          type: "INT32",
          nullable: false,
          defaultEnabled: true,
          defaultValue: "4",
        }),
      /Null/,
    );
    assert.throws(
      () =>
        C.validateSchema([
          { name: "x", type: "INT32" },
          { name: "x", type: "STRING" },
        ]),
      /Duplicate/,
    );
    assert.throws(
      () => C.validateSchema([{ name: "x", type: "INT32", min: 4, max: 2 }]),
      /Minimum/,
    );
    assert.throws(
      () =>
        C.map(table([{ key: "a", amount: 3 }]), [
          { source: "typo", name: "out", type: "STRING", nullable: true },
        ]),
      /does not exist/,
    );
    const mapped = C.map(
      table([
        { key: "a", amount: 3 },
        { key: "b", amount: -1 },
      ]),
      [
        {
          source: "amount",
          name: "total",
          type: "INT32",
          nullable: false,
          min: 0,
        },
      ],
      { policy: "skip", maxRejected: 1 },
    );
    assert.equal(mapped.ok, true);
    assert.equal(mapped.rows.length, 1);
    assert.equal(mapped.rejected, 1);
    assert.equal(mapped.issues[0].severity, "warning");
    assert.equal(
      C.map(
        table([{ key: "a", amount: -1 }]),
        [
          {
            source: "amount",
            name: "total",
            type: "INT32",
            nullable: false,
            min: 0,
          },
        ],
        { policy: "skip", maxRejected: 0 },
      ).ok,
      false,
    );
  });
  test("ordered transformations and validation run on the chosen table and report invalid records", () => {
    const source = C.json(
      '[{"key":" a ","amount":"12"},{"key":" b ","amount":"bad"}]',
    );
    const rules = [
      { field: "key", operation: "trim" },
      { field: "key", operation: "upper" },
      { field: "amount", operation: "convert", type: "INT32", nullable: false },
    ];
    const result = C.transform(source, rules, {
      policy: "skip",
      maxRejected: 1,
    });
    assert.equal(result.ok, true);
    assert.deepEqual(
      result.rows.map((r) => [r.key, r.amount]),
      [["A", 12]],
    );
    assert.equal(result.rejected, 1);
    assert.equal(C.transform(source, rules).ok, false);
    assert.throws(
      () => C.transform(source, [{ field: "portfolio", operation: "trim" }]),
      /not present/,
    );
    assert.equal(
      C.validate(result, [{ field: "amount", operation: "min", value: 20 }]).ok,
      false,
    );
  });
  test("joins support composite keys, all outer modes, projections and relationship checks", () => {
    const left = C.good(
      [...columns, { name: "currency", type: "STRING", nullable: true }],
      [
        { key: "A", currency: "USD", amount: 2 },
        { key: "A", currency: "EUR", amount: 3 },
        { key: null, currency: "USD", amount: 4 },
      ],
    );
    const right = C.good(
      [
        { name: "code", type: "STRING", nullable: true },
        { name: "ccy", type: "STRING", nullable: false },
        { name: "rate", type: "DOUBLE", nullable: false },
      ],
      [
        { code: "A", ccy: "USD", rate: 1 },
        { code: "B", ccy: "GBP", rate: 2 },
        { code: null, ccy: "USD", rate: 3 },
      ],
    );
    const config = {
      joinType: "FULL",
      cardinality: "many-to-one",
      keys: [
        { left: "key", right: "code" },
        { left: "currency", right: "ccy" },
      ],
      projection: [
        { side: "left", field: "amount", name: "amount" },
        { side: "right", field: "rate", name: "rate" },
      ],
    };
    const result = C.join(left, right, config);
    assert.equal(result.rows.length, 5);
    assert.equal(result.joinCounts.matchedLeft, 1);
    assert.equal(result.joinCounts.unmatchedLeft, 2);
    assert.equal(result.joinCounts.unmatchedRight, 2);
    assert.equal(
      C.join(left, right, { ...config, joinType: "INNER" }).rows.length,
      1,
    );
    assert.equal(
      C.join(left, right, { ...config, joinType: "LEFT" }).rows.length,
      3,
    );
    assert.equal(
      C.join(left, right, { ...config, joinType: "RIGHT" }).rows.length,
      3,
    );
    assert.throws(
      () =>
        C.join(
          left,
          { ...right, rows: [...right.rows, right.rows[0]] },
          config,
        ),
      /not unique/,
    );
  });
  test("SQL executes real arbitrary tables, typed parameters, expressions, joins, grouping and limits", () => {
    const tables = {
      Sales: table([
        { key: "a", amount: 3 },
        { key: "a", amount: 4 },
        { key: "b", amount: 1 },
      ]),
      Rates: C.good(
        [
          { name: "code", type: "STRING", nullable: false },
          { name: "rate", type: "DOUBLE", nullable: false },
        ],
        [{ code: "a", rate: 2 }],
      ),
    };
    const resolve = (name) => {
      if (!tables[name]) throw Error("Unknown dataset " + name);
      return tables[name];
    };
    const result = C.query(
      "SELECT s.key AS account, SUM(s.amount * r.rate) AS total FROM Sales s LEFT JOIN Rates r ON s.key = r.code WHERE s.amount >= ? GROUP BY s.key ORDER BY total DESC LIMIT 1",
      resolve,
      [2],
    );
    assert.deepEqual(
      result.rows.map((r) => [r.account, r.total]),
      [["a", 14]],
    );
    assert.equal(result.rows[0]._aggregate, true);
    assert.equal(result.rows[0]._contributors, 2);
    assert.equal(
      C.query("SELECT DISTINCT key FROM Sales ORDER BY key", resolve).rows
        .length,
      2,
    );
    assert.equal(
      C.query(
        "SELECT CASE WHEN amount > 2 THEN 'large' ELSE 'small' END AS size FROM Sales LIMIT 1",
        resolve,
      ).rows[0].size,
      "large",
    );
    assert.throws(
      () => C.query("SELECT typo FROM Sales", resolve),
      /Unknown field/,
    );
    assert.throws(
      () => C.query("SELECT amount FROM Sales WHERE amount > ?", resolve),
      /parameter/i,
    );
    assert.throws(
      () => C.query("UPDATE Sales SET amount = 1", resolve),
      /SELECT|read-only/i,
    );
    assert.throws(
      () => C.query("SELECT MAGIC(key) AS x FROM Sales", resolve),
      /Unsupported function/,
    );
  });
  test("output serialization preserves null and scalars and JDBC prepares APPEND with destination validation", () => {
    const input = table([
      { key: 'A,"B', amount: null },
      { key: "C", amount: 4 },
    ]);
    const csv = C.serialize(input, {
      format: "CSV",
      nullToken: "\\N",
      header: true,
    });
    assert.match(csv, /"A,""B",\\N/);
    assert.deepEqual(JSON.parse(C.serialize(input, { format: "JSON" })), [
      { key: 'A,"B', amount: null },
      { key: "C", amount: 4 },
    ]);
    assert.match(
      C.serialize(input, { format: "XML", rootName: "items", rowName: "item" }),
      /xsi:nil="true"/,
    );
    const jdbc = {
      format: "JDBC",
      mode: "APPEND",
      table: "positions",
      destinationColumns: columns,
    };
    assert.match(C.serialize(input, jdbc), /INSERT INTO.*positions/s);
    assert.throws(
      () => C.serialize(input, { ...jdbc, mode: "REPLACE" }),
      /APPEND/,
    );
    assert.throws(
      () =>
        C.serialize(input, {
          ...jdbc,
          destinationColumns: [
            ...columns,
            { name: "required", type: "STRING", nullable: false },
          ],
        }),
      /required|Required/,
    );
  });
  test("execution limits stop excessive payloads, row counts and expired operations", () => {
    const context = {
      deadline: Date.now() + 1000,
      settings: { maxGeneratedRows: 1, maxPayloadBytes: 1000 },
    };
    assert.throws(() => C.reserveGenerated(context, 2), /Generated/);
    assert.throws(() => C.limits({ ...context, deadline: 0 }), /deadline/);
    assert.throws(
      () =>
        C.limits(
          {
            ...context,
            settings: { maxGeneratedRows: 10, maxPayloadBytes: 1 },
          },
          [{ x: "large" }],
        ),
      /payload/,
    );
  });

  test("rejection budgets span processing stages and JDBC rejects nulls in non-null destination columns", () => {
    const context = {
      deadline: Date.now() + 1000,
      settings: { maxGeneratedRows: 100, maxPayloadBytes: 10000 },
      rejected: 0,
    };
    const input = C.json(
      '[{"key":"a","amount":"1"},{"key":"b","amount":"bad"},{"key":"c","amount":"-1"}]',
    );
    const converted = C.transform(
      input,
      [
        {
          field: "amount",
          operation: "convert",
          type: "INT32",
          nullable: false,
        },
      ],
      { policy: "skip", maxRejected: 1, context },
    );
    assert.equal(converted.ok, true);
    const validated = C.validate(
      converted,
      [{ field: "amount", operation: "min", value: 0 }],
      { policy: "skip", maxRejected: 1, context },
    );
    assert.equal(validated.ok, false);
    assert.equal(context.rejected, 2);
    assert.throws(
      () =>
        C.serialize(table([{ key: "a", amount: null }]), {
          format: "JDBC",
          mode: "APPEND",
          table: "positions",
          destinationColumns: columns.map((c) => ({ ...c, nullable: false })),
        }),
      /Destination record.*Null/,
    );
    const result = C.query(
      "SELECT key, COALESCE(NULL, amount) AS value FROM Sales",
      () => table([{ key: "a", amount: 2 }]),
    );
    assert.equal(result.columns[0].nullable, false);
    assert.equal(result.columns[1].type, "INT32");
    assert.equal(result.rows[0].value, 2);
  });

  test("SQL joins share a cumulative generation budget with visual joins", () => {
    const input = table([
      { key: "A", amount: 1 },
      { key: "B", amount: 2 },
    ]);
    const projection = [{ side: "left", field: "key", name: "key" }];
    const configuration = {
      joinType: "INNER",
      cardinality: "one-to-one",
      keys: [{ left: "key", right: "key" }],
      projection,
    };
    const context = (limit) => ({
      deadline: Date.now() + 10000,
      settings: { maxGeneratedRows: limit, maxPayloadBytes: 100000 },
    });
    const sql =
      "SELECT a.key FROM Prepared a INNER JOIN Lookup b ON a.key=b.key INNER JOIN Lookup c ON a.key=c.key";
    const limited = context(5),
      prepared = C.join(input, input, configuration, limited);
    assert.equal(limited.generated, 2);
    assert.throws(
      () =>
        C.query(
          sql,
          (name) => (name === "Prepared" ? prepared : input),
          [],
          limited,
        ),
      /Generated-record limit.*across join steps/,
    );
    assert.equal(limited.generated, 5);
    const exact = context(6),
      ready = C.join(input, input, configuration, exact);
    assert.deepEqual(
      C.query(
        sql,
        (name) => (name === "Prepared" ? ready : input),
        [],
        exact,
      ).rows.map((row) => row.key),
      ["A", "B"],
    );
    assert.equal(exact.generated, 6);
    const read = context(1);
    assert.equal(
      C.query("SELECT key FROM Source", () => input, [], read).rows.length,
      2,
      "Source rows use the source budget rather than the join generation budget",
    );
    assert.equal(read.generated || 0, 0);
  });

  test("unmatched SQL outer-join records count before filtering, aggregation, and LIMIT", () => {
    const left = table([
        { key: "A", amount: 1 },
        { key: "B", amount: 2 },
      ]),
      right = table([
        { key: "C", amount: 3 },
        { key: "D", amount: 4 },
      ]);
    const resolve = (name) => (name === "L" ? left : right);
    const context = (limit) => ({
      deadline: Date.now() + 10000,
      settings: { maxGeneratedRows: limit, maxPayloadBytes: 100000 },
    });
    for (const type of ["LEFT", "RIGHT"])
      assert.throws(
        () =>
          C.query(
            "SELECT a.key FROM L a " +
              type +
              " JOIN R b ON a.key=b.key LIMIT 1",
            resolve,
            [],
            context(1),
          ),
        /Generated-record limit/,
      );
    assert.throws(
      () =>
        C.query(
          "SELECT COUNT(*) AS n FROM L a FULL JOIN R b ON a.key=b.key WHERE a.key IS NULL",
          resolve,
          [],
          context(3),
        ),
      /Generated-record limit/,
    );
    const exact = context(4);
    assert.equal(
      C.query(
        "SELECT COUNT(*) AS n FROM L a FULL JOIN R b ON a.key=b.key",
        resolve,
        [],
        exact,
      ).rows[0].n,
      4,
    );
    assert.equal(exact.generated, 4);
    assert.equal(
      C.query(
        "SELECT COUNT(*) AS n FROM L a INNER JOIN R b ON a.key=b.key",
        resolve,
        [],
        context(1),
      ).rows[0].n,
      0,
    );
  });

  test("applying a declared query result schema preserves aggregate contributor metadata", () => {
    const aggregate = C.query("SELECT SUM(amount) AS total FROM Source", () =>
      table([
        { key: "A", amount: 1 },
        { key: "B", amount: 2 },
      ]),
    );
    const mapped = C.map(aggregate, [
      { source: "total", name: "total", type: "STRING", nullable: false },
    ]);
    assert.equal(mapped.rows[0].total, "3");
    assert.equal(mapped.rows[0]._aggregate, true);
    assert.equal(mapped.rows[0]._contributors, 2);
  });
})();

// Interface workflows
(() => {
  const { test } = require("node:test");
  const assert = require("node:assert/strict");
  const {
    readFileSync,
    copyFileSync,
    mkdtempSync,
    unlinkSync,
    rmdirSync,
  } = require("node:fs");
  const { join } = require("node:path");
  const { tmpdir } = require("node:os");
  const { JSDOM, VirtualConsole } = testToolingRequire("jsdom");
  const html = readFileSync(join(__dirname, "index.html"), "utf8");
  const D = require("./data.js");
  const STORAGE = "unify-ui-prototype-v1";

  function workspace(t, stored) {
    const errors = [];
    const console = new VirtualConsole();
    console.on("jsdomError", (error) => {
      // jsdom has no viewport. Navigation scroll is excluded from DOM checks.
      if (!error.message.includes("window.scrollTo")) errors.push(error);
    });
    const dom = new JSDOM(readFileSync(join(__dirname, "index.html"), "utf8"), {
      url: "http://127.0.0.1:4321/",
      runScripts: "outside-only",
      virtualConsole: console,
    });
    const w = dom.window,
      d = w.document;
    w.addEventListener("error", (event) => errors.push(event.error));
    if (stored) w.localStorage.setItem(STORAGE, stored);
    for (const script of d.querySelectorAll("script")) {
      const src = script.getAttribute("src");
      if (src)
        assert.equal(
          src,
          "data.js",
          "Only the separate data file may be external",
        );
      w.eval(
        src ? readFileSync(join(__dirname, src), "utf8") : script.textContent,
      );
    }
    const get = (selector) => {
      const el = d.querySelector(selector);
      assert.ok(el, "Missing control: " + selector);
      return el;
    };
    const click = (selector) => get(selector).click();
    const field = (selector, value, type = "change") => {
      const el = get(selector);
      el.value = value;
      el.dispatchEvent(new w.Event(type, { bubbles: true }));
    };
    const check = (selector, value) => {
      const el = get(selector);
      el.checked = value;
      el.dispatchEvent(new w.Event("change", { bubbles: true }));
    };
    const rows = () =>
      d.querySelectorAll('table[aria-label="Preview data"] tbody tr').length;
    const saved = () => JSON.parse(w.localStorage.getItem(STORAGE));
    t.after(() => {
      dom.window.close();
      assert.deepEqual(errors, [], "Unexpected interface errors");
    });
    return {
      w,
      d,
      get,
      click,
      field,
      check,
      rows,
      saved,
      stored: () => w.localStorage.getItem(STORAGE),
    };
  }
  const wait = (ms) => new Promise((resolve) => setTimeout(resolve, ms));
  async function finishPreview(ui) {
    const deadline = Date.now() + 5000;
    while (ui.d.querySelector(".progress-strip") && Date.now() < deadline)
      await wait(25);
    assert.equal(
      ui.d.querySelector(".progress-strip"),
      null,
      "Preview did not complete",
    );
  }

  test(
    "copied mockup runs directly from disk with only its HTML and data file",
    { timeout: 5000 },
    async (t) => {
      const directory = mkdtempSync(join(tmpdir(), "unify-mockup-"));
      const files = ["index.html", "data.js"];
      for (const file of files)
        copyFileSync(join(__dirname, file), join(directory, file));
      let dom;
      t.after(() => {
        dom?.window.close();
        for (const file of files) unlinkSync(join(directory, file));
        rmdirSync(directory);
      });
      const errors = [];
      const console = new VirtualConsole();
      console.on("jsdomError", (error) => errors.push(error));
      dom = await JSDOM.fromFile(join(directory, "index.html"), {
        runScripts: "dangerously",
        resources: "usable",
        virtualConsole: console,
      });
      if (dom.window.document.readyState !== "complete") {
        await new Promise((resolve) =>
          dom.window.addEventListener("load", resolve, { once: true }),
        );
      }
      const document = dom.window.document;
      assert.equal(
        document.querySelectorAll('link[rel="stylesheet"]').length,
        0,
      );
      assert.equal(document.styleSheets.length, 1);
      assert.equal(
        document.querySelectorAll(
          "script[src]:not([src='data.js']), iframe[src], img[src], source[src], video[src], audio[src]",
        ).length,
        0,
      );
      assert.deepEqual(
        [...document.querySelectorAll("script[src]")].map((script) =>
          script.getAttribute("src"),
        ),
        ["data.js"],
      );
      assert.equal(
        document.querySelectorAll('table[aria-label="Preview data"] tbody tr')
          .length,
        12,
      );
      assert.match(
        document.querySelector("h1").textContent,
        /Daily holdings enrichment/,
      );
      const parameter = document.querySelector('[name="minimum-units"]');
      parameter.value = "100";
      parameter.dispatchEvent(
        new dom.window.Event("change", { bubbles: true }),
      );
      document.querySelector('[data-action="preview-stage"]').click();
      const deadline = Date.now() + 3000;
      while (document.querySelector(".progress-strip") && Date.now() < deadline)
        await wait(25);
      assert.equal(document.querySelector(".progress-strip"), null);
      assert.equal(
        document.querySelectorAll('table[aria-label="Preview data"] tbody tr')
          .length,
        10,
      );
      assert.deepEqual(errors, []);
    },
  );

  test("creating a pipeline retains the previous definition and reloads the new design", (t) => {
    const ui = workspace(t);
    ui.click('[data-action="navigate"][data-view="pipelines"]');
    ui.click('[data-action="new-pipeline"]');
    ui.field("#new-pipeline-name", "Client reconciliation");
    ui.click('[data-action="config-save"]');
    assert.match(ui.get("h1").textContent, /Client reconciliation/);
    assert.equal(ui.saved().name, "Client reconciliation");
    assert.equal(ui.saved().version, 1);
    assert.ok(
      ui
        .saved()
        .definitions.some(
          (d) => d.name === "Daily holdings enrichment" && d.content,
        ),
    );
    const reload = workspace(t, ui.stored());
    assert.match(reload.get("h1").textContent, /Client reconciliation/);
    reload.click('[data-action="save-version"]');
    reload.click('[data-action="confirm-save"]');
    assert.equal(
      reload.saved().definitions.find((d) => d.id === reload.saved().pipelineId)
        .version,
      2,
    );
    assert.equal(
      reload.saved().definitions.find((d) => d.id === "daily-holdings").version,
      3,
    );
  });

  test("workbench renders the sample result, typed schema, issues and record lineage", (t) => {
    const ui = workspace(t);
    assert.equal(ui.rows(), 12);
    assert.equal(ui.d.querySelectorAll(".stage-card").length, 6);
    ui.click('[data-action="result-tab"][data-tab="schema"]');
    assert.match(ui.get("#result-content").textContent, /marketValue.*DOUBLE/s);
    ui.click('[data-action="result-tab"][data-tab="issues"]');
    assert.match(
      ui.get("#result-content").textContent,
      /2 input records did not match/,
    );
    ui.click('[data-action="result-tab"][data-tab="results"]');
    ui.click('tr[data-action="row-detail"]');
    assert.match(ui.get('[role="dialog"]').textContent, /Record lineage/);
    ui.w.document.dispatchEvent(
      new ui.w.KeyboardEvent("keydown", { key: "Escape", bubbles: true }),
    );
    assert.equal(ui.d.querySelector('[role="dialog"]'), null);
    assert.equal(
      ui.d.querySelectorAll('script[src^="http"],link[href^="http"],iframe')
        .length,
      0,
    );
  });

  test("pipeline binding inspection can open and preview its actual synthetic data", async (t) => {
    const ui = workspace(t);
    ui.click('[data-action="explorer-tab"][data-tab="pipeline"]');
    ui.click('[data-action="source-toggle"][data-id="instruments"]');
    ui.click('[data-action="source-detail"][data-id="instruments"]');
    assert.match(
      ui.get('[role="dialog"]').textContent,
      /Reference.Instruments.*Pinned v2/s,
    );
    ui.click('[role="dialog"] [data-action="use-source"]');
    assert.match(ui.get("#sql-editor").value, /FROM Instruments AS i/);
    ui.click('[data-action="preview-stage"]');
    await finishPreview(ui);
    assert.equal(ui.rows(), 10);
    assert.equal(ui.d.querySelector(".results-message"), null);
  });

  test("full-page join configuration uses selected fields and relationship settings", async (t) => {
    const ui = workspace(t);
    ui.click('[data-action="select-stage"][data-id="join"]');
    ui.click('[data-action="config-stage"]');
    assert.equal(ui.d.querySelector('[role="dialog"]'), null);
    ui.field('[data-config-setting="joinType"]', "INNER");
    ui.field('[data-config-setting="cardinality"]', "many-to-many");
    ui.click('[data-action="config-section"][data-section="fields"]');
    assert.equal(
      ui.d.querySelectorAll('table[aria-label="Join key pairs"] tbody tr')
        .length,
      1,
    );
    ui.click('[data-action="config-section"][data-section="preview"]');
    assert.match(ui.get(".config-table-note").textContent, /10 sampled/);
    ui.click('[data-action="config-save"]');
    ui.click('[data-action="preview-stage"]');
    await finishPreview(ui);
    assert.equal(ui.rows(), 10);
    ui.click('[data-action="save-version"]');
    ui.click('[data-action="confirm-save"]');
    assert.equal(
      ui.saved().stages.find((s) => s.id === "join").joinType,
      "INNER",
    );
  });

  test("unbounded stage editing supports add, rename, reorder, remove and cancellation", (t) => {
    const ui = workspace(t);
    ui.click('[data-action="add-stage"]');
    ui.click('[data-action="choose-stage-type"][data-kind="Transform"]');
    assert.equal(ui.d.querySelectorAll(".stage-card").length, 7);
    ui.field("#panel-stage-name", "Reconcile positions");
    assert.match(
      ui.get(".stage-card.active").textContent,
      /Reconcile positions/,
    );
    ui.click('[data-action="move-stage-up"]');
    ui.click('[data-action="remove-stage"]');
    ui.click('[data-action="confirm-remove-stage"]');
    assert.equal(ui.d.querySelectorAll(".stage-card").length, 6);
    ui.click('[data-action="run-pipeline"]');
    assert.ok(ui.get("#engine-select").disabled);
    ui.click('[data-action="cancel-run"]');
    ui.click('[data-action="navigate"][data-view="runs"]');
    assert.match(ui.get(".list-table tbody tr").textContent, /Cancelled/);
  });

  test("pipeline preview binds parameters and records the selected engine", async (t) => {
    const ui = workspace(t);
    ui.field("#engine-select", "SQLite");
    ui.field('[name="minimum-units"]', "100");
    ui.click('[data-action="run-pipeline"]');
    await finishPreview(ui);
    assert.equal(ui.rows(), 10);
    ui.click('[data-action="navigate"][data-view="runs"]');
    const latest = ui.get(".list-table tbody tr");
    assert.match(latest.textContent, /Completed.*SQLite.*10/s);
    latest.click();
    assert.match(
      ui.get('[role="dialog"]').textContent,
      /No SQL engine, catalogue database, or destination is connected/,
    );
  });

  test("saving creates a local version and retains the prior saved design across reloads", (t) => {
    const ui = workspace(t);
    const query = "SELECT h.portfolio, h.units FROM Holdings AS h;";
    ui.field("#sql-editor", query, "input");
    ui.click('[data-action="save-version"]');
    ui.field("#save-name", "Reviewed sample pipeline");
    ui.click('[data-action="confirm-save"]');
    assert.equal(ui.saved().version, 4);
    assert.equal(ui.saved().sql, query);
    assert.equal(ui.saved().snapshots[0].version, 3);
    assert.equal(ui.saved().snapshots[0].sql, D.sql);
    const reloaded = workspace(t, ui.stored());
    assert.match(reloaded.get("h1").textContent, /Reviewed sample pipeline/);
    assert.equal(reloaded.get("#sql-editor").value, query);
    reloaded.field("#sql-editor", "SELECT * FROM MissingData;", "input");
    reloaded.field("#engine-select", "SQLite");
    assert.equal(
      reloaded.saved().sql,
      query,
      "engine selection must not silently save a draft",
    );
  });

  test("structured reusable schemas validate columns and retain revisions on reload", (t) => {
    const ui = workspace(t);
    ui.click('[data-action="navigate"][data-view="catalog"]');
    ui.click('[data-action="new-definition"]');
    ui.field("#definition-name", "Client <schema>");
    ui.field('[data-config-field="input"]', "Holdings");
    assert.equal(ui.d.querySelector('[role="dialog"]'), null);
    ui.click('[data-action="config-section"][data-section="fields"]');
    ui.field('[data-config-column="name"][data-index="1"]', "portfolio");
    ui.click('[data-action="config-save"]');
    assert.match(ui.get(".config-error").textContent, /Duplicate/);
    ui.field('[data-config-column="name"][data-index="1"]', "isin");
    ui.click('[data-action="config-save"]');
    ui.field("#library-search", "Client", "input");
    assert.equal(ui.d.querySelectorAll(".definition-card").length, 1);
    ui.click(".definition-card");
    ui.click('[data-action="revise-definition"]');
    ui.field("#definition-name", "Client <schema>");
    ui.click('[data-action="config-section"][data-section="fields"]');
    ui.field('[data-config-column="maxLength"][data-index="0"]', "100");
    ui.click('[data-action="config-save"]');
    const definition = ui
      .saved()
      .definitions.find((d) => d.name === "Client <schema>");
    assert.equal(definition.version, 2);
    assert.equal(definition.schema[0].maxLength, "100");
    assert.equal(ui.d.querySelector("schema"), null);
    const reload = workspace(t, ui.stored());
    reload.click('[data-action="navigate"][data-view="catalog"]');
    reload.field("#library-search", "Client", "input");
    assert.match(reload.get(".definition-card").textContent, /v2/);
  });

  test("connections persist without registering providers or binding pipeline data", (t) => {
    const ui = workspace(t);
    ui.click('[data-action="navigate"][data-view="sources"]');
    ui.click('[data-action="new-source"]');
    ui.field("#new-source-name", "ClientReference");
    ui.field("#new-source-format", "JDBC");
    ui.field("#new-source-location", "jdbc:sqlite:reference.db");
    ui.click('[data-action="confirm-source"]');
    assert.equal(ui.d.querySelectorAll(".source-card").length, 4);
    assert.equal(ui.saved().providers.length, D.providers.length);
    assert.equal(ui.saved().bindings.length, 3);
    const reload = workspace(t, ui.stored());
    reload.click('[data-action="navigate"][data-view="sources"]');
    assert.match(reload.get("#source-grid").textContent, /ClientReference/);
  });

  test("older saved source metadata migrates to connections while retaining the pipeline", (t) => {
    const legacy = {
      name: "Earlier review",
      version: 4,
      sql: D.sql,
      sourceDefinitions: [
        {
          id: "legacy-source",
          name: "LegacyReference",
          display: "Legacy reference",
          format: "JSON",
          location: "/sample-data/legacy.json",
          description: "Earlier local definition.",
        },
      ],
    };
    const ui = workspace(t, JSON.stringify(legacy));
    assert.match(ui.get("h1").textContent, /Earlier review/);
    assert.equal(ui.rows(), 12);
    ui.click('[data-action="navigate"][data-view="sources"]');
    assert.match(ui.get("#source-grid").textContent, /Legacy reference/);
    ui.click('[data-action="navigate"][data-view="providers"]');
    assert.doesNotMatch(
      ui.get("#provider-directory").textContent,
      /Legacy reference/,
    );
  });

  test("query errors have a recoverable result and an inspectable failure record", async (t) => {
    const ui = workspace(t);
    ui.click('[data-action="guide"]');
    ui.click('[data-action="invalid-example"]');
    await finishPreview(ui);
    assert.match(ui.get("#result-content").textContent, /Unknown dataset/);
    assert.equal(ui.saved().runs[0].status, "Failed");
    ui.click('[data-action="reset-query"]');
    assert.equal(ui.get("#sql-editor").value, D.sql);
    ui.w.document.dispatchEvent(
      new ui.w.KeyboardEvent("keydown", {
        key: "k",
        ctrlKey: true,
        bubbles: true,
      }),
    );
    ui.field("#command-search", "Exposure", "input");
    assert.match(ui.get("#command-results").textContent, /Portfolio exposure/);
    assert.equal(ui.d.activeElement.id, "command-search");
  });

  test("output configuration prepares actual JSON and retains the selected filename", (t) => {
    const ui = workspace(t);
    ui.click('[data-action="select-stage"][data-id="output"]');
    ui.click('[data-action="config-output"]');
    ui.field('[data-config-setting="format"]', "JSON");
    ui.field('[data-config-setting="name"]', "reviewed-exposure.json");
    ui.click('[data-action="config-section"][data-section="preview"]');
    const rows = JSON.parse(ui.get(".config-serialized").textContent);
    assert.equal(rows.length, 12);
    assert.equal(typeof rows[0].marketValue, "number");
    assert.equal(ui.d.querySelector('[data-action="simulate-publish"]'), null);
    ui.click('[data-action="config-save"]');
    ui.click('[data-action="save-version"]');
    ui.click('[data-action="confirm-save"]');
    assert.equal(ui.saved().output.name, "reviewed-exposure.json");
  });

  test("catalogue discovery exposes contracts while pipeline data exposes bindings and earlier results", async (t) => {
    const ui = workspace(t);
    assert.match(ui.get("#explorer-items").textContent, /Reference/);
    assert.doesNotMatch(
      ui.get("#explorer-items").textContent,
      /Holdings.csv|Saved definitions|sample rows/,
    );
    ui.click('[data-action="navigate"][data-view="providers"]');
    assert.match(ui.get("h1").textContent, /Catalogue/);
    assert.match(
      ui.get("#provider-directory").textContent,
      /Views.PortfolioExposure/,
    );
    assert.doesNotMatch(
      ui.get("#provider-directory").textContent,
      /Holdings input schema|Exposure output mapping/,
    );
    ui.click('[data-action="provider-detail"][data-id="holdings-reader"]');
    ui.click('[data-action="provider-section"][data-section="Parameters"]');
    assert.match(
      ui.get('[role="dialog"]').textContent,
      /file.*STRING.*Yes.*holdings.csv/s,
    );
    ui.click('[data-action="provider-section"][data-section="Dependencies"]');
    assert.match(
      ui.get('[role="dialog"]').textContent,
      /Holdings input schema.*pinned v1/s,
    );
    ui.click('[data-action="close-modal"]');
    ui.click('[data-action="navigate"][data-view="workspace"]');
    ui.click('[data-action="explorer-tab"][data-tab="pipeline"]');
    assert.match(ui.get("#explorer-items").textContent, /NormalizedHoldings/);
    assert.doesNotMatch(
      ui.get("#explorer-items").textContent,
      /ValidatedExposure/,
    );
    ui.field(
      "#sql-editor",
      "SELECT * FROM ValidatedExposure WHERE units >= ?;",
      "input",
    );
    ui.click('[data-action="preview-stage"]');
    await finishPreview(ui);
    assert.match(ui.get("#result-content").textContent, /declared scope/);
  });

  test("the compact data browser searches and switches scope without duplicate element IDs", (t) => {
    const ui = workspace(t);
    ui.click('[data-action="catalog-mobile"]');
    const ids = [...ui.d.querySelectorAll("[id]")].map((el) => el.id);
    assert.equal(new Set(ids).size, ids.length);
    ui.field("#dialog-source-search", "Instruments", "input");
    assert.match(ui.get("#dialog-explorer-items").textContent, /Instruments/);
    assert.doesNotMatch(
      ui.get("#dialog-explorer-items").textContent,
      /Holdings/,
    );
    ui.field("#dialog-source-search", "", "input");
    ui.click(
      '[role="dialog"] [data-action="explorer-tab"][data-tab="pipeline"]',
    );
    assert.match(
      ui.get("#dialog-explorer-items").textContent,
      /NormalizedHoldings/,
    );
    ui.click(
      '[role="dialog"] [data-action="source-toggle"][data-id="instruments"]',
    );
    ui.click(
      '[role="dialog"] [data-action="source-detail"][data-id="instruments"]',
    );
    assert.match(
      ui.get('[role="dialog"]').textContent,
      /Reference.Instruments/,
    );
  });

  test("binding a known provider pins its contract and permits a separate preview", async (t) => {
    const ui = workspace(t);
    ui.click('[data-action="provider-detail"][data-id="instruments-reader"]');
    ui.click('[data-action="bind-provider"]');
    ui.field("#binding-name", "Instruments");
    ui.click('[data-action="confirm-binding"]');
    assert.match(ui.get(".config-error").textContent, /unique/);
    ui.field("#binding-name", "ClientInstruments");
    ui.click('[data-action="confirm-binding"]');
    assert.match(
      ui.get("#explorer-items").textContent,
      /ClientInstruments.*Reference.Instruments · pinned v2/s,
    );
    const newest = ui.d.querySelectorAll('[data-action="use-source"]');
    newest[newest.length - 1].click();
    ui.click('[data-action="preview-stage"]');
    await finishPreview(ui);
    assert.equal(ui.rows(), 10);
    ui.click('[data-action="save-version"]');
    ui.click('[data-action="confirm-save"]');
    const binding = ui
      .saved()
      .bindings.find((b) => b.name === "ClientInstruments");
    assert.equal(binding.providerVersion, 2);
    assert.equal(binding.parameters.file, "instruments.json");
    assert.equal(binding.connectionId, "reference-files");
    assert.equal(Object.hasOwn(binding, "rows"), false);
    const reload = workspace(t, ui.stored());
    reload.click('[data-action="preview-stage"]');
    await finishPreview(reload);
    assert.equal(reload.rows(), 10);
  });

  test("runtime-shaped readers require values and report missing source samples", async (t) => {
    const ui = workspace(t);
    ui.click('[data-action="provider-detail"][data-id="csv-reader"]');
    ui.click('[data-action="provider-section"][data-section="Columns"]');
    assert.match(
      ui.get('[role="dialog"]').textContent,
      /Columns are not known/,
    );
    ui.click('[data-action="bind-provider"]');
    ui.click('[data-action="confirm-binding"]');
    assert.match(ui.get(".config-error").textContent, /file is required/);
    ui.field("#binding-param-0", "clients.csv");
    ui.click('[data-action="confirm-binding"]');
    const inputs = ui.d.querySelectorAll('[data-action="use-source"]');
    inputs[inputs.length - 1].click();
    ui.click('[data-action="preview-stage"]');
    await finishPreview(ui);
    assert.match(
      ui.get("#result-content").textContent,
      /Configure this provider.*source sample/,
    );
  });

  test("new bindings become selectable inputs for the full-page join workspace", async (t) => {
    const ui = workspace(t);
    ui.click('[data-action="provider-detail"][data-id="instruments-reader"]');
    ui.click('[data-action="bind-provider"]');
    ui.field("#binding-name", "AlternateInstruments");
    ui.click('[data-action="confirm-binding"]');
    ui.click('[data-action="select-stage"][data-id="join"]');
    ui.click('[data-action="config-stage"]');
    ui.field('[data-config-setting="rightInput"]', "AlternateInstruments");
    ui.click('[data-action="config-section"][data-section="fields"]');
    ui.click('[data-action="config-section"][data-section="preview"]');
    assert.match(ui.get(".config-table-note").textContent, /12 sampled/);
    ui.click('[data-action="config-save"]');
    ui.click('[data-action="preview-stage"]');
    await finishPreview(ui);
    assert.equal(ui.rows(), 12);
    assert.match(
      ui.get(".results-message").textContent,
      /2 input records did not match/,
    );
  });

  test("earlier join results expose their schema and preview enriched records", async (t) => {
    const ui = workspace(t);
    ui.click('[data-action="explorer-tab"][data-tab="pipeline"]');
    ui.click('[data-action="inspect-stage-result"][data-id="join"]');
    assert.match(
      ui.get('[role="dialog"]').textContent,
      /instrument.*Nullable/s,
    );
    ui.click('[data-action="query-stage-result"]');
    ui.click('[data-action="preview-stage"]');
    await finishPreview(ui);
    assert.equal(ui.rows(), 12);
    assert.equal(
      ui.d.querySelectorAll('table[aria-label="Preview data"] thead th').length,
      9,
    );
    assert.match(
      ui.get('table[aria-label="Preview data"]').textContent,
      /Aster Technologies/,
    );
  });

  test("writer catalogue entries configure prepared output without becoming query inputs", (t) => {
    const ui = workspace(t);
    ui.click('[data-action="provider-detail"][data-id="csv-output"]');
    assert.match(
      ui.get('[role="dialog"]').textContent,
      /Portable SELECT queries cannot perform publication/,
    );
    ui.click('[data-action="bind-provider"]');
    ui.field("#binding-param-0", "provider-output.csv");
    ui.click('[data-action="config-save"]');
    assert.match(ui.get(".visual-designer").textContent, /provider-output.csv/);
    ui.click('[data-action="save-version"]');
    ui.click('[data-action="confirm-save"]');
    assert.equal(ui.saved().bindings.length, 3);
    assert.equal(ui.saved().output.providerRef.id, "csv-output");
    assert.equal(ui.saved().output.name, "provider-output.csv");
  });

  test("unavailable providers remain inspectable and cannot be bound", (t) => {
    const ui = workspace(t);
    ui.click('[data-action="provider-detail"][data-id="warehouse-reader"]');
    assert.equal(ui.get('[data-action="bind-provider"]').disabled, true);
    assert.match(ui.get('[role="dialog"]').textContent, /Not configured/);
  });

  test("CSV registration maps selected records and persists a reusable configuration", async (t) => {
    const ui = workspace(t);
    ui.click('[data-action="navigate"][data-view="providers"]');
    ui.click('[data-action="register-provider"]');
    assert.equal(ui.d.querySelector('[role="dialog"]'), null);
    assert.match(ui.get("h1").textContent, /Configure CSV provider/);
    ui.click('[data-action="csv-example"]');
    ui.field("#csv-provider-name", "Client.Reference");
    ui.field("#csv-file-name", "client.csv");
    ui.click('[data-action="csv-read"]');
    ui.field("#csv-last-record", "3");
    ui.click('[data-action="csv-map"]');
    ui.field(
      '[data-csv-map="outputName"][data-index="0"]',
      "portfolio",
      "input",
    );
    ui.check('[data-csv-map="trim"][data-index="0"]', true);
    ui.field('[data-csv-map="type"][data-index="2"]', "INT32");
    ui.field('[data-csv-map="type"][data-index="3"]', "DOUBLE");
    ui.check('[data-csv-map="include"][data-index="5"]', false);
    ui.click('[data-action="csv-move"][data-index="2"][data-direction="-1"]');
    ui.click('[data-action="csv-preview"]');
    assert.match(
      ui.get('table[aria-label="Mapped CSV preview"]').textContent,
      /Growth Fund.*1250.*187.42/s,
    );
    assert.equal(
      ui.d.querySelectorAll('table[aria-label="Mapped CSV preview"] tbody tr')
        .length,
      2,
    );
    ui.click('[data-action="csv-save"]');
    assert.match(ui.get("#provider-directory").textContent, /Client.Reference/);
    const registered = ui
      .saved()
      .providers.find((p) => p.name === "Client.Reference");
    assert.equal(registered.columns.length, 5);
    assert.deepEqual(
      registered.columns.map((c) => c.name),
      ["portfolio", "Quantity", "ISIN", "Price", "Currency"],
    );
    assert.equal(registered.columns[1].type, "INT32");
    assert.equal(registered.csvMapping.options.lastRecord, "3");
    assert.equal(registered.parameters[0].defaultValue, "client.csv");
    assert.equal(Object.hasOwn(registered.csvMapping, "text"), false);
    assert.equal(ui.saved().connections.length, 3);
    assert.equal(ui.saved().bindings.length, 3);
    assert.equal(ui.saved().runs.length, D.seedRuns.length);
    const reload = workspace(t, ui.stored());
    reload.click(
      '[data-action="provider-detail"][data-id="' + registered.id + '"]',
    );
    reload.click('[data-action="provider-section"][data-section="Columns"]');
    assert.match(
      reload.get('[role="dialog"]').textContent,
      /portfolio.*Quantity.*INT32/s,
    );
    reload.click('[data-action="bind-provider"]');
    reload.click('[data-action="confirm-binding"]');
    const queries = reload.d.querySelectorAll('[data-action="use-source"]');
    queries[queries.length - 1].click();
    reload.click('[data-action="preview-stage"]');
    await finishPreview(reload);
    assert.equal(reload.rows(), 2);
    assert.match(
      reload.get('table[aria-label="Preview data"]').textContent,
      /Growth Fund/,
    );
  });

  test("CSV mapping errors identify the record and column and block saving until corrected", (t) => {
    const ui = workspace(t);
    ui.click('[data-action="navigate"][data-view="providers"]');
    ui.click('[data-action="register-provider"]');
    ui.field("#csv-provider-name", "Files.Checked");
    ui.field("#csv-file-name", "invalid.csv");
    ui.field("#csv-source-text", "Code,Quantity\n00123,bad\n00456,7", "input");
    ui.click('[data-action="csv-read"]');
    ui.click('[data-action="csv-map"]');
    ui.field('[data-csv-map="type"][data-index="1"]', "INT32");
    ui.click('[data-action="csv-preview"]');
    assert.match(
      ui.get('[aria-label="CSV mapping errors"]').textContent,
      /Source record 2, Quantity: Expected a whole number/,
    );
    assert.equal(ui.get('[data-action="csv-save"]').disabled, true);
    ui.field('[data-csv-map="type"][data-index="1"]', "STRING");
    ui.click('[data-action="csv-preview"]');
    assert.equal(ui.get('[data-action="csv-save"]').disabled, false);
    assert.match(
      ui.get('table[aria-label="Mapped CSV preview"]').textContent,
      /00123.*bad/s,
    );
  });

  test("CSV record selection supports headerless positional mapping and explicit preamble exclusion", (t) => {
    const ui = workspace(t);
    ui.click('[data-action="navigate"][data-view="providers"]');
    ui.click('[data-action="register-provider"]');
    ui.field("#csv-file-name", "no-header.csv");
    ui.field(
      "#csv-source-text",
      "Report export\n00123,5\n00456,8\nEnd of report",
      "input",
    );
    ui.click('[data-action="csv-read"]');
    ui.check("#csv-has-header", false);
    ui.field("#csv-first-record", "2");
    ui.field("#csv-last-record", "3");
    ui.click('[data-action="csv-map"]');
    assert.equal(ui.get("#csv-match-by").value, "position");
    ui.field(
      '[data-csv-map="outputName"][data-index="0"]',
      "identifier",
      "input",
    );
    ui.field('[data-csv-map="type"][data-index="1"]', "INT32");
    ui.click('[data-action="csv-preview"]');
    const output = ui.get('table[aria-label="Mapped CSV preview"]').textContent;
    assert.match(output, /00123.*5.*00456.*8/s);
    assert.doesNotMatch(output, /Report export|End of report/);
  });

  test("CSV mapping handles a real browser File and renders sample values as text", async (t) => {
    const ui = workspace(t);
    ui.click('[data-action="navigate"][data-view="providers"]');
    ui.click('[data-action="register-provider"]');
    const file = new ui.w.File(
      ['Name,Note\nCafé,"<img src=x onerror=alert(1)>"\n'],
      "uploaded.csv",
      { type: "text/csv" },
    );
    const input = ui.get("#csv-file");
    Object.defineProperty(input, "files", { value: [file] });
    input.dispatchEvent(new ui.w.Event("change", { bubbles: true }));
    const deadline = Date.now() + 3000;
    while (
      ui.get("#csv-file-name").value !== "uploaded.csv" &&
      Date.now() < deadline
    )
      await wait(15);
    assert.equal(ui.get("#csv-file-name").value, "uploaded.csv");
    ui.click('[data-action="csv-read"]');
    ui.click('[data-action="csv-map"]');
    ui.click('[data-action="csv-preview"]');
    assert.match(
      ui.get('table[aria-label="Mapped CSV preview"]').textContent,
      /Café.*<img src=x/s,
    );
    assert.equal(ui.d.querySelector('img[src="x"]'), null);
    ui.click('[data-action="csv-step"][data-step="1"]');
    assert.match(ui.get("#csv-source-text").value, /Café/);
  });

  test("saved CSV configurations reopen in the full page and retain column mappings", (t) => {
    const ui = workspace(t);
    ui.click('[data-action="navigate"][data-view="providers"]');
    ui.click('[data-action="register-provider"]');
    ui.click('[data-action="csv-example"]');
    ui.click('[data-action="csv-read"]');
    ui.click('[data-action="csv-map"]');
    ui.field(
      '[data-csv-map="outputName"][data-index="0"]',
      "accountName",
      "input",
    );
    ui.click('[data-action="csv-preview"]');
    ui.click('[data-action="csv-save"]');
    const provider = ui.saved().providers.find((p) => p.csvMapping);
    ui.click('[data-action="configure-csv"][data-id="' + provider.id + '"]');
    assert.equal(ui.d.querySelector('[role="dialog"]'), null);
    ui.click('[data-action="csv-read"]');
    ui.click('[data-action="csv-map"]');
    assert.equal(
      ui.get('[data-csv-map="outputName"][data-index="0"]').value,
      "accountName",
    );
  });

  test("changing file encoding rereads the selected CSV file", async (t) => {
    const ui = workspace(t);
    ui.click('[data-action="navigate"][data-view="providers"]');
    ui.click('[data-action="register-provider"]');
    const bytes = new Uint8Array([78, 97, 109, 101, 10, 67, 97, 102, 233, 10]);
    const input = ui.get("#csv-file");
    Object.defineProperty(input, "files", {
      value: [new ui.w.File([bytes], "legacy.csv")],
    });
    input.dispatchEvent(new ui.w.Event("change", { bubbles: true }));
    let deadline = Date.now() + 3000;
    while (
      ui.get("#csv-file-name").value !== "legacy.csv" &&
      Date.now() < deadline
    )
      await wait(15);
    ui.field("#csv-encoding", "windows-1252");
    deadline = Date.now() + 3000;
    while (
      !ui.get("#csv-source-text").value.includes("Café") &&
      Date.now() < deadline
    )
      await wait(15);
    assert.match(ui.get("#csv-source-text").value, /Café/);
    ui.click('[data-action="csv-read"]');
    ui.click('[data-action="csv-map"]');
    ui.click('[data-action="csv-preview"]');
    assert.match(
      ui.get('table[aria-label="Mapped CSV preview"]').textContent,
      /Café/,
    );
  });

  test("replacing a CSV sample reports missing mapped columns instead of choosing another field", (t) => {
    const ui = workspace(t);
    ui.click('[data-action="navigate"][data-view="providers"]');
    ui.click('[data-action="register-provider"]');
    ui.click('[data-action="csv-example"]');
    ui.click('[data-action="csv-read"]');
    ui.click('[data-action="csv-map"]');
    ui.click('[data-action="csv-step"][data-step="1"]');
    ui.field(
      "#csv-source-text",
      "Different,ISIN,Quantity,Price,Currency,Comment\nFund,ABC,1,2,USD,Note",
      "input",
    );
    ui.click('[data-action="csv-read"]');
    ui.click('[data-action="csv-map"]');
    assert.match(
      ui.get('[data-csv-map="sourceIndex"][data-index="0"]').textContent,
      /Missing: Account/,
    );
    ui.click('[data-action="csv-preview"]');
    assert.match(
      ui.get('[role="alert"]').textContent,
      /Missing source column “Account”/,
    );
    assert.equal(ui.get('[data-action="csv-save"]').disabled, true);
    ui.field('[data-csv-map="sourceIndex"][data-index="0"]', "0");
    ui.click('[data-action="csv-preview"]');
    assert.equal(ui.get('[data-action="csv-save"]').disabled, false);
  });

  test("configured SQL can be saved as a reusable view and queried after reload", async (t) => {
    const ui = workspace(t);
    ui.click('[data-action="navigate"][data-view="catalog"]');
    ui.click('[data-action="definition"][data-id="exposure-query"]');
    ui.click('[data-action="publish-view"]');
    ui.field("#definition-name", "Views.ReviewedExposure");
    ui.click('[data-action="config-section"][data-section="preview"]');
    assert.match(ui.get(".config-table-note").textContent, /12 sampled/);
    ui.click('[data-action="config-save"]');
    const view = ui
      .saved()
      .providers.find((p) => p.name === "Views.ReviewedExposure");
    assert.equal(view.columns.length, 7);
    ui.click('[data-action="provider-detail"][data-id="' + view.id + '"]');
    ui.click('[data-action="bind-provider"]');
    ui.field("#binding-name", "ReviewedExposure");
    ui.field("#binding-param-0", "100");
    ui.click('[data-action="confirm-binding"]');
    const queries = ui.d.querySelectorAll('[data-action="use-source"]');
    queries[queries.length - 1].click();
    ui.click('[data-action="preview-stage"]');
    await finishPreview(ui);
    assert.equal(ui.rows(), 10);
    ui.click('[data-action="save-version"]');
    ui.click('[data-action="confirm-save"]');
    const reload = workspace(t, ui.stored());
    reload.click('[data-action="preview-stage"]');
    await finishPreview(reload);
    assert.equal(reload.rows(), 10);
  });

  function newReader(ui, format, name) {
    ui.click('[data-action="navigate"][data-view="providers"]');
    ui.click('[data-action="config-new-provider"]');
    ui.field('[data-config-field="format"]', format);
    ui.field("#provider-name", name);
  }
  function createJdbcConnection(ui) {
    ui.click('[data-action="config-new-connection"]');
    ui.field("#new-source-name", "Local JDBC");
    ui.field("#new-source-location", "jdbc:sqlite:sample.db");
    ui.click('[data-action="confirm-source"]');
  }

  test("changing the source cannot silently reuse the original normalization fixture", async (t) => {
    const ui = workspace(t);
    ui.click('[data-action="select-stage"][data-id="load"]');
    ui.field("#source-binding-select", "instruments");
    ui.click('[data-action="select-stage"][data-id="normalize"]');
    ui.click('[data-action="preview-stage"]');
    await finishPreview(ui);
    assert.match(
      ui.get("#result-content").textContent,
      /portfolio.*not present/,
    );
    assert.equal(ui.d.querySelector('table[aria-label="Preview data"]'), null);
  });
  test("JSON mapping and queries use supplied records and enforce the 100-record source limit", async (t) => {
    const ui = workspace(t);
    newReader(ui, "JSON", "Files.CountedRows");
    ui.field('[data-config-field="fileName"]', "counted.json");
    ui.field(
      "#config-source-text",
      JSON.stringify(
        Array.from({ length: 150 }, (_, i) => ({
          account: "A" + i,
          quantity: i + 1,
        })),
      ),
      "input",
    );
    ui.click('[data-action="config-analyze"]');
    ui.field('[data-config-column="name"][data-index="1"]', "units");
    ui.click('[data-action="config-section"][data-section="preview"]');
    assert.match(ui.get(".config-table-note").textContent, /100 sampled/);
    assert.match(ui.get(".config-main").textContent, /100 of 150/);
    ui.click('[data-action="config-save"]');
    const provider = ui
      .saved()
      .providers.find((p) => p.name === "Files.CountedRows");
    assert.equal(provider.sourceConfiguration.mapping[1].name, "units");
    assert.equal(JSON.stringify(ui.saved()).includes('"quantity":150'), false);
    ui.click('[data-action="provider-detail"][data-id="' + provider.id + '"]');
    ui.click('[data-action="bind-provider"]');
    ui.field("#binding-name", "CountedRows");
    ui.click('[data-action="confirm-binding"]');
    const inputs = ui.d.querySelectorAll('[data-action="use-source"]');
    inputs[inputs.length - 1].click();
    ui.field(
      "#sql-editor",
      "SELECT account, units * 2 AS doubled FROM CountedRows WHERE units > 98 ORDER BY units",
      "input",
    );
    ui.click('[data-action="preview-stage"]');
    await finishPreview(ui);
    assert.equal(ui.rows(), 2);
    assert.match(ui.get('table[aria-label="Preview data"]').textContent, /198/);
    assert.match(ui.get(".results-message").textContent, /capped/);
  });
  test("XML settings, row selection and field conversion save and reopen in a full page", (t) => {
    const ui = workspace(t);
    newReader(ui, "XML", "Files.XmlPositions");
    ui.click('[data-action="config-example"]');
    ui.field('[data-config-setting="firstRecord"]', "2");
    ui.click('[data-action="config-analyze"]');
    ui.field('[data-config-column="type"][data-index="2"]', "INT32");
    ui.field('[data-config-column="name"][data-index="2"]', "quantity");
    ui.click('[data-action="config-section"][data-section="preview"]');
    assert.match(ui.get(".config-table-note").textContent, /1 sampled/);
    ui.click('[data-action="config-save"]');
    const provider = ui
      .saved()
      .providers.find((p) => p.name === "Files.XmlPositions");
    assert.equal(provider.columns[2].type, "INT32");
    ui.click('[data-action="config-provider"][data-id="' + provider.id + '"]');
    assert.equal(ui.get('[data-config-setting="firstRecord"]').value, "2");
    ui.click('[data-action="config-section"][data-section="fields"]');
    assert.equal(
      ui.get('[data-config-column="name"][data-index="2"]').value,
      "quantity",
    );
    assert.equal(ui.d.querySelector('[role="dialog"]'), null);
  });
  test("JDBC source configuration checks query parameters and retains recorded sample context", (t) => {
    const ui = workspace(t);
    newReader(ui, "JDBC", "Warehouse.RecordedPositions");
    createJdbcConnection(ui);
    ui.field('[data-config-setting="readMode"]', "query");
    ui.click('[data-action="config-example"]');
    ui.click('[data-action="config-analyze"]');
    assert.match(ui.get(".config-error").textContent, /parameter/);
    ui.click('[data-action="config-add-param"]');
    ui.field('[data-config-list="parameters"][data-key="type"]', "INT32");
    ui.field('[data-config-list="parameters"][data-key="value"]', "0");
    ui.click('[data-action="config-analyze"]');
    ui.click('[data-action="config-save"]');
    const provider = ui
      .saved()
      .providers.find((p) => p.name === "Warehouse.RecordedPositions");
    assert.ok(provider);
    assert.equal(
      provider.sourceConfiguration.settings.parameters[0].type,
      "INT32",
    );
    assert.equal(
      Object.hasOwn(
        provider.sourceConfiguration.settings.parameters[0],
        "value",
      ),
      false,
    );
    ui.click('[data-action="config-provider"][data-id="' + provider.id + '"]');
    assert.equal(
      ui.get('[data-config-list="parameters"][data-key="value"]').value,
      "0",
    );
    assert.match(
      ui.get(".config-main").textContent,
      /does not run this query or test a database connection/,
    );
  });
  test("HTTP configuration validates the recorded response before mapping its selected member", (t) => {
    const ui = workspace(t);
    newReader(ui, "HTTP JSON", "Market.RecordedRates");
    ui.click('[data-action="config-example"]');
    ui.field('[data-config-setting="status"]', "503");
    ui.click('[data-action="config-analyze"]');
    assert.match(ui.get(".config-error").textContent, /503/);
    ui.field('[data-config-setting="status"]', "200");
    ui.click('[data-action="config-analyze"]');
    ui.click('[data-action="config-section"][data-section="preview"]');
    assert.match(ui.get(".config-table-note").textContent, /6 sampled/);
    assert.match(ui.get(".config-main").textContent, /GET https:/);
    ui.click('[data-action="config-save"]');
    assert.ok(
      ui.saved().providers.find((p) => p.name === "Market.RecordedRates"),
    );
  });
  test("saved field mappings can be applied to output and serialize the renamed values", (t) => {
    const ui = workspace(t);
    ui.click('[data-action="navigate"][data-view="catalog"]');
    ui.click('[data-action="new-definition"]');
    ui.field('[data-config-field="definitionKind"]', "mapping");
    ui.field("#definition-name", "Account output");
    ui.click('[data-action="config-section"][data-section="fields"]');
    ui.field('[data-config-column="name"][data-index="0"]', "account");
    ui.click('[data-action="config-save"]');
    const mapping = ui
      .saved()
      .definitions.find((d) => d.name === "Account output");
    assert.ok(mapping.mapping);
    ui.click('[data-action="navigate"][data-view="workspace"]');
    ui.click('[data-action="select-stage"][data-id="output"]');
    ui.click('[data-action="config-output"]');
    ui.field('[data-config-field="input"]', "Holdings");
    ui.field('[data-config-setting="format"]', "JSON");
    ui.field('[data-config-setting="mappingId"]', mapping.id);
    ui.click('[data-action="config-section"][data-section="preview"]');
    const rows = JSON.parse(ui.get(".config-serialized").textContent);
    assert.equal(rows[0].account, D.holdings[0].portfolio);
    assert.equal(Object.hasOwn(rows[0], "portfolio"), false);
  });
  test("transformation rules can be configured, saved, loaded into a stage and previewed", async (t) => {
    const ui = workspace(t);
    ui.click('[data-action="navigate"][data-view="catalog"]');
    ui.click('[data-action="new-definition"]');
    ui.field('[data-config-field="definitionKind"]', "transform");
    ui.field("#definition-name", "Uppercase portfolios");
    ui.click('[data-action="config-section"][data-section="fields"]');
    ui.click('[data-action="config-add-rule"]');
    ui.field('[data-config-list="rules"][data-key="operation"]', "upper");
    ui.click('[data-action="config-save"]');
    const definition = ui
      .saved()
      .definitions.find((d) => d.name === "Uppercase portfolios");
    ui.click('[data-action="navigate"][data-view="workspace"]');
    ui.click('[data-action="select-stage"][data-id="normalize"]');
    ui.click('[data-action="config-stage"]');
    ui.field('[data-config-setting="reuseDefinitionId"]', definition.id);
    ui.click('[data-action="config-save"]');
    ui.click('[data-action="preview-stage"]');
    await finishPreview(ui);
    assert.equal(ui.rows(), 12);
    assert.match(
      ui.get('table[aria-label="Preview data"]').textContent,
      new RegExp(D.holdings[0].portfolio.toUpperCase()),
    );
  });
  test("validation rules report actual invalid records and enforce the saved rejection budget", async (t) => {
    const ui = workspace(t);
    ui.click('[data-action="config-runtime"]');
    ui.field('[data-config-setting="policy"]', "skip");
    ui.field('[data-config-setting="maxRejected"]', "20");
    ui.click('[data-action="config-save"]');
    ui.click('[data-action="select-stage"][data-id="validate"]');
    ui.click('[data-action="config-stage"]');
    ui.click('[data-action="config-section"][data-section="fields"]');
    ui.click('[data-action="config-add-rule"]');
    ui.field(
      '[data-config-list="rules"][data-index="4"][data-key="field"]',
      "units",
    );
    ui.field(
      '[data-config-list="rules"][data-index="4"][data-key="operation"]',
      "min",
    );
    ui.field(
      '[data-config-list="rules"][data-index="4"][data-key="value"]',
      "1000",
    );
    ui.click('[data-action="config-section"][data-section="preview"]');
    assert.match(ui.get(".config-main").textContent, /rejected/);
    assert.match(ui.get(".config-diagnostics").textContent, /below 1000/);
    ui.click('[data-action="config-save"]');
    ui.click('[data-action="preview-stage"]');
    await finishPreview(ui);
    assert.ok(ui.rows() < 12);
    assert.ok(ui.rows() > 0);
  });
  test("JDBC output offers APPEND only and checks actual destination columns", (t) => {
    const ui = workspace(t);
    ui.click('[data-action="select-stage"][data-id="output"]');
    ui.click('[data-action="config-output"]');
    ui.field('[data-config-setting="format"]', "JDBC");
    createJdbcConnection(ui);
    const modes = [...ui.get('[data-config-setting="mode"]').options].map(
      (o) => o.value,
    );
    assert.deepEqual(modes, ["APPEND"]);
    ui.click('[data-action="config-section"][data-section="preview"]');
    assert.match(
      ui.get(".config-error").textContent,
      /destination table columns/,
    );
    ui.click('[data-action="config-section"][data-section="settings"]');
    ui.click('[data-action="config-destination-from-input"]');
    ui.click(
      '[data-action="config-add-field"][data-list="destinationColumns"]',
    );
    ui.field(
      '[data-config-column="name"][data-destination="true"][data-index="7"]',
      "requiredColumn",
    );
    ui.check(
      '[data-config-column="nullable"][data-destination="true"][data-index="7"]',
      false,
    );
    ui.click('[data-action="config-section"][data-section="preview"]');
    assert.match(
      ui.get(".config-error").textContent,
      /Required destination column/,
    );
    ui.click('[data-action="config-section"][data-section="settings"]');
    ui.check(
      '[data-config-column="hasDefault"][data-destination="true"][data-index="7"]',
      true,
    );
    ui.click('[data-action="config-section"][data-section="preview"]');
    assert.match(
      ui.get(".config-serialized").textContent,
      /INSERT INTO.*positions/s,
    );
    assert.match(
      ui.get(".config-serialized").textContent,
      /No database writes/,
    );
  });

  test("new pipelines expose source wiring and can start with source and output only", async (t) => {
    const ui = workspace(t);
    ui.click('[data-action="navigate"][data-view="pipelines"]');
    ui.click('[data-action="new-pipeline"]');
    assert.equal(ui.d.querySelector('[role="dialog"]'), null);
    ui.field('[data-config-field="startTemplate"]', "empty");
    ui.field("#new-pipeline-name", "Instrument delivery");
    ui.field('[data-config-pipeline="bindingId"]', "instruments");
    ui.click('[data-action="config-preview"]');
    assert.match(ui.get(".config-table-note").textContent, /10 sampled/);
    ui.click('[data-action="config-save"]');
    assert.equal(ui.d.querySelectorAll(".stage-card").length, 2);
    ui.click('[data-action="run-pipeline"]');
    await finishPreview(ui);
    assert.equal(ui.rows(), 10);
    assert.equal(ui.saved().name, "Instrument delivery");
  });
  test("SQL configuration enforces declared inputs and typed parameters before stage save", async (t) => {
    const ui = workspace(t);
    ui.click('[data-action="config-stage"]');
    ui.click('[data-action="config-section"][data-section="fields"]');
    ui.field(
      "#definition-content",
      "SELECT i.name FROM Instruments i WHERE i.isin = ?",
    );
    ui.field(
      '[data-config-list="parameters"][data-key="name"]',
      "instrumentId",
    );
    ui.field('[data-config-list="parameters"][data-key="type"]', "STRING");
    ui.field(
      '[data-config-list="parameters"][data-key="value"]',
      "US0001000001",
    );
    ui.click('[data-action="config-section"][data-section="preview"]');
    assert.match(ui.get(".config-table-note").textContent, /1 sampled/);
    ui.click('[data-action="config-save"]');
    ui.click('[data-action="preview-stage"]');
    await finishPreview(ui);
    assert.equal(ui.rows(), 1);
    assert.match(
      ui.get('table[aria-label="Preview data"]').textContent,
      /Aster Technologies/,
    );
    savePipeline(ui);
    const typedReload = workspace(t, ui.stored());
    assert.equal(typedReload.rows(), 1);
    assert.match(
      typedReload.get('table[aria-label="Preview data"]').textContent,
      /Aster Technologies/,
    );
    ui.click('[data-action="config-stage"]');
    ui.check('[data-config-query-input="Instruments"]', false);
    ui.click('[data-action="config-section"][data-section="preview"]');
    assert.match(
      ui.get(".config-error").textContent,
      /Declare query input Instruments/,
    );
  });
  test("schemas can be authored independently of available source samples", (t) => {
    const ui = workspace(t);
    ui.click('[data-action="navigate"][data-view="catalog"]');
    ui.click('[data-action="new-definition"]');
    ui.field("#definition-name", "Client contract");
    ui.click('[data-action="config-section"][data-section="fields"]');
    ui.click('[data-action="config-add-field"]');
    ui.field('[data-config-column="name"]', "clientId");
    ui.check('[data-config-column="nullable"]', false);
    ui.click('[data-action="config-save"]');
    const schema = ui
      .saved()
      .definitions.find((d) => d.name === "Client contract");
    assert.equal(schema.schema[0].name, "clientId");
    assert.equal(schema.schema[0].nullable, false);
  });

  function savePipeline(ui) {
    ui.click('[data-action="save-version"]');
    ui.click('[data-action="confirm-save"]');
  }
  function inspectBinding(ui, binding) {
    ui.click('[data-action="explorer-tab"][data-tab="pipeline"]');
    const detail =
      '[data-action="source-detail"][data-id="' + binding.id + '"]';
    if (!ui.d.querySelector(detail))
      ui.click('[data-action="source-toggle"][data-id="' + binding.id + '"]');
    ui.click(detail);
  }

  for (const format of ["CSV", "JSON", "XML", "JDBC", "HTTP JSON"])
    test(
      "reloading a " +
        format +
        " sample restores its existing captured binding without changing configuration",
      async (t) => {
        const ui = workspace(t),
          name = "Files.Reload" + format.replace(/\W/g, ""),
          body =
            format === "CSV"
              ? "Account,Quantity\nFirst,3\nSecond,5\n"
              : D.configurationSamples[format].text;
        const fileName =
          format === "CSV"
            ? "reload.csv"
            : D.configurationSamples[format].fileName;
        if (format === "CSV") {
          ui.click('[data-action="navigate"][data-view="providers"]');
          ui.click('[data-action="register-provider"]');
          ui.field("#csv-provider-name", name);
          ui.field("#csv-file-name", fileName);
          ui.field("#csv-source-text", body, "input");
          ui.click('[data-action="csv-read"]');
          ui.click('[data-action="csv-map"]');
          ui.field('[data-csv-map="type"][data-index="1"]', "INT32");
          ui.click('[data-action="csv-preview"]');
          ui.click('[data-action="csv-save"]');
        } else {
          newReader(ui, format, name);
          ui.field('[data-config-field="fileName"]', fileName);
          ui.field("#config-source-text", body, "input");
          if (format === "JDBC") {
            createJdbcConnection(ui);
            ui.field('[data-config-setting="readMode"]', "query");
            ui.click('[data-action="config-add-param"]');
            ui.field(
              '[data-config-list="parameters"][data-key="name"]',
              "minimum",
            );
            ui.field(
              '[data-config-list="parameters"][data-key="type"]',
              "INT32",
            );
            ui.field('[data-config-list="parameters"][data-key="value"]', "0");
          }
          if (format === "HTTP JSON") {
            ui.click(
              '[data-action="config-add-pair"][data-list="queryParams"]',
            );
            ui.field(
              '[data-config-pair="queryParams"][data-key="name"]',
              "date",
            );
            ui.field(
              '[data-config-pair="queryParams"][data-key="value"]',
              "2026-11-23",
            );
          }
          ui.click('[data-action="config-analyze"]');
          ui.click('[data-action="config-save"]');
        }
        let provider = ui.saved().providers.find((p) => p.name === name);
        assert.ok(provider);
        ui.click(
          '[data-action="provider-detail"][data-id="' + provider.id + '"]',
        );
        ui.click('[data-action="bind-provider"]');
        ui.field("#binding-name", "ReloadedData");
        if (format === "JDBC") ui.field("#binding-param-0", "0");
        if (format === "HTTP JSON") ui.field("#binding-param-0", "2026-11-23");
        ui.click('[data-action="confirm-binding"]');
        // Keep a binding to an older captured contract while the catalogue's current reader changes.
        if (format === "JSON") {
          ui.click('[data-action="navigate"][data-view="providers"]');
          ui.click(
            '[data-action="config-provider"][data-id="' + provider.id + '"]',
          );
          ui.click('[data-action="config-section"][data-section="fields"]');
          ui.field('[data-config-column="name"][data-index="0"]', "newField");
          ui.click('[data-action="config-save"]');
          ui.click('[data-action="navigate"][data-view="workspace"]');
        }
        // The newly bound dataset is expanded by Save binding; select it explicitly before persisting.
        ui.click('[data-action="explorer-tab"][data-tab="pipeline"]');
        ui.click(
          '[data-action="use-source"][data-id="' +
            ui.d
              .querySelectorAll('[data-action="source-detail"]')
              .item(
                ui.d.querySelectorAll('[data-action="source-detail"]').length -
                  1,
              ).dataset.id +
            '"]',
        );
        savePipeline(ui);
        const binding = ui
            .saved()
            .bindings.find((b) => b.name === "ReloadedData"),
          before = ui.saved();
        const reload = workspace(t, ui.stored());
        reload.click('[data-action="preview-stage"]');
        await finishPreview(reload);
        assert.match(
          reload.get("#result-content").textContent,
          /Reload.*sample/i,
        );
        inspectBinding(reload, binding);
        reload.click('[role="dialog"] [data-action="reload-provider-sample"]');
        assert.equal(reload.d.querySelector('[role="dialog"]'), null);
        assert.match(reload.get("h1").textContent, /Reload source sample/);
        assert.equal(reload.d.querySelector("[data-config-column]"), null);
        if (format === "JSON") {
          const file = new reload.w.File([body], fileName, {
              type: "application/json",
            }),
            input = reload.get("#config-sample-file");
          Object.defineProperty(input, "files", { value: [file] });
          input.dispatchEvent(new reload.w.Event("change", { bubbles: true }));
          const deadline = Date.now() + 3000;
          while (
            !reload.get("#config-source-text").value &&
            Date.now() < deadline
          )
            await wait(15);
          assert.equal(reload.get("#config-source-text").value, body);
        } else reload.field("#config-source-text", body, "input");
        reload.click('[data-action="config-section"][data-section="preview"]');
        assert.equal(reload.d.querySelector(".config-error"), null);
        reload.click('[data-action="config-save"]');
        reload.click('[data-action="preview-stage"]');
        await finishPreview(reload);
        const expected = { CSV: 2, JSON: 10, XML: 2, JDBC: 12, "HTTP JSON": 6 }[
          format
        ];
        assert.equal(reload.rows(), expected);
        assert.deepEqual(reload.saved().providers, before.providers);
        assert.deepEqual(reload.saved().bindings, before.bindings);
        assert.deepEqual(reload.saved().definitions, before.definitions);
        if (format === "CSV")
          assert.match(
            reload.get('table[aria-label="Preview data"]').textContent,
            /First.*3/s,
          );
      },
    );

  test("a saved schema retains source selections and field order through repeated provider preview and save", (t) => {
    const schema = {
      id: "counts",
      name: "Count schema",
      kind: "Schema",
      version: 1,
      schema: [
        { name: "count", type: "INT32", nullable: false, min: 0 },
        { name: "label", type: "STRING", nullable: false },
      ],
      content: "count INT32 required\nlabel STRING required",
    };
    const ui = workspace(t, JSON.stringify({ definitions: [schema] }));
    newReader(ui, "JSON", "Files.SchemaMapping");
    ui.field('[data-config-field="fileName"]', "counts.json");
    ui.field(
      "#config-source-text",
      '[{"qty":"3","rawLabel":"Three"}]',
      "input",
    );
    ui.field('[data-config-setting="schemaId"]', "counts");
    ui.click('[data-action="config-analyze"]');
    ui.field('[data-config-column="source"][data-index="0"]', "qty");
    ui.field('[data-config-column="source"][data-index="1"]', "rawLabel");
    ui.click(
      '[data-action="config-move-row"][data-list="fields"][data-index="1"][data-direction="-1"]',
    );
    for (let n = 0; n < 2; n++) {
      ui.click('[data-action="config-section"][data-section="preview"]');
      assert.equal(ui.d.querySelector(".config-error"), null);
      assert.match(
        ui.get('table[aria-label="Configured preview"]').textContent,
        /Three.*3/s,
      );
      ui.click('[data-action="config-section"][data-section="fields"]');
      assert.equal(
        ui.get('[data-config-column="source"][data-index="0"]').value,
        "rawLabel",
      );
    }
    ui.click('[data-action="config-save"]');
    const provider = ui
      .saved()
      .providers.find((p) => p.name === "Files.SchemaMapping");
    assert.deepEqual(
      provider.sourceConfiguration.mapping.map((f) => [f.source, f.name]),
      [
        ["rawLabel", "label"],
        ["qty", "count"],
      ],
    );
    assert.deepEqual(
      ui.saved().definitions.find((d) => d.id === "counts"),
      schema,
    );
  });

  test("full-pipeline results retain the prepared output mapping and selected output input", async (t) => {
    const ui = workspace(t);
    ui.click('[data-action="config-pipeline"]');
    ui.click('[data-action="config-save"]');
    ui.click('[data-action="select-stage"][data-id="output"]');
    ui.click('[data-action="config-output"]');
    ui.click('[data-action="config-section"][data-section="fields"]');
    ui.field('[data-config-column="name"][data-index="0"]', "account");
    ui.check('[data-config-column="include"][data-index="1"]', false);
    ui.click('[data-action="config-save"]');
    ui.click('[data-action="preview-stage"]');
    await finishPreview(ui);
    const stage = ui.get('table[aria-label="Preview data"]').textContent;
    assert.match(stage, /account/);
    ui.click('[data-action="run-pipeline"]');
    await finishPreview(ui);
    assert.equal(ui.get('table[aria-label="Preview data"]').textContent, stage);
    ui.click('[data-action="config-output"]');
    ui.field('[data-config-field="input"]', "Instruments");
    ui.click('[data-action="config-section"][data-section="fields"]');
    // Clear the previous input's fields and explicitly map the new dataset.
    while (
      ui.d.querySelector(
        '[data-action="config-remove-row"][data-list="fields"]',
      )
    )
      ui.click('[data-action="config-remove-row"][data-list="fields"]');
    ui.click('[data-action="config-section"][data-section="settings"]');
    ui.click('[data-action="config-section"][data-section="fields"]');
    ui.click('[data-action="config-save"]');
    savePipeline(ui);
    assert.equal(ui.saved().finalResult, "Instruments");
    ui.click('[data-action="run-pipeline"]');
    await finishPreview(ui);
    assert.equal(ui.rows(), 10);
  });

  for (const keyboard of [false, true])
    test(
      "expanded SQL applies only to its selected stage using " +
        (keyboard ? "Ctrl+Enter" : "Apply query"),
      async (t) => {
        const ui = workspace(t);
        ui.click('[data-action="add-stage"]');
        ui.click('[data-action="choose-stage-type"][data-kind="SQL query"]');
        const previous = ui.get("#sql-editor").value;
        ui.click('[data-action="expand-editor"]');
        assert.equal(ui.get("#expanded-sql").value, previous);
        ui.field("#expanded-sql", "SELECT name FROM Instruments");
        if (keyboard) {
          ui.w.document.dispatchEvent(
            new ui.w.KeyboardEvent("keydown", {
              key: "Enter",
              ctrlKey: true,
              bubbles: true,
            }),
          );
          await finishPreview(ui);
        } else {
          ui.click('[data-action="apply-expanded-sql"]');
          ui.click('[data-action="preview-stage"]');
          await finishPreview(ui);
        }
        assert.equal(
          ui.get("#sql-editor").value,
          "SELECT name FROM Instruments",
        );
        assert.equal(ui.rows(), 10);
        savePipeline(ui);
        assert.equal(ui.saved().sql, D.sql);
        assert.equal(
          ui.saved().stages.find((s) => s.id.startsWith("stage-")).sql,
          "SELECT name FROM Instruments",
        );
        ui.field("#sql-editor", "SELECT name FROM Instruments   \n", "input");
        ui.click('[data-action="format-query"]');
        assert.equal(
          ui.get("#sql-editor").value,
          "SELECT name FROM Instruments\n",
        );
        ui.click('[data-action="reset-query"]');
        savePipeline(ui);
        assert.equal(ui.saved().sql, D.sql);
        assert.equal(
          ui.saved().stages.find((s) => s.id.startsWith("stage-")).sql,
          D.sql,
        );
      },
    );

  test("saving an edited pipeline refreshes the reusable definition reopened from the library", (t) => {
    const ui = workspace(t);
    ui.click('[data-action="navigate"][data-view="pipelines"]');
    ui.click('[data-action="new-pipeline"]');
    ui.field("#new-pipeline-name", "Reusable pipeline");
    ui.click('[data-action="config-save"]');
    ui.click('[data-action="select-stage"][data-id="normalize"]');
    ui.click('[data-action="config-stage"]');
    ui.click('[data-action="config-section"][data-section="fields"]');
    ui.field(
      '[data-config-list="rules"][data-index="0"][data-key="operation"]',
      "upper",
    );
    ui.click('[data-action="config-save"]');
    ui.click('[data-action="select-stage"][data-id="load"]');
    ui.field("#panel-stage-name", "Read reviewed input");
    savePipeline(ui);
    const saved = ui.saved(),
      definition = saved.definitions.find((d) => d.id === saved.pipelineId),
      snapshot = JSON.parse(definition.content);
    assert.deepEqual(snapshot.stages, saved.stages);
    assert.deepEqual(snapshot.bindings, saved.bindings);
    assert.deepEqual(snapshot.output, saved.output);
    const reload = workspace(t, ui.stored());
    reload.click('[data-action="navigate"][data-view="catalog"]');
    reload.click('[data-action="definition"][data-id="' + definition.id + '"]');
    reload.click('[data-action="revise-definition"]');
    assert.match(
      reload.get('table[aria-label="Pipeline inputs and outputs"]').textContent,
      /Read reviewed input/,
    );
    reload.click('[data-action="config-save"]');
    reload.click('[data-action="select-stage"][data-id="normalize"]');
    reload.click('[data-action="config-stage"]');
    reload.click('[data-action="config-section"][data-section="fields"]');
    assert.equal(
      reload.get(
        '[data-config-list="rules"][data-index="0"][data-key="operation"]',
      ).value,
      "upper",
    );
  });

  test("invalid CSV null tokens block prepared output and saving its configuration", (t) => {
    const ui = workspace(t);
    ui.click('[data-action="select-stage"][data-id="output"]');
    ui.click('[data-action="config-output"]');
    ui.field('[data-config-setting="nullToken"]', "missing,value");
    ui.click('[data-action="config-save"]');
    assert.match(ui.get(".config-error").textContent, /CSV null token/);
    assert.equal(ui.d.querySelector(".config-serialized"), null);
    assert.equal(ui.d.querySelector('[data-action="config-download"]'), null);
    ui.field('[data-config-setting="nullToken"]', "NULL");
    ui.click('[data-action="config-section"][data-section="preview"]');
    assert.match(ui.get(".config-serialized").textContent, /NULL/);
    assert.equal(ui.d.querySelector(".config-error"), null);
  });

  test("failed and cancelled sample replacements retain the last accepted records", async (t) => {
    const ui = workspace(t);
    newReader(ui, "JSON", "Files.RetainedSample");
    ui.field('[data-config-field="fileName"]', "sample.json");
    ui.field("#config-source-text", '[{"count":1}]', "input");
    ui.click('[data-action="config-analyze"]');
    ui.click('[data-action="config-save"]');
    const provider = ui
      .saved()
      .providers.find((p) => p.name === "Files.RetainedSample");
    ui.click('[data-action="provider-detail"][data-id="' + provider.id + '"]');
    ui.click('[data-action="bind-provider"]');
    ui.field("#binding-name", "RetainedSample");
    ui.click('[data-action="confirm-binding"]');
    const use = [...ui.d.querySelectorAll('[data-action="use-source"]')].at(-1);
    use.click();
    savePipeline(ui);
    const before = ui.saved(),
      binding = before.bindings.find((b) => b.name === "RetainedSample");
    inspectBinding(ui, binding);
    ui.click('[role="dialog"] [data-action="reload-provider-sample"]');
    assert.equal(ui.get("#config-source-text").value, '[{"count":1}]');
    ui.field("#config-source-text", '[{"count":9}]', "input");
    ui.click('[data-action="config-save"]');
    ui.click('[data-action="preview-stage"]');
    await finishPreview(ui);
    assert.match(ui.get('table[aria-label="Preview data"]').textContent, /9/);
    inspectBinding(ui, binding);
    ui.click('[role="dialog"] [data-action="reload-provider-sample"]');
    ui.field("#config-source-text", '[{"count":"bad"}]', "input");
    ui.click('[data-action="config-save"]');
    assert.match(ui.get(".config-error").textContent, /Record 1.*count/);
    ui.click('[data-action="config-back"]');
    ui.click('[data-action="preview-stage"]');
    await finishPreview(ui);
    assert.match(ui.get('table[aria-label="Preview data"]').textContent, /9/);
    inspectBinding(ui, binding);
    ui.click('[role="dialog"] [data-action="reload-provider-sample"]');
    ui.field("#config-source-text", '[{"count":10}]', "input");
    ui.click('[data-action="config-section"][data-section="preview"]');
    assert.match(ui.get(".config-table-note").textContent, /1 sampled/);
    ui.click('[data-action="config-back"]');
    ui.click('[data-action="preview-stage"]');
    await finishPreview(ui);
    assert.match(ui.get('table[aria-label="Preview data"]').textContent, /9/);
    assert.deepEqual(ui.saved().providers, before.providers);
    assert.deepEqual(ui.saved().bindings, before.bindings);
  });

  test("recorded JDBC samples remain distinct for bindings with different query parameters", async (t) => {
    const ui = workspace(t);
    newReader(ui, "JDBC", "Warehouse.ParameterSamples");
    createJdbcConnection(ui);
    ui.field('[data-config-setting="readMode"]', "query");
    ui.click('[data-action="config-add-param"]');
    ui.field('[data-config-list="parameters"][data-key="name"]', "minimum");
    ui.field('[data-config-list="parameters"][data-key="type"]', "INT32");
    ui.field('[data-config-list="parameters"][data-key="value"]', "0");
    ui.field("#config-source-text", '[{"label":"Initial","units":0}]', "input");
    ui.click('[data-action="config-analyze"]');
    ui.click('[data-action="config-save"]');
    const provider = ui
      .saved()
      .providers.find((p) => p.name === "Warehouse.ParameterSamples");
    for (const [name, value] of [
      ["SampleZero", "0"],
      ["SampleOne", "1"],
    ]) {
      ui.click('[data-action="navigate"][data-view="providers"]');
      ui.click(
        '[data-action="provider-detail"][data-id="' + provider.id + '"]',
      );
      ui.click('[data-action="bind-provider"]');
      ui.field("#binding-name", name);
      ui.field("#binding-alias", name.toLowerCase());
      ui.field("#binding-param-0", value);
      ui.click('[data-action="confirm-binding"]');
    }
    savePipeline(ui);
    const reload = workspace(t, ui.stored()),
      bindings = ui.saved().bindings.filter((b) => b.name.startsWith("Sample"));
    for (const binding of bindings) {
      inspectBinding(reload, binding);
      reload.click('[role="dialog"] [data-action="reload-provider-sample"]');
      reload.field(
        "#config-source-text",
        JSON.stringify([
          {
            label: "For " + binding.parameters.minimum,
            units: binding.parameters.minimum,
          },
        ]),
        "input",
      );
      reload.click('[data-action="config-save"]');
    }
    for (const binding of bindings) {
      reload.click('[data-action="use-source"][data-id="' + binding.id + '"]');
      reload.click('[data-action="preview-stage"]');
      await finishPreview(reload);
      assert.equal(reload.rows(), 1);
      assert.match(
        reload.get('table[aria-label="Preview data"]').textContent,
        new RegExp("For " + binding.parameters.minimum),
      );
    }
    assert.equal(reload.stored().includes("For 0"), false);
    assert.equal(reload.stored().includes("For 1"), false);
  });

  test("editing a different saved pipeline preserves both pipeline identities and their saved configurations", (t) => {
    const ui = workspace(t);
    ui.click('[data-action="navigate"][data-view="pipelines"]');
    ui.click('[data-action="new-pipeline"]');
    ui.field("#new-pipeline-name", "Pipeline A");
    ui.click('[data-action="config-save"]');
    ui.click('[data-action="select-stage"][data-id="load"]');
    ui.field("#panel-stage-name", "Source for A");
    savePipeline(ui);
    const a = ui.saved().pipelineId,
      aVersion = ui.saved().version;
    ui.click('[data-action="navigate"][data-view="pipelines"]');
    ui.click('[data-action="new-pipeline"]');
    ui.field('[data-config-field="startTemplate"]', "empty");
    ui.field("#new-pipeline-name", "Pipeline B");
    ui.field('[data-config-pipeline="bindingId"]', "instruments");
    ui.click('[data-action="config-save"]');
    const b = ui.saved().pipelineId,
      beforeB = ui.saved().definitions.find((d) => d.id === b);
    assert.notEqual(a, b);
    ui.click('[data-action="navigate"][data-view="catalog"]');
    ui.click('[data-action="definition"][data-id="' + a + '"]');
    ui.click('[data-action="revise-definition"]');
    ui.field("#new-pipeline-name", "Reviewed pipeline A");
    ui.click('[data-action="config-save"]');
    ui.field("#engine-select", "SQLite");
    assert.equal(ui.saved().pipelineId, a);
    assert.equal(ui.saved().version, aVersion);
    assert.deepEqual(
      ui.saved().definitions.find((d) => d.id === b),
      beforeB,
    );
    savePipeline(ui);
    assert.equal(ui.saved().pipelineId, a);
    assert.equal(ui.saved().version, aVersion + 1);
    assert.equal(
      ui.saved().definitions.find((d) => d.id === a).name,
      "Reviewed pipeline A",
    );
    assert.deepEqual(
      ui.saved().definitions.find((d) => d.id === b),
      beforeB,
    );
    assert.equal(
      ui
        .saved()
        .snapshots.filter((s) => !s.definitionId)
        .at(-1).pipelineId,
      a,
    );
    const afterA = ui.saved().definitions.find((d) => d.id === a),
      reload = workspace(t, ui.stored());
    reload.click('[data-action="navigate"][data-view="catalog"]');
    reload.click('[data-action="definition"][data-id="' + b + '"]');
    reload.click('[data-action="revise-definition"]');
    assert.equal(
      reload.d.querySelectorAll(
        'table[aria-label="Pipeline inputs and outputs"] tbody tr',
      ).length,
      2,
    );
    reload.click('[data-action="config-save"]');
    savePipeline(reload);
    assert.equal(reload.saved().pipelineId, b);
    assert.deepEqual(
      reload.saved().definitions.find((d) => d.id === a),
      afterA,
    );
    assert.equal(
      JSON.parse(reload.saved().definitions.find((d) => d.id === b).content)
        .stages[0].bindingId,
      "instruments",
    );
  });

  test("the initial pipeline can be reopened from its saved snapshot while a different pipeline is active", (t) => {
    const ui = workspace(t);
    ui.click('[data-action="navigate"][data-view="pipelines"]');
    ui.click('[data-action="new-pipeline"]');
    ui.field('[data-config-field="startTemplate"]', "empty");
    ui.field("#new-pipeline-name", "Another pipeline");
    ui.click('[data-action="config-save"]');
    const id = ui.saved().pipelineId,
      before = ui.saved().definitions.find((d) => d.id === id);
    ui.click('[data-action="navigate"][data-view="catalog"]');
    ui.click('[data-action="definition"][data-id="daily-holdings"]');
    ui.click('[data-action="revise-definition"]');
    assert.equal(
      ui.d.querySelectorAll(
        'table[aria-label="Pipeline inputs and outputs"] tbody tr',
      ).length,
      6,
    );
    ui.click('[data-action="config-save"]');
    savePipeline(ui);
    assert.equal(ui.saved().pipelineId, "daily-holdings");
    assert.deepEqual(
      ui.saved().definitions.find((d) => d.id === id),
      before,
    );
  });

  test("same-named parameters stay local to each SQL stage through all previews and reload", async (t) => {
    const ui = workspace(t);
    ui.field('[name="minimum-units"]', "100");
    ui.click('[data-action="add-stage"]');
    ui.click('[data-action="choose-stage-type"][data-kind="SQL query"]');
    const stageId = ui.get(".stage-card.active").dataset.id;
    ui.click('[data-action="config-stage"]');
    const alias = ui.get('[data-config-field="output"]').value;
    ui.click('[data-action="config-section"][data-section="fields"]');
    ui.field(
      "#definition-content",
      "SELECT * FROM PortfolioExposure WHERE units >= ?",
    );
    ui.click('[data-action="config-add-param"]');
    ui.field(
      '[data-config-list="parameters"][data-key="name"]',
      "minimumUnits",
    );
    ui.field('[data-config-list="parameters"][data-key="type"]', "INT32");
    ui.field('[data-config-list="parameters"][data-key="value"]', "1000");
    ui.click('[data-action="config-section"][data-section="preview"]');
    assert.match(ui.get(".config-table-note").textContent, /3 sampled/);
    ui.click('[data-action="config-save"]');
    assert.equal(ui.get('[name="minimum-units"]').value, "1000");
    ui.click('[data-action="preview-stage"]');
    await finishPreview(ui);
    assert.equal(ui.rows(), 3);
    ui.click('[data-action="select-stage"][data-id="validate"]');
    ui.click('[data-action="config-stage"]');
    ui.field('[data-config-field="input"]', alias);
    ui.click('[data-action="config-save"]');
    ui.click('[data-action="config-pipeline"]');
    ui.click('[data-action="config-preview"]');
    assert.match(ui.get(".config-table-note").textContent, /3 sampled/);
    ui.click('[data-action="config-save"]');
    ui.click('[data-action="run-pipeline"]');
    await finishPreview(ui);
    assert.equal(ui.rows(), 3);
    ui.click('[data-action="select-stage"][data-id="' + stageId + '"]');
    ui.field('[name="minimum-units"]', "500");
    ui.click('[data-action="preview-stage"]');
    await finishPreview(ui);
    assert.equal(ui.rows(), 6);
    ui.click('[data-action="select-stage"][data-id="query"]');
    assert.equal(ui.get('[name="minimum-units"]').value, "100");
    ui.click('[data-action="preview-stage"]');
    await finishPreview(ui);
    assert.equal(ui.rows(), 10);
    savePipeline(ui);
    const reload = workspace(t, ui.stored());
    assert.equal(reload.rows(), 10);
    reload.click('[data-action="select-stage"][data-id="' + stageId + '"]');
    assert.equal(reload.get('[name="minimum-units"]').value, "500");
    reload.click('[data-action="preview-stage"]');
    await finishPreview(reload);
    assert.equal(reload.rows(), 6);
  });

  test("a parameter named minimumUnits can use its declared STRING type without a numeric global override", async (t) => {
    const ui = workspace(t);
    ui.click('[data-action="config-stage"]');
    ui.check('[data-config-query-input="Holdings"]', true);
    ui.click('[data-action="config-section"][data-section="fields"]');
    ui.field(
      "#definition-content",
      "SELECT currency FROM Holdings WHERE currency = ?",
    );
    ui.field('[data-config-list="parameters"][data-key="type"]', "STRING");
    ui.field('[data-config-list="parameters"][data-key="value"]', "USD");
    ui.click('[data-action="config-save"]');
    assert.equal(ui.d.querySelector('[name="minimum-units"]'), null);
    ui.click('[data-action="preview-stage"]');
    await finishPreview(ui);
    assert.equal(
      ui.rows(),
      D.holdings.filter((row) => row.currency === "USD").length,
    );
    savePipeline(ui);
    const reload = workspace(t, ui.stored());
    assert.equal(reload.rows(), ui.rows());
  });

  test("expected result schemas persist for reusable queries and enforce view contracts after schema revisions", async (t) => {
    const schema = {
      id: "units-text",
      name: "Units as text",
      kind: "Schema",
      version: 1,
      schema: [{ name: "units", type: "STRING", nullable: false }],
      content: "units STRING required",
    };
    const ui = workspace(t, JSON.stringify({ definitions: [schema] }));
    ui.click('[data-action="navigate"][data-view="catalog"]');
    ui.click('[data-action="new-definition"]');
    ui.field('[data-config-field="definitionKind"]', "query");
    ui.field("#definition-name", "Views.TypedUnits");
    ui.check('[data-config-query-input="Holdings"]', true);
    ui.click('[data-action="config-section"][data-section="fields"]');
    ui.field("#definition-content", "SELECT units FROM Holdings");
    ui.click('[data-action="config-remove-row"][data-list="parameters"]');
    ui.field('[data-config-setting="resultSchemaId"]', schema.id);
    ui.check('[data-config-field="view"]', true);
    ui.click('[data-action="config-save"]');
    let provider = ui
        .saved()
        .providers.find((p) => p.name === "Views.TypedUnits"),
      definition = ui
        .saved()
        .definitions.find((d) => d.id === provider.queryRef.id);
    assert.deepEqual(provider.resultSchemaRef, { id: schema.id, version: 1 });
    assert.deepEqual(definition.resultSchemaRef, provider.resultSchemaRef);
    assert.equal(provider.columns[0].type, "STRING");
    ui.click('[data-action="navigate"][data-view="catalog"]');
    ui.click('[data-action="definition"][data-id="' + schema.id + '"]');
    ui.click('[data-action="revise-definition"]');
    ui.click('[data-action="config-section"][data-section="fields"]');
    ui.field('[data-config-column="type"]', "INT32");
    ui.click('[data-action="config-save"]');
    assert.equal(
      ui.saved().definitions.find((d) => d.id === schema.id).version,
      2,
    );
    ui.click('[data-action="definition"][data-id="' + definition.id + '"]');
    ui.click('[data-action="revise-definition"]');
    ui.click('[data-action="config-section"][data-section="preview"]');
    assert.match(
      ui.get('table[aria-label="Configured preview"] thead').textContent,
      /unitsSTRING/,
    );
    ui.click('[data-action="config-save"]');
    assert.deepEqual(
      ui.saved().definitions.find((d) => d.id === definition.id)
        .resultSchemaRef,
      { id: schema.id, version: 1 },
    );
    ui.click('[data-action="navigate"][data-view="providers"]');
    ui.click('[data-action="config-provider"][data-id="' + provider.id + '"]');
    ui.click('[data-action="config-section"][data-section="preview"]');
    assert.match(
      ui.get('table[aria-label="Configured preview"] thead').textContent,
      /unitsSTRING/,
    );
    ui.click('[data-action="config-save"]');
    provider = ui.saved().providers.find((p) => p.id === provider.id);
    assert.deepEqual(provider.resultSchemaRef, { id: schema.id, version: 1 });
    ui.click('[data-action="provider-detail"][data-id="' + provider.id + '"]');
    ui.click('[data-action="bind-provider"]');
    ui.field("#binding-name", "TypedUnits");
    ui.click('[data-action="config-section"][data-section="fields"]');
    ui.field('[data-config-column="name"]', "unitText");
    ui.click('[data-action="confirm-binding"]');
    [...ui.d.querySelectorAll('[data-action="use-source"]')].at(-1).click();
    ui.click('[data-action="preview-stage"]');
    await finishPreview(ui);
    assert.equal(ui.rows(), 12);
    ui.click('[data-action="result-tab"][data-tab="schema"]');
    assert.match(ui.get("#result-content").textContent, /unitTextSTRING/);
    savePipeline(ui);
    const reload = workspace(t, ui.stored());
    assert.equal(reload.rows(), 12);
    reload.click('[data-action="result-tab"][data-tab="schema"]');
    assert.match(reload.get("#result-content").textContent, /unitTextSTRING/);
    reload.click('[data-action="config-stage"]');
    reload.field('[data-config-setting="reuseDefinitionId"]', definition.id);
    reload.click('[data-action="config-save"]');
    reload.click('[data-action="preview-stage"]');
    await finishPreview(reload);
    reload.click('[data-action="result-tab"][data-tab="schema"]');
    assert.match(reload.get("#result-content").textContent, /unitsSTRING/);
  });

  for (const format of ["JDBC", "HTTP JSON"])
    test(
      "catalogue sample reload permits entering required " +
        format +
        " invocation values before validation",
      async (t) => {
        const ui = workspace(t);
        newReader(ui, format, "Recorded.Required" + format.replace(/\W/g, ""));
        if (format === "JDBC") {
          createJdbcConnection(ui);
          ui.field('[data-config-setting="readMode"]', "query");
          ui.click('[data-action="config-add-param"]');
          ui.field(
            '[data-config-list="parameters"][data-key="name"]',
            "minimum",
          );
          ui.field('[data-config-list="parameters"][data-key="type"]', "INT32");
          ui.field('[data-config-list="parameters"][data-key="value"]', "0");
          ui.field("#config-source-text", '[{"units":1}]', "input");
        } else {
          ui.click('[data-action="config-add-pair"][data-list="queryParams"]');
          ui.field('[data-config-pair="queryParams"][data-key="name"]', "date");
          ui.field(
            '[data-config-pair="queryParams"][data-key="value"]',
            "2026-11-23",
          );
          ui.field("#config-source-text", '{"data":[{"rate":1.1}]}', "input");
        }
        ui.click('[data-action="config-analyze"]');
        ui.click('[data-action="config-save"]');
        const provider = ui
            .saved()
            .providers.find((p) => p.name.startsWith("Recorded.Required")),
          before = ui.saved();
        const reload = workspace(t, ui.stored());
        reload.click('[data-action="navigate"][data-view="providers"]');
        reload.click(
          '[data-action="provider-detail"][data-id="' + provider.id + '"]',
        );
        reload.click('[data-action="reload-provider-sample"]');
        assert.match(reload.get("h1").textContent, /Reload source sample/);
        assert.equal(
          reload.get('[data-config-list="parameters"][data-key="value"]').value,
          "",
        );
        assert.equal(reload.d.querySelector('[role="dialog"]'), null);
        reload.click('[data-action="config-save"]');
        assert.match(
          reload.get(".config-error").textContent,
          /Parameter.*required/,
        );
        const value = format === "JDBC" ? "0" : "2026-11-24";
        reload.field(
          '[data-config-list="parameters"][data-key="value"]',
          value,
        );
        reload.field(
          "#config-source-text",
          format === "JDBC" ? '[{"units":2}]' : '{"data":[{"rate":2.2}]}',
          "input",
        );
        reload.click('[data-action="config-save"]');
        assert.deepEqual(reload.saved().providers, before.providers);
        reload.click('[data-action="navigate"][data-view="providers"]');
        reload.click(
          '[data-action="provider-detail"][data-id="' + provider.id + '"]',
        );
        reload.click('[data-action="bind-provider"]');
        reload.field("#binding-name", "RequiredSample");
        reload.field("#binding-param-0", value);
        reload.click('[data-action="confirm-binding"]');
        [...reload.d.querySelectorAll('[data-action="use-source"]')]
          .at(-1)
          .click();
        reload.click('[data-action="preview-stage"]');
        await finishPreview(reload);
        assert.equal(reload.rows(), 1);
        assert.match(
          reload.get('table[aria-label="Preview data"]').textContent,
          format === "JDBC" ? /2/ : /2.20/,
        );
      },
    );

  test("pipeline join limits are cumulative across visual and SQL stages and fail before output", async (t) => {
    const ui = workspace(t);
    ui.click('[data-action="config-runtime"]');
    ui.field('[data-config-setting="maxGeneratedRows"]', "20");
    ui.field('[data-config-setting="policy"]', "skip");
    ui.field('[data-config-setting="maxRejected"]', "100");
    ui.click('[data-action="config-save"]');
    ui.click('[data-action="run-pipeline"]');
    await finishPreview(ui);
    assert.equal(ui.saved().runs[0].status, "Failed");
    assert.match(
      ui.get("#result-content").textContent,
      /Generated-record limit.*across join steps/,
    );
    assert.equal(ui.saved().runs[0].destination, "Not started");
    ui.click('[data-action="config-runtime"]');
    ui.field('[data-config-setting="maxGeneratedRows"]', "36");
    ui.click('[data-action="config-save"]');
    ui.click('[data-action="run-pipeline"]');
    await finishPreview(ui);
    assert.equal(ui.saved().runs[0].status, "Completed");
    assert.equal(ui.rows(), 12);
  });

  test("legacy pipeline snapshots do not borrow another pipeline’s bindings or output settings", (t) => {
    const stages = [
      {
        id: "read",
        name: "Read instruments",
        kind: "Source",
        icon: "file",
        bindingId: "instruments",
        input: "Instruments",
        output: "InputRows",
        version: 1,
      },
      {
        id: "output",
        name: "Output",
        kind: "Output",
        icon: "upload",
        input: "InputRows",
        output: "OutputRows",
        version: 1,
      },
    ];
    const current = {
      id: "current-pipeline",
      name: "Current",
      kind: "Pipeline",
      version: 1,
      icon: "workflow",
      content: JSON.stringify({
        stages,
        bindings: [D.initialBindings.find((b) => b.id === "instruments")],
        output: { format: "JSON", name: "current.json", mode: "CREATE_NEW" },
        sql: D.sql,
      }),
    };
    const legacy = {
      id: "legacy-pipeline",
      name: "Legacy saved pipeline",
      kind: "Pipeline",
      version: 3,
      icon: "workflow",
      content: JSON.stringify({ stages: D.stages, sql: D.sql }),
    };
    const ui = workspace(
      t,
      JSON.stringify({
        pipelineId: current.id,
        name: current.name,
        version: 1,
        stages,
        bindings: [D.initialBindings.find((b) => b.id === "instruments")],
        output: { format: "JSON", name: "current.json", mode: "CREATE_NEW" },
        definitions: [current, legacy],
      }),
    );
    ui.click('[data-action="navigate"][data-view="catalog"]');
    ui.click('[data-action="definition"][data-id="' + legacy.id + '"]');
    ui.click('[data-action="revise-definition"]');
    ui.click('[data-action="config-save"]');
    assert.equal(ui.d.querySelector(".config-error"), null);
    savePipeline(ui);
    assert.equal(ui.saved().pipelineId, legacy.id);
    assert.equal(ui.saved().bindings.length, 3);
    assert.equal(ui.saved().output.format, "CSV");
    assert.deepEqual(
      ui.saved().definitions.find((d) => d.id === current.id),
      current,
    );
  });
  function structure(ui) { return ui.saved().structure; }
  function manage(ui, kind) { ui.click('[data-action="manage-structure"][data-kind="'+kind+'"]'); }
  function addStructure(ui, kind, name, namespaceId) {
    manage(ui,kind);
    ui.click('[data-action="structure-new"][data-kind="'+kind+'"]');
    ui.field('#structure-name',name,'input');
    ui.field('#structure-description','Configuration for '+name,'input');
    if(namespaceId)ui.field('#structure-namespace',namespaceId);
    ui.click('[data-action="structure-save"]');
    assert.equal(ui.d.querySelector('.config-error'),null);
    return structure(ui)[kind].find(item=>item.name===name);
  }
  function chooseNamespace(ui, namespaceId) { ui.field('#scope-namespace',namespaceId); }
  function chooseWorkspace(ui, workspaceId) { ui.field('#scope-workspace',workspaceId); }
  function openStructure(ui,kind,id) { manage(ui,kind);ui.click('[data-action="structure-open"][data-kind="'+kind+'"][data-id="'+id+'"]'); }
  function removeStructure(ui,kind,id) {
    manage(ui,kind);ui.click('[data-action="structure-delete"][data-kind="'+kind+'"][data-id="'+id+'"]');
    ui.click('[data-action="structure-confirm-delete"]');
    assert.equal(ui.d.querySelector('.config-error'),null);
  }

  test('legacy configuration migrates to organisation and default namespace without losing saved resources',t=>{
    const legacy={name:'Existing pipeline',version:7,pipelineId:'existing-pipeline',sql:D.sql,stages:D.stages,bindings:D.initialBindings,providers:D.providers,connections:D.connections};
    const ui=workspace(t,JSON.stringify(legacy)),model=structure(ui);
    assert.equal(model.formatVersion,2);
    assert.equal(model.organisations.length,1);
    assert.equal(model.namespaces[0].name,'default');
    assert.equal(model.namespaces[0].organisationId,model.organisations[0].id);
    assert.equal(Object.hasOwn(model.namespaces[0],'workspaceId'),false);
    assert.equal(model.workspaces[0].organisationId,model.organisations[0].id);
    assert.equal(model.namespaceData[model.activeNamespaceId].providers.length,D.providers.length);
    assert.equal(ui.saved().name,legacy.name);
    assert.equal(ui.saved().version,7);
    assert.equal(ui.saved().namespaceId,model.activeNamespaceId);
    const reload=workspace(t,ui.stored());
    assert.equal(structure(reload).activeWorkspaceId,model.activeWorkspaceId);
    assert.equal(reload.saved().pipelineId,'existing-pipeline');
  });

  test('organisation and namespace management creates empty resources, validates names, and protects default',t=>{
    const ui=workspace(t),org=addStructure(ui,'organisations','Research organisation');
    ui.field('#scope-organisation',org.id);
    const model=structure(ui),ns=model.namespaces.find(item=>item.organisationId===org.id);
    assert.equal(ns.name,'default');
    assert.deepEqual(Object.values(model.namespaceData[ns.id]).filter(Array.isArray).map(items=>items.length),[0,0,0,0,0,0]);
    ui.click('[data-action="navigate"][data-view="workspace"]');
    assert.match(ui.get('h1').textContent,/Design workspace/);
    assert.match(ui.get('.scope-empty').textContent,/Start configuring/);
    const research=addStructure(ui,'namespaces','Research');
    chooseNamespace(ui,research.id);
    assert.equal(structure(ui).activeNamespaceId,research.id);
    manage(ui,'namespaces');
    ui.click('[data-action="structure-new"][data-kind="namespaces"]');
    ui.field('#structure-name','research','input');
    ui.click('[data-action="structure-save"]');
    assert.match(ui.get('.config-error').textContent,/already used/);
    assert.equal(ui.get('#structure-name').value,'research');
    ui.field('#structure-name','x'.repeat(101),'input');
    ui.click('[data-action="structure-save"]');
    assert.match(ui.get('.config-error').textContent,/1–100/);
    ui.click('[data-action="structure-cancel"]');
    ui.click('[data-action="structure-edit"][data-id="'+research.id+'"]');
    ui.field('#structure-name','Analytics','input');ui.click('[data-action="structure-save"]');
    assert.equal(structure(ui).namespaces.find(item=>item.id===research.id).name,'Analytics');
    assert.equal(ui.get('[data-action="structure-delete"][data-id="'+ns.id+'"]').disabled,true);
    ui.click('[data-action="structure-edit"][data-id="'+ns.id+'"]');
    assert.equal(ui.get('#structure-name').readOnly,true);
  });

  test('namespace selection scopes catalogue, connections, definitions, runs, and runtime settings',t=>{
    const ui=workspace(t),original=structure(ui).activeNamespaceId,ns=addStructure(ui,'namespaces','Empty');
    chooseNamespace(ui,ns.id);
    for(const view of ['providers','sources','catalog','pipelines','runs']){
      ui.click('[data-action="navigate"][data-view="'+view+'"]');
      assert.doesNotMatch(ui.get('#app').textContent,/Files\.Holdings|Daily holdings enrichment|Reference files/);
    }
    newReader(ui,'JDBC','Readers.Prices');
    createJdbcConnection(ui);
    ui.click('[data-action="config-example"]');ui.click('[data-action="config-analyze"]');
    ui.click('[data-action="config-save"]');
    let resources=structure(ui).namespaceData[ns.id];
    assert.equal(resources.providers.length,1);assert.equal(resources.connections.length,1);
    assert.equal(resources.providers[0].name,'Readers.Prices');
    chooseNamespace(ui,original);
    assert.equal(structure(ui).namespaceData[original].providers.some(item=>item.name==='Readers.Prices'),false);
    newReader(ui,'JDBC','Readers.Prices');createJdbcConnection(ui);
    ui.click('[data-action="config-example"]');ui.click('[data-action="config-analyze"]');ui.click('[data-action="config-save"]');
    assert.equal(ui.d.querySelector('.config-error'),null);
    chooseNamespace(ui,ns.id);
    const reload=workspace(t,ui.stored());
    assert.equal(structure(reload).activeNamespaceId,ns.id);
    assert.equal(reload.saved().providers.length,1);
    assert.equal(reload.saved().connections[0].name,'Local JDBC');
    assert.equal(reload.saved().definitions.length,0);assert.equal(reload.saved().runs.length,0);
  });

  test('design workspaces share namespace resources and retain separate pipeline drafts across switching and reload',t=>{
    const ui=workspace(t),model=structure(ui),first=model.activeWorkspaceId,ns=model.activeNamespaceId;
    const second=addStructure(ui,'workspaces','Second editing session',ns);
    openStructure(ui,'workspaces',first);
    const query='SELECT isin FROM EnrichedHoldings LIMIT 2';
    ui.field('#sql-editor',query,'input');
    chooseWorkspace(ui,second.id);
    assert.notEqual(ui.get('#sql-editor').value,query);
    assert.equal(ui.saved().providers.length,D.providers.length);
    ui.field('#sql-editor','SELECT isin FROM EnrichedHoldings LIMIT 3','input');
    chooseWorkspace(ui,first);
    assert.equal(ui.get('#sql-editor').value,query);
    const reload=workspace(t,ui.stored());
    assert.equal(reload.get('#sql-editor').value,query);
    assert.equal(structure(reload).sessions[first].contexts[ns].dirty,true);
    assert.notEqual(reload.saved().sql,query,'Draft recovery must not publish the pipeline');
    assert.equal(Object.keys(structure(ui).namespaceData).length,1);
    assert.equal(Object.hasOwn(second,'namespaces'),false);
  });

  test('pending full-page reader configuration resumes per design workspace, without persisting business records',t=>{
    const ui=workspace(t),first=structure(ui).activeWorkspaceId,ns=structure(ui).activeNamespaceId;
    const second=addStructure(ui,'workspaces','Other editor',ns);
    newReader(ui,'JSON','Readers.CustomerDraft');
    ui.field('[data-config-field="raw"]','[{"customer":"private-record-713"}]','input');
    ui.click('[data-action="config-analyze"]');
    chooseWorkspace(ui,second.id);
    assert.equal(ui.d.querySelector('#provider-name'),null);
    chooseWorkspace(ui,first);
    assert.equal(ui.get('#provider-name').value,'Readers.CustomerDraft');
    assert.equal(ui.get('[data-config-field="raw"]').value,'[{"customer":"private-record-713"}]');
    assert.equal(ui.stored().includes('private-record-713'),false);
    const reload=workspace(t,ui.stored());
    assert.equal(reload.get('#provider-name').value,'Readers.CustomerDraft');
    assert.equal(reload.get('[data-config-field="raw"]').value,'');
    assert.match(reload.get('#app').textContent,/Reload the sample/);
    assert.equal(reload.saved().providers.some(item=>item.name==='Readers.CustomerDraft'),false);
  });

  test('CSV row and column configuration survives scope changes and reload with sample reload required',t=>{
    const ui=workspace(t),first=structure(ui).activeWorkspaceId,ns=structure(ui).activeNamespaceId;
    const second=addStructure(ui,'workspaces','CSV comparison',ns);
    ui.click('[data-action="navigate"][data-view="providers"]');ui.click('[data-action="register-provider"]');
    ui.click('[data-action="csv-example"]');ui.field('#csv-provider-name','Files.DraftCsv','input');
    ui.click('[data-action="csv-read"]');ui.field('#csv-last-record','3');ui.click('[data-action="csv-map"]');
    ui.field('[data-csv-map="outputName"][data-index="0"]','renamedPortfolio','input');
    chooseWorkspace(ui,second.id);chooseWorkspace(ui,first);
    assert.equal(ui.get('[data-csv-map="outputName"][data-index="0"]').value,'renamedPortfolio');
    const context=structure(ui).sessions[first].contexts[ns];
    assert.equal(context.csvDraft.options.lastRecord,3);
    assert.equal(Object.hasOwn(context.csvDraft,'text'),false);
    assert.equal(Object.hasOwn(context.csvDraft,'selection'),false);
    const reload=workspace(t,ui.stored());
    assert.equal(reload.get('#csv-provider-name').value,'Files.DraftCsv');
    assert.equal(reload.get('#csv-text').value,'');
    assert.equal(structure(reload).sessions[first].contexts[ns].csvDraft.mappings[0].outputName,'renamedPortfolio');
  });

  test('organisation context restricts namespaces and design workspaces and rejects cross-organisation selection',t=>{
    const ui=workspace(t),first=structure(ui),org=addStructure(ui,'organisations','Separate owner');
    ui.field('#scope-organisation',org.id);
    const model=structure(ui);
    assert.equal(ui.get('#scope-namespace').options.length,1);
    assert.equal(ui.get('#scope-workspace').options.length,1);
    assert.notEqual(model.activeNamespaceId,first.activeNamespaceId);
    const select=ui.get('#scope-namespace'),foreign=ui.d.createElement('option');
    foreign.value=first.activeNamespaceId;foreign.textContent='Foreign';select.append(foreign);
    ui.field('#scope-namespace',first.activeNamespaceId);
    assert.equal(structure(ui).activeOrganisationId,org.id);
    assert.equal(structure(ui).activeNamespaceId,model.activeNamespaceId);
    assert.match(ui.get('.toast').textContent,/same organisation/);
    const workspaceSelect=ui.get('#scope-workspace'),foreignWorkspace=ui.d.createElement('option');
    foreignWorkspace.value=first.activeWorkspaceId;workspaceSelect.append(foreignWorkspace);
    ui.field('#scope-workspace',first.activeWorkspaceId);
    assert.equal(structure(ui).activeWorkspaceId,model.activeWorkspaceId);
  });

  test('deleting a design workspace preserves all resources in the referenced namespace',t=>{
    const ui=workspace(t),before=structure(ui),second=addStructure(ui,'workspaces','Disposable session',before.activeNamespaceId);
    openStructure(ui,'workspaces',second.id);
    removeStructure(ui,'workspaces',second.id);
    const after=structure(ui);
    assert.equal(after.workspaces.some(item=>item.id===second.id),false);
    assert.deepEqual(after.namespaceData,before.namespaceData);
    assert.deepEqual(after.namespaces,before.namespaces);
    assert.equal(after.activeWorkspaceId,before.activeWorkspaceId);
  });

  test('pending drafts block deletion until explicitly discarded, and discarding retains saved resources',t=>{
    const ui=workspace(t),first=structure(ui),second=addStructure(ui,'workspaces','Draft session',first.activeNamespaceId);
    openStructure(ui,'workspaces',second.id);ui.field('#sql-editor','SELECT isin FROM EnrichedHoldings LIMIT 1','input');
    manage(ui,'workspaces');
    assert.equal(ui.get('[data-action="structure-delete"][data-id="'+second.id+'"]').disabled,true);
    ui.click('[data-action="structure-discard"][data-id="'+second.id+'"]');
    assert.match(ui.get('.structure-main').textContent,/Saved namespace resources remain/);
    ui.click('[data-action="structure-confirm-delete"]');
    assert.equal(structure(ui).sessions[second.id].contexts[first.activeNamespaceId].dirty,false);
    assert.equal(ui.get('[data-action="structure-delete"][data-id="'+second.id+'"]').disabled,false);
    removeStructure(ui,'workspaces',second.id);
    assert.deepEqual(structure(ui).namespaceData,first.namespaceData);
  });

  test('deleting an empty namespace reassigns editing sessions without deleting them, and empty organisations can be removed',t=>{
    const ui=workspace(t),original=structure(ui),ns=addStructure(ui,'namespaces','Temporary');
    const second=addStructure(ui,'workspaces','Temporary editor',ns.id);
    openStructure(ui,'workspaces',second.id);
    removeStructure(ui,'namespaces',ns.id);
    assert.equal(structure(ui).workspaces.length,2);
    assert.equal(structure(ui).sessions[second.id].namespaceId,original.activeNamespaceId);
    assert.equal(structure(ui).activeNamespaceId,original.activeNamespaceId);
    const org=addStructure(ui,'organisations','Empty organisation');
    ui.field('#scope-organisation',org.id);
    removeStructure(ui,'organisations',org.id);
    assert.equal(structure(ui).organisations.length,1);
    assert.equal(structure(ui).namespaces.some(item=>item.organisationId===org.id),false);
    assert.equal(structure(ui).workspaces.some(item=>item.organisationId===org.id),false);
    assert.equal(structure(ui).activeOrganisationId,original.activeOrganisationId);
  });

  test('saved namespace resources and pending provider drafts prevent namespace and organisation deletion',t=>{
    const ui=workspace(t),org=addStructure(ui,'organisations','Guarded owner');
    ui.field('#scope-organisation',org.id);
    const ns=addStructure(ui,'namespaces','Pending');chooseNamespace(ui,ns.id);
    newReader(ui,'JSON','Readers.Unsaved');
    manage(ui,'namespaces');assert.equal(ui.get('[data-action="structure-delete"][data-id="'+ns.id+'"]').disabled,true);
    manage(ui,'organisations');assert.equal(ui.get('[data-action="structure-delete"][data-id="'+org.id+'"]').disabled,true);
    manage(ui,'workspaces');ui.click('[data-action="structure-discard"][data-id="'+structure(ui).activeWorkspaceId+'"]');ui.click('[data-action="structure-confirm-delete"]');
    manage(ui,'namespaces');assert.equal(ui.get('[data-action="structure-delete"][data-id="'+ns.id+'"]').disabled,false);
    ui.click('[data-action="navigate"][data-view="sources"]');ui.click('[data-action="new-source"]');
    ui.field('#new-source-name','Saved files');ui.field('#new-source-location','./saved');ui.click('[data-action="confirm-source"]');
    manage(ui,'namespaces');assert.equal(ui.get('[data-action="structure-delete"][data-id="'+ns.id+'"]').disabled,true);
  });

  test('a stale design workspace cannot overwrite a newer saved pipeline in the namespace',t=>{
    const ui=workspace(t),first=structure(ui),second=addStructure(ui,'workspaces','Revision comparison',first.activeNamespaceId);
    openStructure(ui,'workspaces',second.id);
    ui.field('#sql-editor','SELECT isin FROM EnrichedHoldings LIMIT 1','input');
    chooseWorkspace(ui,first.activeWorkspaceId);savePipeline(ui);
    const version=ui.saved().version;
    chooseWorkspace(ui,second.id);ui.click('[data-action="save-version"]');ui.click('[data-action="confirm-save"]');
    assert.match(ui.get('[role="dialog"]').textContent,/Another design workspace saved a newer revision/);
    assert.equal(ui.saved().definitions.find(item=>item.id===first.sessions[first.activeWorkspaceId].contexts[first.activeNamespaceId].draft.pipelineId).version,version);
  });

  test('catalogue retains cards and adds list view, collapsible categories, global search, and persistent preferences',async t=>{
    const ui=workspace(t);ui.click('[data-action="navigate"][data-view="providers"]');
    assert.ok(ui.d.querySelector('.provider-card'));
    assert.ok(ui.d.querySelector('details[data-catalogue-group="Files"]'));
    let panel=ui.get('details[data-catalogue-group="Files"]');panel.open=false;panel.dispatchEvent(new ui.w.Event('toggle'));
    assert.equal(ui.d.querySelector('details[data-catalogue-group="Files"] .provider-card'),null);
    ui.click('[data-action="catalogue-mode"][data-mode="list"]');
    assert.equal(ui.get('details[data-catalogue-group="Files"]').open,false);
    assert.equal(ui.d.querySelector('.provider-card'),null);
    ui.field('#provider-search','Files','input');
    assert.equal(ui.get('details[data-catalogue-group="Files"]').open,true);
    assert.ok(ui.d.querySelector('table[aria-label="Catalogue Files"] tbody tr'));
    ui.field('#provider-search','','input');
    panel=ui.get('details[data-catalogue-group="Files"]');panel.open=true;panel.dispatchEvent(new ui.w.Event('toggle'));
    ui.field('#catalogue-connector','CSV');
    assert.doesNotMatch(ui.get('#provider-directory').textContent,/Reference\.Instruments/);
    const reload=workspace(t,ui.stored());reload.click('[data-action="navigate"][data-view="providers"]');
    assert.equal(reload.get('[data-action="catalogue-mode"][data-mode="list"]').getAttribute('aria-pressed'),'true');
    assert.equal(reload.get('#catalogue-connector').value,'CSV');
    assert.equal(reload.get('details[data-catalogue-group="Files"]').open,true);
    await wait(10);
  });

  test('catalogue renders bounded category pages and searches beyond the current page for thousands of entries',t=>{
    const providers=Array.from({length:1200},(_,index)=>({...D.providers.find(item=>item.kind==='Reader'),id:'bulk-'+index,name:'Files.Entry'+String(index).padStart(4,'0'),description:'Directory entry '+index}));
    const ui=workspace(t,JSON.stringify({providers}));ui.click('[data-action="navigate"][data-view="providers"]');
    assert.equal(ui.d.querySelectorAll('.provider-card').length,50);
    assert.match(ui.get('#catalogue-match-count').textContent,/1200/);
    ui.click('[data-action="catalogue-mode"][data-mode="list"]');
    assert.equal(ui.d.querySelectorAll('table[aria-label="Catalogue Files"] tbody tr').length,50);
    ui.click('[data-action="catalogue-page"][data-page="2"]');
    assert.match(ui.get('.catalogue-pagination').textContent,/Showing 51–100 of 1200/);
    assert.match(ui.get('table[aria-label="Catalogue Files"]').textContent,/Files\.Entry0050/);
    ui.field('#provider-search','Entry1199','input');
    assert.equal(ui.d.querySelectorAll('table[aria-label="Catalogue Files"] tbody tr').length,1);
    assert.match(ui.get('table[aria-label="Catalogue Files"]').textContent,/Files\.Entry1199/);
    ui.click('[data-action="config-provider"][data-id="bulk-1199"]');
    assert.equal(ui.get('#provider-name').value,'Files.Entry1199');
    const reload=workspace(t,ui.stored());assert.equal(reload.get('#provider-name').value,'Files.Entry1199');
  });

})();

require("node:test").test(
  "mockup folder contains only the HTML, data, and single test companion",
  () => {
    require("node:assert/strict").deepEqual(
      require("node:fs").readdirSync(__dirname).sort(),
      ["data.js", "index.html", "mockup.test.cjs"],
    );
  },
);
