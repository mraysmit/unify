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

import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * The typed outcome of every host service and store call (TD "Host configuration API"). Expected
 * outcomes are values, not exceptions; exceptions are reserved for programming errors.
 *
 * <ul>
 *   <li>{@link Ok}: the value.</li>
 *   <li>{@link Invalid}: validation failed, with located issues.</li>
 *   <li>{@link Denied}: the access policy refused the action on the resource; nothing changed.</li>
 *   <li>{@link Conflict}: a concurrent revision or a stale draft; nothing was written, and the
 *       current state is attached.</li>
 *   <li>{@link Unavailable}: the store or an endpoint cannot be reached or is incompatible.</li>
 * </ul>
 */
public sealed interface Result<T> {

    record Ok<T>(T value) implements Result<T> {
    }

    record Invalid<T>(List<Issue> issues) implements Result<T> {
        public Invalid {
            if (issues.isEmpty()) {
                throw new IllegalArgumentException("An invalid result needs at least one issue");
            }
            issues = List.copyOf(issues);
        }
    }

    record Denied<T>(String action, String resource) implements Result<T> {
        public Denied {
            Objects.requireNonNull(action, "action");
            Objects.requireNonNull(resource, "resource");
        }
    }

    /** {@code current} describes what is there now, for example the latest version. */
    record Conflict<T>(String reason, Object current) implements Result<T> {
        public Conflict {
            Objects.requireNonNull(reason, "reason");
        }
    }

    record Unavailable<T>(String reason) implements Result<T> {
        public Unavailable {
            Objects.requireNonNull(reason, "reason");
        }
    }

    /**
     * One located validation issue. {@code location} is a path into the request or definition
     * (for example {@code body.columns[2].type}); messages never repeat business values.
     */
    record Issue(String code, String location, String message) {
        public Issue {
            Objects.requireNonNull(code, "code");
            Objects.requireNonNull(message, "message");
        }
    }

    static <T> Result<T> ok(T value) {
        return new Ok<>(value);
    }

    static <T> Result<T> invalid(List<Issue> issues) {
        return new Invalid<>(issues);
    }

    static <T> Result<T> invalid(String code, String location, String message) {
        return new Invalid<>(List.of(new Issue(code, location, message)));
    }

    static <T> Result<T> denied(String action, String resource) {
        return new Denied<>(action, resource);
    }

    static <T> Result<T> conflict(String reason, Object current) {
        return new Conflict<>(reason, current);
    }

    static <T> Result<T> unavailable(String reason) {
        return new Unavailable<>(reason);
    }

    default boolean isOk() {
        return this instanceof Ok;
    }

    /** The value of an {@link Ok}; fails for any other outcome. */
    default T orElseThrow() {
        if (this instanceof Ok<T> ok) {
            return ok.value();
        }
        throw new IllegalStateException("Not ok: " + this);
    }

    /** Transforms the value of an {@link Ok}; every other outcome passes through unchanged. */
    @SuppressWarnings("unchecked")
    default <U> Result<U> map(Function<? super T, ? extends U> mapper) {
        if (this instanceof Ok<T> ok) {
            return new Ok<>(mapper.apply(ok.value()));
        }
        return (Result<U>) this;
    }

    /** Continues with another call when this is {@link Ok}; every other outcome passes through. */
    @SuppressWarnings("unchecked")
    default <U> Result<U> then(Function<? super T, Result<U>> next) {
        if (this instanceof Ok<T> ok) {
            return next.apply(ok.value());
        }
        return (Result<U>) this;
    }
}
