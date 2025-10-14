package run.halo.app.extension.index;

import java.util.Set;
import run.halo.app.extension.Extension;

public interface IndexAttribute<E extends Extension, K extends Comparable<K>> {

    /**
     * Specify this class is belonged to which extension.
     *
     * @return the extension class.
     */
    Class<E> getObjectType();

    /**
     * Gets the value type of the attribute.
     *
     * @return the value type of the attribute.
     */
    Class<K> getKeyType();

    /**
     * Get the value of the attribute.
     *
     * @param object the object to get value from.
     * @return the value of the attribute must not be null.
     */
    Set<K> getValues(E object);

}
