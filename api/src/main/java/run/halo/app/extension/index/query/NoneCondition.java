package run.halo.app.extension.index.query;

public record NoneCondition(String indexName) implements IndexCondition {

    @Override
    public Condition not() {
        return new AllCondition(indexName);
    }

    @Override
    public String toString() {
        return "NONE " + indexName;
    }
}
