package io.lighty.modules.northbound.restconf.community.impl.tests;

import io.lighty.modules.northbound.restconf.community.impl.config.JsonRestConfServiceType;
import io.lighty.modules.northbound.restconf.community.impl.config.RestConfConfiguration;
import io.lighty.modules.northbound.restconf.community.impl.util.RestConfConfigUtils;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.net.InetAddress;

public class RestConfConfigurationTest {

    @Test
    public void testRestConfConfiguration() {
        RestConfConfiguration defaultRestConfConfiguration = RestConfConfigUtils.getDefaultRestConfConfiguration();
        RestConfConfiguration restConfConfiguration = new RestConfConfiguration(defaultRestConfConfiguration);

        Assert.assertEquals(defaultRestConfConfiguration, restConfConfiguration);
        Assert.assertTrue(defaultRestConfConfiguration.hashCode() == restConfConfiguration.hashCode());

        restConfConfiguration.setHttpPort(3333);
        restConfConfiguration.setJsonRestconfServiceType(JsonRestConfServiceType.DRAFT_02);
        restConfConfiguration.setInetAddress(InetAddress.getLoopbackAddress());
        restConfConfiguration.setWebSocketPort(4444);

        Assert.assertNotEquals(defaultRestConfConfiguration, restConfConfiguration);
        Assert.assertFalse(defaultRestConfConfiguration.hashCode() == restConfConfiguration.hashCode());
    }

}
