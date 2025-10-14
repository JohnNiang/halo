package run.halo.app.extension.index;

import java.util.Set;
import run.halo.app.extension.Extension;

public interface LabelIndex<E extends Extension> extends Index<E, String> {

    @Override
    default String getName() {
        return "metadata.labels";
    }

    Set<String> exists(String labelKey);

    Set<String> equal(String labelKey, String labelValue);

    Set<String> notEqual(String labelKey, String labelValue);

    Set<String> in(String labelKey, Set<String> labelValues);

    Set<String> notIn(String labelKey, Set<String> labelValues);

}
