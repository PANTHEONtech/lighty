package io.lighty.aaa;

import org.apache.shiro.config.Ini;
import org.apache.shiro.config.Ini.Section;
import org.apache.shiro.web.env.IniWebEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;


public class iniWebEnvironment extends IniWebEnvironment {

    private static final Logger LOG = LoggerFactory.getLogger(iniWebEnvironment.class);
    public static final String DEFAULT_SHIRO_INI_FILE = "etc/shiro.ini";
    public static final String SHIRO_FILE_PREFIX = "file:/";

    public iniWebEnvironment() {
    }

    @Override
    public void init() {
        // Initialize the Shiro environment from etc/shiro.ini then delegate to
        // the parent class
        Ini ini;
        try {
            ini = createDefaultShiroIni();
            // appendCustomIniRules(ini);
            setIni(ini);
        } catch (FileNotFoundException e) {
            final String ERROR_MESSAGE = "Could not find etc/shiro.ini";
            LOG.error(ERROR_MESSAGE, e);
        }
        super.init();
    }



    private Section getOrCreateUrlSection(final Ini ini) {
        final String URL_SECTION_TITLE = "urls";
        Section urlSection = ini.getSection(URL_SECTION_TITLE);
        if (urlSection == null) {
            LOG.debug("shiro.ini does not contain a [urls] section; creating one");
            urlSection = ini.addSection(URL_SECTION_TITLE);
        } else {
            LOG.debug("shiro.ini contains a [urls] section; appending rules to existing");
        }
        return urlSection;
    }


    static Ini createDefaultShiroIni() throws FileNotFoundException {
        return createShiroIni(DEFAULT_SHIRO_INI_FILE);
    }


    static Ini createShiroIni(final String path) throws FileNotFoundException {
        File f = new File(path);
        Ini ini = new Ini();
        final String fileBasedIniPath = createFileBasedIniPath(f.getAbsolutePath());
        ini.loadFromPath(fileBasedIniPath);
        return ini;
    }

    static String createFileBasedIniPath(final String path) {
        String fileBasedIniPath = SHIRO_FILE_PREFIX + path;
        LOG.debug(fileBasedIniPath);
        return fileBasedIniPath;
    }
}
