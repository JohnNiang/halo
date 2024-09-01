package run.halo.app.extension.index;

import lombok.experimental.UtilityClass;
import run.halo.app.extension.Extension;

@UtilityClass
public class PrimaryKeySpecUtils {
    public static final String PRIMARY_INDEX_NAME = "metadata.name";

    /**
     * Primary key index spec.
     *
     * @param type the type
     * @param <E> the type parameter of {@link Extension}
     * @return the index spec
     */
    public static <E extends Extension> IndexSpec<String> primaryKeyIndexSpec(Class<E> type) {
        return new IndexSpec<String>()
            .setName(PRIMARY_INDEX_NAME)
            .setUnique(true)
            .setIndexFunc(IndexAttributeFactory.simpleAttribute(type,
                e -> e.getMetadata().getName())
            );
    }

    public static String getObjectPrimaryKey(Extension obj) {
        return obj.getMetadata().getName();
    }
}
