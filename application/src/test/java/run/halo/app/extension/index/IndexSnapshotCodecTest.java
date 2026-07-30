package run.halo.app.extension.index;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class IndexSnapshotCodecTest {

    @Test
    void shouldRoundTrip() throws Exception {
        var snapshot = new IndicesSnapshot(
                List.of(
                        new IndexSnapshot(
                                "spec.slug",
                                "fp-1",
                                "java.lang.String",
                                List.of(
                                        new IndexSnapshot.Entry(List.of("hello"), List.of("p1", "p2")),
                                        new IndexSnapshot.Entry(List.of("world"), List.of("p3"))),
                                List.of("p4")),
                        new IndexSnapshot(
                                "metadata.labels",
                                "label-fp",
                                "label",
                                List.of(new IndexSnapshot.Entry(List.of("app", "halo"), List.of("p1"))),
                                List.of())),
                Map.of("p1", 1L, "p2", 2L, "p3", 7L, "p4", 9L));

        var out = new ByteArrayOutputStream();
        IndexSnapshotCodec.write(snapshot, out);
        var restored = IndexSnapshotCodec.read(new ByteArrayInputStream(out.toByteArray()));

        assertThat(restored).isEqualTo(snapshot);
    }

    @Test
    void shouldRejectBadMagic() {
        var garbage = new byte[] {0x1f, (byte) 0x8b, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16};
        assertThatThrownBy(() -> IndexSnapshotCodec.read(new ByteArrayInputStream(garbage)))
                .isInstanceOf(IndexSnapshotCorruptedException.class);
    }

    @Test
    void shouldRejectTruncatedStream() throws Exception {
        var snapshot = new IndicesSnapshot(List.of(), Map.of("a", 1L));
        var out = new ByteArrayOutputStream();
        IndexSnapshotCodec.write(snapshot, out);
        var truncated = new byte[out.size() / 2];
        System.arraycopy(out.toByteArray(), 0, truncated, 0, truncated.length);
        assertThatThrownBy(() -> IndexSnapshotCodec.read(new ByteArrayInputStream(truncated)))
                .isInstanceOf(IndexSnapshotCorruptedException.class);
    }
}
