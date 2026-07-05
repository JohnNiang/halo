package run.halo.app.extension.materialized;

import lombok.Data;
import org.springframework.data.relational.core.mapping.Column;

/**
 * Composite primary key for {@link ExtensionLabel}.
 *
 * @author halo
 * @since 2.21.0
 */
@Data
public class ExtensionLabelId {

    @Column("extension_name")
    private String extensionName;

    @Column("label_key")
    private String labelKey;
}
