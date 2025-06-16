package run.halo.app;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Example;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.core.query.Query;
import reactor.test.StepVerifier;
import run.halo.app.perf.entity.LabelEntity;
import run.halo.app.perf.entity.UserEntity;
import run.halo.app.perf.repository.LabelEntityRepository;
import run.halo.app.perf.repository.UserEntityRepository;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
//@ActiveProfiles("mysql")
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
}
