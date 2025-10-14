package run.halo.app.extension.index.query;

record AndCondition(Condition left, Condition right) implements Condition {

    @Override
    public Condition not() {
        return left.not().or(right.not());
    }

}
