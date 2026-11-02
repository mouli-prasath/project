package com.tripplanner;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * The single most valuable test in this suite for the kind of bug we just
 * hit (a JPA mapping error that only surfaces at Hibernate schema
 * generation time): this loads the ENTIRE Spring context, including every
 * entity, exactly like `mvn spring-boot:run` does. If any bean fails to
 * wire up or Hibernate can't generate the schema, this test fails with the
 * same error you'd see at actual startup - just as part of `mvn test`,
 * catching it before you even try to run the app.
 *
 * NOTE: this needs a real, reachable MySQL instance with the same
 * credentials as application.yml (DB_USERNAME/DB_PASSWORD env vars) - it is
 * NOT an isolated test database. That's a deliberate simplicity trade-off;
 * swapping this to Testcontainers (an ephemeral, throwaway MySQL container
 * spun up just for tests) is the natural upgrade if you want CI to run
 * without a real MySQL server present. See README "Known limitations".
 */
@SpringBootTest
class TripPlannerApplicationTests {

    @Test
    void contextLoads() {
        // Intentionally empty - the test's entire job is to confirm
        // SpringApplication.run() completes without throwing.
    }
}
