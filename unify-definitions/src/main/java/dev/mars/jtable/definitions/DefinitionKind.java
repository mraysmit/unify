/*
 * Copyright 2025 Mark Andrew Ray-Smith Cityline Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dev.mars.jtable.definitions;

/**
 * Every kind of stored definition (TD "Definition store"). Connections are versioned definitions
 * too, so a run can record exactly which connection settings it used; their payload holds
 * settings and a credential reference, never a credential value.
 */
public enum DefinitionKind {
    SCHEMA,
    MAPPING,
    TRANSFORMATION,
    SQL_QUERY,
    JOIN_PLAN,
    STAGE,
    PIPELINE,
    READER_PROVIDER,
    WRITER_PROVIDER,
    VIEW,
    CONNECTION,
    EXECUTION_SETTINGS,
    SELECTION_PROFILE;

    /** Whether definitions of this kind appear in the provider catalogue (FR034). */
    public boolean inCatalogue() {
        return this == READER_PROVIDER || this == WRITER_PROVIDER || this == VIEW;
    }
}
