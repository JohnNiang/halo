package run.halo.app.extension.indexer.query;

record NotCondition(Condition condition) implements Condition {

    @Override
    public Condition not() {
        return condition;
    }

}
