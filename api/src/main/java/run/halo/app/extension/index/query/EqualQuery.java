package run.halo.app.extension.index.query;

import java.util.Map;
import java.util.NavigableSet;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.core.sql.Condition;
import org.springframework.data.relational.core.sql.SQL;
import org.springframework.data.relational.core.sql.TableLike;
import org.springframework.r2dbc.core.binding.MutableBindings;
import org.springframework.util.Assert;

public class EqualQuery extends SimpleQuery {

    public EqualQuery(String fieldName, String value) {
        super(fieldName, value);
    }

    public EqualQuery(String fieldName, String value, boolean isFieldRef) {
        super(fieldName, value, isFieldRef);
        Assert.notNull(value, "Value must not be null, use IsNull or IsNotNull instead");
    }

    @Override
    public NavigableSet<String> matches(QueryIndexView indexView) {
        if (isFieldRef) {
            return indexView.findMatchingIdsWithEqualValues(fieldName, value);
        }
        return indexView.findIds(fieldName, value);
    }

    @Override
    public Criteria toCriteria(Map<String, String> fieldNameMap) {
        var columnName = fieldNameMap.getOrDefault(this.fieldName, this.fieldName);
        return Criteria.where(columnName).is(value);
    }

    @Override
    public Condition toCondition(Map<String, String> fieldNameMap, TableLike table,
        MutableBindings bindings) {
        var columnName = fieldNameMap.getOrDefault(this.fieldName, this.fieldName);
        return table.column(columnName).isEqualTo(
            SQL.bindMarker(bindings.bind(value).getPlaceholder())
        );
    }

    @Override
    public String toString() {
        if (isFieldRef) {
            return fieldName + " = " + value;
        }
        return fieldName + " = '" + value + "'";
    }
}
