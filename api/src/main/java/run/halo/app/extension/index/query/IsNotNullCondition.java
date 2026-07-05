package run.halo.app.extension.index.query;

public record IsNotNullCondition(String indexName) implements IndexCondition {

    @Override
    public Condition not() {
        return new IsNullCondition(indexName);
    }

    @Override
    public String toString() {
        return indexName + " IS NOT NULL";
    }
}
