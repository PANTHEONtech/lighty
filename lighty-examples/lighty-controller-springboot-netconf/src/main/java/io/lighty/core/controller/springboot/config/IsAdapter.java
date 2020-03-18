package io.lighty.core.controller.springboot.config;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.casbin.jcasbin.model.Model;
import org.casbin.jcasbin.persist.Adapter;
import org.casbin.jcasbin.persist.Helper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IsAdapter implements Adapter {

    private static final Logger LOG = LoggerFactory.getLogger(IsAdapter.class);

    private final String classPath;

    public IsAdapter(String classPath) {
        this.classPath = classPath;
    }

    @Override
    public void loadPolicy(Model model) {
        if (classPath.equals("")) {
            // throw new Error("invalid file path, file path cannot be empty");
            return;
        }

        loadPolicyClassPath(model, Helper::loadPolicyLine);
    }

    @Override
    public void savePolicy(Model model) {
        LOG.warn("savePolicy is not implemented !");
    }

    @Override
    public void addPolicy(String sec, String ptype, List<String> rule) {
        throw new Error("not implemented");
    }

    @Override
    public void removePolicy(String sec, String ptype, List<String> rule) {
        throw new Error("not implemented");
    }

    @Override
    public void removeFilteredPolicy(String sec, String ptype, int fieldIndex, String... fieldValues) {
        throw new Error("not implemented");
    }

    private void loadPolicyClassPath(Model model, Helper.loadPolicyLineHandler<String, Model> handler) {
        InputStream is = IsAdapter.class.getResourceAsStream(classPath);
        BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));

        String line;
        try {
            while((line = br.readLine()) != null) {
                handler.accept(line, model);
            }

            is.close();
            br.close();
        } catch (IOException e) {
            IllegalStateException ioErr = new IllegalStateException("IO error occurred", e);
            LOG.error("IO error occurred", ioErr);
            throw ioErr;
        }
    }

}
