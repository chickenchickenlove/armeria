/*
 * Copyright 2020 LINE Corporation
 *
 * LINE Corporation licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package com.linecorp.armeria.common.reactor3;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.reactivestreams.Subscription;

import com.linecorp.armeria.client.ClientRequestContext;
import com.linecorp.armeria.common.HttpMethod;
import com.linecorp.armeria.common.HttpRequest;
import com.linecorp.armeria.common.RequestContext;
import com.linecorp.armeria.common.RequestContextAccessor;
import com.linecorp.armeria.common.util.SafeCloseable;
import com.linecorp.armeria.internal.testing.AnticipatedException;
import com.linecorp.armeria.internal.testing.GenerateNativeImageTrace;

import reactor.core.publisher.Hooks;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;
import reactor.util.context.Context;
import reactor.util.function.Tuple2;

@GenerateNativeImageTrace
class RequestContextPropagationMonoTest {

    static Stream<Arguments> provideContextWriteAndCaptureTestCase() {
        return Stream.of(
                // shouldContextWrite, shouldContextCapture.
                Arguments.of(true, false),
                Arguments.of(false, true)
        );
    }

    @BeforeAll
    static void setUp() {
        Hooks.enableAutomaticContextPropagation();
    }

    @AfterAll
    static void tearDown() {
        Hooks.disableAutomaticContextPropagation();
    }

    @ParameterizedTest
    @MethodSource("provideContextWriteAndCaptureTestCase")
    void monoCreate_success(boolean shouldContextWrite,
                            boolean shouldContextCapture) {
        final ClientRequestContext ctx = newContext();
        final Mono<Object> mono;
        mono = addCallbacks(Mono.create(sink -> {
            assertThat(ctxExists(ctx)).isTrue();
            sink.success("foo");
        }).publishOn(Schedulers.single()), ctx, shouldContextWrite, shouldContextCapture);

        if (shouldContextCapture) {
            try (SafeCloseable ignored = ctx.push()) {
                StepVerifier.create(mono)
                            .expectNextMatches(s -> "foo".equals(s))
                            .verifyComplete();
            }
        } else {
            StepVerifier.create(mono)
                        .expectNextMatches(s -> "foo".equals(s))
                        .verifyComplete();
        }
    }

    @ParameterizedTest
    @MethodSource("provideContextWriteAndCaptureTestCase")
    void monoCreate_error(boolean shouldContextWrite,
                          boolean shouldContextCapture) {
        final ClientRequestContext ctx = newContext();
        final Mono<Object> mono;
        mono = addCallbacks(Mono.create(sink -> {
            assertThat(ctxExists(ctx)).isTrue();
            sink.error(new AnticipatedException());
        }).publishOn(Schedulers.single()), ctx, shouldContextWrite, shouldContextCapture);

        if (shouldContextCapture) {
            try (SafeCloseable ignored = ctx.push()) {
                StepVerifier.create(mono)
                            .verifyErrorMatches(t -> t instanceof AnticipatedException);
            }
        } else {
            StepVerifier.create(mono)
                        .verifyErrorMatches(t -> t instanceof AnticipatedException);
        }
    }

    @ParameterizedTest
    @MethodSource("provideContextWriteAndCaptureTestCase")
    void monoCreate_currentContext(boolean shouldContextWrite,
                                   boolean shouldContextCapture) {
        final ClientRequestContext ctx = newContext();
        final Mono<Object> mono;
        mono = addCallbacks(Mono.create(sink -> {
            assertThat(ctxExists(ctx)).isTrue();
            sink.success("foo");
        }).publishOn(Schedulers.single()), ctx, shouldContextWrite, shouldContextCapture);

        if (shouldContextCapture) {
            try (SafeCloseable ignored = ctx.push()) {
                StepVerifier.create(mono)
                            .expectNextMatches(s -> "foo".equals(s))
                            .verifyComplete();
            }
        } else {
            StepVerifier.create(mono)
                        .expectNextMatches(s -> "foo".equals(s))
                        .verifyComplete();
        }
    }

    @ParameterizedTest
    @MethodSource("provideContextWriteAndCaptureTestCase")
    void monoDefer(boolean shouldContextWrite,
                   boolean shouldContextCapture) {
        final ClientRequestContext ctx = newContext();
        final Mono<String> mono;
        mono = addCallbacks(Mono.defer(() -> Mono.fromSupplier(() -> {
            assertThat(ctxExists(ctx)).isTrue();
            return "foo";
        })).publishOn(Schedulers.single()), ctx, shouldContextWrite, shouldContextCapture);

        if (shouldContextCapture) {
            try (SafeCloseable ignored = ctx.push()) {
                StepVerifier.create(mono)
                            .expectNextMatches(s -> "foo".equals(s))
                            .verifyComplete();
            }
        } else {
            StepVerifier.create(mono)
                        .expectNextMatches(s -> "foo".equals(s))
                        .verifyComplete();
        }
    }

    @ParameterizedTest
    @MethodSource("provideContextWriteAndCaptureTestCase")
    void monoFromPublisher(boolean shouldContextWrite,
                           boolean shouldContextCapture) {
        final ClientRequestContext ctx = newContext();
        final Mono<Object> mono;
        mono = addCallbacks(Mono.from(s -> {
            assertThat(ctxExists(ctx)).isTrue();
            s.onSubscribe(noopSubscription());
            s.onNext("foo");
            s.onComplete();
        }).publishOn(Schedulers.single()), ctx, shouldContextWrite, shouldContextCapture);

        if (shouldContextCapture) {
            try (SafeCloseable ignored = ctx.push()) {
                StepVerifier.create(mono)
                            .expectNextMatches(s -> "foo".equals(s))
                            .verifyComplete();
            }
        } else {
            StepVerifier.create(mono)
                        .expectNextMatches(s -> "foo".equals(s))
                        .verifyComplete();
        }
    }

    @ParameterizedTest
    @MethodSource("provideContextWriteAndCaptureTestCase")
    void monoError(boolean shouldContextWrite,
                   boolean shouldContextCapture) {
        final ClientRequestContext ctx = newContext();
        final Mono<Object> mono;
        mono = addCallbacks(Mono.error(() -> {
            assertThat(ctxExists(ctx)).isTrue();
            return new AnticipatedException();
        }).publishOn(Schedulers.single()), ctx, shouldContextWrite, shouldContextCapture);

        if (shouldContextCapture) {
            try (SafeCloseable ignored = ctx.push()) {
                StepVerifier.create(mono)
                            .verifyErrorMatches(t -> t instanceof AnticipatedException);
            }
        } else {
            StepVerifier.create(mono)
                        .verifyErrorMatches(t -> t instanceof AnticipatedException);
        }
    }

    @ParameterizedTest
    @MethodSource("provideContextWriteAndCaptureTestCase")
    void monoFirst(boolean shouldContextWrite,
                   boolean shouldContextCapture) {
        final ClientRequestContext ctx = newContext();
        final Mono<String> mono;
        mono = addCallbacks(Mono.firstWithSignal(Mono.delay(Duration.ofMillis(1000)).then(Mono.just("bar")),
                                                 Mono.fromCallable(() -> {
                                                     assertThat(ctxExists(ctx)).isTrue();
                                                     return "foo";
                                                 }))
                                .publishOn(Schedulers.single()), ctx, shouldContextWrite, shouldContextCapture);

        if (shouldContextCapture) {
            try (SafeCloseable ignored = ctx.push()) {
                StepVerifier.create(mono)
                            .expectNextMatches(s -> "foo".equals(s))
                            .verifyComplete();
            }
        } else {
            StepVerifier.create(mono)
                        .expectNextMatches(s -> "foo".equals(s))
                        .verifyComplete();
        }
    }

    @ParameterizedTest
    @MethodSource("provideContextWriteAndCaptureTestCase")
    void monoFromFuture(boolean shouldContextWrite,
                        boolean shouldContextCapture) {
        final CompletableFuture<String> future = new CompletableFuture<>();
        future.complete("foo");
        final ClientRequestContext ctx = newContext();
        final Mono<String> mono;
        mono = addCallbacks(Mono.fromFuture(future)
                                .publishOn(Schedulers.single()), ctx, shouldContextWrite, shouldContextCapture);

        if (shouldContextCapture) {
            try (SafeCloseable ignored = ctx.push()) {
                StepVerifier.create(mono)
                            .expectNextMatches(s -> "foo".equals(s))
                            .verifyComplete();
            }
        } else {
            StepVerifier.create(mono)
                        .expectNextMatches(s -> "foo".equals(s))
                        .verifyComplete();
        }
    }

    @ParameterizedTest
    @MethodSource("provideContextWriteAndCaptureTestCase")
    void monoDelay(boolean shouldContextWrite,
                   boolean shouldContextCapture) {
        final CompletableFuture<String> future = new CompletableFuture<>();
        future.complete("foo");
        final ClientRequestContext ctx = newContext();
        final Mono<String> mono;
        mono = addCallbacks(Mono.delay(Duration.ofMillis(100)).then(Mono.fromCallable(() -> {
            assertThat(ctxExists(ctx)).isTrue();
            return "foo";
        })).publishOn(Schedulers.single()), ctx, shouldContextWrite, shouldContextCapture);

        if (shouldContextCapture) {
            try (SafeCloseable ignored = ctx.push()) {
                StepVerifier.create(mono)
                            .expectNextMatches(s -> "foo".equals(s))
                            .verifyComplete();
            }
        } else {
            StepVerifier.create(mono)
                        .expectNextMatches(s -> "foo".equals(s))
                        .verifyComplete();
        }
    }

    @ParameterizedTest
    @MethodSource("provideContextWriteAndCaptureTestCase")
    void monoZip(boolean shouldContextWrite,
                 boolean shouldContextCapture) {
        final CompletableFuture<String> future = new CompletableFuture<>();
        future.complete("foo");
        final ClientRequestContext ctx = newContext();
        final Mono<Tuple2<String, String>> mono;
            mono = addCallbacks(Mono.zip(Mono.fromSupplier(() -> {
                assertThat(ctxExists(ctx)).isTrue();
                return "foo";
            }), Mono.fromSupplier(() -> {
                assertThat(ctxExists(ctx)).isTrue();
                return "bar";
            })).publishOn(Schedulers.single()), ctx, shouldContextWrite, shouldContextCapture);

        if (shouldContextCapture) {
            try (SafeCloseable ignored = ctx.push()) {
                StepVerifier.create(mono)
                            .expectNextMatches(t -> "foo".equals(t.getT1()) && "bar".equals(t.getT2()))
                            .verifyComplete();
            }
        } else {
            StepVerifier.create(mono)
                        .expectNextMatches(t -> "foo".equals(t.getT1()) && "bar".equals(t.getT2()))
                        .verifyComplete();
        }
    }

    @Test
    void subscriberContextIsNotMissing() {
        final ClientRequestContext ctx = newContext();
        final Mono<String> mono;
        mono = Mono.deferContextual(Mono::just).handle((reactorCtx, sink) -> {
            assertThat((String) reactorCtx.get("foo")).isEqualTo("bar");
            sink.next("baz");
        });

        final Mono<String> mono1 = mono.contextWrite(reactorCtx -> reactorCtx.put("foo", "bar"));
        StepVerifier.create(mono1)
                    .expectNextMatches(s -> "baz".equals(s))
                    .verifyComplete();
    }

    @Test
    void ctxShouldBeCleanUpEvenIfErrorOccursDuringReactorOperationOnSchedulerThread() {
        // Given
        final ClientRequestContext ctx = newContext();
        final Mono<String> mono;

        // When
        mono = Mono.just("Hello")
                   .delayElement(Duration.ofMillis(1000))
                   .map(s -> {
                       if (s.equals("Hello")) {
                           throw new RuntimeException();
                       }
                       return s;
                   })
                   .contextWrite(Context.of(RequestContextAccessor.accessorKey(), ctx));

        // Then
        StepVerifier.create(mono)
                    .expectError(RuntimeException.class)
                    .verify();

        assertThat(ctxExists(ctx)).isFalse();
    }

    @Test
    void ctxShouldBeCleanUpEvenIfErrorOccursDuringReactorOperationOnMainThread() {
        // Given
        final ClientRequestContext ctx = newContext();
        final Mono<String> mono;

        // When
        mono = Mono.just("Hello")
                   .map(s -> {
                       if (s.equals("Hello")) {
                           throw new RuntimeException();
                       }
                       return s;
                   })
                   .contextWrite(Context.of(RequestContextAccessor.accessorKey(), ctx));

        // Then
        StepVerifier.create(mono)
                    .expectError(RuntimeException.class)
                    .verify();

        assertThat(ctxExists(ctx)).isFalse();
    }

    static Subscription noopSubscription() {
        return new Subscription() {
            @Override
            public void request(long n) {}

            @Override
            public void cancel() {}
        };
    }

    static boolean ctxExists(ClientRequestContext ctx) {
        return RequestContext.currentOrNull() == ctx;
    }

    static ClientRequestContext newContext() {
        return ClientRequestContext.builder(HttpRequest.of(HttpMethod.GET, "/"))
                                   .build();
    }

    private static <T> Mono<T> addCallbacks(Mono<T> mono0, ClientRequestContext ctx,
                                            boolean shouldContextWrite,
                                            boolean shouldContextCapture) {
        // doOnCancel and doFinally do not have context because we cannot add a hook to the cancel.
        final Mono<T> mono = mono0.doFirst(() -> assertThat(ctxExists(ctx)).isTrue())
                                  .doOnSubscribe(s -> assertThat(ctxExists(ctx)).isTrue())
                                  .doOnRequest(l -> assertThat(ctxExists(ctx)).isTrue())
                                  .doOnNext(foo -> assertThat(ctxExists(ctx)).isTrue())
                                  .doOnSuccess(t -> assertThat(ctxExists(ctx)).isTrue())
                                  .doOnEach(s -> assertThat(ctxExists(ctx)).isTrue())
                                  .doOnError(t -> assertThat(ctxExists(ctx)).isTrue())
                                  .doAfterTerminate(() -> assertThat(ctxExists(ctx)).isTrue());

        if (shouldContextWrite) {
            return mono.contextWrite(Context.of(RequestContextAccessor.accessorKey(), ctx));
        }

        if (shouldContextCapture) {
            return mono.contextCapture();
        }

        return mono;
    }
}
