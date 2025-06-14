package run.halo.app.perf.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Data
@Table("labels")
public class LabelEntity {

    @Id
    private Long id;

    private String entityType;

    private String entityId;

    private String labelName;

    private String labelValue;

}
