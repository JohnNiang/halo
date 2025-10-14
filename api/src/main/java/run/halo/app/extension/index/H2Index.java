package run.halo.app.extension.index;

import org.springframework.lang.NonNull;
import run.halo.app.extension.Extension;
import run.halo.app.extension.index.h2.IntegerIndex;
import run.halo.app.extension.index.h2.IntegerUniqueIndex;
import run.halo.app.extension.index.h2.StringIndex;
import run.halo.app.extension.index.h2.StringUniqueIndex;

public interface H2Index<E extends Extension, K extends Comparable<K>> extends Index<E, K> {

    /**
     * Get the table name of this index.
     *
     * @return the table name of this index
     */
    @NonNull
    default String getTableName() {
        var keyType = getKeyType();
        if (keyType == String.class) {
            return isUnique() ? StringUniqueIndex.TABLE_NAME : StringIndex.TABLE_NAME;
        }
        if (keyType == Integer.class) {
            return isUnique() ? IntegerUniqueIndex.TABLE_NAME : IntegerIndex.TABLE_NAME;
        }
        throw new IllegalArgumentException("Unsupported key type: " + keyType);
    }

    String equal(K key);

}
