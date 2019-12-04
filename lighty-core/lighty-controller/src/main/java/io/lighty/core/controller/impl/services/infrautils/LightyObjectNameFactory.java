package io.lighty.core.controller.impl.services.infrautils;


import com.codahale.metrics.jmx.DefaultObjectNameFactory;
import javax.management.ObjectName;

/**
 * Custom transformer of String Metric ID to JMX ObjectName.
 *
 * @author Michael Vorburger.ch
 */
public class LightyObjectNameFactory extends DefaultObjectNameFactory { // TODO implements ObjectNameFactory {

    // TODO tune the conversion of label name/values to appropriate JMX ObjectName.. perhaps using some <> and [] ?

    @Override
    public ObjectName createName(String type, String domain, String name) {
        return super.createName(type, domain, name);
    }

}