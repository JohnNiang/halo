package run.halo.app.extension.indexer;

import java.util.Collection;
import java.util.Set;
import run.halo.app.extension.Extension;

public interface Index<E extends Extension, K extends Comparable<K>> {

    String getName();

    Class<K> getKeyType();

    IndexOperation prepareInsert(E extension);

    IndexOperation prepareUpdate(E newExtension);

    IndexOperation prepareDelete(E extension);

    Set<String> equal(K key);

    Set<String> notEqual(K key);

    Set<String> all();

    Set<String> between(K fromKey, boolean fromInclusive, K toKey, boolean toInclusive);

    Set<String> notBetween(K fromKey, boolean fromInclusive, K toKey, boolean toInclusive);

    Set<String> in(Collection<K> keys);

    Set<String> notIn(Collection<K> keys);

    Set<String> lessThan(K key, boolean inclusive);

    Set<String> greaterThan(K key, boolean inclusive);

    Set<String> isNull();

    Set<String> isNotNull();

    Set<String> stringContains(String keyword);

    Set<String> stringNotContains(String keyword);

    Set<String> stringStartsWith(String prefix);

    Set<String> stringEndsWith(String suffix);

}
