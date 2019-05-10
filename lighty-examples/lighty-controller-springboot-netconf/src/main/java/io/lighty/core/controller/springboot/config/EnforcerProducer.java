package io.lighty.core.controller.springboot.config;

import org.casbin.jcasbin.main.Enforcer;
import org.casbin.jcasbin.model.Model;
import org.casbin.jcasbin.persist.Adapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

@Configuration
public class EnforcerProducer {

    private static final Logger LOG = LoggerFactory.getLogger(EnforcerProducer.class);

    private final Enforcer enforcer;

    public EnforcerProducer() {
        this("/data/security/authz_model.conf", "/data/security/authz_policy.csv");
    }

    public EnforcerProducer(String modelClassPath, String policyClassPath) {
        LOG.info("initializing enforcer ...");
        Model model = new Model();
        Adapter adapter = new IsAdapter(policyClassPath);

        InputStream is = EnforcerProducer.class.getResourceAsStream(modelClassPath);
        String text = new BufferedReader(new InputStreamReader(is))
                .lines().collect(Collectors.joining("\n"));
        model.loadModelFromText(text);
        this.enforcer = new Enforcer(model, adapter);
    }

    @Bean
    public Enforcer getEnforcer() {
        return this.enforcer;
    }

}
