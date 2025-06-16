package run.halo.app.extension.index.query;

import java.util.Map;
import java.util.NavigableSet;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.lang.NonNull;
import run.halo.app.extension.Metadata;

/**
 * A {@link Query} is used to match {@link QueryIndexView} objects.
 *
 * @author guqing
 * @since 2.12.0
 */
public interface Query {

    /**
     * Matches the given {@link QueryIndexView} and returns the matched object names see
     * {@link Metadata#getName()}.
     *
     * @param indexView the {@link QueryIndexView} to match.
     * @return the matched object names ordered by natural order.
     */
    NavigableSet<String> matches(QueryIndexView indexView);

    @NonNull
    Criteria toCriteria(Map<String, String> fieldNameMap);

}
