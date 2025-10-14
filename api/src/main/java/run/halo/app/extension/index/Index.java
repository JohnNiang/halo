package run.halo.app.extension.index;

import java.io.Closeable;
import java.util.Set;
import run.halo.app.extension.Extension;

public interface Index<E extends Extension, K extends Comparable<K>> extends Closeable {

    String getName();

    Class<K> getKeyType();

    default boolean isUnique() {
        return false;
    }

    IndexOperation prepareInsert(E extension);

    IndexOperation prepareUpdate(E newExtension);

    IndexOperation prepareDelete(String primaryKey);

    Set<K> getKeys(String primaryKey);

}
