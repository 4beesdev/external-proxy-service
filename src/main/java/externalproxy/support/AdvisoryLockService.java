package externalproxy.support;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class AdvisoryLockService {

    private final JdbcTemplate jdbcTemplate;

    public AdvisoryLockService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void lock(String ipHash) {
        jdbcTemplate.query(
                "select pg_advisory_xact_lock(hashtextextended(?, 0))",
                ps -> ps.setString(1, ipHash),
                rs -> null
        );
    }
}

