package run.halo.app.extension.materialized;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Embedded;
import org.springframework.data.relational.core.mapping.Table;

/**
 * R2DBC entity mapping to the {@code extension_labels} table with a composite primary key.
 *
 * @author halo
 * @since 2.21.0
 */
@Data
@Table(name = "extension_labels")
public class ExtensionLabel {

    @Id
    @Embedded(onEmpty = Embedded.OnEmpty.USE_NULL)
    private ExtensionLabelId id;

    private String labelValue;
}
