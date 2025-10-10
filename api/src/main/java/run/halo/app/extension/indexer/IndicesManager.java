package run.halo.app.extension.indexer;

import run.halo.app.extension.Extension;

public interface IndicesManager {

    <E extends Extension> void add(Class<E> type, Indices<E> indices);

    <E extends Extension> Indices<E> get(Class<E> type);
}
