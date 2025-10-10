package run.halo.app.extension.indexer.query;

record AndCondition(Condition left, Condition right) implements Condition {

    @Override
    public Condition not() {
        return left.not().or(right.not());
    }

}
