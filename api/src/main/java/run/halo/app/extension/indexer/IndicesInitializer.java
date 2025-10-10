package run.halo.app.extension.indexer;

import run.halo.app.extension.Extension;

public interface IndicesInitializer<E extends Extension> {

    void initialize(Class<E> type);

}
