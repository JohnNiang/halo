package run.halo.app.extension.index;

import java.util.function.Function;
import org.springframework.util.Assert;
import run.halo.app.extension.Extension;

public class SingleValueBuilder<E extends Extension, K extends Comparable<K>>
    extends AbstractValueIndexSpecBuilder<E, K> {

    private final Function<E, K> indexFunc;

    SingleValueBuilder(
        String name, Class<K> keyType, Function<E, K> indexFunc
    ) {
        super(name, keyType);
        Assert.notNull(indexFunc, "Index function must not be null");
        this.indexFunc = indexFunc;
    }

    public SingleValueBuilder(String name, Function<E, K> indexFunc) {
        super(name);
        Assert.notNull(indexFunc, "Index function must not be null");
        this.indexFunc = indexFunc;
    }

    @Override
    public SingleValueIndexSpec<E, K> build() {
        return new SingleValueIndexSpec<>() {
            @Override
            public K getValue(E extension) {
                return indexFunc.apply(extension);
            }

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
        };
    }
}
