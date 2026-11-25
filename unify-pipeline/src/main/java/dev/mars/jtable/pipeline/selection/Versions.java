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

/** Numeric dotted-version comparison: {@code 1.10.0} is newer than {@code 1.9.2}. */
final class Versions {

    private Versions() {
    }

    /**
     * Compares two versions by their leading numeric parts; missing parts count as 0 and any
     * non-numeric suffix of a part (such as {@code -dev}) is ignored.
     */
    static int compare(String a, String b) {
        if (a == null || b == null || a.isBlank() || b.isBlank()) {
            throw new IllegalArgumentException("A version cannot be blank");
        }
        String[] left = a.split("\\.");
        String[] right = b.split("\\.");
        for (int i = 0; i < Math.max(left.length, right.length); i++) {
            long l = i < left.length ? number(left[i], a) : 0;
            long r = i < right.length ? number(right[i], b) : 0;
            if (l != r) {
                return Long.compare(l, r);
            }
        }
        return 0;
    }

    private static long number(String part, String version) {
        int end = 0;
        while (end < part.length() && Character.isDigit(part.charAt(end))) {
            end++;
        }
        if (end == 0) {
            throw new IllegalArgumentException("Not a version: " + version);
        }
        return Long.parseLong(part.substring(0, end));
    }
}
