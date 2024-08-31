package run.halo.app.extension.index;

import com.google.common.base.Objects;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class IndexSpec<T extends Comparable<? super T>> {
    private String name;

    private IndexAttribute<T> indexFunc;

    private OrderType order;

    private DataType dataType = DataType.string;

    private boolean unique;

    public enum OrderType {
        ASC,
        DESC
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        var indexSpec = (IndexSpec<?>) o;
        return Objects.equal(name, indexSpec.name);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(name);
    }
}
