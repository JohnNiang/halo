package run.halo.app.extension.indexer.query;

record StringStartsWithCondition(String indexName, String prefix) implements Condition {

    @Override
    public Condition not() {
        return new StringEndsWithCondition(indexName, prefix);
    }

}
