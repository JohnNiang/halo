package run.halo.app.extension.indexer.query;

record StringEndsWithCondition(String indexName, String suffix) implements Condition {

    @Override
    public Condition not() {
        return new StringStartsWithCondition(indexName, suffix);
    }

}
