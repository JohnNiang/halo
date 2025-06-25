package run.halo.app;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Example;
import org.springframework.data.r2dbc.core.R2dbcEntityOperations;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.r2dbc.dialect.DialectResolver;
import org.springframework.data.relational.core.dialect.RenderContextFactory;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.core.query.Query;
import org.springframework.data.relational.core.sql.AsteriskFromTable;
import org.springframework.data.relational.core.sql.Conditions;
import org.springframework.data.relational.core.sql.SQL;
import org.springframework.data.relational.core.sql.Select;
import org.springframework.data.relational.core.sql.StatementBuilder;
import org.springframework.r2dbc.core.binding.MutableBindings;
import reactor.test.StepVerifier;
import run.halo.app.perf.config.HaloPreparedOperation;
import run.halo.app.perf.entity.LabelEntity;
import run.halo.app.perf.entity.UserEntity;
import run.halo.app.perf.repository.LabelEntityRepository;
import run.halo.app.perf.repository.UserEntityRepository;

@SpringBootTest
// @ActiveProfiles("mysql")
//@ActiveProfiles("postgresql")
class UserEntityTest {

    @Autowired
    UserEntityRepository userEntityRepository;

    @Test
    void updateUser() {
        userEntityRepository.findById("johnniang")
            .doOnNext(user -> {
                user.setDisplayName("John Niang Updated");
                user.setBio("An updated bio for John Niang.");
            })
            .flatMap(userEntityRepository::save)
            .as(StepVerifier::create)
            .expectNextCount(1)
            .verifyComplete();
    }

    @Test
    void createUser() {
        var user = new UserEntity();
        user.setId("johnniang");
        user.markAsNew();
        user.setDisplayName("John Niang");
        user.setBio("A software engineer and open source enthusiast.");
        user.setEmail("johnniang@halo.run");
        user.setEncodedPassword("xxx");
        user.setFinalizers(Set.of("prevent-deletion"));
        user.setAnnotations(Map.of("key", "value"));
        user.setVersion(0L);
        user.setTwoFactorAuthEnabled(false);
        user.setEmailVerified(false);
        user.setDisabled(false);

        userEntityRepository.save(user)
            .as(StepVerifier::create)
            .assertNext(created -> assertNotNull(created.getId()))
            .verifyComplete();

        var userQuery = new UserEntity();
        // userQuery.setDisplayName("halo");
        userEntityRepository.findAll(Example.of(userQuery))
            .as(StepVerifier::create)
            .assertNext(found -> {
                System.out.println(found);
                assertFalse(found.isDisabled());
            })
            .verifyComplete();
    }

    @Test
    void createLabels(@Autowired LabelEntityRepository labelEntityRepository) {
        var label = new LabelEntity();
        label.setEntityType("user");
        label.setEntityId("johnniang");
        label.setLabelName("alabel");
        label.setLabelValue("avalue");

        labelEntityRepository.save(label)
            .as(StepVerifier::create)
            .assertNext(got -> {
                Assertions.assertNotNull(got.getId());
            })
            .verifyComplete();
    }

    @Test
    void queryByLabels(@Autowired R2dbcEntityTemplate template) {
        var criteria = Criteria.where("id").in("johnniang");
        Query query = Query.query(criteria);
        template.select(UserEntity.class)
            .matching(query);
    }

    @Test
    void queryWithSql(@Autowired R2dbcEntityTemplate template) {
        var builder = StatementBuilder.select();
        var usersTable = SQL.table("users");
        var labelsTable = SQL.table("labels");
        var select = builder.select(AsteriskFromTable.create(usersTable), AsteriskFromTable.create(labelsTable))
            .from(usersTable)
            .leftOuterJoin(labelsTable)
            .on(usersTable.column("id").isEqualTo(labelsTable.column("entity_id")))
            .build(true);

        template.getDatabaseClient().sql("""
                select users.* from users left join labels on users.id = labels.entity_id where labels.entity_type = 'user'\
                """)
            .mapProperties(UserEntity.class)
            .all()
            .doOnNext(System.out::println)
            .as(StepVerifier::create)
            .expectNextCount(1)
            .verifyComplete()
        ;
    }

    @Test
    void sqlGenerateTest(@Autowired R2dbcEntityOperations entityOperations) {

        var connectionFactory = entityOperations.getDatabaseClient().getConnectionFactory();
        var dialect = DialectResolver.getDialect(connectionFactory);
        var renderContextFactory = new RenderContextFactory(dialect);
        var renderContext = renderContextFactory.createRenderContext();

        var bindMarkersFactory = dialect.getBindMarkersFactory();
        var bindMarkers = bindMarkersFactory.create();

        var users = SQL.table("users");
        var labels = SQL.table("labels");
        var bindings = new MutableBindings(bindMarkers);

        var subselect = Select.builder().select(labels.column("entity_id"))
            .from(labels)
            .where(labels.column("entity_id").isEqualTo(users.column("id"))
                .and(labels.column("entity_type").isEqualTo(
                    SQL.bindMarker(bindings.bind("user").getPlaceholder())
                ))
                .and(labels.column("label_name").isEqualTo(
                    SQL.bindMarker(bindings.bind("halo.run/hidden-user").getPlaceholder())
                ))
                .and(labels.column("label_value").isEqualTo(
                    SQL.bindMarker(bindings.bind("true").getPlaceholder())
                ))
            )
            .build(false);

        var select = Select.builder()
            .select(users.asterisk())
            .from(users)
            .where(Conditions.in(users.column("id"), subselect)
                .and(users.column("disabled").isEqualTo(
                    SQL.bindMarker(bindings.bind(true).getPlaceholder())
                ))
            )
            .build(true);

        var operation = new HaloPreparedOperation(select, renderContext, bindings);
        var entities = entityOperations.query(operation, UserEntity.class)
            .all()
            .collectList()
            .block();
        System.out.println(entities);
    }


}
