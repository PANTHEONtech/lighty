package io.lighty.core.controller.springboot.test;

import io.lighty.core.controller.springboot.config.EnforcerProducer;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import org.casbin.jcasbin.main.Enforcer;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class TestEnforcer {

    @Parameterized.Parameters
    public static Collection<Object[]> data() throws IOException {
        final EnforcerProducer producer2 =
                new EnforcerProducer("/data/security/authz_model.conf", "/data/security/authz_policy.csv");

        return Arrays.asList(new Object[][] {
                { "alice", "/services/data/netconf/list", "GET", true, producer2 },
                { "bob", "/services/data/netconf/list", "GET", true, producer2 },
                { "bob", "/services/data/netconf/id/xxx", "PUT", true, producer2 },
                { "alice", "/services/data/netconf/id/xxx", "PUT", false, producer2 },
                { "bob", "/services/data/netconf/id/xxx", "DELETE", true, producer2 },
                { "alice", "/services/data/netconf/id/xxx", "DELETE", false, producer2 },

                { "alice", "/services/data/topology/list", "GET", true, producer2 },
                { "bob", "/services/data/topology/list", "GET", true, producer2 },
                { "bob", "/services/data/topology/id/xxx", "PUT", true, producer2 },
                { "alice", "/services/data/topology/id/xxx", "PUT", false, producer2 },
                { "bob", "/services/data/topology/id/xxx", "DELETE", true, producer2 },
                { "alice", "/services/data/topology/id/xxx", "DELETE", false, producer2 },
        });
    }

    private final String user;
    private final String path;
    private final String method;
    private final boolean expectedResult;
    private final EnforcerProducer producer;

    public TestEnforcer(String user, String path, String method, boolean expectedResult, EnforcerProducer producer) {
        this.user = user;
        this.path = path;
        this.method = method;
        this.expectedResult = expectedResult;
        this.producer = producer;
    }

    @Test
    public void testEnforcer() {
        Enforcer enforcer = producer.getEnforcer();
        boolean result = enforcer.enforce(user, path, method);
        Assert.assertTrue(result == expectedResult);
    }

}
