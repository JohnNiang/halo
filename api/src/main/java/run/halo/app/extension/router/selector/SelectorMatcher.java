package run.halo.app.extension.router.selector;

import org.springframework.data.relational.core.query.Criteria;
import org.springframework.lang.NonNull;

public interface SelectorMatcher {

    String getKey();

    /**
     * Returns true if a field value matches.
     *
     * @param s the field value
     * @return the boolean
     */
    boolean test(String s);

    @NonNull
    Criteria toCriteria();
}
