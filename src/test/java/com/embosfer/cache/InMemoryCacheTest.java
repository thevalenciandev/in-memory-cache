package com.embosfer.cache;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.concurrent.CountDownLatch;
import java.util.stream.Stream;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class InMemoryCacheTest {

    @Mock
    DataSource<String, String> slowDataSource;

    InMemoryCache<String, String> inMemoryCache;

    @Before
    public void setUp() {
        inMemoryCache = new InMemoryCache<>(5, slowDataSource);
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
        inMemoryCache = new InMemoryCache<>(2, slowDataSource);

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
        Thread[] threads = new Thread[threadNumber];
        inMemoryCache = new InMemoryCache<>(threadNumber, slowDataSource);

        String key = "key";
        when(slowDataSource.getValueFor(key)).thenReturn("a-value");

        CountDownLatch threadsReady = new CountDownLatch(threadNumber);
        CountDownLatch threadsDone = new CountDownLatch(threadNumber);
        for (int i = 0; i < threadNumber; i++) {
            threads[i] = new Thread(worker(threadsReady, threadsDone, key), "thread-" + i);
        }

        Stream.of(threads).forEach(Thread::start);
        threadsDone.await();

        verify(slowDataSource, times(1)).getValueFor(key);
    }

    private Runnable worker(CountDownLatch threadsReady, CountDownLatch threadsDone, String key) {
        return () -> {
            threadsReady.countDown();
            try {
                inMemoryCache.getValueFor(key);
            } catch (InterruptedException e) {
                throw new RuntimeException("Issue while geting value for " + key);
            } finally {
                threadsDone.countDown();
            }
        };
    }
}
