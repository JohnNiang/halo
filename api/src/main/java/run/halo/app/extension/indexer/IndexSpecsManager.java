package run.halo.app.extension.indexer;

import java.util.List;
import run.halo.app.extension.Extension;

public interface IndexSpecsManager {

    <E extends Extension> void add(Class<E> type, List<IndexSpec<E, ?>> specs);

    <E extends Extension> void remove(Class<E> type);

    <E extends Extension> List<IndexSpec<E, ?>> get(Class<E> type);

}
