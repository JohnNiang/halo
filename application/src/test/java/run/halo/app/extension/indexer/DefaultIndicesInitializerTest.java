package run.halo.app.extension.indexer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import run.halo.app.extension.Extension;
import run.halo.app.extension.ExtensionConverter;
import run.halo.app.extension.GVK;
import run.halo.app.extension.Metadata;
import run.halo.app.extension.MetadataOperator;
import run.halo.app.extension.Scheme;
import run.halo.app.extension.index.IndexEngine;
import run.halo.app.extension.index.IndexEngineTestSupport;
import run.halo.app.extension.index.IndexSnapshotManager;
import run.halo.app.extension.index.IndexSpecs;
import run.halo.app.extension.index.IndicesSnapshot;
import run.halo.app.extension.store.ExtensionStore;
import run.halo.app.extension.store.ExtensionStoreClient;
import run.halo.app.extension.store.NameVersion;

class DefaultIndicesInitializerTest {

    private static final String PREFIX = "/registry/fakeposts";

    @TempDir
    Path workDir;

    IndexEngine indexEngine;

    ExtensionStoreClient storeClient;

    ExtensionConverter converter;

    IndexSnapshotManager snapshotManager;

    DefaultIndicesInitializer initializer;

    Scheme scheme;

    @BeforeEach
    void setUp() {
        indexEngine = IndexEngineTestSupport.newIndexEngine();
        storeClient = mock(ExtensionStoreClient.class);
        converter = mock(ExtensionConverter.class);
        snapshotManager = new IndexSnapshotManager(workDir.resolve("indices"));
        initializer = new DefaultIndicesInitializer(indexEngine, storeClient, converter, snapshotManager);
        scheme = Scheme.buildFromType(FakePost.class);
        lenient().when(converter.convertFrom(any(), any(ExtensionStore.class))).thenAnswer(inv -> {
            ExtensionStore es = inv.getArgument(1);
            var name = es.getName().substring(es.getName().lastIndexOf('/') + 1);
            var post = fakePost(name, es.getVersion());
            return post;
        });
    }

    private void registerType() {
        indexEngine
                .getIndicesManager()
                .add(
                        FakePost.class,
                        List.of(IndexSpecs.<FakePost, String>single("spec.slug", String.class)
                                .indexFunc(p -> p.slug)
                                .nullable(false)
                                .build()));
    }

    private static ExtensionStore storeOf(String name, long version) {
        return new ExtensionStore(PREFIX + "/" + name, new byte[] {1}, version);
    }

    private static FakePost fakePost(String name, long version) {
        var post = new FakePost(name, "slug-" + name);
        post.getMetadata().setVersion(version);
        return post;
    }

    @Test
    void shouldFullBuildWhenNoSnapshot() {
        registerType();
        when(storeClient.listBy(anyString(), any(), anyInt()))
                .thenReturn(List.of(storeOf("a", 1L), storeOf("b", 1L)))
                .thenReturn(List.of());

        initializer.doInitialize(scheme);

        assertThat(indexEngine.retrieveAll(FakePost.class, null, null)).containsExactlyInAnyOrder("a", "b");
        verify(storeClient, never()).listNameVersionsByNamePrefix(anyString());
    }

    @Test
    void shouldRestoreFromSnapshotAndApplyDelta() {
        registerType();
        // snapshot contains a(v1), c(v1) and gone(v1); database contains a(v1), b(v1) and c(v2)
        // => a stays untouched, b and c are (re)fetched, gone is deleted
        indexEngine.insert(List.of(fakePost("a", 1L), fakePost("c", 1L), fakePost("gone", 1L)));
        var indices = indexEngine.getIndicesManager().get(FakePost.class);
        snapshotManager.save(FakePost.class, indices.dump());
        // simulate a restart: drop the in-memory indices and register the type from scratch
        indexEngine.getIndicesManager().remove(FakePost.class);
        registerType();

        when(storeClient.listNameVersionsByNamePrefix(PREFIX))
                .thenReturn(List.of(
                        new NameVersion(PREFIX + "/a", 1L),
                        new NameVersion(PREFIX + "/b", 1L),
                        new NameVersion(PREFIX + "/c", 2L)));
        when(storeClient.listByNames(anyList())).thenAnswer(inv -> {
            List<String> names = inv.getArgument(0);
            return names.stream()
                    .map(name -> {
                        var simpleName = name.substring(name.lastIndexOf('/') + 1);
                        var version = "c".equals(simpleName) ? 2L : 1L;
                        return storeOf(simpleName, version);
                    })
                    .toList();
        });

        initializer.doInitialize(scheme);

        assertThat(indexEngine.retrieveAll(FakePost.class, null, null)).containsExactlyInAnyOrder("a", "b", "c");
        // delta recovery must not fall back to a full-table data scan
        verify(storeClient, never()).listBy(anyString(), any(), anyInt());
    }

    @Test
    void shouldMergedScanRecoverWhenFingerprintsMismatch() {
        registerType();
        // a snapshot whose index entries match nothing: every index is dirty and must be rebuilt
        // by a merged full-table scan which also applies the delta against the manifest
        snapshotManager.save(FakePost.class, new IndicesSnapshot(List.of(), Map.of("gone", 1L)));
        when(storeClient.listBy(anyString(), any(), anyInt()))
                .thenReturn(List.of(storeOf("a", 1L)))
                .thenReturn(List.of());

        initializer.doInitialize(scheme);

        assertThat(indexEngine.retrieveAll(FakePost.class, null, null)).containsExactly("a");
    }

    @Test
    void shouldFallBackToFullBuildOnCorruptedSnapshot() throws IOException {
        registerType();
        snapshotManager.save(FakePost.class, new IndicesSnapshot(List.of(), Map.of("a", 1L)));
        var snapshotFile = workDir.resolve("indices").resolve(FakePost.class.getName() + ".snapshot.gz");
        Files.write(snapshotFile, "corrupted".getBytes(StandardCharsets.UTF_8));
        when(storeClient.listBy(anyString(), any(), anyInt()))
                .thenReturn(List.of(storeOf("a", 1L)))
                .thenReturn(List.of());

        initializer.doInitialize(scheme);

        assertThat(indexEngine.retrieveAll(FakePost.class, null, null)).containsExactly("a");
    }

    @GVK(group = "", version = "v1alpha1", kind = "FakePost", plural = "fakeposts", singular = "fakepost")
    static class FakePost implements Extension {

        private final Metadata metadata = new Metadata();

        String slug;

        FakePost(String name, String slug) {
            metadata.setName(name);
            metadata.setCreationTimestamp(Instant.now());
            this.slug = slug;
        }

        @Override
        public Metadata getMetadata() {
            return metadata;
        }

        @Override
        public void setMetadata(MetadataOperator metadata) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setApiVersion(String apiVersion) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setKind(String kind) {
            throw new UnsupportedOperationException();
        }
    }
}
