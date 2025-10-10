package run.halo.app.extension.indexer.query;

record OrCondition(Condition left, Condition right) implements Condition {

    @Override
    public Condition not() {
        return left.not().and(right.not());
    }

}
