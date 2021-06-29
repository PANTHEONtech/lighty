/*
 * Copyright (c) 2021 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.modules.southbound.gnmi.wrappers;

import com.google.common.base.Strings;
import gnmi.Gnmi;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.mdsal.binding.dom.codec.impl.BindingCodecContext;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.QNameModule;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeWriter;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.model.api.Module;

public class PathWrapper {
    private static final String REGEX_SLASH = "/";
    private static final String REGEX_SEMICOLON = ";";
    private static final Pattern KEY_VAL_PAIR = Pattern.compile("(?<key>(\\w.+))=(?<val>(\\w.+))");
    private static final String REGEX_BEGIN_KEY = "[";
    private static final String REGEX_END_KEY = "]";
    private final BindingCodecContext bindingCodecContext;

    public PathWrapper(BindingCodecContext bindingCodecContext) {
        this.bindingCodecContext = bindingCodecContext;
    }

    public <T extends DataObject> Gnmi.Path.Builder getPathBuilder(InstanceIdentifier<T> instanceIdentifier)
            throws IOException {
        return getPathBuilder(bindingCodecContext.toYangInstanceIdentifier(instanceIdentifier));
    }

    public Gnmi.Path.Builder getPathBuilder(YangInstanceIdentifier instanceIdentifier) throws IOException {
        NormalizedNode<?, ?> normalizedNode =
                ImmutableNodes.fromInstanceId(this.bindingCodecContext.getRuntimeContext().getEffectiveModelContext(),
                        instanceIdentifier);
        String prefix = getPrefix(instanceIdentifier);
        if (prefix != null) {
            InstanceIdentifierNodeStreamWriter instanceIdentifierNodeStreamWriter =
                    new InstanceIdentifierNodeStreamWriter(prefix);
            NormalizedNodeWriter normalizedNodeWriter =
                    NormalizedNodeWriter.forStreamWriter(instanceIdentifierNodeStreamWriter, false);
            normalizedNodeWriter.write(normalizedNode);
            return pathBuilder(instanceIdentifierNodeStreamWriter.getUrl());
        }
        return null;
    }

    public static Gnmi.Path.Builder pathBuilder(String xpath) {
        final Gnmi.Path.Builder pathBuilder = Gnmi.Path.newBuilder();
        if (!Strings.isNullOrEmpty(xpath)) {
            String[] tokens = xpath.split(REGEX_SLASH);
            for (String token : tokens) {
                if (!StringUtils.isBlank(token)) {
                    String elem;
                    Map<String, String> keys = new HashMap<>();
                    final int beginKey = token.indexOf(REGEX_BEGIN_KEY);
                    final int endKey = token.indexOf(REGEX_END_KEY);
                    if (beginKey > 0) {
                        elem = token.substring(0, beginKey);
                        String keyPeers = token.substring(beginKey + 1, endKey);
                        for (String keyValPair : keyPeers.split(REGEX_SEMICOLON)) {
                            final Matcher matcher = KEY_VAL_PAIR.matcher(keyValPair);
                            if (matcher.matches()) {
                                keys.put(matcher.group("key"), matcher.group("val"));
                            }
                        }
                    } else {
                        elem = token;
                    }
                    gnmi.Gnmi.PathElem.Builder elemBuilder = Gnmi.PathElem.newBuilder().setName(elem);
                    if (!keys.isEmpty()) {
                        elemBuilder.putAllKey(keys);
                    }
                    pathBuilder.addElem(elemBuilder.build());
                }
            }
        }
        return pathBuilder;
    }

    private String getPrefix(YangInstanceIdentifier instanceIdentifier) {
        @NonNull QNameModule targetQname = instanceIdentifier.getLastPathArgument().getNodeType().getModule();
        Optional<Module> targetModule =
                this.bindingCodecContext.getRuntimeContext().getEffectiveModelContext().findModule(targetQname);
        return targetModule.map(Module::getName).orElse(null);
    }
}
