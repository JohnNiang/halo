package run.halo.app.extension.indexer.query;

record NotEqualCondition(String indexName, Object value) implements Condition {

    @Override
    public Condition not() {
        return new EqualCondition(indexName, value);
    }
}
