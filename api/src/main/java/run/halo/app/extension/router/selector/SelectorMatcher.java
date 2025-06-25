package run.halo.app.extension.router.selector;

import org.springframework.data.relational.core.sql.Condition;
import org.springframework.data.relational.core.sql.TableLike;
import org.springframework.r2dbc.core.binding.MutableBindings;

public interface SelectorMatcher {

    String getKey();

    /**
     * Returns true if a field value matches.
     *
     * @param s the field value
     * @return the boolean
     */
    boolean test(String s);

    Condition toCondition(TableLike table, MutableBindings bindings);

}
