package com.metabroadcast.promise;

import java.util.Optional;
import java.util.concurrent.ExecutionException;

import com.google.common.base.Throwables;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class PromiseTest {

    @Captor private ArgumentCaptor<Exception> exceptionCaptor;

    @Mock private ExceptionConsumer exceptionConsumer;

    private String expectedResult;

    @Before
    public void setUp() throws Exception {
        expectedResult = "result";
    }

    @Test
    public void promiseCallsFunctionsInOrder() throws Exception {
        Promise<String> promise = Promise.wrap(Futures.immediateFuture(expectedResult));

        String output = promise
                .then(result -> result + "_A")
                .then(result -> result + "_B")
                .get();

        assertThat(output, is(expectedResult + "_A_B"));
    }

    @Test
    public void promiseCallsFunctionsInOrderWhenExecutorIsSpecified() throws Exception {
        Promise<String> promise = Promise.wrap(Futures.immediateFuture(expectedResult));

        String output = promise
                .then(result -> result + "_A", MoreExecutors.sameThreadExecutor())
                .then(result -> result + "_B", MoreExecutors.sameThreadExecutor())
                .get();

        assertThat(output, is(expectedResult + "_A_B"));
    }

    @Test
    public void promiseWithExceptionHandlingReturnsOptionalWithResultIfNoExceptionIsThrown()
            throws Exception {
        Promise<String> promise = Promise.wrap(Futures.immediateFuture(expectedResult));

        Optional<String> output = promise
                .get(exception -> exceptionConsumer.handleException(exception));

        assertThat(output.isPresent(), is(true));
        assertThat(output.get(), is(expectedResult));

        verify(exceptionConsumer, never()).handleException(any(Exception.class));
    }

    @Test
    public void promiseWithExceptionHandlingReturnsOptionalEmptyIfExceptionIsThrown()
            throws Exception {
        RuntimeException expectedException = new RuntimeException();
        Promise<String> promise = Promise.wrap(
                Futures.immediateFailedFuture(expectedException)
        );

        Optional<String> output = promise
                .get(exception -> exceptionConsumer.handleException(exception));

        assertThat(output.isPresent(), is(false));

        verify(exceptionConsumer).handleException(exceptionCaptor.capture());

        @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
        Exception actualException = exceptionCaptor.getValue();

        assertThat(actualException, instanceOf(ExecutionException.class));
        assertThat(actualException.getCause(), sameInstance(expectedException));
    }

    @Test
    public void promiseWithExceptionHandlingReturnsOptionalEmptyIfResultIsNull()
            throws Exception {
        ListenableFuture<String> listenableFuture = Futures.immediateFuture(null);
        Promise<String> promise = Promise.wrap(listenableFuture);

        Optional<String> output = promise
                .get(Throwables::propagate);

        assertThat(output.isPresent(), is(false));
    }

    public interface ExceptionConsumer {

        void handleException(Exception exception);
    }
}
