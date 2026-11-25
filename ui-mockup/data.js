/*
© Mark Andrew Ray-Smith Cityline Ltd 2025
Version 1.4.0
*/
(function (root) {
  "use strict";
  const sql = `SELECT h.portfolio, h.isin, i.name AS instrument,
 i.assetClass, h.currency, h.units,
 h.units * h.price * fx.rate AS marketValue
FROM NormalizedHoldings AS h
LEFT JOIN Instruments AS i ON h.isin = i.isin
LEFT JOIN FxRates AS fx ON h.currency = fx.currency
WHERE h.units >= ?
ORDER BY h.portfolio, h.isin;`;
  const holdings = [
    ["Global Equity", "US0001000001", "USD", 1250, 187.42],
    ["Global Equity", "GB0001000002", "GBP", 840, 42.75],
    ["European Growth", "DE0001000003", "EUR", 360, 128.6],
    ["Balanced Alpha", "CH0001000004", "CHF", 420, 96.15],
    ["Global Equity", "JP0001000005", "JPY", 2100, 2840],
    ["European Growth", "FR0001000006", "EUR", 680, 74.8],
    ["Balanced Alpha", "US0001000007", "USD", 1500, 63.2],
    ["Global Equity", "NL0001000008", "EUR", 75, 418.3],
    ["Balanced Alpha", "GB0001000009", "GBP", 940, 18.65],
    ["European Growth", "SE0001000010", "SEK", 50, 215.4],
    ["Global Equity", "US0001000011", "USD", 320, 53.7],
    ["Balanced Alpha", "DE0001000012", "EUR", 270, 89.3],
  ].map(([portfolio, isin, currency, units, price], index) => ({
    portfolio,
    isin,
    currency,
    units,
    price,
    _ordinal: index + 1,
  }));
  const instruments = [
    ["US0001000001", "Aster Technologies", "Equity"],
    ["GB0001000002", "Northstar Energy", "Equity"],
    ["DE0001000003", "Rhein Industries", "Equity"],
    ["CH0001000004", "Alpine Healthcare", "Equity"],
    ["JP0001000005", "Hikari Mobility", "Equity"],
    ["FR0001000006", "Maison Consumer", "Equity"],
    ["US0001000007", "Summit Infrastructure", "Equity"],
    ["NL0001000008", "Delta Semiconductors", "Equity"],
    ["GB0001000009", "Harbour Utilities", "Equity"],
    ["SE0001000010", "Nordic Materials", "Equity"],
  ].map(([isin, name, assetClass], index) => ({
    isin,
    name,
    assetClass,
    _ordinal: index + 1,
  }));
  const rates = [
    ["USD", 1],
    ["GBP", 1.26],
    ["EUR", 1.08],
    ["CHF", 1.12],
    ["JPY", 0.0067],
    ["SEK", 0.094],
  ].map(([currency, rate], index) => ({
    currency,
    rate,
    _ordinal: index + 1,
  }));
  const field = (name, type, nullable, description) => ({
    name,
    type,
    nullable: Boolean(nullable),
    description: description || "",
  });
  const sources = [
    {
      id: "holdings",
      name: "Holdings",
      display: "Holdings.csv",
      format: "CSV",
      icon: "file",
      alias: "h",
      description: "Daily positions across three sample portfolios.",
      location: "/sample-data/holdings.csv",
      version: 1,
      schema: [
        field("portfolio", "STRING"),
        field("isin", "STRING"),
        field("currency", "STRING"),
        field("units", "INT32"),
        field("price", "DOUBLE"),
      ],
      rows: holdings,
    },
    {
      id: "instruments",
      name: "Instruments",
      display: "Instruments.json",
      format: "JSON",
      icon: "braces",
      alias: "i",
      description: "Instrument reference data and asset classifications.",
      location: "/sample-data/instruments.json",
      version: 2,
      schema: [
        field("isin", "STRING"),
        field("name", "STRING"),
        field("assetClass", "STRING"),
      ],
      rows: instruments,
    },
    {
      id: "fx",
      name: "FxRates",
      display: "FX rates",
      format: "HTTP JSON",
      icon: "globe",
      alias: "fx",
      description: "Illustrative conversion rates into US dollars.",
      location: "https://example.invalid/fx-rates",
      version: 1,
      schema: [field("currency", "STRING"), field("rate", "DOUBLE")],
      rows: rates,
    },
  ];
  const stages = [
    {
      id: "load",
      name: "Load holdings",
      kind: "Source",
      icon: "file",
      input: "Holdings",
      output: "RawHoldings",
      version: 1,
    },
    {
      id: "normalize",
      name: "Normalize fields",
      kind: "Transform",
      icon: "sliders",
      input: "RawHoldings",
      output: "NormalizedHoldings",
      version: 2,
    },
    {
      id: "join",
      name: "Join instruments",
      kind: "Join",
      icon: "join",
      input: "NormalizedHoldings",
      output: "EnrichedHoldings",
      version: 1,
      joinType: "LEFT",
      right: "Instruments",
      leftKey: "isin",
      rightKey: "isin",
    },
    {
      id: "query",
      name: "Calculate exposure",
      kind: "SQL query",
      icon: "code",
      input: "EnrichedHoldings",
      output: "PortfolioExposure",
      version: 3,
    },
    {
      id: "validate",
      name: "Validate output",
      kind: "Validation",
      icon: "shield",
      input: "PortfolioExposure",
      output: "ValidatedExposure",
      version: 1,
    },
    {
      id: "output",
      name: "Publish output",
      kind: "Output",
      icon: "upload",
      input: "ValidatedExposure",
      output: "holdings-enriched.csv",
      version: 1,
    },
  ];
  const definitions = [
    {
      id: "daily-holdings",
      name: "Daily holdings enrichment",
      kind: "Pipeline",
      version: 3,
      description:
        "Combine positions, instrument reference data, and currency rates.",
      usedBy: 3,
      icon: "workflow",
    },
    {
      id: "exposure-query",
      name: "Portfolio exposure",
      kind: "SQL query",
      version: 3,
      description:
        "Enrich holdings and calculate an illustrative market value.",
      usedBy: 2,
      icon: "code",
      content: sql,
    },
    {
      id: "holdings-schema",
      name: "Holdings input schema",
      kind: "Schema",
      version: 1,
      description:
        "Five typed fields with required portfolio and instrument keys.",
      usedBy: 4,
      icon: "table",
      content:
        "portfolio   STRING  required\nisin        STRING  required\ncurrency    STRING  required\nunits       INT32   required\nprice       DOUBLE  required",
    },
    {
      id: "normalize-rules",
      name: "Normalize holding fields",
      kind: "Transform",
      version: 2,
      description: "Trim identifiers and align the declared logical types.",
      usedBy: 3,
      icon: "sliders",
      content:
        "Trim portfolio and isin\nKeep currency case-sensitive\nConvert units to INT32\nConvert price to DOUBLE\nReject invalid numeric values",
    },
    {
      id: "instrument-join",
      name: "Instrument enrichment",
      kind: "Join",
      version: 1,
      description: "Retain every holding and match instruments by ISIN.",
      usedBy: 2,
      icon: "join",
      content:
        "LEFT JOIN Instruments AS i\nON h.isin = i.isin\nUnmatched instruments: preserve null\nExpected relationship: many-to-one",
    },
    {
      id: "output-mapping",
      name: "Exposure output mapping",
      kind: "Mapping",
      version: 1,
      description: "Select seven columns and retain explicit null values.",
      usedBy: 1,
      icon: "columns",
      content:
        "portfolio → portfolio\nisin → isin\ni.name → instrument (nullable)\ni.assetClass → assetClass (nullable)\ncurrency → currency\nunits → units\nunits × price × rate → marketValue",
    },
  ];
  const seedRuns = [
    {
      id: "DEMO-1042",
      name: "Daily holdings enrichment",
      status: "Completed",
      engine: "DuckDB",
      mode: "Preview",
      date: "2026-11-22T09:42:00+08:00",
      duration: 1140,
      input: 12,
      output: 12,
      unmatched: 2,
      version: 3,
      destination: "Not started",
      stepCount: 6,
    },
    {
      id: "DEMO-1041",
      name: "Daily holdings enrichment",
      status: "Completed",
      engine: "SQLite",
      mode: "Preview",
      date: "2026-11-22T09:38:00+08:00",
      duration: 1030,
      input: 12,
      output: 10,
      unmatched: 2,
      version: 3,
      destination: "Not started",
      stepCount: 6,
    },
    {
      id: "DEMO-1040",
      name: "Daily holdings enrichment",
      status: "Failed",
      engine: "DuckDB",
      mode: "Preview",
      date: "2026-11-22T09:31:00+08:00",
      duration: 250,
      input: 0,
      output: 0,
      unmatched: 0,
      version: 2,
      destination: "Not started",
      stepCount: 6,
      error:
        "Unknown dataset “InstrumentMaster”. Bind a source or use Instruments.",
    },
    {
      id: "DEMO-1037",
      name: "Daily holdings enrichment",
      status: "Abandoned",
      kind: "Execution",
      engine: "DuckDB",
      mode: "Execution (simulated)",
      principal: "demo.user",
      date: "2026-11-21T17:05:00+08:00",
      duration: 2410,
      input: 12,
      output: 0,
      unmatched: 0,
      version: 2,
      destination: "Unknown",
      stepCount: 6,
      error:
        "The service instance stopped during publication. The job was not restarted; repeat it from history if needed.",
    },
  ];
  const connections = [
    {
      id: "positions-files",
      name: "Position files",
      connector: "CSV",
      location: "/sample-data",
      credentialRef: "",
      description: "Example directory for holdings inputs and CSV output.",
    },
    {
      id: "reference-files",
      name: "Reference files",
      connector: "JSON",
      location: "/sample-data",
      credentialRef: "",
      description: "Example directory for instrument reference files.",
    },
    {
      id: "rates-service",
      name: "Rates service",
      connector: "HTTP JSON",
      location: "https://example.invalid/fx-rates",
      credentialRef: "workspace/rates-reader",
      description: "Illustrative HTTP connection; no service is contacted.",
    },
  ];
  const parameter = (name, type, required, defaultValue, description) => ({
    name,
    type,
    required,
    defaultValue,
    description,
  });
  const providers = [
    {
      id: "holdings-reader",
      name: "Files.Holdings",
      version: 1,
      kind: "Reader",
      capability: "Read",
      format: "CSV",
      schemaMode: "Declared",
      columns: sources[0].schema,
      parameters: [
        parameter(
          "file",
          "STRING",
          true,
          "holdings.csv",
          "File relative to the selected connection directory.",
        ),
      ],
      fixtureId: "holdings",
      description: "Read typed holdings using the declared input schema.",
      documentation: "Reader contract with a fixed five-column result.",
      dependencies: [
        {
          kind: "Schema",
          id: "holdings-schema",
          name: "Holdings input schema",
          version: 1,
        },
      ],
      available: true,
    },
    {
      id: "instruments-reader",
      name: "Reference.Instruments",
      version: 2,
      kind: "Reader",
      capability: "Read",
      format: "JSON",
      schemaMode: "Declared",
      columns: sources[1].schema,
      parameters: [
        parameter(
          "file",
          "STRING",
          true,
          "instruments.json",
          "File relative to the selected connection directory.",
        ),
      ],
      fixtureId: "instruments",
      description: "Read instrument names and asset classifications by ISIN.",
      documentation: "Fixed-schema instrument reference reader.",
      dependencies: [],
      available: true,
    },
    {
      id: "rates-reader",
      name: "Reference.FxRates",
      version: 1,
      kind: "Reader",
      capability: "Read",
      format: "HTTP JSON",
      schemaMode: "Declared",
      columns: sources[2].schema,
      parameters: [
        parameter(
          "baseCurrency",
          "STRING",
          true,
          "USD",
          "Reporting currency. The local fixture supports USD only.",
        ),
      ],
      fixtureId: "fx",
      description: "Read currency conversion rates from a configured endpoint.",
      documentation:
        "The endpoint comes from a connection reference, not SQL text.",
      dependencies: [],
      available: true,
    },
    {
      id: "csv-reader",
      name: "Files.Csv",
      version: 1,
      kind: "Reader",
      capability: "Read",
      format: "CSV",
      schemaMode: "At binding",
      columns: [],
      parameters: [
        parameter(
          "file",
          "STRING",
          true,
          undefined,
          "File relative to the selected connection directory.",
        ),
      ],
      description: "Read a CSV whose columns are declared when it is bound.",
      documentation:
        "Choose an input schema during binding. Catalogue discovery does not read a file.",
      dependencies: [],
      available: true,
    },
    {
      id: "csv-output",
      name: "Outputs.Csv",
      version: 1,
      kind: "Writer",
      capability: "Write",
      format: "CSV",
      schemaMode: "Input contract",
      columns: [],
      parameters: [
        parameter(
          "file",
          "STRING",
          true,
          "holdings-enriched.csv",
          "Destination filename. Publication mode is chosen in the output stage.",
        ),
      ],
      description: "Publish a prepared result as a complete CSV file.",
      documentation:
        "Used only through an explicit output stage. It cannot be queried in SELECT.",
      dependencies: [
        {
          kind: "Mapping",
          id: "output-mapping",
          name: "Exposure output mapping",
          version: 1,
        },
      ],
      available: true,
    },
    {
      id: "warehouse-reader",
      name: "Warehouse.Positions",
      version: 1,
      kind: "Reader",
      capability: "Read",
      format: "JDBC",
      schemaMode: "At binding",
      columns: [],
      parameters: [],
      description: "Example provider awaiting a configured JDBC connection.",
      documentation:
        "Unconfigured entries can be inspected but cannot be bound.",
      dependencies: [],
      available: false,
    },
    {
      id: "exposure-view",
      name: "Views.PortfolioExposure",
      version: 1,
      kind: "View",
      capability: "Read",
      format: "View",
      schemaMode: "Declared",
      columns: [
        field("portfolio", "STRING"),
        field("isin", "STRING"),
        field("instrument", "STRING", true),
        field("assetClass", "STRING", true),
        field("currency", "STRING"),
        field("units", "INT32"),
        field("marketValue", "DOUBLE"),
      ],
      parameters: [
        parameter(
          "minimumUnits",
          "INT32",
          false,
          0,
          "Minimum holding units. Bound as a value when the view is invoked.",
        ),
      ],
      description:
        "Reusable holdings exposure with pinned preparation and query definitions.",
      documentation:
        "Published read-only view. Saving SQL alone does not publish a catalogue entry.",
      sqlSnapshot: sql,
      queryRef: { id: "exposure-query", version: 3 },
      dependencies: [
        {
          kind: "Provider",
          id: "holdings-reader",
          name: "Files.Holdings",
          version: 1,
        },
        {
          kind: "Provider",
          id: "instruments-reader",
          name: "Reference.Instruments",
          version: 2,
        },
        {
          kind: "Provider",
          id: "rates-reader",
          name: "Reference.FxRates",
          version: 1,
        },
        {
          kind: "Transform",
          id: "normalize-rules",
          name: "Normalize holding fields",
          version: 2,
        },
        {
          kind: "SQL query",
          id: "exposure-query",
          name: "Portfolio exposure",
          version: 3,
        },
      ],
      available: true,
    },
  ];
  const sampleConnections = {
    holdings: "positions-files",
    instruments: "reference-files",
    fx: "rates-service",
  };
  const initialBindings = sources.map((source) => {
    const provider = providers.find((p) => p.fixtureId === source.id);
    return {
      id: source.id,
      name: source.name,
      alias: source.alias,
      providerId: provider.id,
      providerVersion: provider.version,
      providerSnapshot: provider,
      // Readers carry no connection (FR034); each binding selects one.
      connectionId: sampleConnections[source.id],
      parameters: Object.fromEntries(
        provider.parameters.map((p) => [p.name, p.defaultValue]),
      ),
      fixtureId: source.id,
      schema: source.schema,
    };
  });
  providers.find((p) => p.id === "exposure-view").inputBindings = JSON.parse(
    JSON.stringify(initialBindings),
  );
  providers.find((p) => p.id === "exposure-view").preparationStages =
    JSON.parse(JSON.stringify(stages.slice(0, 3)));
  const data = {
    structureDefaults: {
      organisations: [
        {
          id: "organisation-default",
          name: "Default organisation",
          description: "Initial organisation for the local mockup.",
        },
      ],
      namespaces: [
        {
          id: "namespace-default",
          organisationId: "organisation-default",
          name: "default",
          description: "Initial resource namespace.",
          isDefault: true,
        },
      ],
      workspaces: [
        {
          id: "workspace-default",
          organisationId: "organisation-default",
          name: "Demo workspace",
          description: "Configuration editing session.",
        },
      ],
    },
    configurationSamples: {
      JSON: {
        fileName: "instruments.json",
        text: JSON.stringify(
          instruments.map(({ _ordinal, ...row }) => row),
          null,
          2,
        ),
      },
      XML: {
        fileName: "positions.xml",
        text: "<records><record><portfolio>Growth Fund</portfolio><isin>US0001000001</isin><units>12</units><price>18.75</price></record><record><portfolio>Income Fund</portfolio><isin>GB0001000002</isin><units>8</units><price>42.50</price></record></records>",
      },
      JDBC: {
        fileName: "positions-result.json",
        text: JSON.stringify(
          holdings.map(({ _ordinal, ...row }) => row),
          null,
          2,
        ),
      },
      "HTTP JSON": {
        fileName: "response.json",
        text: JSON.stringify(
          { data: rates.map(({ _ordinal, ...row }) => row) },
          null,
          2,
        ),
      },
    },
    csvExample: {
      fileName: "holdings.csv",
      text: 'Account,ISIN,Quantity,Price,Currency,Comment\n" Growth Fund ",US0001000001,1250,187.42,USD,"Keep the comma, here"\nIncome Fund,GB0001000002,840,42.75,GBP,Second record\nBalanced Fund,DE0001000003,360,128.60,EUR,Third record\n',
    },
    sql,
    sources,
    holdings,
    instruments,
    rates,
    stages,
    definitions,
    seedRuns,
    connections,
    providers,
    initialBindings,
  };
  if (typeof module !== "undefined" && module.exports) module.exports = data;
  else root.UnifyData = data;
})(typeof globalThis !== "undefined" ? globalThis : this);
