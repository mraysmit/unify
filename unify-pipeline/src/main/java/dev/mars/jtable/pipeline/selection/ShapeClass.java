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
package dev.mars.jtable.pipeline.selection;

/**
 * The pipeline shape classes the use-case tests measure (TD "Selector inputs"). Every pipeline
 * falls into exactly one: the first that applies, in declaration order.
 */
public enum ShapeClass {
    /** Two or more joins. */
    MULTI_JOIN,
    /** A join plus aggregation or DISTINCT. */
    JOIN_AGGREGATE,
    /** Aggregation or DISTINCT without a join. */
    AGGREGATE,
    /** Exactly one join. */
    JOIN,
    /** Ordering without joins or aggregation. */
    SORT,
    /** Load, filter, project, map and convert only. */
    SCAN;

    /** The shape class of a pipeline with these features. */
    public static ShapeClass classify(int joins, boolean aggregation, boolean distinct, boolean ordering) {
        if (joins < 0) {
            throw new IllegalArgumentException("The join count cannot be negative");
        }
        boolean aggregates = aggregation || distinct;
        if (joins >= 2) {
            return MULTI_JOIN;
        }
        if (joins == 1) {
            return aggregates ? JOIN_AGGREGATE : JOIN;
        }
        if (aggregates) {
            return AGGREGATE;
        }
        return ordering ? SORT : SCAN;
    }
}
