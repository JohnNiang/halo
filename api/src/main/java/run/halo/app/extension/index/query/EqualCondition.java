package run.halo.app.extension.index.query;

record EqualCondition(String indexName, Object key) implements IndexCondition {

    @Override
    public Condition not() {
        return new NotEqualCondition(indexName, key);
    }

}
