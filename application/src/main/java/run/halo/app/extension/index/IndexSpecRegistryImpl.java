package run.halo.app.extension.index;

import static run.halo.app.extension.index.DataType.INSTANT;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.lang.NonNull;
import run.halo.app.extension.Extension;
import run.halo.app.extension.ExtensionStoreUtil;
import run.halo.app.extension.Scheme;

/**
 * <p>A default implementation of {@link IndexSpecRegistry}.</p>
 *
 * @author guqing
 * @since 2.12.0
 */
public class IndexSpecRegistryImpl implements IndexSpecRegistry {
    private final ConcurrentMap<String, IndexSpecs> extensionIndexSpecs = new ConcurrentHashMap<>();

    @Override
    public IndexSpecs indexFor(Scheme scheme) {
        var keySpace = getKeySpace(scheme);
        var indexSpecs = new DefaultIndexSpecs();
        useDefaultIndexSpec(scheme.type(), indexSpecs);
        extensionIndexSpecs.put(keySpace, indexSpecs);
        return indexSpecs;
    }

    @Override
    public IndexSpecs getIndexSpecs(Scheme scheme) {
        var keySpace = getKeySpace(scheme);
        var result = extensionIndexSpecs.get(keySpace);
        if (result == null) {
            throw new IllegalArgumentException(
                "No index specs found for extension type: " + scheme.groupVersionKind()
                    + ", make sure you have called indexFor() before calling getIndexSpecs()");

        }
        return result;
    }

    @Override
    public boolean contains(Scheme scheme) {
        var keySpace = getKeySpace(scheme);
        return extensionIndexSpecs.containsKey(keySpace);
    }

    @Override
    public void removeIndexSpecs(Scheme scheme) {
        var keySpace = getKeySpace(scheme);
        extensionIndexSpecs.remove(keySpace);
    }

    @Override
    @NonNull
    public String getKeySpace(Scheme scheme) {
        return ExtensionStoreUtil.buildStoreNamePrefix(scheme);
    }

    <E extends Extension> void useDefaultIndexSpec(Class<E> extensionType,
        IndexSpecs indexSpecs) {
        var nameIndexSpec = PrimaryKeySpecUtils.primaryKeyIndexSpec(extensionType);
        indexSpecs.add(nameIndexSpec);

        var creationTimestampIndexSpec = new IndexSpec()
            .setDataType(INSTANT)
            .setName("metadata.creationTimestamp")
            .<Instant>setIndexFunc(IndexAttributeFactory.simpleAttribute(extensionType,
                e -> e.getMetadata().getCreationTimestamp())
            );
        indexSpecs.add(creationTimestampIndexSpec);

        var deletionTimestampIndexSpec = new IndexSpec()
            .setDataType(INSTANT)
            .setName("metadata.deletionTimestamp")
            .<Instant>setIndexFunc(IndexAttributeFactory.simpleAttribute(extensionType,
                e -> e.getMetadata().getDeletionTimestamp())
            );
        indexSpecs.add(deletionTimestampIndexSpec);

        indexSpecs.add(LabelIndexSpecUtils.labelIndexSpec(extensionType));
    }
}
