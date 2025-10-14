package run.halo.app.extension.indexer;

public interface IndexOperation {

    void prepare();

    void commit();

    void rollback();

}
