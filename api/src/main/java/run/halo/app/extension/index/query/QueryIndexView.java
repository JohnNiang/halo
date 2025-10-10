package run.halo.app.extension.index.query;

import java.util.List;
import java.util.NavigableSet;
import org.springframework.data.domain.Sort;
import run.halo.app.extension.Extension;
import run.halo.app.extension.Metadata;
import run.halo.app.extension.index.IndexEntry;
import run.halo.app.extension.indexer.IndexSpec;

/**
 * <p>A view of an index entries that can be queried.</p>
 * <p>Explanation of naming:</p>
 * <ul>
 *     <li>fieldName: a field of an index, usually {@link IndexSpec#getName()}</li>
 *     <li>fieldValue: a value of a field, e.g. a value of a field "name" could be "foo"</li>
 *     <li>id: the id of an object pointing to object position, see {@link Metadata#getName()}</li>
 * </ul>
 *
 * @author guqing
 * @since 2.12.0
 */
public interface QueryIndexView<E extends Extension> {

    /**
     * Gets all object ids for a given field name and field value.
     *
     * @param fieldName the field name
     * @param fieldValue the field value
     * @return all indexed object ids associated with the given field name and field value
     * @throws IllegalArgumentException if the field name is not indexed
     */
    <K extends Comparable<K>> NavigableSet<String> findIds(String fieldName, Object fieldValue);

    /**
     * Gets all object ids for a given field name without null cells.
     *
     * @param fieldName the field name
     * @return all indexed object ids for the given field name
     * @throws IllegalArgumentException if the field name is not indexed
     */
    NavigableSet<String> getIdsForField(String fieldName);

    /**
     * Gets all object ids in this view.
     *
     * @return all object ids in this view
     */
    NavigableSet<String> getAllIds();

    <K extends Comparable<K>> NavigableSet<String> findIdsGreaterThan(String fieldName,
        Object fieldValue,
        boolean orEqual);

    NavigableSet<String> findIdsLessThan(String fieldName, Object fieldValue, boolean orEqual);

    NavigableSet<String> between(String fieldName, String lowerValue, boolean lowerInclusive,
        String upperValue, boolean upperInclusive);

    List<String> sortBy(NavigableSet<String> resultSet, Sort sort);

    <K extends Comparable<K>> IndexEntry<E, K> getIndexEntry(String fieldName);

    /**
     * Acquire a read lock on the indexer.
     * if you need to operate on more than one {@code IndexEntry} at the same time, you need to
     * lock first.
     *
     * @see #getIndexEntry(String)
     */
    void acquireReadLock();

    void releaseReadLock();
}
