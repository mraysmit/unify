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
const { JSDOM, VirtualConsole } = require("jsdom");
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
    if (src) assert.equal(src, "data.js", "Only sample data may be external");
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
  "copied HTML runs directly from disk with only its separate data file",
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
    assert.equal(document.querySelectorAll('link[rel="stylesheet"]').length, 0);
    assert.equal(document.styleSheets.length, 1);
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
    assert.deepEqual(errors, []);
  },
);

test("creating a pipeline retains the previous definition and reloads the new design", (t) => {
  const ui = workspace(t);
  ui.click('[data-action="navigate"][data-view="pipelines"]');
  ui.click('[data-action="new-pipeline"]');
  ui.field("#new-pipeline-name", "Client reconciliation");
  ui.click('[data-action="confirm-pipeline"]');
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
    /2 holdings have no matching instrument/,
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

test("visual join designer generates executable sample SQL for selected outputs", async (t) => {
  const ui = workspace(t);
  ui.click('[data-action="editor-mode"][data-mode="visual"]');
  ui.field("#designer-join", "INNER");
  ui.field("#designer-cardinality", "many-to-many");
  ui.get('input[name="designer-column"][value="portfolio"]').checked = false;
  ui.get('input[name="designer-column"][value="isin"]').checked = false;
  ui.click('[data-action="generate-sql"]');
  assert.match(ui.get("#sql-editor").value, /INNER JOIN Instruments/);
  assert.doesNotMatch(ui.get("#sql-editor").value, /ORDER BY/);
  ui.click('[data-action="preview-stage"]');
  await finishPreview(ui);
  assert.equal(ui.rows(), 10);
  ui.click('[data-action="save-version"]');
  ui.click('[data-action="confirm-save"]');
  assert.equal(
    ui.saved().stages.find((s) => s.kind === "Join").cardinality,
    "many-to-many",
  );
});

test("unbounded stage editing supports add, rename, reorder, remove and cancellation", (t) => {
  const ui = workspace(t);
  ui.click('[data-action="add-stage"]');
  ui.click('[data-action="choose-stage-type"][data-kind="Transform"]');
  assert.equal(ui.d.querySelectorAll(".stage-card").length, 7);
  ui.click('[data-action="stage-settings"]');
  ui.field("#dialog-stage-name", "Reconcile positions");
  ui.click('[data-action="close-modal"]');
  assert.match(ui.get(".stage-card.active").textContent, /Reconcile positions/);
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

test("reusable definitions can be created, revised, searched, and restored safely", (t) => {
  const ui = workspace(t);
  ui.click('[data-action="navigate"][data-view="catalog"]');
  ui.click('[data-action="new-definition"]');
  ui.field("#definition-name", "Client <schema>");
  ui.field("#definition-kind", "Schema");
  ui.field("#definition-content", "clientId STRING required");
  ui.click('[data-action="confirm-definition"]');
  ui.field("#library-search", "Client", "input");
  assert.equal(ui.d.querySelectorAll(".definition-card").length, 1);
  ui.click(".definition-card");
  ui.click('[data-action="revise-definition"]');
  ui.field(
    "#definition-content",
    "clientId STRING required\nname STRING nullable",
  );
  ui.click('[data-action="confirm-definition"]');
  const definition = ui
    .saved()
    .definitions.find((d) => d.name === "Client <schema>");
  assert.equal(definition.version, 2);
  assert.equal(
    ui.saved().snapshots.find((s) => s.definitionId === definition.id).content,
    "clientId STRING required",
  );
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
  ui.field("#new-source-location", "example-connection/reference");
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

test("query errors have a recoverable result and an inspectable failure record", (t) => {
  const ui = workspace(t);
  ui.click('[data-action="guide"]');
  ui.click('[data-action="invalid-example"]');
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

test("output review and publication remain clearly simulated", (t) => {
  const ui = workspace(t);
  ui.click('[data-action="select-stage"][data-id="output"]');
  ui.field("#output-format", "JSON");
  ui.field("#output-name", "reviewed-exposure.json");
  ui.click('[data-action="review-output"]');
  assert.match(ui.get('[role="dialog"]').textContent, /reviewed-exposure.json/);
  assert.match(
    ui.get('[role="dialog"]').textContent,
    /Publication is simulated/,
  );
  ui.click('[data-action="simulate-publish"]');
  assert.equal(ui.saved().runs[0].mode, "Publication simulation");
  assert.equal(ui.saved().runs[0].destination, "Committed (simulated)");
  ui.click('[data-action="save-version"]');
  ui.click('[data-action="confirm-save"]');
  assert.equal(ui.saved().output.name, "reviewed-exposure.json");
});

test("catalogue discovery exposes contracts while pipeline data exposes bindings and earlier results", (t) => {
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
  ui.field("#sql-editor", "SELECT * FROM ValidatedExposure;", "input");
  ui.click('[data-action="preview-stage"]');
  assert.match(ui.get("#result-content").textContent, /declared scope/);
});

test("the compact data browser searches and switches scope without duplicate element IDs", (t) => {
  const ui = workspace(t);
  ui.click('[data-action="catalog-mobile"]');
  const ids = [...ui.d.querySelectorAll("[id]")].map((el) => el.id);
  assert.equal(new Set(ids).size, ids.length);
  ui.field("#dialog-source-search", "Instruments", "input");
  assert.match(ui.get("#dialog-explorer-items").textContent, /Instruments/);
  assert.doesNotMatch(ui.get("#dialog-explorer-items").textContent, /Holdings/);
  ui.field("#dialog-source-search", "", "input");
  ui.click('[role="dialog"] [data-action="explorer-tab"][data-tab="pipeline"]');
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
  assert.match(ui.get('[role="dialog"]').textContent, /Reference.Instruments/);
});

test("binding a known provider pins its contract and permits a separate preview", async (t) => {
  const ui = workspace(t);
  ui.click('[data-action="provider-detail"][data-id="instruments-reader"]');
  ui.click('[data-action="bind-provider"]');
  ui.field("#binding-name", "Instruments");
  ui.click('[data-action="confirm-binding"]');
  assert.match(ui.get("#modal-error").textContent, /unique/);
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

test("runtime-shaped readers request a schema and fail explicitly when no sample fixture exists", (t) => {
  const ui = workspace(t);
  ui.click('[data-action="provider-detail"][data-id="csv-reader"]');
  ui.click('[data-action="provider-section"][data-section="Columns"]');
  assert.match(
    ui.get('[role="dialog"]').textContent,
    /Columns are not known at catalogue discovery/,
  );
  ui.click('[data-action="bind-provider"]');
  ui.click('[data-action="confirm-binding"]');
  assert.match(ui.get("#modal-error").textContent, /file is required/);
  ui.field("#binding-param-0", "clients.csv");
  ui.click('[data-action="confirm-binding"]');
  // Choose the newly expanded binding, independently of initial inputs.
  const inputs = ui.d.querySelectorAll('[data-action="use-source"]');
  inputs[inputs.length - 1].click();
  ui.click('[data-action="preview-stage"]');
  assert.match(ui.get("#result-content").textContent, /no local data fixture/);
});

test("new bindings become selectable visual-join inputs", async (t) => {
  const ui = workspace(t);
  ui.click('[data-action="provider-detail"][data-id="instruments-reader"]');
  ui.click('[data-action="bind-provider"]');
  ui.field("#binding-name", "AlternateInstruments");
  ui.click('[data-action="confirm-binding"]');
  ui.click('[data-action="editor-mode"][data-mode="visual"]');
  ui.field("#designer-right", "AlternateInstruments");
  ui.click('[data-action="generate-sql"]');
  assert.match(
    ui.get("#sql-editor").value,
    /LEFT JOIN AlternateInstruments AS i/,
  );
  ui.click('[data-action="preview-stage"]');
  await finishPreview(ui);
  assert.equal(ui.rows(), 12);
  assert.match(
    ui.get(".results-message").textContent,
    /2 holdings have no instrument match/,
  );
});

test("earlier join results expose their schema and preview enriched records", async (t) => {
  const ui = workspace(t);
  ui.click('[data-action="explorer-tab"][data-tab="pipeline"]');
  ui.click('[data-action="inspect-stage-result"][data-id="join"]');
  assert.match(ui.get('[role="dialog"]').textContent, /instrument.*Nullable/s);
  ui.click('[data-action="query-stage-result"]');
  ui.click('[data-action="preview-stage"]');
  await finishPreview(ui);
  assert.equal(ui.rows(), 12);
  assert.equal(
    ui.d.querySelectorAll('table[aria-label="Preview data"] thead th').length,
    8,
  );
  assert.match(
    ui.get('table[aria-label="Preview data"]').textContent,
    /Aster Technologies/,
  );
});

test("writer catalogue entries configure output without becoming query inputs", (t) => {
  const ui = workspace(t);
  ui.click('[data-action="provider-detail"][data-id="csv-output"]');
  assert.match(
    ui.get('[role="dialog"]').textContent,
    /Portable SELECT queries cannot perform publication/,
  );
  assert.equal(
    ui.get('[data-action="bind-provider"]').textContent,
    "Configure output",
  );
  ui.click('[data-action="bind-provider"]');
  ui.field("#binding-param-0", "provider-output.csv");
  ui.click('[data-action="confirm-binding"]');
  assert.match(
    ui.get(".output-provider-banner").textContent,
    /Outputs.Csv.*Position files/s,
  );
  ui.click('[data-action="save-version"]');
  ui.click('[data-action="confirm-save"]');
  assert.equal(ui.saved().bindings.length, 3);
  assert.deepEqual(ui.saved().output.providerRef, {
    id: "csv-output",
    version: 1,
  });
  assert.equal(ui.saved().output.name, "provider-output.csv");
  ui.click('[data-action="select-stage"][data-id="query"]');
  ui.field("#sql-editor", "SELECT * FROM Outputs.Csv;", "input");
  ui.click('[data-action="preview-stage"]');
  assert.match(ui.get("#result-content").textContent, /Unknown dataset/);
});

test("unavailable providers remain inspectable and cannot be bound", (t) => {
  const ui = workspace(t);
  ui.click('[data-action="provider-detail"][data-id="warehouse-reader"]');
  assert.equal(ui.get('[data-action="bind-provider"]').disabled, true);
  assert.match(ui.get('[role="dialog"]').textContent, /Not configured/);
});

test("registration creates metadata without a connection, binding, or data read", (t) => {
  const ui = workspace(t);
  ui.click('[data-action="navigate"][data-view="providers"]');
  ui.click('[data-action="register-provider"]');
  ui.field("#provider-name", "Client.Reference");
  ui.field("#provider-schema-mode", "Declared");
  ui.field("#provider-columns", "clientId STRING\nname STRING nullable");
  ui.click('[data-action="confirm-provider"]');
  assert.match(ui.get("#provider-directory").textContent, /Client.Reference/);
  const registered = ui
    .saved()
    .providers.find((p) => p.name === "Client.Reference");
  assert.equal(registered.columns.length, 2);
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
    /clientId.*STRING.*name.*Nullable/s,
  );
});

test("publishing a query creates a callable view and revisions preserve existing bindings", async (t) => {
  const ui = workspace(t);
  const publish = () => {
    ui.click('[data-action="navigate"][data-view="catalog"]');
    ui.click('[data-action="definition"][data-id="exposure-query"]');
    ui.click('[data-action="publish-view"]');
    ui.field("#view-name", "Views.ReviewedExposure");
    ui.click('[data-action="confirm-view"]');
  };
  publish();
  const view = ui
    .saved()
    .providers.find((p) => p.name === "Views.ReviewedExposure");
  assert.equal(view.version, 1);
  assert.equal(view.queryRef.version, 3);
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
  ui.click('[data-action="navigate"][data-view="catalog"]');
  ui.click('[data-action="definition"][data-id="exposure-query"]');
  ui.click('[data-action="revise-definition"]');
  ui.field(
    "#definition-content",
    D.sql.replace("h.units >= ?", "h.units >= 1000"),
  );
  ui.click('[data-action="confirm-definition"]');
  publish();
  assert.equal(ui.saved().providers.find((p) => p.id === view.id).version, 2);
  assert.equal(
    ui.saved().bindings.find((b) => b.name === "ReviewedExposure")
      .providerVersion,
    1,
  );
  assert.equal(
    ui.saved().providerVersions.find((p) => p.id === view.id).sqlSnapshot,
    D.sql,
  );
  ui.click('[data-action="provider-detail"][data-id="' + view.id + '"]');
  ui.field("#provider-version", "1");
  ui.click('[data-action="provider-section"][data-section="Dependencies"]');
  assert.match(
    ui.get('[role="dialog"]').textContent,
    /Portfolio exposure.*pinned v3/s,
  );
  ui.click('[data-action="definition"][data-id="exposure-query"]');
  assert.match(ui.get(".definition-detail-code").textContent, /h.units >= \?/);
  ui.click('[data-action="close-modal"]');
  ui.click('[data-action="navigate"][data-view="workspace"]');
  ui.click('[data-action="preview-stage"]');
  await finishPreview(ui);
  assert.equal(
    ui.rows(),
    10,
    "Published revision must not replace the pinned view",
  );
  const reload = workspace(t, ui.stored());
  reload.click('[data-action="preview-stage"]');
  await finishPreview(reload);
  assert.equal(reload.rows(), 10);
});
