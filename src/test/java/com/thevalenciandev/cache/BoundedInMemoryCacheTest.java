package com.thevalenciandev.cache;

import org.hamcrest.MatcherAssert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.concurrent.CountDownLatch;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.internal.matchers.ThrowableMessageMatcher.hasMessage;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class BoundedInMemoryCacheTest {

    @Mock
    DataSource<String, String> slowDataSource;

    BoundedInMemoryCache<String, String> inMemoryCache;

    @Before
    public void setUp() {
        inMemoryCache = new BoundedInMemoryCache<>(5, slowDataSource);
    }

    @Test
    public void delegatesToDataSourceOnlyIfValueIsNotAlreadyCachedForKey() throws Exception {

        String unCachedKey = "key1";
        String value = "a-value";

        when(slowDataSource.getValueFor(unCachedKey)).thenReturn(value);

        // first try to get value for key
        assertThat(inMemoryCache.getValueFor(unCachedKey), is(value));
        verify(slowDataSource).getValueFor(unCachedKey);

        // second try to get value for key - should be cached now
        assertThat(inMemoryCache.getValueFor(unCachedKey), is(value));
        verifyNoMoreInteractions(slowDataSource);
    }

    @Test
    public void leastRecentlyUsedKeyIsEvictedUponSizeOverflow() throws Exception {
        inMemoryCache = new BoundedInMemoryCache<>(2, slowDataSource);

        String key1 = "key1";
        String key2 = "key2";
        String key3 = "key3";
        String value1 = "value1";
        String value2 = "value2";
        String value3 = "value3";

        when(slowDataSource.getValueFor(key1)).thenReturn(value1);
        when(slowDataSource.getValueFor(key2)).thenReturn(value2);
        when(slowDataSource.getValueFor(key3)).thenReturn(value3); // <-- size overflow, key1 should be removed

        assertThat(inMemoryCache.getValueFor(key1), is(value1));
        assertThat(inMemoryCache.getValueFor(key2), is(value2));
        assertThat(inMemoryCache.getValueFor(key3), is(value3));

        // request key1 again and check it's not cached anymore
        assertThat(inMemoryCache.getValueFor(key1), is(value1));

        verify(slowDataSource, times(2)).getValueFor(key1);
        verify(slowDataSource, times(1)).getValueFor(key2);
        verify(slowDataSource, times(1)).getValueFor(key3);
    }

    @Test
    public void isThreadSafe() throws Exception {
        int threadNumber = 50;
        String key = "key";
        Thread[] threads = new Thread[threadNumber];
        inMemoryCache = new BoundedInMemoryCache<>(threadNumber, slowDataSource);

        when(slowDataSource.getValueFor(key)).thenReturn("a-value");

        CountDownLatch threadsReady = new CountDownLatch(1);
        CountDownLatch allThreadsFinished = new CountDownLatch(threadNumber);
        for (int i = 0; i < threadNumber; i++) {
            threads[i] = new Thread(worker(threadsReady, allThreadsFinished, key), "thread-" + i);
            threads[i].start();
        }

        threadsReady.countDown(); // release all threads
        allThreadsFinished.await();

        verify(slowDataSource, times(1)).getValueFor(key);
    }

    @Test
    public void blowsUpAndProvidesKeyInfoIfUnderlyingDataSourceThrowsAnException() throws Exception {

        when(slowDataSource.getValueFor("key")).thenThrow(new RuntimeException("BOOM!"));

        MatcherAssert.assertThat(ExceptionThrower.exceptionFrom(() -> inMemoryCache.getValueFor("key")),
                allOf(instanceOf(RuntimeException.class),
                        hasMessage(containsString("key"))));
    }

    private Runnable worker(CountDownLatch threadsReady, CountDownLatch threadsDone, String key) {
        return () -> {
            try {
                threadsReady.await();
                inMemoryCache.getValueFor(key);
            } catch (InterruptedException e) {
                throw new RuntimeException("Issue while geting value for " + key);
            } finally {
                threadsDone.countDown();
            }
        };
    }
}
