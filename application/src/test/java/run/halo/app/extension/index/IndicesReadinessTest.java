package run.halo.app.extension.index;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import run.halo.app.extension.Extension;
import run.halo.app.extension.Metadata;
import run.halo.app.extension.MetadataOperator;

class IndicesReadinessTest {

    @Test
    void awaitReadyShouldBlockUntilMarkReady() throws InterruptedException {
        var manager = new DefaultIndicesManager();
        manager.add(FakeExt.class, List.of());
        var queried = new AtomicBoolean(false);
        var threadStarted = new CountDownLatch(1);
        var thread = new Thread(() -> {
            threadStarted.countDown();
            manager.awaitReady(FakeExt.class);
            queried.set(true);
        });
        thread.start();
        assertThat(threadStarted.await(5, TimeUnit.SECONDS)).isTrue();
        Thread.sleep(200);
        assertThat(queried).isFalse();
        manager.markReady(FakeExt.class);
        thread.join(5000);
        assertThat(queried).isTrue();
    }

    @Test
    void awaitReadyShouldThrowForUnknownType() {
        var manager = new DefaultIndicesManager();
        assertThatThrownBy(() -> manager.awaitReady(FakeExt.class)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void markReadyShouldBeIdempotent() {
        var manager = new DefaultIndicesManager();
        manager.add(FakeExt.class, List.of());
        manager.markReady(FakeExt.class);
        manager.markReady(FakeExt.class);
        manager.awaitReady(FakeExt.class); // returns immediately
    }

    @Test
    void removeShouldDiscardLatch() {
        var manager = new DefaultIndicesManager();
        manager.add(FakeExt.class, List.of());
        manager.markReady(FakeExt.class);
        manager.remove(FakeExt.class);
        assertThatThrownBy(() -> manager.awaitReady(FakeExt.class)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void awaitReadyShouldTimeout() {
        var manager = new DefaultIndicesManager();
        manager.setReadyTimeout(Duration.ofMillis(100));
        manager.add(FakeExt.class, List.of());
        assertThatThrownBy(() -> manager.awaitReady(FakeExt.class))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not ready");
    }

    static class FakeExt implements Extension {
        private final Metadata metadata = new Metadata();

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
