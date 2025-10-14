package run.halo.app.extension.indexer.query;

record LabelEqualsCondition(String labelKey, String labelValue) implements LabelCondition {

    @Override
    public Condition not() {
        return new LabelNotEqualsCondition(labelKey, labelValue);
    }
}
