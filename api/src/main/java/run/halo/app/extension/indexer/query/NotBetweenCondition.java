package run.halo.app.extension.indexer.query;

record NotBetweenCondition(
    String indexName, Object fromKey, boolean fromInclusive, Object toKey, boolean toInclusive)
    implements IndexCondition {

    @Override
    public Condition not() {
        return new BetweenCondition(indexName, fromKey, !fromInclusive, toKey, !toInclusive);
    }
}
