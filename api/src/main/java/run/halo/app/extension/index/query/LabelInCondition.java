package run.halo.app.extension.index.query;

import java.util.Set;

record LabelInCondition(String labelKey, Set<String> labelValues) implements LabelCondition {

    @Override
    public Condition not() {
        return new LabelNotInCondition(labelKey, labelValues);
    }

}
