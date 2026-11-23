© Mark Andrew Ray-Smith Cityline Ltd 2025\
Version 1.4.0

# Unify UI/UX prototype

Open [index.html](ui-mockup/index.html) directly in a browser. All UI, styling, and application JavaScript are embedded in that single HTML file. Data is separate in **data.js**.

Copy **index.html** and **data.js** to run or share the mockup. **mockup.test.cjs** is the single optional automated-test companion. Opening the mockup needs no build step, Node.js, node_modules, internet connection, or database setup. The mockup folder contains exactly those three files. Development tooling and test dependencies are in **scripts/ui-mockup/**.

An optional local server provides a consistent origin for browser storage:

```powershell
node scripts/ui-mockup/serve.mjs
```

Open **http://127.0.0.1:4321**; stop the server with Ctrl+C. Nothing is remotely hosted.

## Ownership and design sessions

The ownership hierarchy is **Organisation → Namespace → resources**. A **design workspace** is an organisation-owned configuration session that references namespaces and holds working drafts. It is separate from resource ownership:

```text
Organisation
├── Namespaces
│   ├── default
│   │   └── Catalogue, connections, definitions, pipelines, settings, runs
│   └── Other namespaces, each with its own resources
└── Design workspaces
    └── Editing sessions referencing the organisation's namespace resources
```

The context bar selects Organisation and Namespace separately from Design workspace. **Manage structure** opens full-page management for organisations, namespaces, and design workspaces, including create, rename, descriptions, selection, and guarded deletion. Names must be unique within their parent organisation; organisation names are unique in this local prototype.

Each organisation starts with a protected **default** namespace and an empty design workspace. Existing mockup configuration migrates into the initial organisation's default namespace and Demo workspace session. Additional organisations and namespaces start empty. Catalogue, connections, definitions, saved pipelines, execution settings, and run history follow the selected namespace. Design workspaces in the same organisation can reference the same namespace resources and keep separate drafts, including a separate context for each namespace they edit. Switching namespace does not transfer ownership or copy resources.

Draft configuration is retained when switching context and recovered after reload. Saving a reader, connection, or reusable definition writes that resource to the selected namespace; applying pipeline binding, stage, and output settings changes the session draft. **Save version** publishes that pipeline draft to the namespace. A stale session cannot overwrite a resource revised in another session without reopening its latest configuration. Uploaded samples stay in the current tab and editing session; after reload they must be supplied again. Built-in examples remain available from data.js.

Deleting a design workspace removes its session, leaving namespace resources intact. Pending drafts must first be saved or explicitly discarded. Namespaces containing resources or pending configuration cannot be deleted, and default cannot be deleted or renamed. Removing an empty namespace returns sessions referencing it to default. Organisations can be removed only when their namespaces contain no resources or pending drafts; their empty namespaces and sessions are then removed. At least one organisation and one design workspace per organisation remain.

These are local configuration boundaries in a mockup. They do not implement authentication or tenant authorization.

## Catalogue at scale

**Cards** retains the existing entry view; **List** adds compact rows with kind, connector, schema, availability, Configure, and Inspect. **Files**, **Outputs**, **Reference**, and **Views** are collapsible categories within the selected namespace's catalogue. The prefix in `Files.Holdings` is a category, separate from the owning namespace `default`.

Each expanded category displays up to 50 entries per page, with stable name ordering, counts, Previous, and Next. Search covers all matching entries across every category and page, including collapsed categories; matching categories expand during search. Capability and connector filters narrow the results. View mode, filters, page selection, and collapsed panels are retained per design workspace and namespace. This bounds rendered entries per category; production-scale server search and pagination are outside this local mockup.

## Configuration review

Substantial configuration opens in a full-page workspace. The steps retain edits while users move between settings, fields/rules, and preview diagnostics. Contract inspection and small confirmations remain dialogs.

| Workflow           | Where to start                                                            | Working behavior                                                                                                                                                                                                                                                                                 |
| ------------------ | ------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| CSV reader         | Catalogue → New CSV provider / Configure CSV                              | FileReader upload or pasted text; delimiter, quote and encoding; header and record selection; quoted multiline records; ordered, selected, renamed and typed columns; trim and null rules; located errors.                                                                                       |
| JSON reader        | Catalogue → New source provider → JSON                                    | Root array or one object member, file/paste/example sample, record range, saved schema, scalar type inference and field mapping. Duplicate members fail; selected nested objects/arrays fail.                                                                                                    |
| XML reader         | New source provider → XML                                                 | Root and row local names, namespace URI, scalar child mapping, nil values, record range and conversion. Malformed XML, DTDs and selected nested/repeated children fail.                                                                                                                          |
| JDBC reader        | New source provider → JDBC                                                | Connection reference, table/schema or source-native read-only SELECT, typed positional parameters, supplied JSON result sample and output mapping. Changing parameters requires a matching recorded result.                                                                                      |
| HTTP JSON reader   | New source provider → HTTP JSON                                           | GET endpoint, query parameters, permitted headers, credential reference, timeouts, recorded response status/content type/body and member selection. Preview shows the prepared request and maps the supplied response.                                                                           |
| Connections        | Connections → New connection / connection card                            | File directories, H2/SQLite JDBC URLs or HTTP endpoints, credential references and connector settings. Syntax validation is labeled separately from a live connection test. A provider, binding or JDBC output editor can create a connection and return to its retained settings.               |
| Input bindings     | Provider contract → Add to pipeline; Pipeline data → input → Edit binding | Unique dataset/alias, compatible connection, typed parameter values, saved schema and optional field mapping. Save validates the contract; preview checks the available records.                                                                                                                 |
| Schemas            | Definition library → New definition → Schema                              | Structured names, logical types, nullability, missing-value defaults, numeric/text bounds and permitted values. Duplicate/invalid fields block save. Author independently, or select data to infer fields and validate sample records.                                                           |
| Field mappings     | New definition → Field mapping                                            | Select a real input; include, rename, reorder, convert and constrain its fields. Save and reopen the mapping; apply it to output through Load saved output mapping.                                                                                                                              |
| Transformations    | Select a Transform stage → Configure stage                                | Actual selected input and named result, ordered trim/case/conversion/empty-to-null rules, add/remove/reorder and record errors. Save reusable rules in the Definition library and load them into a stage.                                                                                        |
| Validation         | Select a Validation stage → Configure stage                               | Required fields, numeric bounds, text length and permitted values on actual input records. Fail or skip according to the operation's rejection budget.                                                                                                                                           |
| Joins              | Select a Join stage → Configure stage                                     | Arbitrary declared inputs and earlier results, composite key pairs, INNER/LEFT/RIGHT/FULL, relationship assertions, selected output fields/aliases and matched/unmatched/generated counts. No fixed ISIN-only join.                                                                              |
| SQL and views      | Select a SQL stage → Configure stage; query definition → Publish as view  | Declared inputs, SQL, typed parameters, optional expected schema, actual sample results and errors. Save queries and expose read-only catalogue views. Existing bindings retain their captured contracts.                                                                                        |
| Pipelines          | Pipelines → New pipeline; workbench → Configure pipeline                  | Full-page creation from the current configuration or source/output boundaries only; source bindings, stage inputs, named results and final result. Add any number of stages in the workbench and configure each individually. Earlier-result scope is enforced; invalid wiring fails explicitly. |
| Output             | Output stage → Configure output; writer → Configure output                | Choose a prepared dataset, apply ordered field mapping and prepare real CSV/JSON/XML. CSV has separator/quote/header/null/line-ending settings; XML has element and namespace settings. Download the prepared file from Preview.                                                                 |
| JDBC output        | Configure output → JDBC                                                   | APPEND only, compatible JDBC connection, existing table/schema, supplied destination column metadata, generated/default flags and required/type/null checks. Preview prepares a parameterized transactional append plan.                                                                         |
| Execution settings | Workbench stage settings → Execution settings                             | Source preview count, source/generated-record budgets, UTF-8 working-payload budget, cooperative deadline and operation-wide invalid-record rejection budget. Limits affect preview processing.                                                                                                  |

The workbench initially contains 12 holdings, 10 instruments and six currency rates. Two holdings have no instrument match. These are examples, not hidden substitutes for configured input. Changing the source to Instruments and previewing the holdings normalization now reports missing fields.

Catalogue describes reusable reader/writer operations and read-only views. **Pipeline data** contains the operation's explicit bindings and earlier stage results. **Connections** holds endpoints and credential references; creating a connection neither registers a provider nor adds a binding. The **Definition library** stores reusable configuration. Use each object's Configure/Edit action to maintain it; the contract inspector is read-only.

Provider, connection and definition configurations are saved in this browser under their owning namespace. Pipeline binding/stage/output edits recover as unpublished session drafts after reload; **Save version** updates the reusable saved pipeline. Creating a new pipeline saves its initial configuration. Uploaded/pasted business records remain in the current tab and are excluded from saved/exported configuration. The built-in examples are in data.js. CSV configurations retain the earlier save-under-a-new-name behavior; other configured readers can be reopened and updated under their catalogue name. Existing bindings keep their captured provider contract.

After a page reload, open **Pipeline data → input → Reload sample**, load the matching file or paste its records, preview, and choose **Use sample**. The full-page workflow uses that binding's captured reader settings and parameter values, including an older reader contract. It keeps samples separate for different connections and parameters. Failed validation or leaving without Use sample retains the last accepted records. Reload sample does not create a provider or alter saved configuration. The reader contract inspector also offers Reload sample. Required invocation values can be entered there before validation; incomplete values display form errors. An existing sample is prefilled only when its connection and parameter values match.

**Save version** refreshes the reusable pipeline definition with its current stages, bindings, queries, and output settings. Full-pipeline previews retain the prepared output mapping. Opening a saved pipeline restores that pipeline’s identity and configuration, so saving it preserves other pipeline definitions. Expanded SQL, formatting, reset, and parameter-value controls apply to the selected SQL stage. Editor, pipeline, and reloaded previews use the same stage-local parameter values.

## Preview behavior and boundaries

- PostgreSQL 19, Apache Calcite, SQLite, DuckDB and external connectors are **not connected**. This implements the local configuration and sample-processing UX. The technical-design draft remains unchanged pending functional-design approval.
- CSV, JSON and XML previews parse supplied records. JDBC and HTTP previews use explicitly supplied result/response samples; settings validation never claims a live connection or request succeeded. No JDBC query, HTTP request or external publication is executed.
- Mapping, ordered transformations, validation, joins and the documented local SQL subset run against selected tables. Stage preview resolves its required earlier stages; pipeline preview executes the configured sequence. Cancellation is available between processing steps. Browser JavaScript work is cooperative and does not provide driver/native-engine cancellation guarantees.
- Local SQL supports SELECT projections/aliases, arithmetic and comparisons, CASE/CAST, TRIM/LOWER/UPPER/LENGTH/COALESCE/NULLIF, typed positional parameters, joins, WHERE, GROUP BY/HAVING, COUNT/SUM/AVG/MIN/MAX, DISTINCT, ORDER BY and LIMIT. Unsupported syntax/functions, unknown fields and writes fail. Aggregate expressions reject ungrouped fields, including fields inside arithmetic, functions, casts, and CASE expressions. Subqueries, WITH, UNION, the complete portable profile and backend compatibility require Calcite and actual engine integration. The engine selector records the intended backend, not a performance comparison.
- Defaults are 100 sampled records across source reads, a 10,000-record source budget, 10,000 generated rows, 1 MiB working payload and a ten-second local deadline. Configurable previews support up to 1,000 sampled records. Result and source qualifications identify sampling; rows outside the sample are not validated. Uploaded samples have a separate 1 MiB cap; JSON/XML accept at most 1,000 sample records. These are prototype limits, not product capacity claims.
- Expected query result schemas are retained by reusable queries and views. Views enforce their captured result contract, including after the schema is revised; binding-specific field mappings then apply to the view result.
- The generated-record budget is cumulative across visual and SQL joins in one operation. Matched and unmatched outer-join rows consume the budget during generation, before filtering, grouping, or LIMIT. Source records have their own separate budget.
- Join keys require matching logical types; add an explicit conversion when needed. Null keys do not match. Projections require unique output names. Aggregate rows show contributor counts instead of an invented single-record origin.
- File output downloads contain actual mapped and serialized preview values. The unquoted configured CSV null token (default \N) represents null; literal values are quoted. Null tokens containing a separator, quote character, or line break are rejected. JDBC preparation checks supplied destination metadata, but real metadata discovery, transactions, precision and database constraints require backend tests. Prepared output never commits to a destination.
- Browser localStorage holds configuration and local history, not PostgreSQL-backed durability. Supplied history/log timestamps are illustrative; new elapsed times measure local preview work. Availability is example host metadata rather than implemented authorization. Do not enter real credentials; use reference names.

Ctrl+K opens workspace search; Ctrl+Enter previews the selected stage; Ctrl+S opens Save version. Escape closes inspection dialogs. The mockup loads its local data.js file; documentation links open only when clicked.

## FINBOURNE Luminesce references

The workflow takes inspiration from public official documentation, with original Unify styling and inline icons. No product logos, screenshots, or assets are copied.

- [Luminesce query designer](https://support.lusid.com/docs/how-do-i-use-the-luminesce-query-designer): catalogue discovery, SQL and visual editing, output columns, join configuration, and reusable views.
- [Portfolio valuation walkthrough](https://support.lusid.com/docs/getting-started-with-luminesce-retrieving-a-portfolio-valuation): source-field inspection, execution progress, results, export, and history.
- [Custom views](https://support.lusid.com/docs/creating-custom-views-sysadminsetupview): reusable query definitions.
- [Luminesce overview](https://support.lusid.com/docs/luminesce-and-lumipy) and [platform architecture](https://support.lusid.com/docs/understanding-the-luminesce-platform-architecture): providers expose operations against data sources.
- [Provider registration](https://support.lusid.com/docs/sysregistration) and [parameter/field metadata](https://support.lusid.com/docs/sysfield): catalogue discovery and provider contracts.
- [Provider types](https://support.lusid.com/docs/what-are-a-data-provider-and-a-direct-provider): known result schemas and schemas determined by the requested resource.
- [Trino concepts](https://trino.io/docs/current/overview/concepts.html#catalog) and [Calcite adapters](https://calcite.apache.org/docs/adapter.html#schema-adapters): distinguish connector settings, namespaces, and queryable objects.

The reference screenshots could not be loaded; the inspiration is based on the documented workflows, not a verified visual reproduction.

## Verification

Node.js 20+ runs the optional server and tests. jsdom is a development-only dependency:

```powershell
npm --prefix scripts/ui-mockup ci --ignore-scripts
npm --prefix scripts/ui-mockup test
```

The suite exercises parsing, conversion, mapping, schema constraints, ordered rules, composite joins, SQL, serialization, budgets and full-page DOM workflows. It also checks direct file opening with only index.html and data.js, save/reopen, parameter binding, missing-source failures, recorded JDBC/HTTP samples, JDBC APPEND requirements and the 100-record preview regression. Regression coverage also checks sample reloads for all five reader formats, retained mappings, stage-specific SQL editing, reusable pipeline snapshots, grouped expressions, custom CSV null tokens, pipeline identity isolation, per-stage parameters, captured view schemas, required sample parameters, and cumulative join generation.

Structure checks cover legacy migration, empty namespaces, resource isolation, organisation ownership validation, shared namespace resources with separate session drafts, draft recovery without persisted business records, deletion and discard behavior, stale revision conflicts, first-pipeline creation, scoped settings, and catalogue paging/search over 1,200 entries.

All automated checks are consolidated in **docs/ui-mockup/mockup.test.cjs**. The test-only jsdom dependency is installed under scripts/ui-mockup/node_modules. The mockup itself needs only its HTML and data file. These checks do not verify visual layout, real transport, Calcite plans, database execution or destination writes. Browser visual review remains pending.
