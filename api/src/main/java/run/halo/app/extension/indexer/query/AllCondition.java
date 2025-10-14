package run.halo.app.extension.indexer.query;

record AllCondition(String indexName) implements Condition {

    @Override
    public Condition not() {
        return new NoneCondition(indexName);
    }

}
