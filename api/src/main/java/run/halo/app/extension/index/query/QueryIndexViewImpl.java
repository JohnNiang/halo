package run.halo.app.extension.index.query;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.BiPredicate;
import org.springframework.data.domain.Sort;
import run.halo.app.extension.index.IndexEntry;
import run.halo.app.extension.index.IndexEntryOperator;
import run.halo.app.extension.index.IndexEntryOperatorImpl;
import run.halo.app.extension.index.Indexer;

/**
 * A default implementation for {@link run.halo.app.extension.index.query.QueryIndexView}.
 *
 * @author guqing
 * @since 2.17.0
 */
public class QueryIndexViewImpl implements QueryIndexView {

    public static final String PRIMARY_INDEX_NAME = "metadata.name";

    private final Indexer indexer;

    /**
     * Construct a new {@link QueryIndexViewImpl} with the given {@link Indexer}.
     *
     * @throws IllegalArgumentException if the primary index does not exist
     */
    public QueryIndexViewImpl(Indexer indexer) {
        // check if primary index exists
        indexer.getIndexEntry(PRIMARY_INDEX_NAME);
        this.indexer = indexer;
    }

    @Override
    public <T extends Comparable<? super T>> NavigableSet<String> findIds(String fieldName,
        T fieldValue) {
        var operator = this.<T>getEntryOperator(fieldName);
        return operator.find(fieldValue);
    }

    @Override
    public NavigableSet<String> getIdsForField(String fieldName) {
        var operator = getEntryOperator(fieldName);
        return new TreeSet<>(operator.getValues());
    }

    @Override
    public NavigableSet<String> getAllIds() {
        return new TreeSet<>(allIds());
    }

    @Override
    public NavigableSet<String> findMatchingIdsWithEqualValues(
        String fieldName1, String fieldName2) {
        indexer.acquireReadLock();
        try {
            return findIdsWithKeyComparator(fieldName1, fieldName2, (k1, k2) -> {
                var compare = k1.compareTo(k2);
                return compare == 0;
            });
        } finally {
            indexer.releaseReadLock();
        }
    }

    @Override
    public NavigableSet<String> findMatchingIdsWithGreaterValues(String fieldName1,
        String fieldName2, boolean orEqual) {
        indexer.acquireReadLock();
        try {
            return findIdsWithKeyComparator(fieldName1, fieldName2, (k1, k2) -> {
                var compare = k1.compareTo(k2);
                return orEqual ? compare <= 0 : compare < 0;
            });
        } finally {
            indexer.releaseReadLock();
        }
    }

    @Override
    public <T extends Comparable<? super T>> NavigableSet<String> findIdsGreaterThan(
        String fieldName, T fieldValue,
        boolean orEqual) {
        var operator = this.<T>getEntryOperator(fieldName);
        return operator.greaterThan(fieldValue, orEqual);
    }

    @Override
    public NavigableSet<String> findMatchingIdsWithSmallerValues(String fieldName1,
        String fieldName2, boolean orEqual) {
        indexer.acquireReadLock();
        try {
            return findIdsWithKeyComparator(fieldName1, fieldName2, (k1, k2) -> {
                var compare = k1.compareTo(k2);
                return orEqual ? compare >= 0 : compare > 0;
            });
        } finally {
            indexer.releaseReadLock();
        }
    }

    @Override
    public <T extends Comparable<? super T>> NavigableSet<String> findIdsLessThan(String fieldName,
        T fieldValue,
        boolean orEqual) {
        var operator = this.<T>getEntryOperator(fieldName);
        return operator.lessThan(fieldValue, orEqual);
    }

    @Override
    public <T extends Comparable<? super T>> NavigableSet<String> between(String fieldName,
        T lowerValue, boolean lowerInclusive,
        T upperValue, boolean upperInclusive) {
        var operator = this.<T>getEntryOperator(fieldName);
        return operator.range(lowerValue, upperValue, lowerInclusive, upperInclusive);
    }

    @Override
    public List<String> sortBy(NavigableSet<String> ids, Sort sort) {
        if (sort.isUnsorted()) {
            return new ArrayList<>(ids);
        }
        indexer.acquireReadLock();
        try {
            var combinedComparator = sort.stream()
                .map(this::comparatorFrom)
                .reduce(Comparator::thenComparing)
                .orElseThrow();
            return ids.stream()
                .sorted(combinedComparator)
                .toList();
        } finally {
            indexer.releaseReadLock();
        }
    }

    Comparator<String> comparatorFrom(Sort.Order order) {
        IndexEntry<?> indexEntry = getIndexEntry(order.getProperty());
        var idPositionMap = indexEntry.getIdPositionMap();
        var isDesc = order.isDescending();
        // This sort algorithm works leveraging on that the idPositionMap is a map of id -> position
        // if the id is not in the map, it means that it is not indexed, and it will be placed at
        // the end
        return (a, b) -> {
            var indexOfA = idPositionMap.get(a);
            var indexOfB = idPositionMap.get(b);

            var isAIndexed = indexOfA != null;
            var isBIndexed = indexOfB != null;

            if (!isAIndexed && !isBIndexed) {
                return 0;
            }
            // un-indexed item are always at the end
            if (!isAIndexed) {
                return isDesc ? -1 : 1;
            }
            if (!isBIndexed) {
                return isDesc ? 1 : -1;
            }
            return isDesc ? Integer.compare(indexOfB, indexOfA)
                : Integer.compare(indexOfA, indexOfB);
        };
    }

    @Override
    public <T extends Comparable<? super T>> IndexEntry<T> getIndexEntry(String fieldName) {
        return indexer.getIndexEntry(fieldName);
    }

    @Override
    public void acquireReadLock() {
        indexer.acquireReadLock();
    }

    @Override
    public void releaseReadLock() {
        indexer.releaseReadLock();
    }

    private <T extends Comparable<? super T>> IndexEntryOperator<T> getEntryOperator(
        String fieldName) {
        IndexEntry<T> indexEntry = getIndexEntry(fieldName);
        return createIndexEntryOperator(indexEntry);
    }

    private <T extends Comparable<? super T>> IndexEntryOperator<T> createIndexEntryOperator(
        IndexEntry<T> entry) {
        return new IndexEntryOperatorImpl<>(entry);
    }

    private Set<String> allIds() {
        var indexEntry = getIndexEntry(PRIMARY_INDEX_NAME);
        return createIndexEntryOperator(indexEntry).getValues();
    }

    /**
     * Must lock the indexer before calling this method.
     */
    private <T extends Comparable<? super T>> NavigableSet<String> findIdsWithKeyComparator(
        String fieldName1, String fieldName2, BiPredicate<T, T> keyComparator
    ) {
        // get entries from indexer for fieldName1
        IndexEntry<T> indexEntry1 = getIndexEntry(fieldName1);
        Collection<? extends Map.Entry<T, String>> entriesA = indexEntry1.entries();

        Map<String, List<T>> keyMap = new HashMap<>();
        for (Map.Entry<T, String> entry : entriesA) {
            keyMap.computeIfAbsent(entry.getValue(), v -> new ArrayList<>()).add(entry.getKey());
        }

        NavigableSet<String> result = new TreeSet<>();

        // get entries from indexer for fieldName2
        IndexEntry<T> indexEntry2 = getIndexEntry(fieldName2);
        Collection<? extends Map.Entry<T, String>> entriesB = indexEntry2.entries();
        for (Map.Entry<T, String> entry : entriesB) {
            List<T> matchedKeys = keyMap.get(entry.getValue());
            if (matchedKeys != null) {
                for (T key : matchedKeys) {
                    if (keyComparator.test(entry.getKey(), key)) {
                        result.add(entry.getValue());
                        // found one match, no need to continue
                        break;
                    }
                }
            }
        }
        return result;
    }
}
