package com.faultlens.api.it;

import static org.assertj.core.api.Assertions.assertThat;

import com.faultlens.collector.config.CollectorProperties;
import com.faultlens.collector.service.SourceLeaseManager;
import java.lang.reflect.Constructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;

/** P08: per-source leases on a real Redis, as two instances would see them. */
class SourceLeaseIT extends AbstractIntegrationTest {
    @Autowired
    private StringRedisTemplate redis;

    private SourceLeaseManager instance(String id, long ttlMs) throws Exception {
        CollectorProperties properties = new CollectorProperties();
        properties.getLease().setTtlMs(ttlMs);
        Constructor<SourceLeaseManager> constructor = SourceLeaseManager.class.getDeclaredConstructor(
                StringRedisTemplate.class, CollectorProperties.class, String.class);
        constructor.setAccessible(true);
        return constructor.newInstance(redis, properties, id);
    }

    @Test
    void onlyOneInstanceHoldsASourceAndOnlyTheOwnerCanRenewOrRelease() throws Exception {
        long source = 900_001L;
        SourceLeaseManager a = instance("a", 30_000);
        SourceLeaseManager b = instance("b", 30_000);

        assertThat(a.tryAcquire(source)).isTrue();
        assertThat(a.tryAcquire(source)).as("re-entrant for the owner").isTrue();
        assertThat(b.tryAcquire(source)).isFalse();
        assertThat(b.renew(source)).isFalse();
        b.release(source); // not the owner: no effect
        assertThat(a.ownerOf(source)).contains("a");

        a.release(source);
        assertThat(b.tryAcquire(source)).isTrue();
        assertThat(a.renew(source)).as("a lost it").isFalse();
    }

    @Test
    void anEditTakesTheLeaseOverAndARevokeFreesIt() throws Exception {
        long source = 900_002L;
        SourceLeaseManager a = instance("a", 30_000);
        SourceLeaseManager b = instance("b", 30_000);
        assertThat(a.tryAcquire(source)).isTrue();

        b.takeOver(source);
        assertThat(a.renew(source)).as("a stops at its next heartbeat").isFalse();
        assertThat(b.renew(source)).isTrue();

        a.revoke(source); // disabled on a
        assertThat(b.ownerOf(source)).isEmpty();
    }

    @Test
    void aCrashedOwnersLeaseExpiresAndAnotherInstanceTakesOver() throws Exception {
        long source = 900_003L;
        SourceLeaseManager crashed = instance("crashed", 300);
        SourceLeaseManager survivor = instance("survivor", 30_000);
        assertThat(crashed.tryAcquire(source)).isTrue();
        assertThat(survivor.tryAcquire(source)).isFalse();

        Thread.sleep(600); // no heartbeat from the crashed instance

        assertThat(survivor.tryAcquire(source)).isTrue();
        assertThat(survivor.ownerOf(source)).contains("survivor");
    }
}
