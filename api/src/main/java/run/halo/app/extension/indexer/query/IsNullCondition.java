package run.halo.app.extension.indexer.query;

record IsNullCondition(String indexName) implements IndexCondition {

    @Override
    public Condition not() {
        return new IsNotNullCondition(indexName);
    }

}
