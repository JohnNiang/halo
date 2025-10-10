package run.halo.app.extension.indexer;

import java.util.NavigableSet;

public interface IndexQuery<K extends Comparable<K>> {

    NavigableSet<String> find(K key);

    NavigableSet<String> find(Iterable<K> keys);

    NavigableSet<String> lessThan(K key, boolean orEqual);

    NavigableSet<String> greaterThan(K key, boolean orEqual);

    NavigableSet<String> range(K start, K end, boolean startInclusive, boolean endInclusive);

}
