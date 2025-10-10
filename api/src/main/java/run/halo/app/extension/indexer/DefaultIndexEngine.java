package run.halo.app.extension.indexer;

import org.springframework.data.domain.Sort;
import run.halo.app.extension.Extension;
import run.halo.app.extension.ListOptions;
import run.halo.app.extension.ListResult;
import run.halo.app.extension.PageRequest;

public class DefaultIndexEngine implements IndexEngine {

    private final IndicesManager indicesManager;

    public DefaultIndexEngine(IndicesManager indicesManager) {
        this.indicesManager = indicesManager;
    }

    @Override
    public <E extends Extension> void insert(Iterable<E> extensions) {
        extensions.forEach(extension -> {
            // get indices manager
            var indices = indicesManager.get((Class<E>) extension.getClass());
            indices.insert(extension);
        });
    }

    @Override
    public <E extends Extension> void update(Iterable<E> extensions) {
        extensions.forEach(extension -> {
            var indices = indicesManager.get((Class<E>) extension.getClass());
            indices.update(extension);
        });
    }

    @Override
    public <E extends Extension> void delete(Iterable<E> extensions) {
        extensions.forEach(extension -> {
            var indices = indicesManager.get((Class<E>) extension.getClass());
            indices.delete(extension);
        });
    }

    @Override
    public <E extends Extension> ListResult<String> retrieve(Class<E> type, ListOptions options,
        PageRequest page) {
        var indices = indicesManager.get(type);
        return null;
    }

    @Override
    public <E extends Extension> Iterable<String> retrieveAll(Class<E> type, ListOptions options,
        Sort sort) {
        return null;
    }

}
