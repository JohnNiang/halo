package run.halo.app.extension.indexer;

import lombok.EqualsAndHashCode;
import org.springframework.core.ResolvableType;
import org.springframework.util.Assert;
import run.halo.app.extension.Extension;
import run.halo.app.extension.GVK;

@EqualsAndHashCode
public abstract class AbstractIndexAttribute<E extends Extension, K extends Comparable<K>>
    implements IndexAttribute<E, K> {

    private final Class<E> objectType;

    private final Class<K> keyType;

    /**
     * Creates a new {@link AbstractIndexAttribute} for the given object type.
     *
     */
    public AbstractIndexAttribute() {
        var resolvableType = ResolvableType.forClass(this.getClass());
        // get generic type
        this.objectType = (Class<E>) resolvableType
            .getGeneric(0)
            .getRawClass();
        this.keyType = (Class<K>) resolvableType
            .getGeneric(1)
            .getRawClass();

        Assert.state(isValidExtension(objectType),
            "Invalid extension type, make sure you have annotated it with @" + GVK.class
                .getSimpleName());
    }

    @Override
    public Class<E> getObjectType() {
        return this.objectType;
    }

    @Override
    public Class<K> getKeytype() {
        return keyType;
    }

    boolean isValidExtension(Class<? extends Extension> type) {
        return type.getAnnotation(GVK.class) != null;
    }
}
