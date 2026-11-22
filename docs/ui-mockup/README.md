© Mark Andrew Ray-Smith Cityline Ltd 2025\
Version 1.1.0

# Unify UI/UX prototype

Open [index.html](index.html) directly in a browser. All layout, styling, and application JavaScript are embedded in this single HTML file. Sample data stays separate in **data.js**, alongside the HTML.

To copy or share the mockup, take just **index.html** and **data.js**. It needs no build step, Node.js, node_modules, internet connection, or database setup. The remaining files support development, testing, and the optional local server.

For consistent browser storage, an optional local-only server is included:

```powershell
cd docs/ui-mockup
node serve.mjs
```

Then open **http://127.0.0.1:4321**. Stop the server with Ctrl+C. Nothing is published or remotely hosted.

## Suggested review

1. **Catalogue:** discover namespaced reader/writer providers and published views. Inspect Parameters, Columns, Dependencies, and Usage. `Files.Csv` reports that its columns are resolved at binding; `Warehouse.Positions` illustrates an unavailable provider. Catalogue entries describe contracts, without sample-row counts.
2. **Pipeline data:** switch the workbench's data browser to Pipeline data. Inspect the three bound inputs and earlier-stage results, including `NormalizedHoldings`. Later-stage results are excluded from the selected query's scope. This is separate from the catalogue and Definition library.
3. **Bind a provider:** inspect `Reference.Instruments`, select Add to pipeline, supply a unique logical name and alias, and choose its connection and parameter values. Query the new binding, preview it, and save a pipeline version to retain it. Supplied fixtures support their default connection and reader parameters.
4. **Publish a view:** open Portfolio exposure in the Definition library and choose Publish as view. Publish a name such as `Views.ReviewedExposure`, then discover and bind it with minimumUnits = 100. Query binding previews 10 rows. Republish the same name after revising the query: existing bindings retain their original provider and query versions. The provider inspector can display earlier view versions and their pinned dependencies.
5. **Connections and definitions:** create or inspect endpoint metadata and credential references in Connections. Creating a connection does not register a provider or bind data. Save schemas, mappings, transforms, joins, queries, and pipelines independently in the Definition library.
6. **Stages and visual designer:** edit and reorder stages, generate a left or inner join from output-column choices, and inspect typed results, issues, and row lineage. The fixtures contain 12 holdings, 10 instruments, and six currency rates; two holdings have no instrument match.
7. **Writers:** inspect `Outputs.Csv` and choose Configure output. This selects a writer, connection, filename, and publication mode in the output stage. Writers do not become SELECT inputs. Review and simulate business-data publication separately from publishing view metadata.
8. **Engines and runs:** switch SQLite/DuckDB while retaining definitions. Preview or cancel, inspect history, and try the query-error example in Prototype guide. Export sample results as CSV or the current design as JSON.

Ctrl+K opens workspace search; Ctrl+Enter previews the selected stage; Ctrl+S opens Save version; Escape closes a dialog. Dialogs contain focus, controls have labels, and layouts adapt to narrow screens.

## Prototype boundaries

This is a host-application UX exploration alongside the [functional design](../FUNCTIONAL_DESIGN.md). It does not change the embedded-library baseline. The technical-design draft remains unchanged pending functional review.

- PostgreSQL 19, Apache Calcite, SQLite, DuckDB, and external connectors are **not connected**. The engine selector and publication workflow simulate the proposed product behavior.
- The small JavaScript preview evaluates sample projections, multiplication, the supplied instrument/rate joins, units comparisons, ordering, and limits. It is not a SQL parser or engine. Unsupported sample operations report a limitation; full portable SQL belongs to the planned integration.
- Stage progress is simulated. Adding/reordering a stage changes the design; it does not implement a general pipeline executor. Source schemas and transformation policies shown are illustrative. Many-to-many is a saved relationship setting; the supplied reference fixture has unique keys.
- Provider registration and view publication are local metadata operations. Registration exposes approved connector kinds; this demonstration supports CSV writer registration. A custom provider can be registered and bound but has no data fixture. Supplied views and newly published sample queries can be invoked with `SELECT *` through their bindings; composing views, joining their results, and general view execution require the planned backend. The functional design describes the complete target behavior.
- Saved versions, reusable definitions, provider registrations, published-view versions, connections, and run records use this browser's localStorage. Pipeline bindings and edits require Save version. Availability is illustrative host metadata rather than an implemented permissions system. Storage availability for directly opened files depends on the browser; use the local server if persistence is unavailable. Exports provide a separate local copy.
- Supplied history and timings are illustrative, with no backend performance comparison. New elapsed times measure the UI simulation. CSV downloads contain actual computed sample values; unquoted `\N` represents null, while a literal token is quoted.
- The application makes no external requests. Documentation links open only when clicked. Source locations are example metadata; do not enter real credentials.

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

Node.js 20+ runs the core tests and optional server. DOM integration tests use the test-only jsdom dependency:

```powershell
npm ci --ignore-scripts
npm test
```

Without installing packages: `node --test prototype.test.cjs` checks sample computation, query failures, CSV semantics, and the local server. DOM tests exercise navigation, joins, stages, parameters, versions, reusable definitions, recovery, and publication simulation. They do not verify visual layout or replace browser review.
