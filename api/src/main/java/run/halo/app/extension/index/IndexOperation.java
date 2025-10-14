package run.halo.app.extension.index;

public interface IndexOperation {

    void prepare();

    void commit();

    void rollback();

}
