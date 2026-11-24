© Mark Andrew Ray-Smith Cityline Ltd 2025\
Version 0.1

# Unify Technical Design

Status: Draft derived from the functional design  
Date: 2026-11-22  
Requirements baseline: [Unify Functional Design version 0.1](FUNCTIONAL_DESIGN.md)

## Design authority and implementation status

This design specifies the implementation of FR001 through FR024 and NFR001 through NFR007 in the functional design. Functional scope, value semantics, destination guarantees, and acceptance scenarios are defined there. A change to one of those contracts requires a functional design revision before an incompatible technical change.

The architecture and types below are proposed. Existing repository classes are identified explicitly; new type names and configuration examples are design interfaces, not currently available APIs. This document does not claim that the implementation or its tests already conform to the target contract.

## Repository baseline

| Area | Existing implementation | Required technical change |
| --- | --- | --- |
| Tables | [`ITable`](../unify-core/src/main/java/dev/mars/jtable/core/model/ITable.java), [`TableCore`](../unify-core/src/main/java/dev/mars/jtable/core/table/TableCore.java), `Column`, `Row`, `Cell`, `TableBuilder` | Introduce explicit schema metadata, strict value normalization, and atomic mutations |
| Alternative tables | `OptimizedTableCore`, `ProfiledOptimizedTableCore`, collection profiler | Share canonical semantics; qualify each implementation with the same contract tests |
| I/O contracts | [`IDataReader`](../unify-io-common/src/main/java/dev/mars/jtable/io/common/datasource/IDataReader.java), `IDataWriter`, `IDataSourceConnection`, table adapters | Add typed requests, lifecycle ownership, connector capabilities, and operation results |
| Mappings | [`MappingConfiguration`](../unify-io-common/src/main/java/dev/mars/jtable/io/common/mapping/MappingConfiguration.java), `ColumnMapping`, JSON/YAML/JDBC serializers | Compile immutable validated mappings and introduce a versioned configuration envelope |
| CSV | Separate readers, writers, and mapping readers/writers | Share one parser and one mapping execution path; retain quote provenance |
| JSON | Jackson parsing with first-record inference; some errors are printed and swallowed | Use bounded parsing and the common normalization and result pipeline |
| XML | Regular-expression parsing and manual escaping | Replace with structured, namespace-aware parsing and writing |
| JDBC | Readers and writers with separate connection paths; mapping writer binds strings and executes inserts individually | Use one connection lease per write, typed binding, metadata checks, and an explicit transaction |
| HTTP | `RESTConnection` and `RESTReader`; separate simplified response parsing | Use the same JSON decoder as file input, finite timeouts, and explicit response cleanup |
| NoSQL | Placeholder connection and sample-record reader | Exclude from the supported connector registry |
| Integration | CSV-to-H2 and CSV-to-SQLite demos and tests | Turn representative flows into acceptance coverage without making demos production orchestration |

The root [POM](../pom.xml) declares Java 23 properties but configures compiler source and target 17. The proposed baseline is Java 17 bytecode, expressed once using the compiler release setting. Implementation must first verify that the source and selected dependencies satisfy that baseline and align the README and module configuration. A different supported Java baseline is a documented technical decision, not an incidental compiler setting.

## Module architecture

Retain the existing Maven modules. `unify-io-files` continues to house JDBC and HTTP implementations despite its historical name; renaming artifacts is unnecessary for the functional baseline.

```mermaid
flowchart TD
    Host[Host application] --> Facade[Operation facade in unify-io-common]
    Facade --> Core[Schema and table model in unify-core]
    Facade --> SPI[Connector contracts in unify-io-common]
    Implementations[Connectors in unify-io-files] --> SPI
    Implementations --> Core
    Host --> Implementations
    Integration[unify-integration] --> Facade
    Integration --> Implementations
    Coverage[unify-coverage] -. aggregates .-> Core
    Coverage -. aggregates .-> Facade
    Coverage -. aggregates .-> Implementations
    Coverage -. aggregates .-> Integration
```

| Module | Target responsibility |
| --- | --- |
| `unify-core` | Logical types, schema, typed cells, conversion, validation, table storage, and table contracts; no connector dependencies |
| `unify-io-common` | Immutable operation definitions, mapping compiler, orchestration, capabilities, credential-provider contract, lifecycle leases, results, and configuration serialization |
| `unify-io-files` | Physical parsers, encoders, file publication, JDBC dialects and transport, HTTP transport, and connector registration |
| `unify-integration` | Executable examples and acceptance workflows using public APIs |
| `unify-coverage` | Coverage aggregation; no production behavior |

The host supplies a connector registry to the operation facade through constructor injection. The common module never imports concrete connectors. No dependency-injection framework, database ORM, server, or background scheduler is required. Registries may be shared when immutable; cursors, connections, counters, and cancellation state are operation-scoped.

Keep logging through SLF4J. Logging bindings belong to the host or runnable demonstrations; production library modules should not force Logback on consumers. Update JPMS descriptors alongside package exports and dependency changes, including access needed for configuration serialization and public exception types.

## Core schema and table model

### Target types

| Proposed type | Contents and invariant |
| --- | --- |
| `LogicalType` | STRING, INT32, DOUBLE, BOOLEAN, DATE, TIME, LOCAL_DATETIME |
| `ColumnDefinition` | Name, logical type, nullability, optional typed default, validation rules, and text conversion settings |
| `TableSchema` | Immutable ordered column definitions plus an immutable name-to-index lookup |
| `SourceValue` | Explicit MISSING, NULL, or SCALAR value; source scalar may retain its original lexical token |
| `CellValue` | NULL or one value of the declared logical type; MISSING never enters table storage |
| `TypedRow` | A fixed-length ordered vector of validated cells |
| `TableSnapshot` | Immutable schema and rows, with no open source resources |
| `MutableTable` | Schema-fixed storage with validated atomic append and cell-update operations |

Canonical Java representations are `String`, `Integer`, `Double`, `Boolean`, `LocalDate`, `LocalTime`, and `LocalDateTime`. Internal null cells use an explicit null representation. API adapters may expose Java null where required by existing signatures.

Validate a full candidate row before appending it. Convert and validate an updated value before replacing its cell. Never mutate shared schema metadata or expose mutable backing collections. A schema can be replaced only while a table has no rows; conversion of populated data creates a new table.

Use ordered column vectors and a name-to-index lookup rather than repeatedly building column lists for indexed access. Begin with ordinary operation-confined collections. Concurrent collection selection is an implementation option only after correctness and workload measurements; it does not establish table-wide thread safety.

### Conversion and validation pipeline

One `ValueNormalizer` implements the following order for every connector and programmatic mutation:

1. Resolve MISSING using a prevalidated explicit default, or nullability. Preserve the distinction between NULL and SCALAR.
2. Apply an explicitly configured text trim operation to textual scalars.
3. Apply empty-as-null if enabled; do not apply the missing-value default to the resulting null.
4. Enforce nullability.
5. Convert to the declared logical type using the configured locale and temporal patterns.
6. Reject overflow, non-finite doubles, unrecognized booleans, unsupported values, and invalid temporal fields.
7. Apply inclusive numeric bounds, string length bounds measured in Unicode code points, and typed permitted-value checks.

Defaults are normalized and validated when compiling the configuration. Validation metadata itself must be consistent: minimum cannot exceed maximum, and rules must apply to the declared type. The pipeline never manufactures a date using the clock and never uses permissive boolean conversion that maps arbitrary tokens to false.

Typed doubles use canonical value formatting on output. The old per-row `originalDoubleStrings` behavior is not part of the new value contract. Any lexical compatibility behavior retained for legacy entry points is isolated from canonical table storage.

### Inference algorithm

The schema resolver consumes at most the configured sample count, bounded by the read or preview record limit. Store sample records as raw `SourceValue` records and replay them through normalization after freezing the schema. Do not count sample replay as a second read or charge the same payload twice.

Infer names in encounter order across the sample, tracking missing or null fields as nullable. Headerless CSV requires a supplied positional definition. Classify non-null scalars as boolean, signed 32-bit integer, finite double, supported temporal value, or string. Integer-looking tokens outside INT32 and significant leading-zero tokens classify as string before fractional-number classification. Temporal inference uses the documented default patterns; custom input patterns require an explicit type.

The combination rules are: identical types remain unchanged; INT32 plus DOUBLE becomes DOUBLE; any other conflicting types become STRING; all-null columns become nullable STRING. Preserve original lexical tokens for string fallback, including large JSON integer tokens. After schema freeze, new unselected fields are ignored only when explicit projection allows it; otherwise a new field or incompatible value is a located record error. Empty input without schema follows FR002 rather than guessing a zero-column table.

## Mapping compilation

Compile configuration into an immutable `MappingPlan` before processing records. Each rule contains one validated selector, target index, target column definition, typed default, and normalization settings. Compile input and output mappings separately. An input plan produces the canonical table schema; an output plan produces the destination representation schema without mutating the source table.

Reject malformed selectors, duplicate target names, invalid positional definitions, unsupported option names, and incompatible capabilities. A well-formed selector for a field absent in an individual record produces MISSING and follows FR004; it is not itself a syntax error. For CSV, bind header names to positions once. Explicit mapping may intentionally ignore extra source fields, but CSV record width must still match its declared or header-established width.

Output conversion is checked for every accepted row before opening a destination for mutation. Requested type changes are explicit mapping operations. Encoders must not independently infer types or replace conversion failures with empty strings.

## Public execution API and connector contracts

The target facade has synchronous read, write, transfer, validate, and preview operations. These conceptual signatures describe the proposed API surface:

```java
ReadResult read(ReadRequest request, ExecutionContext context);
OperationResult write(WriteRequest request, ExecutionContext context);
OperationResult transfer(TransferRequest request, ExecutionContext context);
ValidationResult validate(OperationDefinition definition);
PreviewResult preview(PreviewRequest request, ExecutionContext context);
```

`ExecutionContext` contains the operation identifier, clock/deadline, cancellation signal, limits, credential provider, and diagnostic policy. Requests are immutable snapshots. A write takes a table snapshot so concurrent host mutation cannot change the data after validation.

`validate` checks configuration and declared capabilities without opening a destination for mutation. Source-aware validation occurs during preview or execution. Expected configuration, data, transport, and publication failures return typed results. `ReadResult` exposes a table only on successful completion. A legacy exception-based facade may adapt failures to existing exceptions while retaining the structured result as context.

| Proposed connector contract | Responsibility |
| --- | --- |
| `ConnectorDescriptor` | Identifier, typed option schema, supported directions, types and shapes, verified products, ownership modes, and publication guarantee |
| `SourceConnector` | Open an owned or borrowed `RecordCursor` and expose source metadata |
| `RecordCursor` | Produce one complete located raw record at a time and report recoverable record errors separately from fatal parsing errors |
| `DestinationConnector` | Validate representation and destination metadata, then create a `WriteSession` |
| `WriteSession` | Write prepared rows, publish or commit once, abort if possible, expose publication state, and release resources |
| `ResourceLease` | Associate a resource with OWNED or BORROWED lifecycle policy |
| `CredentialProvider` | Resolve a typed credential reference in memory without making it serializable configuration |

The registry contains CSV, JSON, XML, H2 JDBC, SQLite JDBC, and HTTP JSON implementations when their required capabilities are implemented and verified. It must not register the current NoSQL sample reader as supported. Product verification metadata includes the driver and product versions exercised by release tests; generic JDBC availability is not proof of compatibility.

## Versioned configuration

Introduce a `schemaVersion` envelope. Version 1 is the proposed first strict contract; existing unversioned `MappingConfiguration` content is legacy input. Connector options are typed and reject unknown fields. JSON and YAML decode into the same validated intermediate definition; neither format may enable executable tags or arbitrary class instantiation.

The following proposed definition illustrates separate input and output mappings. It is not accepted by the existing serializers without implementation changes.

```json
{
  "schemaVersion": 1,
  "kind": "transfer",
  "source": {
    "connector": "csv",
    "location": "persondata.csv",
    "options": { "header": true, "encoding": "UTF-8", "nullToken": "\\N" }
  },
  "inputMapping": [
    { "source": { "name": "id" }, "target": "personId", "type": "INT32", "nullable": false },
    { "source": { "name": "name" }, "target": "fullName", "type": "STRING", "nullable": false },
    { "source": { "name": "age" }, "target": "age", "type": "INT32", "nullable": true }
  ],
  "destination": {
    "connector": "jdbc-sqlite",
    "location": "jdbc:sqlite:people.db",
    "options": { "table": "person_data", "mode": "APPEND" }
  },
  "outputMapping": [
    { "source": { "name": "personId" }, "target": "person_id", "type": "INT32", "nullable": false },
    { "source": { "name": "fullName" }, "target": "full_name", "type": "STRING", "nullable": false },
    { "source": { "name": "age" }, "target": "age", "type": "INT32", "nullable": true }
  ],
  "errorPolicy": { "mode": "FAIL_FAST" },
  "limits": { "maxRecords": 100000, "deadlineMillis": 300000 }
}
```

A definition may supply an explicit schema instead of deriving it from complete input mapping rules, or request inference. Ambiguous combinations fail validation. Settings not shown above inherit the functional defaults. An endpoint needing credentials stores `credentialRef`, never a password, bearer token, or secret-bearing URL.

Use explicit legacy migration code rather than unrestricted bean deserialization. Migration identifies ambiguous empty/default semantics, inferred timezone or type assumptions, inline secrets, and options that lack a target equivalent. It returns located migration issues and does not silently reinterpret them. A host must externalize any credentials before saving a strict definition.

Existing JDBC mapping persistence may remain available through the legacy compatibility surface. New JDBC-backed configuration persistence, if retained, stores the same versioned definition and preserves ordering; it must not invent a second operation schema. It is not required to add a new configuration database to run ordinary transfers.

## Execution lifecycle and publication state

```mermaid
flowchart LR
    A[Validate definition] --> B[Read and resolve schema]
    B --> C[Normalize and materialize]
    C --> D[Prepare all output values]
    D --> E[Validate destination]
    E --> F[Write staged file or transaction]
    F --> G[Publish or commit]
    G --> H[Cleanup and return result]
    B -. failure .-> X[Cleanup and classify outcome]
    C -. failure .-> X
    D -. failure .-> X
    F -. failure .-> X
    G -. uncertain .-> X
```

Each stage checks cancellation and the deadline. Generate the operation identifier before validation so configuration failures also have an identifier. Close source resources after materialization and before destination writes, including when source and destination use the same SQLite database or local file.

Preview stops after its sample limit and reports whether EOF was observed or sampling was capped. Schema inference during preview uses only that sample. Validation of later data is performed by a new full execution; preview does not cache a promise of source consistency.

The coordinator tracks completion separately from destination outcome:

| Event | Completion | Destination outcome | Confirmed written |
| --- | --- | --- | --- |
| Read or preview succeeds | SUCCESS or SUCCESS_WITH_REJECTIONS | Not applicable | Not applicable |
| Failure or cancellation before destination mutation starts | FAILED or CANCELLED | NOT_STARTED | 0 |
| Successful file publication or database commit | SUCCESS or SUCCESS_WITH_REJECTIONS | COMMITTED | Number of published records |
| Write fails and transaction rollback is confirmed | FAILED or CANCELLED | ROLLED_BACK | 0 |
| Commit acknowledgement or rollback outcome cannot be established | FAILED | UNKNOWN | Unknown, represented without an invented number |
| Cancellation arrives after confirmed publication | SUCCESS or SUCCESS_WITH_REJECTIONS with a late-cancellation warning | COMMITTED | Number of published records |

Discarding an unpublished temporary file leaves the destination NOT_STARTED. A database transaction that has begun and is then successfully aborted reports ROLLED_BACK, including zero-row transactions. A zero-row successful database write may commit a zero-row transaction after schema validation and report COMMITTED with zero written.

Track complete records read, accepted, rejected, attempted writes, and confirmed writes separately. A fatal failure after a complete record is read but before classification may leave an unclassified read record; report its location and do not force the completed-run count identity. An incomplete terminal record is reported separately and is not counted as read. The result includes first retained issues, total issue count, and a truncation flag. Multiple field issues may belong to one rejected record.

Only a record-local conversion or validation error with a known next-record boundary qualifies for SKIP_RECORD. Preserve diagnostics but stop immediately when the explicit rejection limit is exceeded. A malformed document, limit violation, transport failure, or destination error cannot be downgraded to a rejected row.

## File publication

Encode to a uniquely named temporary file in the destination directory after all value conversion has passed. Use a restrictive creation policy appropriate to the filesystem and close the encoder and file before publication. Force staged file content where supported. Never stream partial output directly over an existing destination.

CREATE_NEW requires an atomic operation that fails if the target exists. One proposed local-filesystem implementation publishes a hard link to the completed staged file using `Files.createLink`, then removes the temporary name. This is enabled only for providers verified to support the necessary same-filesystem behavior. Do not replace it with an existence check followed by a potentially overwriting move.

REPLACE uses a verified atomic replacement primitive on the same filesystem. The Java implementation can request atomic move and replacement, but provider semantics must be verified rather than assuming that a particular option combination guarantees replacement on every platform. If the provider cannot supply the requested guarantee, fail without changing the target; do not fall back to delete-then-move or copy-overwrite.

Filesystem capability checks are repeated at publication because permissions or the destination may have changed. Failure to delete a temporary name after confirmed publication becomes a cleanup warning. A pre-publication failure removes only that operation's temporary artifact. Never delete a target merely because publication raised an ambiguous error; classify its outcome as UNKNOWN if completion cannot be established.

Reject unsupported symbolic-link destination arrangements and require the host to provide a trusted destination directory. Independent CREATE_NEW attempts cannot overwrite one another. Concurrent explicit replacements require host coordination when last-writer behavior is unacceptable.

The guarantee covers process-visible complete publication, not power-loss durability or a distributed transaction. Crash recovery, directory fsync portability, and automatic stale-artifact deletion require a separate extension. Temporary artifacts use a recognizable ownership marker so operators can identify them without treating arbitrary files as disposable.

## JDBC implementation

### Reads

Use one prepared statement and result set per source read. A table read generates a select from separately represented schema and table identifiers. A custom query is a trusted host-supplied read query with bound value parameters; never interpolate record values into SQL. Do not infer safety by inspecting only the first keyword or rewrite user query text.

Read-only enforcement must include database permissions or a verified database read-only mode. A JDBC read-only hint alone is insufficient. The supported-product fixture must establish that the chosen source configuration rejects mutation, including mutating statements disguised as queries. If a read-only query contract cannot be established for a product, advertise only the narrower verified capability.

Result metadata supplies ordinal, label, logical type information, and nullability where available. Duplicate labels require caller-provided aliases. Read cells using typed access and preserve SQL null. For products such as SQLite with flexible storage, apply the declared schema and validate each actual value rather than trusting a declared column type alone.

### Destination preflight

Use a product dialect for identifier quoting, metadata interpretation, type compatibility, and typed parameter binding. Schema, table, and column names are separate identifier segments. Reject invalid control characters and quote identifiers using the tested dialect; identifiers cannot be bound as value parameters. Caller SQL remains trusted application configuration.

Inspect the existing destination table and validate all selected columns. Check omitted non-nullable columns for a database default or generation rule. Validate string lengths, numeric ranges, and temporal representations against available metadata and the dialect contract. Unknown or ambiguous compatibility fails explicitly instead of relying on database coercion.

For H2, verify native integer, double, boolean, date, time, and timestamp bindings. For SQLite, define and test integer/real/text bindings, boolean 0/1 representation, and local temporal text representation. Exact details belong to each dialect's executable compatibility contract. The destination must preserve the logical values allowed by FR023; narrowing or precision loss is rejected before insertion.

### Transaction algorithm

1. Acquire an OWNED connection from the configured factory or data source. The factory may be host-supplied; the leased connection is still owned by this operation.
2. Verify transaction support, deadline/cancellation capabilities, metadata, and destination compatibility. Require a fresh connection without an active external transaction.
3. Disable autocommit and prepare one typed INSERT statement.
4. Bind already prepared rows with the declared SQL types and explicit SQL null types. Execute bounded batches without committing between batches.
5. Check cancellation immediately before commit. Commit once after all batches succeed.
6. On a failure before a commit attempt, request rollback and report ROLLED_BACK only after rollback succeeds.
7. If commit throws, retain UNKNOWN unless product-specific evidence conclusively establishes the outcome. A later successful rollback does not prove that an earlier commit was not applied.
8. Close statements and release the connection. Post-commit cleanup failures become warnings and cannot turn a confirmed commit into a reported rollback.

Do not run DDL, truncate, upsert, or retry in this path. Batch size is a transport optimization, not a transaction boundary. A failure in a later batch must roll back inserts from earlier batches.

Borrowed caller connections are permitted for reads with no changes to transaction state. Reject borrowed connections for baseline writes rather than committing the caller's transaction or promising a durable result from a savepoint. A failed connection restoration or reset must cause an owned pooled connection to be invalidated according to the pool contract rather than returned as healthy.

## File decoders and encoders

### CSV codec

Implement one record scanner shared by ordinary and mapped CSV APIs. It returns decoded field text, whether the field was quoted, record ordinal, and physical line information. Retaining quote provenance is necessary to distinguish an unquoted null token from quoted literal text.

The scanner has explicit field-start, unquoted-field, quoted-field, and after-quote states. Delimiters and record separators have structural meaning only outside quotes. A doubled quote inside a quoted field represents one quote. A quote in an unquoted field or unexpected content after a closing quote is invalid. EOF in a quoted field is fatal; EOF elsewhere completes the final record when one exists. Handle LF and CRLF without splitting quoted multiline fields. Preserve trailing empty fields and reject inconsistent widths.

Validate delimiter and quote settings as distinct single characters that are not record separators. Decode with the specified charset and report malformed input rather than silently substituting replacement characters. UTF-8 input may contain an initial byte-order mark, which is consumed once before header parsing. A physical empty record is a one-field empty record, then subject to width and value validation.

The writer quotes values containing delimiters, quotes, CR/LF, or a literal null-token collision, and doubles internal quotes. A null uses the unquoted configured null token. Reject a null token containing a delimiter, quote, or line break because it cannot be emitted unquoted unambiguously. Without a null token, null output is a representation error found before publication. A lossless export does not add spreadsheet formula-escaping characters; any such host transformation must be explicit.

### JSON codec

Use Jackson for both file and HTTP JSON. Enable duplicate-member detection and bound nesting, scalar size, and total bytes. Accept a root array or one selected root-object member containing an array. Consume and validate the rest of the document, including trailing content, before declaring a full read successful.

Records must be objects. Preserve native null and scalar tokens; nested selected fields fail. Unselected nested fields may be skipped structurally, subject to the same depth and byte limits. Integer tokens must not be narrowed before schema inference or explicit conversion. Configure the parser without polymorphic type activation.

Use a JSON generator for output, emitting native numbers and booleans, formatted temporal strings, and explicit null. Emit fields in mapping order. Do not use a generic string accessor for every cell.

### XML codec

Use a namespace-aware JDK XML parser and writer, with StAX as the proposed implementation. Disable DTD and external entity support, install a resolver that refuses external access, and reject document-type events. Verify these properties against the actual parser provider in tests. Predefined XML character entities remain usable.

Match elements by namespace URI and local name. Keep an explicit root/row/field parser state, rejecting invalid selected nesting, mixed content, repeated selected fields, and malformed nil elements. Represent `xsi:nil="true"` or `xsi:nil="1"` as NULL; preserve an empty non-nil element as empty text. Apply FR004 when a child is absent.

Validate output element names and namespace bindings during preflight. A table column name that cannot be represented as a configured XML element name requires explicit renaming; do not sanitize it silently. Let the XML writer handle escaping and namespace declarations. Emit UTF-8 with a matching declaration. XML encoding declarations govern input decoding; conflicting explicit encoding settings fail validation.

## HTTP JSON connector

Keep HTTP transport separate from the JSON record decoder. The existing `HttpURLConnection` boundary can be retained with a new operation-scoped wrapper that disables redirects, configures finite connect/read timeouts, checks the overall deadline, and always closes the body and disconnects the connection. A shared connection object must not carry mutable per-operation headers or credentials.

Construct one logical GET request from typed endpoint settings and encoded query parameters. Reject disallowed methods and header overrides. Resolve credentials only when opening the request, use normal TLS certificate verification, and require explicit permission for non-loopback cleartext transport. Do not perform a preliminary probe request that duplicates the GET.

Accept only successful responses with a supported JSON media type and an actual JSON body. Reject redirects and non-success status with a sanitized issue. Read the body through byte-counting and progress-aware wrappers. Recompute the remaining operation budget when applying each blocking timeout and close the request when cancellation is signalled. No application-level retry is performed. Low-level transport behavior must be verified by request-counting protocol fixtures rather than assumed.

The baseline requests uncompressed content and rejects unsupported content encoding. A future decompression feature must enforce both encoded and decoded limits. Error bodies are drained only within a small configured diagnostic budget or closed immediately; they are not logged by default.

No pagination, token refresh, XML/CSV fallback parser, response-format guessing, or HTTP writes are introduced in this connector.

## Resource ownership and cleanup

| Resource | Ownership and release rule |
| --- | --- |
| Files and parsers opened by a connector | OWNED; close on every exit path |
| Temporary output file | OWNED; discard before publication or unlink staging name after publication |
| HTTP response and connection | OWNED; close body and disconnect on success, error, timeout, or cancellation |
| JDBC statements and result sets | OWNED even when their connection is borrowed; close before releasing connection |
| JDBC connection leased for an operation | OWNED; close or return/invalidate through its provider |
| Caller-provided read connection or stream | BORROWED; leave it open and preserve caller transaction state |
| Credential material | Operation-scoped; never serialize it or attach it to public diagnostic objects |

Use explicit closeable leases and structured cleanup, with suppressed cleanup failures attached internally to the primary failure. Report sanitized cleanup issues without losing the initial phase or error. Release locks and owned resources before returning a successful read or preview. A cleanup failure before successful read completion produces a failed read; after confirmed destination publication it produces a warning on the confirmed result.

## Limits cancellation and diagnostic safety

Use one `LimitBudget` per invocation. It enforces physical input bytes, complete records, columns, per-value UTF-8 bytes, aggregate materialized value bytes, retained issues, and the overall deadline. The functional design defines the initial values. Encoded input size and materialized-value size are different budgets; defaults, JSON unescaping, and projection can change their relationship.

Read incrementally even though accepted records are ultimately materialized. Reject excessive headers, column counts, nesting, or scalar lengths before allocating unbounded buffers. Raw inference samples and accepted typed records share the operation's materialization budget. Preparing output must not create a second unbounded copy; retain a bounded prepared representation or rescan the immutable snapshot with the same deterministic plan.

Check cancellation between records, batches, and immediately before publication. Use supported statement timeout/cancel APIs and transport timeouts for blocking calls. A deadline requests cooperative termination; it is not an in-process thread-kill guarantee when a driver ignores cancellation. After an overdue blocking call returns, do not begin publication. Qualify supported drivers with cancellation tests and document any maximum blocking interval. Never report rollback until it is confirmed.

Diagnostics use stable error codes and immutable issue objects containing phase, connector identity, source record ordinal, optional physical position, target/source field identifier, and sanitized message. Do not store raw endpoint URLs, SQL parameters, authorization headers, JDBC connection properties, response bodies, or record values by default. Keep internal causes out of default public serialization when driver messages might contain secrets.

An explicit value-diagnostics mode may include record values after host-controlled redaction and size limits. It never includes resolved credentials. The default logging event contains operation identifier, connector identifiers, elapsed time, phase, counts, completion, and destination outcome. Logging does not replace the programmatic result.

## Compatibility and migration

Introduce the strict facade and configuration version without silently changing current `ITable`, reader/writer, and serializer behavior. Characterize existing observable behavior before redirecting old entry points. Legacy APIs remain a clearly named compatibility surface during a documented deprecation period; they do not establish conformance with the strict functional baseline.

Where feasible, legacy adapters delegate to canonical storage with an explicit legacy normalization or presentation policy. Keep changes requiring different semantics behind the strict API until a declared breaking release. Do not permit mixed strict and legacy mutation of the same execution snapshot.

Unify ordinary and mapping readers/writers behind the same codec and mapping pipeline after their compatibility boundaries are explicit. Retire swallowed exceptions, implicit type inference in writers, and ad hoc connection creation in the strict path. Do not preserve the NoSQL sample output as a successful strict connector result.

Existing JDBC configuration serialization and experimental create-table or REST methods can remain available as legacy features. They are not automatically exposed in the supported strict registry. Migrations must distinguish this exclusion from removal of public methods.

## Verification strategy

Mockito and substitute mocking frameworks are prohibited. Use real implementations, protocol fixtures, purpose-built fakes for narrow internal contracts, and live integration environments where compatibility cannot be established locally.

| Verification layer | Required coverage |
| --- | --- |
| Core behavior | Atomic mutation, strict conversion, null/default distinctions, inference combination rules, schema order, validation, and limit arithmetic |
| Property tests | Logical-value round trips under an explicit schema; mapping-order preservation; mutation failure leaves state unchanged |
| File fixtures | Actual encoded files with quoting, line breaks, Unicode, malformed encoding, null-token collisions, empty data, and mid-write failures |
| File publication | Actual temporary directories; CREATE_NEW races; replacement failures; unsupported-provider behavior; old-file preservation; cleanup |
| JSON and XML | Actual parser and generator behavior, duplicate members, depth/size boundaries, namespaces, nil, nested selected values, and refused external resolution |
| HTTP protocol | Local server asserting method, URL encoding, headers, request count, response handling, error status, redirect refusal, timeouts, cancellation, and closure |
| JDBC compatibility | Real H2 and SQLite; metadata, typed binding, nulls, temporal values, empty results, quoted identifiers, constraints, batch rollback, read-only access, and connection cleanup |
| Outcome classification | Publication succeeds then acknowledgement is lost; rollback fails; cleanup fails before and after commit |
| Configuration | JSON/YAML equivalence, ordered mappings, unknown fields and versions, legacy migrations, and secret exclusion |
| Full workflows | FD acceptance scenarios AC01 through AC18 through public APIs |

For uncertain-commit classification, a purpose-built JDBC connection decorator may delegate a real commit to a real database and then throw, allowing verification that the result remains UNKNOWN even when rows exist. This verifies coordinator semantics, not vendor transport compatibility. Use a protocol fault proxy or live environment when a release claim depends on a particular driver's network failure behavior. Lifecycle fakes cannot establish database compatibility.

Configure test discovery explicitly. Several existing JDBC test names end in `TestH2` or `TestSQLite`; verify that the effective test runner includes them rather than assuming the normal suffix patterns discover them. Fail release verification when a required connector suite runs no tests. Keep benchmarks separate from correctness tests.

Use `mvn test` for the defined local suite and `mvn verify` for complete configured acceptance and coverage checks. Publish product/driver versions and actual executed test counts with release verification. Coverage aggregation must include all intended production modules rather than only one package. Performance baselines are measured after correctness using recorded heap, dataset shape, parser options, and hardware; this design supplies no unmeasured throughput target.

## Requirement traceability

Technical sections below are named design areas; acceptance identifiers refer to the functional design.

| Requirement | Implementing design areas | Acceptance coverage |
| --- | --- | --- |
| FR001 | Core schema and table model; public execution API | AC01 |
| FR002 | Inference algorithm; bounded cursors | AC04 |
| FR003 | Conversion pipeline; typed encoders and JDBC binders | AC01, AC03, AC11, AC18 |
| FR004 | SourceValue states; normalization; format null codecs | AC03, AC11 |
| FR005 | Mapping compilation; separate input/output plans | AC03, AC14 |
| FR006 | Compiled validation rules; atomic row acceptance | AC01, AC03 |
| FR007 | Materialization; read result publication | AC06 |
| FR008 | File publication; preflight encoding | AC07, AC18 |
| FR009 | Execution lifecycle; output preparation | AC07, AC08, AC15 |
| FR010 | Record error classification; bounded rejection policy | AC04, AC08 |
| FR011 | Completion and destination state; independent counters | AC06, AC08, AC10, AC17 |
| FR012 | Versioned configuration; strict option validation; migration | AC05, AC12, AC14 |
| FR013 | Public preview; sample and EOF metadata | AC05 |
| FR014 | CSV scanner and encoder | AC02, AC03, AC18 |
| FR015 | Shared JSON codec | AC03, AC11, AC12 |
| FR016 | Namespace-aware XML codec and external-resolution refusal | AC03, AC11, AC12 |
| FR017 | JDBC read contract and metadata | AC09 |
| FR018 | Destination preflight; typed transactional append | AC09, AC10, AC18 |
| FR019 | HTTP JSON connector and protocol fixtures | AC13 |
| FR020 | Capability descriptors and supported registry | AC12 |
| FR021 | Resource leases; outcome-aware cleanup | AC13, AC15 |
| FR022 | Safe issues, redaction, and structured logging | AC14, AC17 |
| FR023 | Explicit output schema; representation preflight | AC02, AC11, AC18 |
| FR024 | LimitBudget; cancellation; publication state | AC13, AC15 |
| NFR001 | One normalization and mapping pipeline | AC03 |
| NFR002 | Incremental parsing and bounded materialization | AC15 |
| NFR003 | Typed configuration; prepared values; safe codecs | AC12 |
| NFR004 | Explicit commit and publication state machine | AC09, AC10 |
| NFR005 | Version envelope and legacy compatibility boundary | AC14 |
| NFR006 | Immutable registry/requests; operation-confined resources | AC16 |
| NFR007 | Actual files, HTTP fixtures, real databases, live tests as needed | AC09, AC13 |

## Implementation sequence

| Stage | Deliverable | Exit evidence |
| --- | --- | --- |
| 1 | Confirm functional scope; reconcile Java build; characterize existing compatibility | Reviewed FD decisions D01 through D06, effective build settings, executed-test inventory |
| 2 | Schema, SourceValue/CellValue, normalization, validation, inference, atomic tables | AC01, AC03, AC04 and core contract/property tests |
| 3 | Strict requests, configuration envelope, mapping compiler, results, limits, ownership, preview | AC05, AC06, AC08, AC14, AC15, AC17 |
| 4 | Shared CSV/JSON/XML codecs and verified file publication | AC02, AC07, AC11, AC12, AC18 |
| 5 | H2/SQLite read contracts, metadata preflight, transactional write sessions | AC09, AC10, destination cases from AC18 |
| 6 | Single-response HTTP JSON using the shared decoder | AC13 |
| 7 | End-to-end qualification, concurrent independent operations, migration guides, capability documentation | AC01 through AC18, including AC16; accurate supported-connector matrix |

Stages add conformance to one functional baseline; incomplete connectors remain absent from the supported registry. Existing demos may continue through compatibility APIs while strict APIs are implemented.

## Technical decisions requiring implementation verification

| ID | Proposed decision | Required evidence before release |
| --- | --- | --- |
| TD01 | Java 17 release target with a single Maven compiler baseline | Clean compilation, dependency compatibility, and effective configuration |
| TD02 | Retain modules and use constructor-injected connector registration | Dependency direction and JPMS checks |
| TD03 | One canonical conversion pipeline and shared codec per format | Cross-connector contract tests and legacy characterization |
| TD04 | Same-directory staging with verified no-overwrite publication and atomic replacement | Tests on every advertised filesystem provider and operating system |
| TD05 | One owned JDBC transaction per append; no batch commits | Real database rollback, cleanup, and uncertain-outcome scenarios |
| TD06 | Shared Jackson JSON codec, JDK structured XML codec, explicit CSV scanner | Parser boundary, null, encoding, duplicate, and malformed-input fixtures |
| TD07 | No distributed transaction, automatic write retry, or production NoSQL connector in this baseline | Registry validation and public capability documentation |

Implementation feedback that requires changing a functional guarantee must identify the affected FR or NFR, revise the functional design, and update the corresponding traceability and acceptance coverage.
