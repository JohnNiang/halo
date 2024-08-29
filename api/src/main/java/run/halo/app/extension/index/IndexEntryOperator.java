package run.halo.app.extension.index;

import java.util.Collection;
import java.util.NavigableSet;
import java.util.Set;

public interface IndexEntryOperator<T extends Comparable<? super T>> {

    /**
     * Search all values that key less than the target key.
     *
     * @param key target key
     * @param orEqual whether to include the value of the target key
     * @return object names that key less than the target key
     */
    NavigableSet<String> lessThan(T key, boolean orEqual);

    /**
     * Search all values that key greater than the target key.
     *
     * @param key target key
     * @param orEqual whether to include the value of the target key
     * @return object names that key greater than the target key
     */
    NavigableSet<String> greaterThan(T key, boolean orEqual);

    /**
     * Search all values that key in the range of [start, end].
     *
     * @param start start key
     * @param end end key
     * @param startInclusive whether to include the value of the start key
     * @param endInclusive whether to include the value of the end key
     * @return object names that key in the range of [start, end]
     */
    NavigableSet<String> range(T start, T end, boolean startInclusive,
        boolean endInclusive);

    /**
     * Find all values that key equals to the target key.
     *
     * @param key target key
     * @return object names that key equals to the target key
     */
    NavigableSet<String> find(T key);

    NavigableSet<String> findIn(Collection<T> keys);

    /**
     * Get all values in the index entry.
     *
     * @return a set of all object names
     */
    Set<String> getValues();
}
