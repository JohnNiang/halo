package run.halo.app.extension.indexer.query;

record StringNotEndsWithCondition(String indexName, String suffix) implements IndexCondition {

    @Override
    public Condition not() {
        return new StringEndsWithCondition(indexName, suffix);
    }

}
