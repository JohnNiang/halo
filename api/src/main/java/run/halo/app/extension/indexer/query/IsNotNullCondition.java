package run.halo.app.extension.indexer.query;

record IsNotNullCondition(String indexName) implements Condition {

    @Override
    public Condition not() {
        return new IsNullCondition(indexName);
    }

}
