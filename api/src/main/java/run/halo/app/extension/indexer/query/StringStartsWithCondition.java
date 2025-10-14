package run.halo.app.extension.indexer.query;

record StringStartsWithCondition(String indexName, String prefix) implements IndexCondition {

    @Override
    public Condition not() {
        return new StringNotStartsWithCondition(indexName, prefix);
    }

}
