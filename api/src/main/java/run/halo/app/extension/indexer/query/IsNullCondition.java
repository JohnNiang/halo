package run.halo.app.extension.indexer.query;

record IsNullCondition(String indexName) implements Condition {

    @Override
    public Condition not() {
        return new IsNotNullCondition(indexName);
    }

}
