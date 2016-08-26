package com.metabroadcast.promise;

import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.google.common.base.Throwables;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A wrapper for {@link ListenableFuture} that provides convenience methods for chaining
 * sequential transformations on a future while avoiding the cumbersome
 * {@code MoreFutures.transform(future, function)} syntax and also allows for incorporating
 * exception handling in a fashion similar to JavaScript promises.
 * <p>
 * This code is opinionated with respect to:
 * <ul>
 *     <li>never throwing a checked exception. An unhandled exception will be propagated via
 *     {@link Throwables#propagate(Throwable)}</li>
 *     <li>preferring Java 8 interfaces over Guava where possible. Specifically this code will
 *     only accept Java 8 {@link Function} and will translate internally to
 *     {@link com.google.common.base.Function} where the {@link ListenableFuture} interface
 *     requires it.</li>
 * </ul>
 * <p>
 * Chaining example:
 * <pre>{@code
 * Promise.wrap(future)
 *     .then(firstFunction)
 *     .then(secondFunction)
 *     .get()
 * }</pre>
 * <p>
 * Chaining example with exception handling:
 * <pre>{@code
 * Promise.wrap(future)
 *     .then(firstFunction)
 *     .then(secondFunction)
 *     .get(exception -> log.error("Promise failed", e))
 * }</pre>
 */
public class Promise<V> implements ListenableFuture<V> {

    private final ListenableFuture<V> future;

    private Promise(@Nonnull ListenableFuture<V> future) {
        this.future = checkNotNull(future);
    }

    /**
     * Create a promise by wrapping the given {@link ListenableFuture}
     */
    @Nonnull
    public static <V> Promise<V> wrap(@Nonnull ListenableFuture<V> future) {
        return new Promise<>(future);
    }

    @Override
    public void addListener(@Nonnull Runnable listener, @Nonnull Executor executor) {
        future.addListener(listener, executor);
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return future.cancel(mayInterruptIfRunning);
    }

    @Override
    public boolean isCancelled() {
        return future.isCancelled();
    }

    @Override
    public boolean isDone() {
        return future.isDone();
    }

    /**
     * This method wraps a call to {@link ListenableFuture#get()}
     * <p>
     * Any exception thrown will be wrapped in {@link Throwables#propagate(Throwable)}.
     *
     * @return the computed result
     */
    @Override
    public V get() {
        try {
            return future.get();
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    /**
     * This method wraps a call to {@link ListenableFuture#get(long, TimeUnit)}
     * <p>
     * Any exception thrown will be wrapped in {@link Throwables#propagate(Throwable)}.
     *
     * @param timeout the maximum time to wait
     * @param unit the time unit of the timeout argument
     * @return the computed result
     */
    @Override
    public V get(long timeout, @Nonnull TimeUnit unit) {
        try {
            return future.get(timeout, unit);
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    /**
     * This method wraps a call to {@link ListenableFuture#get()}
     *
     * @param exceptionConsumer consumer to handle any exception thrown
     * @return the computed result or {@link Optional#empty()} if an exception was thrown or if
     * the result is null
     */
    @Nonnull
    public Optional<V> get(@Nonnull Consumer<Exception> exceptionConsumer) {
        try {
            return Optional.ofNullable(future.get());
        } catch (Exception e) {
            exceptionConsumer.accept(e);
            return Optional.empty();
        }
    }

    /**
     * This method wraps a call to {@link ListenableFuture#get(long, TimeUnit)}
     *
     * @param exceptionConsumer consumer to handle any exception thrown
     * @param timeout the maximum time to wait
     * @param unit the time unit of the timeout argument
     * @return the computed result or {@link Optional#empty()} if an exception was thrown or if
     * the result is null
     */
    @Nonnull
    public Optional<V> get(
            @Nonnull Consumer<Exception> exceptionConsumer,
            long timeout,
            @Nonnull TimeUnit unit
    ) {
        try {
            return Optional.ofNullable(future.get(timeout, unit));
        } catch (Exception e) {
            exceptionConsumer.accept(e);
            return Optional.empty();
        }
    }

    /**
     * Returns a new {@link Promise} whose result will be the result of this promise with the given
     * function applied to it.
     * <p>
     * This method wraps a call to
     * {@link Futures#transform(ListenableFuture, com.google.common.base.Function)}
     *
     * @param function function to apply to the promise result
     * @param <O> function return type
     * @return a promise that holds the function output
     */
    @Nonnull
    public <O> Promise<O> then(@Nonnull Function<V, O> function) {
        return Promise.wrap(
                Futures.transform(
                        future,
                        getGuavaFunction(function)
                )
        );
    }

    /**
     * Returns a new {@link Promise} whose result will be the result of this promise with the given
     * function applied to it.
     * <p>
     * This method wraps a call to
     * {@link Futures#transform(ListenableFuture, com.google.common.base.Function)}
     *
     * @param function function to apply to the promise result
     * @param executor executor to run the function in
     * @param <O> function return type
     * @return a promise that holds the function output
     */
    @Nonnull
    public <O> Promise<O> then(@Nonnull Function<V, O> function, @Nonnull Executor executor) {
        return Promise.wrap(
                Futures.transform(
                        future,
                        getGuavaFunction(function),
                        executor
                )
        );
    }

    @Nonnull
    private <T, R> com.google.common.base.Function<T, R> getGuavaFunction(
            @Nonnull Function<T, R> function
    ) {
        return new com.google.common.base.Function<T, R>() {

            @Nullable
            @Override
            public R apply(@Nullable T input) {
                return function.apply(input);
            }
        };
    }
}
