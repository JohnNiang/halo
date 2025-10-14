package run.halo.app.extension.indexer.h2;

import lombok.Data;

@Data
public class AbstractIndex<T> {

    private Long id;

    private String extensionName;

    private String extensionType;

    private String indexName;

    private T indexKey;

}
