package run.halo.app.extension.indexer;

public interface IndexOperation {

    void commit();

    void rollback();

}
