package com.embosfer.cache;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class InMemoryCacheTest {

    @Mock DataSource<String, String> delegate;

    InMemoryCache<String, String> inMemoryCache;

    @Before
    public void setUp() {
        inMemoryCache = new InMemoryCache<>(5, delegate);
    }

    @Test
    public void delegatesToDataSourceOnlyIfValueIsMissingForKey() {

        String missingKey = "key1";
        String value1 = "value1";

        when(delegate.getValueFor(missingKey)).thenReturn(value1);

        assertThat(inMemoryCache.getValueFor(missingKey), is(value1));
        verify(delegate).getValueFor(missingKey);

        assertThat(inMemoryCache.getValueFor(missingKey), is(value1));
        verifyNoMoreInteractions(delegate);
    }

    // TODO rename test?
    @Test
    public void supportsLRUCapabilities() {
        inMemoryCache = new InMemoryCache<>(2, delegate);

        String key1 = "key1";
        String key2 = "key2";
        String key3 = "key3";
        String value1 = "value1";
        String value2 = "value2";
        String value3 = "value3";

        when(delegate.getValueFor(key1)).thenReturn(value1);
        when(delegate.getValueFor(key2)).thenReturn(value2);
        when(delegate.getValueFor(key3)).thenReturn(value3);

        assertThat(inMemoryCache.getValueFor(key1), is(value1));
        assertThat(inMemoryCache.getValueFor(key2), is(value2));
        assertThat(inMemoryCache.getValueFor(key3), is(value3));

        assertThat(inMemoryCache.getValueFor(key1), is(value1));

        verify(delegate, times(2)).getValueFor(key1);
        verify(delegate, times(1)).getValueFor(key2);
        verify(delegate, times(1)).getValueFor(key3);
    }
}
