package run.halo.app.extension.indexer.query;

import java.util.Set;
import org.springframework.util.Assert;

record InCondition(String indexName, Set<Object> keys) implements Condition {

    public InCondition {
        Assert.notEmpty(keys, "Keys must not be empty");
    }

    @Override
    public Condition not() {
        return new NotInCondition(indexName, keys);
    }

}
