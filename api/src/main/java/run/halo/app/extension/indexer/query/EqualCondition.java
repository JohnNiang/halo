package run.halo.app.extension.indexer.query;

record EqualCondition(String indexName, Object key) implements Condition {

    @Override
    public Condition not() {
        return new NotEqualCondition(indexName, key);
    }

}
