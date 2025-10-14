package run.halo.app.extension.index.query;

record NotEqualCondition(String indexName, Object value) implements IndexCondition {

    @Override
    public Condition not() {
        return new EqualCondition(indexName, value);
    }
}
