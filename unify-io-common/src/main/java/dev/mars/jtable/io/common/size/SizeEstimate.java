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
package dev.mars.jtable.io.common.size;

import dev.mars.jtable.core.schema.ColumnDefinition;
import dev.mars.jtable.core.schema.LogicalType;
import dev.mars.jtable.core.schema.TableSchema;

import java.util.Map;

/**
 * A source size normalised to a record estimate and a byte estimate, the units engine selection
 * works in (TD "Size probes"). A measured value is used directly; the other is derived
 * conservatively, towards the engine that tolerates more data:
 *
 * <ul>
 *   <li>bytes give records by dividing by the minimum encoded record width, an upper bound;</li>
 *   <li>records give bytes from declared widths, or the profile's measured average width per
 *       logical type.</li>
 * </ul>
 *
 * @param records         the record estimate
 * @param bytes           the byte estimate
 * @param recordsMeasured whether {@code records} was measured rather than derived
 * @param bytesMeasured   whether {@code bytes} was measured rather than derived
 */
public record SizeEstimate(long records, long bytes, boolean recordsMeasured, boolean bytesMeasured) {

    public SizeEstimate {
        if (records < 0 || bytes < 0) {
            throw new IllegalArgumentException("Estimates cannot be negative");
        }
    }

    /** No data. */
    public static final SizeEstimate ZERO = new SizeEstimate(0, 0, true, true);

    /**
     * Normalises a byte size (a file or a staged copy). {@code minimumRecordWidth} is the
     * smallest number of bytes one record can occupy in the source format, so the record estimate
     * is an upper bound.
     */
    public static SizeEstimate fromBytes(SourceSize size, long minimumRecordWidth) {
        if (size.unit() != SourceSize.Unit.BYTES) {
            throw new IllegalArgumentException("Not a byte size: " + size);
        }
        if (minimumRecordWidth < 1) {
            throw new IllegalArgumentException("The minimum record width must be at least 1 byte");
        }
        long records = size.value() / minimumRecordWidth + (size.value() % minimumRecordWidth == 0 ? 0 : 1);
        return new SizeEstimate(records, size.value(), false, true);
    }

    /**
     * Normalises a record count. The byte estimate is the count times the average record width:
     * for each column, its declared width when {@code declaredWidths} names one, otherwise the
     * measured average for its logical type in {@code averageWidths}.
     */
    public static SizeEstimate fromRecords(SourceSize size, TableSchema schema, Map<String, Long> declaredWidths,
                                           Map<LogicalType, Long> averageWidths) {
        if (size.unit() != SourceSize.Unit.RECORDS) {
            throw new IllegalArgumentException("Not a record count: " + size);
        }
        long width = 0;
        for (ColumnDefinition column : schema.columns()) {
            Long declared = declaredWidths.get(column.name());
            Long average = averageWidths.get(column.type());
            if (declared == null && average == null) {
                throw new IllegalArgumentException("No width for column '" + column.name() + "' of type " + column.type());
            }
            width += declared != null ? declared : average;
        }
        return new SizeEstimate(size.value(), Math.multiplyExact(size.value(), width), true, false);
    }

    /** The sum of two estimates; measured only when both parts were. */
    public SizeEstimate plus(SizeEstimate other) {
        return new SizeEstimate(Math.addExact(records, other.records), Math.addExact(bytes, other.bytes),
                recordsMeasured && other.recordsMeasured, bytesMeasured && other.bytesMeasured);
    }
}
