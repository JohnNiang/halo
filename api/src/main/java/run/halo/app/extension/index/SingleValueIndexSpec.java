package run.halo.app.extension.index;

import java.util.function.Function;
import org.springframework.lang.Nullable;
import run.halo.app.extension.Extension;

public interface SingleValueIndexSpec<E extends Extension, K extends Comparable<K>>
    extends ValueIndexSpec<E, K> {

    @Nullable
    K getValue(E extension);

    static <E extends Extension, K extends Comparable<K>> SingleValueBuilder<E, K> builder(
        String name, Class<K> keyType, Function<E, K> indexFunc
    ) {
        return new SingleValueBuilder<>(name, keyType, indexFunc);
    }

}
