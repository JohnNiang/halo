package run.halo.app.extension.index;

import java.util.Set;
import java.util.function.Function;
import org.springframework.util.Assert;
import run.halo.app.extension.Extension;

public class MultiValueBuilder<E extends Extension, K extends Comparable<K>>
    extends AbstractValueIndexSpecBuilder<E, K> {

    private final Function<E, Set<K>> indexFunc;

    MultiValueBuilder(
        String name, Class<K> keyType, Function<E, Set<K>> indexFunc
    ) {
        super(name, keyType);
        Assert.notNull(indexFunc, "Index function must not be null");
        this.indexFunc = indexFunc;
    }

    @Override
    public MultiValueIndexSpec<E, K> build() {
        return new MultiValueIndexSpec<>() {
            @Override
            public String getName() {
                return name;
            }

            @Override
            public boolean isUnique() {
                return unique;
            }

            @Override
            public boolean isNullable() {
                return nullable;
            }

            @Override
            public Class<K> getKeyType() {
                return keyType;
            }

            @Override
            public Set<K> getValues(E extension) {
                return indexFunc.apply(extension);
            }
        };
    }
}
