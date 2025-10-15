package run.halo.app.extension.index;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import org.springframework.util.Assert;
import run.halo.app.extension.Extension;

abstract class AbstractValueIndexSpecBuilder<E extends Extension, K extends Comparable<K>> {

    protected final String name;

    protected final Class<K> keyType;

    protected boolean unique = false;

    protected boolean nullable = true;

    protected AbstractValueIndexSpecBuilder(String name, Class<K> keyType) {
        Assert.notNull(name, "Index name must not be null");
        Assert.notNull(keyType, "Key type must not be null");
        this.name = name;
        this.keyType = keyType;
    }

    protected AbstractValueIndexSpecBuilder(String name) {
        Assert.notNull(name, "Index name must not be null");
        this.name = name;
        var genericSuperclass = getClass().getGenericSuperclass();
        if (!(genericSuperclass instanceof ParameterizedType parameterizedType)) {
            throw new IllegalStateException("Cannot resolve parameterized type");
        }
        Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
        if (actualTypeArguments.length != 2) {
            throw new IllegalStateException("Actual type arguments length is not 2");
        }
        if (!(actualTypeArguments[1] instanceof Class<?> clazz)) {
            throw new IllegalStateException("Cannot resolve key type");
        }
        this.keyType = (Class<K>) clazz;
    }

    public AbstractValueIndexSpecBuilder<E, K> setUnique(boolean unique) {
        this.unique = unique;
        return this;
    }

    public AbstractValueIndexSpecBuilder<E, K> setNullable(boolean nullable) {
        this.nullable = nullable;
        return this;
    }

    public abstract ValueIndexSpec<E, K> build();

}
