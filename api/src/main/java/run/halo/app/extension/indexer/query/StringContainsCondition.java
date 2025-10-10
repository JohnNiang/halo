package run.halo.app.extension.indexer.query;

record StringContainsCondition(String indexName, String keyword) implements Condition {

    @Override
    public Condition not() {
        return new StringNotContainsCondition(indexName, keyword);
    }

}
