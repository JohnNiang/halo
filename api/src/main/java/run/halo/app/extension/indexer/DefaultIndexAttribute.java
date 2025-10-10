package run.halo.app.extension.indexer;

import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import run.halo.app.extension.Extension;

class DefaultIndexAttribute<E extends Extension, K extends Comparable<K>>
    extends AbstractIndexAttribute<E, K> {

    private final Function<E, Set<K>> valuesFunc;


    /**
     * Creates a new {@link AbstractIndexAttribute} for the given object type.
     *
     */
    public DefaultIndexAttribute(Function<E, Set<K>> valuesFunc) {
        this.valuesFunc = valuesFunc;
    }

    @Override
    public Set<K> getValues(E e) {
        if (!checkType(e)) {
            throw new IllegalArgumentException("Object type does not match");
        }
        return Optional.ofNullable(this.valuesFunc.apply(e)).orElse(Set.of());
    }

    private boolean checkType(Extension object) {
        return getObjectType().isInstance(object);
    }
}
