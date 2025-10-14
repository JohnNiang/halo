package run.halo.app.extension.indexer.query;

record LabelExistsCondition(String labelKey) implements LabelCondition {

    @Override
    public Condition not() {
        return new LabelNotExistsCondition(labelKey);
    }

}
