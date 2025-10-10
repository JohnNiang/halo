package run.halo.app.extension.indexer.query;

record BetweenCondition(
    String indexName, Object fromKey, boolean fromInclusive, Object toKey, boolean toInclusive)
    implements Condition {

    @Override
    public Condition not() {
        return new NotBetweenCondition(indexName, fromKey, !fromInclusive, toKey, !toInclusive);
    }
}
