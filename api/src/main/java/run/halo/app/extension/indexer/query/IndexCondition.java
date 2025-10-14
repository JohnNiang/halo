package run.halo.app.extension.indexer.query;

public interface IndexCondition extends Condition {

    /**
     * Get the index name.
     *
     * @return the index name
     */
    String indexName();

}
