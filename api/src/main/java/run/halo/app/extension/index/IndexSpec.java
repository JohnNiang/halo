package run.halo.app.extension.index;

import com.google.common.base.Objects;
import lombok.Data;
import lombok.experimental.Accessors;
import run.halo.app.extension.Extension;

@Data
@Accessors(chain = true)
public class IndexSpec<E extends Extension, K extends Comparable<K>> {

    private String name;

    private IndexAttribute<E, K> indexFunc;

    private OrderType order;

    private boolean unique;

    public enum OrderType {
        ASC,
        DESC
    }

    public Class<E> getObjectType() {
        return indexFunc.getObjectType();
    }

    public Class<K> getKeyType() {
        return indexFunc.getKeyType();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        IndexSpec indexSpec = (IndexSpec) o;
        return Objects.equal(name, indexSpec.name);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(name);
    }
}
