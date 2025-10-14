package run.halo.app.extension.index;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.commons.io.IOUtils;
import run.halo.app.extension.Extension;

class DefaultIndices<E extends Extension> implements Indices<E> {

    private final Map<String, Index<E, ?>> indexMap;

    private final Cache<String, ReadWriteLock> lockCache;

    private volatile boolean closed;

    public DefaultIndices(List<Index<E, ?>> indices) {
        this.indexMap = indices.stream()
            .collect(Collectors.toUnmodifiableMap(Index::getName, Function.identity()));
        this.lockCache = Caffeine.newBuilder()
            .expireAfterAccess(Duration.ofHours(1))
            .maximumSize(10_000)
            .build();
    }

    @Override
    public void close() throws IOException {
        closed = true;
        IOUtils.close(indexMap.values().toArray(Index[]::new));
        lockCache.invalidateAll();
    }

    @Override
    public void insert(E extension) {
        ensureNotClosed();
        // get primary key
        var primaryKey = extension.getMetadata().getName();
        var lock = Objects.requireNonNull(
            lockCache.get(primaryKey, pk -> new ReentrantReadWriteLock())
        ).writeLock();
        var updaters = new ArrayList<IndexOperation>();
        lock.lock();
        try {
            for (var index : indexMap.values()) {
                updaters.add(index.prepareInsert(extension));
            }
            updaters.forEach(IndexOperation::commit);
        } catch (Exception e) {
            updaters.forEach(IndexOperation::rollback);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void update(E extension) {
        ensureNotClosed();
        var primaryKey = extension.getMetadata().getName();
        var lock = Objects.requireNonNull(
            lockCache.get(primaryKey, pk -> new ReentrantReadWriteLock())
        ).writeLock();
        var updaters = new ArrayList<IndexOperation>();
        lock.lock();
        try {
            for (var index : indexMap.values()) {
                updaters.add(index.prepareUpdate(extension));
            }
            updaters.forEach(IndexOperation::commit);
        } catch (Exception e) {
            updaters.forEach(IndexOperation::rollback);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void delete(E extension) {
        ensureNotClosed();
        var primaryKey = extension.getMetadata().getName();
        var lock = Objects.requireNonNull(
            lockCache.get(primaryKey, pk -> new ReentrantReadWriteLock())
        ).writeLock();
        var updaters = new ArrayList<IndexOperation>();
        lock.lock();
        try {
            for (var index : indexMap.values()) {
                updaters.add(index.prepareDelete(primaryKey));
            }
            updaters.forEach(IndexOperation::commit);
        } catch (Exception e) {
            updaters.forEach(IndexOperation::rollback);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public <K extends Comparable<K>> Index<E, K> getIndex(String indexName) {
        ensureNotClosed();
        var index = (Index<E, K>) indexMap.get(indexName);
        if (index == null) {
            throw new IllegalArgumentException("No index found with name: " + indexName);
        }
        return index;
    }

    private void ensureNotClosed() {
        if (closed) {
            throw new IllegalStateException("Indices has been closed");
        }
    }
}
