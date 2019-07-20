# in-memory-cache

This project uses Gradle 5.4.1.
You can build it executing "gradlew build".
You can execute tests by executing "gradlew test"

Dependencies are defined in build.gradle. Needs Java 8+.

The main class is BoundedInMemoryCache and it’s got a test that you can run as well, BoundedInMemoryCacheTest.

The cache is a DataSource itself, and you must pass a (potentially slow) DataSource delegate to this cache in the constructor, as well as the cache size.

Assumptions:
- Keys are sanitized (never null)
- Keys implement equals and hashCode
- It’s a cache for performance sensitive applications
- There can be multiple cache misses in very quick succession
- Underlying (slow) data source calculations will eventually finish. Otherwise future.get() could block forever, and we’d need a smarter wait of waiting (with timeout), and cache eviction of those cancelled tasks