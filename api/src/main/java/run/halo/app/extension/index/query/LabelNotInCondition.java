package run.halo.app.extension.index.query;

import java.util.Set;

record LabelNotInCondition(String labelKey, Set<String> labelValues) implements LabelCondition {

    @Override
    public Condition not() {
        return new LabelInCondition(labelKey, labelValues);
    }

}
