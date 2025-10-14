package run.halo.app.extension.indexer.query;

record LabelNotEqualsCondition(String labelKey, String labelValue) implements LabelCondition {

    @Override
    public Condition not() {
        return new LabelEqualsCondition(labelKey, labelValue);
    }
}
