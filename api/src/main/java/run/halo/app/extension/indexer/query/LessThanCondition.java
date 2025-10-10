package run.halo.app.extension.indexer.query;

record LessThanCondition(String indexName, Object upperBound, boolean inclusive)
    implements Condition {

    @Override
    public Condition not() {
        return new GreaterThanCondition(indexName, upperBound, !inclusive);
    }

}
