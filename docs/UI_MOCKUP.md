© Mark Andrew Ray-Smith Cityline Ltd 2025\
Version 1.4.0

# Unify UI/UX prototype

The mockup lives in the top-level [ui-mockup/](../ui-mockup/) folder. Open [index.html](../ui-mockup/index.html) directly in a browser. All UI, styling, and application JavaScript are embedded in that single HTML file. Data is separate in **data.js**.

The folder contains exactly three files: **index.html**, **data.js**, and **tests.js** (the in-browser tests). Copy **index.html** and **data.js** to run or share the mockup; add **tests.js** to run the tests too. Nothing beyond HTML and JavaScript is needed: no build step, server, Node.js, npm, internet connection, or database setup. Nothing is remotely hosted.

## Automated demo

**Run demo** at the bottom of the sidebar opens a demo panel. Tick scenarios, choose a speed (Slow, Normal, Fast, or Instant for a test run) and press **Run**.
- A visible pointer moves to each control and clicks, types or chooses.
- Its bubble names each action, for example "Click: New connection", and the panel caption narrates the scenario.
- Each scenario's checks appear with ✓ or ✗ as they run.
- Pause and Stop work at any time, and timeouts don't count paused time.
- Every scenario is ticked by default; All and None change the selection. The panel can be minimised, or closed when no demo is running.
- When the run ends, the panel shows how many checks passed.

| Scenario | Shows |
| --- | --- |
| Preview a pipeline on sample data | Changing a query parameter and previewing every stage without writing to a destination |
| Configure a reader from a sample | A new JSON reader configured from a sample, previewed and saved without a connection |
| Bind a reader and preview its data | An explicit binding with its dataset name, SQL alias and chosen connection, then a preview of its records |
| Join datasets and edit the query | Join configuration on the Join stage's tabs, with a suggested name for a colliding field, then Apply and a stage preview with match counts; inserting a field into SQL and checking the query; renaming a result and seeing the rewritten query |
| Prepare output and download a preview sample | JSON output, its serialized preview, and the `preview-` download, with no execution started |
| Save or discard before starting a new pipeline | The save-or-discard prompt, and discarding unsaved changes |
| Let Unify choose the engine | Automatic engine selection, its use case, and how a run records it |
| Execute as a background job | An execution shown Queued, then Running, then Completed in Run history, and repeated. The demo's own app slows the simulated job so each state can be seen |
| Validate a connection, then test it | Settings validation kept separate from an explicit, simulated live test |
| Find a provider in the catalogue | The compact list view and catalogue search |
| What each role can do | Viewer, Operator, Editor, Publisher and Administrator permissions, including the editing controls an Operator cannot use |
| Export a bundle and import it under new names | Bundle export, name conflicts, and import with new names |
| Namespaces keep resources apart | A new namespace with an empty catalogue and no connections, while default keeps its resources |
| Two design workspaces, one conflict | Separate drafts in two workspaces, and a stale save refused after the other workspace saved a newer version |
| Collapse data browser categories | Collapsing a category, a search opening it, and the collapse returning when the search is cleared |
| See connections as a list | The Connections list view, its search, and Inspect from a row |
| Browse the organisation structure as a tree | Organisations with their Namespaces and Design workspaces branches, collapsing and expanding a branch |
| Find and open a saved pipeline | The Pipelines table, the pipeline open in the workbench, its flow, and Open |
| Get help on a screen or an action | The info popups for a screen and for an action, and that opening one does not run the action |
| Sign out and sign in | The simulated identity-provider sign-in |

Each scenario starts from fresh sample data in an in-memory store. While it runs, a transparent layer blocks clicks and keys outside the panel.
- **Before the demo:** if one of your executions is still queued or running, the demo doesn't start, and asks you to wait for it to finish. Otherwise the app saves your current work.
- **After the demo:** the app reopens from that saved state, on the page you had open, as after a reload. Anything not yet saved, such as an uploaded sample, is cleared. Downloads during the demo are captured, not saved. The demo publishes its results as JSON on the results element (`data-results`), and the test suite runs every scenario at Instant speed and requires every check to pass.

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

The header shows the current context as `Organisation / Namespace / Design workspace`. Clicking it opens a menu with the Organisation, Namespace and Design workspace selectors and **Manage structure**; Escape or a click outside closes it. The menu is disabled while a preview runs or structure editing is open. On narrow screens the header shows only the menu icon.

**Manage structure** opens full-page management for organisations, namespaces, and design workspaces, including create, rename, descriptions, selection, and guarded deletion. The Organisations panel is a tree: each organisation has a **Namespaces** branch and a **Design workspaces** branch, each with a count, and every organisation and branch can be collapsed. A namespace row shows its resource counts and how many workspaces reference it; a workspace row shows its selected namespace; the current organisation, namespace and workspace are marked Current. Selecting an item in another organisation switches to that organisation first. Names must be unique within their parent organisation; organisation names are unique in this local prototype.

Each organisation starts with a protected **default** namespace and an empty design workspace. Existing mockup configuration migrates into the initial organisation's default namespace and Demo workspace session. Additional organisations and namespaces start empty. Catalogue, connections, definitions, saved pipelines, execution settings, and run history follow the selected namespace. Design workspaces in the same organisation can reference the same namespace resources and keep separate drafts, including a separate context for each namespace they edit. Switching namespace does not transfer ownership or copy resources.

Draft configuration is retained when switching context and recovered after reload. Saving a reader, connection, or reusable definition writes that resource to the selected namespace; applying pipeline binding, stage, and output settings changes the session draft. **Save version** publishes that pipeline draft to the namespace. A stale session cannot overwrite a resource revised in another session without reopening its latest configuration. Uploaded samples stay in the current tab and editing session; after reload they must be supplied again. Built-in examples remain available from data.js.

Deleting a design workspace removes its session, leaving namespace resources intact. Pending drafts must first be saved or explicitly discarded. Namespaces containing resources or pending configuration cannot be deleted, and default cannot be deleted or renamed. Removing an empty namespace returns sessions referencing it to default. Organisations can be removed only when their namespaces contain no resources or pending drafts; their empty namespaces and sessions are then removed. At least one organisation and one design workspace per organisation remain.

Switching context:

- The current page stays open (Workbench, Catalogue, Pipelines, Connections, Definition library, Run history or structure management). If the target session has an open full-page editor draft, that editor reopens instead.
- Choosing another organisation selects its first design workspace, in that workspace's last namespace or default.
- The first time a session opens a namespace, its draft starts from the namespace's first saved pipeline, or empty.
- The selected stage's preview is recalculated.
- The engine choice (DuckDB by default), Minimum units, selected stage, tabs, searches, filters and catalogue layout are kept per design workspace and namespace.

These are local configuration boundaries in a mockup. They do not implement authentication or tenant authorization.

## Catalogue at scale

**Cards** retains the existing entry view; **List** adds compact rows with kind, connector, schema, availability, Configure, and Inspect. Categories are the prefix of each entry's `Category.Name` (the sample data has Files, Outputs, Reference, Views and Warehouse) and are collapsible within the selected namespace's catalogue. The prefix in `Files.Holdings` is a category, separate from the owning namespace `default`.

Each expanded category displays up to 50 entries per page, with stable name ordering, counts, Previous, and Next. Search covers all matching entries across every category and page, including collapsed categories; matching categories expand during search. Capability and connector filters narrow the results. View mode, filters, page selection, and collapsed panels are retained per design workspace and namespace. This bounds rendered entries per category; production-scale server search and pagination are outside this local mockup.

## Configuration review

**Pipeline stages are configured on the workbench.** Selecting a stage shows its configuration as tabs in the stage editor; there is no separate Configure stage screen. Edits are held until they are applied:
- A bar under the editor shows **Unapplied changes** with **Apply** and **Revert**.
- Unapplied changes are also applied automatically before Preview this stage, Preview pipeline, Execute, Save version (including Ctrl+Enter and Ctrl+S), selecting another stage, adding, moving or removing a stage, Review output, export, and leaving the workbench.
- Creating a connection from a stage tab (a binding's or the output's connection) opens the full-page connection editor; saving or leaving it returns to the stage tab with the unapplied edits kept and the new connection selected.
- Applying runs the same checks as saving a configuration. If they fail, nothing changes, the edits are kept and the error is shown in the editor.
- Applied changes are part of the pipeline draft until **Save version**.

**Everything else opens in a full-page workspace:** readers, connections, definitions, new input bindings, writer output configuration, the pipeline (inputs and final result) and execution settings. Its steps retain edits while users move between settings, fields/rules, and preview diagnostics. Contract inspection and small confirmations remain dialogs.

| Workflow           | Where to start                                                            | Working behavior                                                                                                                                                                                                                                                                                 |
| ------------------ | ------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| CSV reader         | Catalogue → New CSV provider / Configure CSV                              | FileReader upload or pasted text; delimiter, quote and encoding; header and record selection; quoted multiline records; ordered, selected, renamed and typed columns; trim and null rules; located errors. Delimiter: comma, semicolon, tab or pipe. Quote: double, single or none. Encoding: UTF-8 or windows-1252, applied when a file is read. Blank data records raise an error or are skipped. Source columns match by header name or position. Each column has allow-null, trim and empty-to-null; there are no defaults or value constraints. Save provider is enabled only after a successful preview, and always creates a new provider (an existing one opens as "…Copy"). |
| JSON reader        | Catalogue → New source provider → JSON                                    | Root array or one object member, file/paste/example sample, record range, saved schema, scalar type inference and field mapping. Duplicate members fail; selected nested objects/arrays fail.                                                                                                    |
| XML reader         | New source provider → XML                                                 | Root and row local names, namespace URI, scalar child mapping, nil values, record range and conversion. Malformed XML, DTDs and selected nested/repeated children fail.                                                                                                                          |
| JDBC reader        | New source provider → JDBC                                                | Connection reference, table/schema or source-native read-only SELECT, typed positional parameters, supplied JSON result sample and output mapping. Changing parameters requires a matching recorded result.                                                                                      |
| HTTP JSON reader   | New source provider → HTTP JSON                                           | GET endpoint, query parameters, permitted headers, credential reference, timeouts, recorded response status/content type/body and member selection. Preview shows the prepared request and maps the supplied response. |
| All non-CSV readers | Same as above                                                             | Every JSON, XML, JDBC and HTTP JSON reader has a first/last record range and "Use saved output schema". Saving requires a unique `Category.Name` and a compatible connection. JSON and XML also need a source filename. |
| Connections        | Connections → New connection / connection card                            | File directories, H2/SQLite JDBC URLs or HTTP endpoints, credential references and connector settings. **Validate settings** checks syntax without contacting anything; **Test connection** is a separate, explicit check (FR040), simulated here: files report a readable directory, JDBC an opened connection, HTTP a TLS handshake by default or a GET whose body is not read when chosen explicitly, and `.invalid` hosts are unreachable. Test results are shown, never stored. A provider, binding or JDBC output editor can create a connection and return to its retained settings. JDBC product is SQLite or H2, and the URL must start with `jdbc:sqlite:` or `jdbc:h2:`. HTTP connections have connect and read timeouts. Credentials embedded in a URL are rejected. Names are unique, ignoring case. An HTTP reader takes the saved connection's base URL as its endpoint. |
| Input bindings     | Source stage → Input, Binding settings and Fields tabs; Provider contract → Add to pipeline (new binding); Pipeline data → input → Inspect binding → Edit binding | Unique dataset/alias, compatible connection, typed parameter values, saved schema and optional field mapping. Save or Apply validates the contract; preview checks the available records. On the Source stage, the Input tab chooses which binding the stage reads; it is locked while the binding has unapplied changes. Applying updates the binding for every stage that reads it. |
| Schemas            | Definition library → New definition → Schema                              | Structured names, logical types, nullability, missing-value defaults, numeric/text bounds and permitted values. Duplicate/invalid fields block save. Author independently, or select data to infer fields and validate sample records. |
| Field mappings     | New definition → Field mapping                                            | Select a real input; include, rename, reorder, convert and constrain its fields. Save and reopen the mapping; apply it to output through Load saved output mapping. In schemas and mappings each field has trim, empty-to-null, a default used only when the field is missing, min/max, minimum and maximum length, and permitted values as a JSON array. |
| Other definitions  | Definition library → New definition                                       | Six types: Schema, Field mapping, Transformation rules, Validation rules, Join and SQL query. Names are unique within a type, ignoring case. |
| Transformations    | Transform stage → Input and settings, Rules tabs                          | Actual selected input and named result, ordered trim/case/conversion/empty-to-null rules, add/remove/reorder and record errors. Save reusable rules in the Definition library and load them into a stage. Operations: trim ordinary spaces, uppercase ASCII, lowercase ASCII, convert logical type (with nullability), empty text to null. |
| Validation         | Validation stage → Input and settings, Rules tabs                         | Required fields, numeric bounds, text length and permitted values on actual input records. Fail or skip according to the operation's rejection budget. Operations: required/non-null, minimum and maximum value, minimum and maximum text length, permitted values (JSON array). |
| Joins              | Join stage → Inputs and settings, Match keys and fields tabs              | Arbitrary declared inputs and earlier results, composite key pairs, INNER/LEFT/RIGHT/FULL, relationship assertions, selected output fields/aliases and matched/unmatched/generated counts. No fixed ISIN-only join. Expected relationship: many-to-many, many-to-one, one-to-many or one-to-one. Output fields can be included or renamed. Joins and SQL stages can also load a saved definition ("Load reusable configuration"). |
| SQL and views      | SQL stage → SQL, Inputs and result, Parameters tabs; query definition → Publish as view | Declared inputs, SQL, typed parameters, optional expected schema, actual sample results and errors. On a stage, the SQL is edited on the SQL tab, declared inputs and a reusable query on Inputs and result, and parameters and the expected result schema on Parameters. Save queries and expose read-only catalogue views. Existing bindings retain their captured contracts. "Expose as a read-only catalogue view" publishes on save. The view name must be a `Category.Name` that no reader or writer uses. |
| Pipelines          | Pipelines → New pipeline; workbench → Configure pipeline                  | Full-page creation from the current configuration or source/output boundaries only; source bindings, stage inputs, named results and final result. Add any number of stages in the workbench and configure each individually. Earlier-result scope is enforced; invalid wiring fails explicitly. New pipeline is disabled until the session has an input binding. It starts from a copy of the current pipeline or from source and output only. Creating it also saves the previous design to the Definition library. |
| Output             | Output stage → Destination and format, Field mapping tabs; writer → Configure output | Choose a prepared dataset, apply ordered field mapping and prepare real CSV/JSON/XML. CSV has separator/quote/header/null/line-ending settings; XML has element and namespace settings. JSON has a pretty-print option. File outputs create new (fail if it exists) or replace the complete file. Encoding is UTF-8 only, and the record separator is LF or CRLF. "Load saved output mapping" applies a Field mapping definition. Download the prepared file from this workspace's preview. Its name starts with `preview-` because it holds only the preview sample (FR013). The workbench's Export preview and Review output download the current result as `unify-preview.csv` (CSV with `\N` for null, whatever the output format). The CSV reader downloads its mapped preview as `mapped-preview.csv`. |
| JDBC output        | Output format → JDBC                                                      | APPEND only, compatible JDBC connection, existing table/schema, supplied destination column metadata, generated/default flags and required/type/null checks. Preview prepares a parameterized transactional append plan.                                                                         |
| Execution settings | Stage settings → Pipeline preview settings → Change execution settings    | Source preview count, source/generated-record budgets, UTF-8 working-payload budget, cooperative deadline and operation-wide invalid-record rejection budget. Limits affect preview processing.                                                                                                  |
| Engine selection   | Workbench → engine selector                                               | **Automatic**, SQLite or DuckDB (FR031, ADR-003). Automatic applies an illustrative selection profile (the product's profile is derived from detailed use-case testing): the pipeline's shape class and its bound record count select a use case, shown in the results summary and recorded with each run. An explicit choice is recorded as explicit. The illustrative profile has four use cases: SCAN/SORT up to 50,000 records → SQLite, above that → DuckDB; JOIN/AGGREGATE/JOIN_AGGREGATE/MULTI_JOIN up to 10,000 → SQLite, above that → DuckDB. The shape comes from stage kinds and SQL keywords. The record count is the sum of the bound inputs' sample rows, so with sample-sized data automatic selection chooses SQLite. |
| Executions and runs | Workbench → **Execute (simulated)**; Run history                          | Executions run as simulated background jobs (FR046): queued, then running, then completed, with the destination committed only in simulation. Run history shows kind (preview or execution), status, engine with its selection, and who submitted it. A finished, failed, cancelled or abandoned execution can be repeated. The repeat is a new run that names the original, but it resubmits the pipeline currently in the workbench, with its current version and engine selection and the counts of its last preview, rather than the original run's settings. Executions cannot be cancelled; Cancel stops only a preview. Reloading while a job is queued or running stands in for a stopped service instance: the job is marked **Abandoned** and never restarted. |
| Sign-in and roles  | Header → Role; Sign out                                                   | Simulated sign-in (FR044) and roles (FR045). The header shows the signed-in user and a role switcher: Viewer, Editor, Publisher, Operator or Administrator (the default). Actions the role cannot use are disabled with a tooltip naming the roles that can; every role except Administrator sees a notice listing what it cannot do, and read-only roles see non-editable fields; a denied attempt, including Ctrl+Enter or Ctrl+S, changes nothing and explains why. Sign out shows a simulated identity-provider sign-in where a name and role are chosen; runs record the signed-in principal. In the product, roles come from identity-provider groups, not a switcher. |
| Bundles            | Versions → Export current design; Pipelines → Import bundle               | Export writes a versioned bundle of the pipeline with the providers and connections its bindings use, never records, samples or history (FR012). Import into a namespace with clashing names imports nothing and asks for a new name for every clash; only a complete set of unused names imports, with references remapped to the new resources. |

The workbench initially contains 12 holdings, 10 instruments and six currency rates. Two holdings have no instrument match. These are examples, not hidden substitutes for configured input. Changing the source to Instruments and previewing the holdings normalization now reports missing fields.

Catalogue describes reusable reader/writer operations and read-only views. **Pipeline data** contains the operation's explicit bindings and earlier stage results. **Connections** holds endpoints and credential references; creating a connection neither registers a provider nor adds a binding. The **Definition library** stores reusable configuration. Use each object's Configure/Edit action to maintain it; the contract inspector is read-only.

Provider, connection and definition configurations are saved in this browser under their owning namespace. Pipeline binding/stage/output edits recover as unpublished session drafts after reload; **Save version** updates the reusable saved pipeline. Creating a new pipeline saves its initial configuration. Uploaded/pasted business records remain in the current tab and are excluded from saved/exported configuration. The built-in examples are in data.js. CSV configurations retain the earlier save-under-a-new-name behavior; other configured readers can be reopened and updated under their catalogue name. Existing bindings keep their captured provider contract.

After a page reload, open **Pipeline data → input → Reload sample**, load the matching file or paste its records, preview, and choose **Use sample**. The full-page workflow uses that binding's captured reader settings and parameter values, including an older reader contract. It keeps samples separate for different connections and parameters. Failed validation or leaving without Use sample retains the last accepted records. Reload sample does not create a provider or alter saved configuration. The reader contract inspector also offers Reload sample. Required invocation values can be entered there before validation; incomplete values display form errors. An existing sample is prefilled only when its connection and parameter values match.

**Save version** refreshes the reusable pipeline definition with its current stages, bindings, queries, and output settings. Full-pipeline previews retain the prepared output mapping. Opening a saved pipeline restores that pipeline’s identity and configuration, so saving it preserves other pipeline definitions. SQL editing, formatting, reset, and parameter-value controls apply to the selected SQL stage. Editor, pipeline, and reloaded previews use the same stage-local parameter values.

## Screens in detail

### Workbench

- **Page header.** Versions, Save version, Execution engine, Preview pipeline (Cancel while running) and Execute (simulated). Execute is enabled only after the last preview succeeded, and the execution takes its counts from that preview.
- **Empty namespace.** A namespace without a pipeline shows New connection, New reader and Browse catalogue, plus its bound inputs. New pipeline is disabled until an input binding exists.
- **Pipeline stages.**
  - Configure pipeline (full page) and Add stage. Selecting a stage card shows it in the stage editor and Stage settings.
  - Add stage offers SQL query, Transform, Join or Validation. The new stage is inserted after the selected one, never after Output, and takes the previous stage's result as its input.
  - Stages move earlier or later within the boundaries: Source stays first and Output last.
  - Remove stage asks for confirmation. Source and Output cannot be removed.
  - Renaming a stage's result updates later stages that use it. Renaming it in Stage settings also replaces the old name in the sample query text.
  - While a preview runs, design changes are blocked until it finishes or is cancelled, and the organisation, namespace and workspace cannot be switched.
- **Stage editor.** The selected stage's configuration, as tabs:

  | Stage | Tabs |
  | --- | --- |
  | Source | **Input** (the binding the stage reads, with a summary) · **Binding settings** (dataset name, SQL alias, connection, parameters, schema) · **Fields** (the binding's field selection and mapping) |
  | SQL query | **SQL** (highlighted editor with line numbers, Format SQL, which removes trailing spaces, and Restore sample SQL) · **Inputs and result** (declared inputs, Load reusable configuration) · **Parameters** (typed parameters and the expected result schema) |
  | Transform, Validation | **Input and settings** (input data, Load reusable configuration) · **Rules** (ordered rules: add, remove, reorder) |
  | Join | **Inputs and settings** (left and right inputs, join type, expected relationship) · **Match keys and fields** (key pairs and output fields) |
  | Output | **Destination and format** · **Field mapping**. Review output opens the full-page output preview. |

  - Stage name and result alias are not on the tabs; they are edited in Stage settings.
  - The Apply bar under the tabs is described under Configuration review. Its state updates as you type.
  - **Expand editor** enlarges the editor in place by hiding the data browser and Stage settings; **Restore layout** brings them back.
- **Stage settings.**
  - The stage's kind, its position ("Stage 4 of 6") and version.
  - **This stage:** Stage name; Result alias, with a hint naming how later stages read it (not editable on Output); and on SQL stages, Minimum units when the stage declares that INT32 parameter, otherwise the declared parameter names or "None declared".
  - **Position in pipeline:** Move earlier and Move later. A move that would break the Source-first, Output-last order is disabled, and its tooltip says why.
  - **Preview this stage** runs only the selected stage, calculating the earlier stages it reads from; Preview pipeline in the header runs every stage.
  - **Remove stage**, disabled with a reason for Source and Output.
  - **Pipeline preview settings**, shaded and marked as applying to every stage: the source limit (records per input) and invalid-record policy, read-only, with **Change execution settings**.
  - Below 1200 pixels wide the Stage settings column is hidden.
- **Data browser.**
  - Catalogue tab: a compact list grouped by category. Each category is collapsible and shows its entry count; a collapsed category renders none of its entries. Collapsed categories are remembered per design workspace and namespace. A search opens every category with matches, and clearing it restores the remembered collapse.
  - Pipeline data tab: bound inputs with their pinned provider version and SQL alias. Clicking a field inserts `alias.field` into the query.
  - Inspect binding. Query binding replaces the SQL stage's query with `SELECT alias.*` and clears its parameters.
  - Earlier stage results in scope, with Inspect and Insert query.
  - **Width.** The button beside Open catalogue switches between the layout's standard width for the screen size (226 pixels on most screens) and a wide one (420 pixels). Dragging the right edge sets any width from 200 to 640 pixels; with the edge focused, the arrow keys change it by 20 pixels (80 with Shift), and Home and End go to the limits. Double-clicking the edge restores the standard width. The width is remembered. The data browser opened as a dialog from Catalogue is not resizable.
- **Results.**
  - A summary showing status, result name, engine and "Destination unchanged". Messages appear for unmatched join records and sample limits.
  - Tabs:
    - Results: sortable; click a row for its values and source-record lineage.
    - Schema: type, nullability and origin.
    - Issues.
    - Lineage: contributing records per source.
    - Activity: an illustrative log.
  - Check query runs the local checks. While a preview runs, a progress strip shows stage n of m with Cancel.
- **Save version.** Name (required) and a version note.
- **Versions.** Lists the current version, versions saved this session and illustrative earlier versions. Older versions can't be opened. Export current design writes a bundle.

### Catalogue and contracts

- **Filters.** All, Read, Write or Views, plus a Connector filter (CSV, JSON, XML, JDBC, HTTP JSON).
- **Cards.** Each card shows availability, kind, capability, version, parameter count and schema summary, with Configure (by kind) and Inspect contract. Entries marked Not configured can be inspected but not added.
- **Contract inspector.** A version selector covering every saved version, and the tabs Overview, Parameters, Columns, Dependencies and Usage. It offers Open CSV mapping, Reload sample, and Add to pipeline or Configure output.

### Definition library

- Type filters, search, and cards showing each definition's version and reference count.
- Inspect shows the identifier and content, with Create revision. SQL queries also offer Open in workbench and Publish as view.

### Pipelines page

- The subtitle names the namespace and says what the design workspace has open, for example "Demo workspace has “Daily holdings enrichment” open in the workbench, with unsaved changes."
- **Saved pipelines** is one table: Name, Version, Flow (inputs → stages → output format), Engine, Last run and Actions.
  - The open pipeline is marked "Open in workbench", or "Open in workbench · unsaved".
  - Imported, migrated and snapshot pipelines carry a badge instead of their origin note.
  - Last run links to the latest run of that pipeline, or shows "Never run". Runs record the pipeline's identifier; older runs are matched by name.
- **Open** loads the pipeline into the workbench. **Configure** opens its full-page configuration; on the open pipeline it edits the current design, unsaved changes included. If the workbench has unsaved changes for a different pipeline, both ask to save or discard first, and either choice continues to the requested pipeline.
- **Export bundle** is offered on the open pipeline's row.
- Import bundle and New pipeline. When New pipeline is unavailable, a note says it needs an input binding.

### Connections

- **Cards** or **List**, remembered per design workspace and namespace. The list has one row per connection: name and description, connector, endpoint, credential reference and Inspect. Search filters either view.

### Run history

- Summary counts: all runs, queued or running, completed, failed or abandoned, cancelled.
- Columns: operation, kind, status, engine, result rows, started, submitted by, destination. Only the latest 40 runs are kept.
- Previews are recorded as completed, failed or cancelled runs.
- Run detail shows:
  - input and result counts, and simulated elapsed time;
  - the destination outcome, and the engine selection with its use case and profile;
  - stage progress.
- An abandoned run's destination is Unknown if it was publishing, otherwise Not started.

### Roles in the prototype

| Role          | Can                                                             |
| ------------- | --------------------------------------------------------------- |
| Viewer        | Discover and inspect                                            |
| Editor        | Also edit, preview, export and test connections                 |
| Publisher     | Also register providers, publish views and import bundles       |
| Operator      | Discover, inspect, execute and repeat runs (no preview)         |
| Administrator | Everything, including organisations, namespaces and workspaces  |

The sign-in page defaults to "Demo User" with the Editor role. The recorded principal is derived from the display name.

### Info popups

- Every screen title, the context menu, the role switcher, the execution engine, the data browser, the Cards/List switches, Stage settings and its pipeline preview settings, the Pipelines Actions column, the organisation tree and about 30 major actions have an ⓘ icon.
- Clicking it opens a popup with a title, an explanation, numbered steps where useful, and a note on conditions or roles. Escape, ✕ or a click outside closes it, and it closes when the page changes.
- Popups work for every role, including on actions the role cannot use, and opening one never runs the action.
- All the text is in one `HELP` table in the app script; `window.UnifyApp.help` exposes it.

### Other

- The Prototype guide dialog and the sidebar's Organisation structure entry.
- Escape closes an open info popup first, then the context menu, then any dialog.
- Structure names are 1–100 characters.

## Processing rules

### Configuration

- Any change clears the last configuration preview ("Settings changed; run the configured preview again").
- Saving a reader, definition or output, and applying a stage's changes, runs the preview first and is blocked while it reports errors. A blocked Apply keeps the edits and shows the error in the stage editor.
- Changing a reader's source format resets its settings, sample and fields. Changing a definition's type opens a fresh editor of that type.
- File readers (CSV, JSON, XML) accept any file connection (CSV, JSON or XML connector).

### Logical types

- STRING, INT32 (whole numbers in the 32-bit range), DOUBLE (finite), BOOLEAN (`true` or `false`, ignoring case).
- DATE `YYYY-MM-DD` (a real calendar date, leap years included), TIME `HH:mm:ss` with optional fractional seconds, DATETIME `YYYY-MM-DDTHH:mm:ss` without a time zone.
- Column names are simple identifiers. Internal names such as `_lineage` are reserved.
- Numeric bounds apply only to numeric columns. Minimum must not exceed maximum, and the same holds for lengths.
- A default must itself satisfy the column's type and rules.
- Inference from JSON samples:
  - all booleans → BOOLEAN;
  - all whole numbers in range → INT32;
  - other numbers → DOUBLE;
  - anything else → STRING.

  A column is nullable when any sampled value is null or missing. XML values are text, so they infer STRING.
- Parameter names must be simple and unique. A required parameter needs a value.

### Readers

- **CSV.** At most 128 source columns and 1,000 records. Delimiter and quote must be different single characters. The header must come before the first data record, and the selected range must lie inside the sample.
  - A leading byte-order mark is ignored.
  - A quote must start a field, and no text may follow a closing quote.
  - Header names must be nonempty and unique; otherwise choose headerless input and map by position.
  - Every selected record must have the header's field count.
  - Output names default to the header names, with other characters replaced by `_`. New columns default to STRING, not nullable.
  - Any mapping error blocks saving; the skip policy does not apply in the CSV reader.
- **JSON.** The array is at the root or under one member. Each entry must be an object, and duplicate members fail.
- **XML.**
  - Root and row names must match the configured local names and namespace URI.
  - Children in another namespace appear as `{uri}name`.
  - `xsi:nil` of `true` or `1` means null; a nil element with content fails.
  - Mixed text, nested children and repeated children fail.
- **JDBC.**
  - The source query must be one read-only SELECT. Write, DDL, ATTACH, PRAGMA, COPY, INSTALL, LOAD and MERGE keywords, and multiple statements, are rejected.
  - Each `?` needs one declared parameter.
  - A table read needs a simple table name.
  - The recorded result is supplied as a JSON array.
- **HTTP JSON.**
  - Only HTTP or HTTPS. HTTPS is required for anything other than `localhost`, `127.0.0.1` or `[::1]`.
  - Credentials in the URL, and Authorization, Proxy-Authorization or Cookie headers, are rejected; use a credential reference.
  - Header names and values are validated, and timeouts must be positive.
  - The recorded status must be 2xx, the content type must be JSON, and a successful response must have a body.

### Bindings during preview

- Each binding is read once per preview and reused by every stage that uses it.
- The binding's input schema is applied first, then its field mapping.
- A reader's sample must match the binding's connection and filename. A JDBC or HTTP sample must also match its query parameters; otherwise preview asks for a matching sample through Reload sample.
- The built-in example inputs (Holdings, Instruments, FxRates) preview only with their declared connection and default parameter values.
- A view binding runs the view's captured query over its captured inputs and preparation stages, then applies its result schema.

### Transformations, validation and diagnostics

- Trim and case operations require a STRING field. Empty-to-null makes the column nullable, and convert sets the column's type and nullability.
- Validation rules apply to the existing columns. A permitted-values rule needs at least one value.
- Each step keeps its first 100 issues. The rejection budget is shared across all stages of a preview.
- A stage's error is reported with the stage name. Stage inputs that form a cycle fail.

### Joins

- At least one key pair is required.
- The relationship check fails when the key is not unique on the left (one-to-one, one-to-many) or on the right (one-to-one, many-to-one).
- Output order: left records in order, each followed by its matches. RIGHT and FULL joins then append unmatched right records.
- The default projection includes every field. A right-side name that collides is prefixed `right_`.
- Counts shown: left, right, matched left, matched right, unmatched left, unmatched right, generated.

### Local SQL details

- Also supported:
  - AND, OR, NOT, IS [NOT] NULL, `||` concatenation, TRUE and FALSE;
  - `--` and `/* */` comments, and double-quoted identifiers;
  - the CAST type aliases INTEGER/INT, REAL, VARCHAR/TEXT and BOOL.
- `*` and `alias.*` expand to every column. SQL joins accept any boolean ON condition, and the record order follows the visual join rules.
- HAVING and ORDER BY can refer to output aliases. Aggregates are not allowed in WHERE, JOIN conditions or GROUP BY.
- Not supported: IN, BETWEEN, LIKE, OFFSET, NULLS FIRST/LAST.
- Typing is strict:
  - comparisons need matching types (use CAST);
  - arithmetic needs numbers;
  - NOT, AND and OR need BOOLEAN.
- Division always returns DOUBLE (5 / 2 = 2.5). Division by zero and non-finite results are errors.
- Computed columns need an AS alias. Duplicate output names and nested aggregates are rejected.
- Null sorts first ascending and last descending.
- COUNT of no rows is 0. SUM, AVG, MIN and MAX of no rows are null.
- TRIM removes spaces, LOWER and UPPER change ASCII letters only, and LENGTH counts code points.
- Results shown in the workbench are capped at the preview record limit.

### Output serialization

- **CSV.** Every non-null value is quoted, and the header is optional.
- **JSON.** Native scalars, with nulls kept.
- **XML.**
  - Writes `xsi:nil="true"` for null.
  - Root, row and column names must be valid element names.
  - The namespace URI is written as the default namespace.
- **JDBC.** Before the plan is prepared, the output is checked for:
  - destination columns that don't exist, and type mismatches;
  - mapped generated columns;
  - each record's conversion and constraints;
  - required columns that are not mapped and have no generated value or default.

  It also needs a JDBC connection and destination columns. The prepared plan is one parameterized INSERT in a single transaction.

## Preview behavior and boundaries

- PostgreSQL 19, Apache Calcite, SQLite, DuckDB and external connectors are **not connected**. This implements the local configuration and sample-processing UX. The technical-design draft remains unchanged pending functional-design approval.
- CSV, JSON and XML previews parse supplied records. JDBC and HTTP previews use explicitly supplied result/response samples; settings validation never claims a live connection or request succeeded. No JDBC query, HTTP request or external publication is executed.
- Mapping, ordered transformations, validation, joins and the documented local SQL subset run against selected tables. Stage preview resolves its required earlier stages; pipeline preview executes the configured sequence. Cancellation is available between processing steps. Browser JavaScript work is cooperative and does not provide driver/native-engine cancellation guarantees.
- Local SQL supports SELECT projections/aliases, arithmetic and comparisons, CASE/CAST, TRIM/LOWER/UPPER/LENGTH/COALESCE/NULLIF, typed positional parameters, joins, WHERE, GROUP BY/HAVING, COUNT/SUM/AVG/MIN/MAX, DISTINCT, ORDER BY and LIMIT. Unsupported syntax/functions, unknown fields and writes fail. Aggregate expressions reject ungrouped fields, including fields inside arithmetic, functions, casts, and CASE expressions. Subqueries, WITH, UNION, the complete portable profile and backend compatibility require Calcite and actual engine integration. The engine selector, including its automatic mode, demonstrates selection and recording only; it is not a performance comparison.
- Defaults are 100 sampled records across source reads, a 10,000-record source budget, 10,000 generated rows, 1 MiB working payload and a ten-second local deadline. Configurable previews support up to 1,000 sampled records. Result and source qualifications identify sampling; rows outside the sample are not validated. Uploaded samples have a separate 1 MiB cap; JSON/XML accept at most 1,000 sample records. These are prototype limits, not product capacity claims.
- Expected query result schemas are retained by reusable queries and views. Views enforce their captured result contract, including after the schema is revised; binding-specific field mappings then apply to the view result.
- The generated-record budget is cumulative across visual and SQL joins in one operation. Matched and unmatched outer-join rows consume the budget during generation, before filtering, grouping, or LIMIT. Source records have their own separate budget.
- Join keys require matching logical types; add an explicit conversion when needed. Null keys do not match. Projections require unique output names. Aggregate rows show contributor counts instead of an invented single-record origin.
- File output downloads contain actual mapped and serialized preview values. The unquoted configured CSV null token (default \N) represents null; literal values are quoted. Null tokens containing a separator, quote character, or line break are rejected. JDBC preparation checks supplied destination metadata, but real metadata discovery, transactions, precision and database constraints require backend tests. Prepared output never commits to a destination.
- Browser localStorage holds configuration and local history, not PostgreSQL-backed durability. Supplied history/log timestamps are illustrative; new elapsed times measure local preview work. Availability is example host metadata rather than implemented authorization. Do not enter real credentials; use reference names.

Ctrl+K opens workspace search; Ctrl+Enter previews the selected stage; Ctrl+S opens Save version. Escape closes an info popup, the context menu or an inspection dialog. The mockup loads its local data.js file; documentation links open only when clicked.

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

The tests run in the browser from the same folder. Open **index.html** with `?tests` added to its address:

| Address                       | Runs                                                             |
| ----------------------------- | ---------------------------------------------------------------- |
| `ui-mockup/index.html?tests`       | every test                                                       |
| `ui-mockup/index.html?tests=logic` | logic tests only: the embedded parsing, SQL, join and output libraries |
| `ui-mockup/index.html?tests=ui`    | UI workflow tests only                                           |

A results panel beside the app lists each test as it runs, with the failure message for any that fail; its buttons re-run all tests or either group. In test mode the app is not started on its own. Each UI workflow test starts the real app on the page, drives it, and stops it again, so the workflows can be watched as they run. Every start uses a fresh in-memory store, so configuration saved in the browser is never read or changed by the tests.

To run the same tests from a terminal or CI without opening a browser window, use the optional PowerShell runner in **scripts/** (it is not part of the mockup folder):

```powershell
.\scripts\run-ui-mockup-tests.ps1          # all tests; also: logic | ui, -All, -Keep, -TimeoutSeconds
```

It opens `index.html?tests` in headless Microsoft Edge (or Google Chrome) with a throwaway profile under `%TEMP%\unify-mockup-tests`, reads the result line the page writes to the browser log, prints a summary per group with any failure messages, and exits with 0 when everything passes, 1 when a test fails and 2 when the run could not complete. Tests run on real time; a full run takes about half a minute.

The suite exercises parsing, conversion, mapping, schema constraints, ordered rules, composite joins, SQL, serialization, budgets and full-page DOM workflows. It also checks direct file opening with only index.html and data.js, save/reopen, parameter binding, missing-source failures, recorded JDBC/HTTP samples, JDBC APPEND requirements and the 100-record preview regression. Regression coverage also checks sample reloads for all five reader formats, retained mappings, stage-specific SQL editing, reusable pipeline snapshots, grouped expressions, custom CSV null tokens, pipeline identity isolation, per-stage parameters, captured view schemas, required sample parameters, and cumulative join generation. Tests for the design 0.14 additions cover automatic and explicit engine selection and their run records, background executions from queued to completed (the running state is not asserted), repeating a run, abandonment after a restart, the seeded abandoned run, connection settings validation kept separate from simulated live tests, bundle export and conflict-checked import with renaming, and stage progress. Role tests check what each of the five roles can and cannot use, that denied actions and shortcuts change nothing, and that sign-out, sign-in, the recorded principal and the chosen role persist across a reload.

Stage configuration tests cover the tabs of each stage kind on the workbench: the Source stage's binding tabs and its locked picker; Apply, Revert, and automatic apply when another stage is selected or Ctrl+Enter previews; a refused Apply that keeps its edits and shows the error (undeclared query input, JDBC destination columns, CSV null token); a connection created from the output tab returning to it; Review output; Expand in place; and read-only tabs for an Operator. Workbench and navigation tests cover collapsible data browser categories, the Connections list view, the organisation tree, the header context menu, the Pipelines table with its save-or-discard prompt, and the info popups for every role.

Structure checks cover legacy migration, empty namespaces, resource isolation, organisation ownership validation, shared namespace resources with separate session drafts, draft recovery without persisted business records, deletion and discard behavior, stale revision conflicts, first-pipeline creation, scoped settings, and catalogue paging/search over 1,200 entries.

All automated checks are consolidated in **ui-mockup/tests.js**. The mockup itself needs only its HTML and data file. These checks do not verify visual layout, real transport, Calcite plans, database execution or destination writes. Browser visual review remains pending.
