package run.halo.app.extension.indexer.h2;

import lombok.Data;
import org.springframework.data.relational.core.mapping.Table;

@Table(ExtensionLabelsIndex.TABLE_NAME)
@Data
public class ExtensionLabelsIndex {

    public static final String TABLE_NAME = "extension_labels_index";

    private Long id;

    private String extensionName;

    private String labelKey;

    private String labelValue;

}
