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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HexFormat;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

/**
 * The stored form of one definition version: {@code {envelopeVersion, kind, body}} (TD
 * "Payloads"). The body is validated against its kind's versioned schema before saving; stored
 * versions are never rewritten, and an older envelope version is upgraded in memory on load.
 *
 * <p>{@link #contentSha256()} hashes a canonical form, with object keys sorted and no
 * insignificant whitespace, so two saves of the same content always hash equally. A revision
 * whose hash equals the latest version's returns that version unchanged.</p>
 */
public record Envelope(int envelopeVersion, DefinitionKind kind, JsonNode body) {

    private static final ObjectMapper JSON = new ObjectMapper();

    public Envelope {
        if (envelopeVersion < 1) {
            throw new IllegalArgumentException("Envelope versions start at 1");
        }
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(body, "body");
        if (!body.isObject()) {
            throw new IllegalArgumentException("A definition body must be a JSON object");
        }
        body = body.deepCopy();
    }

    /** The envelope as stored: {@code {"envelopeVersion":…, "kind":…, "body":…}}. */
    public ObjectNode toJson() {
        ObjectNode node = JSON.createObjectNode();
        node.put("envelopeVersion", envelopeVersion);
        node.put("kind", kind.name());
        node.set("body", body.deepCopy());
        return node;
    }

    /** Reads a stored envelope. */
    public static Envelope fromJson(JsonNode node) {
        if (node == null || !node.isObject() || !node.path("envelopeVersion").canConvertToInt()
                || !node.path("kind").isTextual() || !node.path("body").isObject()) {
            throw new IllegalArgumentException("Not a definition envelope");
        }
        return new Envelope(node.path("envelopeVersion").asInt(), DefinitionKind.valueOf(node.path("kind").asText()),
                node.path("body"));
    }

    /** Reads a stored envelope from JSON text. */
    public static Envelope parse(String json) {
        try {
            return fromJson(JSON.readTree(json));
        } catch (IOException e) {
            throw new IllegalArgumentException("Not valid JSON: " + e.getMessage(), e);
        }
    }

    /** The SHA-256 of the canonical envelope, as lower-case hex. */
    public String contentSha256() {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(canonical(toJson()).getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }

    /** The canonical text: keys sorted, no insignificant whitespace, arrays in order. */
    static String canonical(JsonNode node) {
        try {
            return JSON.writeValueAsString(sorted(node));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static JsonNode sorted(JsonNode node) {
        if (node.isObject()) {
            List<String> names = new ArrayList<>();
            Iterator<String> it = node.fieldNames();
            it.forEachRemaining(names::add);
            Collections.sort(names);
            ObjectNode copy = JSON.createObjectNode();
            for (String name : names) {
                copy.set(name, sorted(node.get(name)));
            }
            return copy;
        }
        if (node.isArray()) {
            ArrayNode copy = JSON.createArrayNode();
            node.forEach(element -> copy.add(sorted(element)));
            return copy;
        }
        return node;
    }
}
