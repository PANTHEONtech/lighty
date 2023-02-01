/*
 * Copyright (c) 2021 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.modules.gnmi.commons.util;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.XMLNamespace;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.builder.DataContainerNodeBuilder;
import org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeWriter;
import org.opendaylight.yangtools.yang.data.codec.gson.JSONCodecFactory;
import org.opendaylight.yangtools.yang.data.codec.gson.JSONCodecFactorySupplier;
import org.opendaylight.yangtools.yang.data.codec.gson.JSONNormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.codec.gson.JsonParserStream;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableContainerNodeBuilder;
import org.opendaylight.yangtools.yang.data.util.DataSchemaContextTree;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.util.SchemaInferenceStack;
import org.opendaylight.yangtools.yang.model.util.SchemaInferenceStack.Inference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class DataConverter {
    private static final Logger LOG = LoggerFactory.getLogger(DataConverter.class);

    private DataConverter() {
        // Utility class
    }

    public static String jsonStringFromNormalizedNodes(@NonNull YangInstanceIdentifier identifier,
                                                       @NonNull NormalizedNode data,
                                                       @NonNull EffectiveModelContext context) {
        JSONCodecFactory jsonCodecFactory = JSONCodecFactorySupplier.RFC7951.createSimple(context);
        if (isListEntry(data)) {
            return createJsonWithNestedWriter(toInference(identifier, context), data, jsonCodecFactory);
        } else {
            return createJsonWithExclusiveWriter(toParentInference(identifier, context), data, jsonCodecFactory);
        }
    }

    public static NormalizedNode nodeFromJsonString(@NonNull YangInstanceIdentifier yangInstanceIdentifier,
            @NonNull String inputJson,
            @NonNull EffectiveModelContext context) {
        return fromJson(inputJson, toParentInference(yangInstanceIdentifier, context));
    }

    private static String createJsonWithExclusiveWriter(Inference inference, NormalizedNode data,
            JSONCodecFactory jsonCodecFactory) {
        Writer writer = new StringWriter();
        var jsonWriter = new JsonWriter(writer);
        XMLNamespace namespace = data.getIdentifier().getNodeType().getNamespace();
        NormalizedNodeStreamWriter nodeWriter = JSONNormalizedNodeStreamWriter
                .createExclusiveWriter(jsonCodecFactory, inference, namespace, jsonWriter);
        NormalizedNodeWriter normalizedNodeWriter = NormalizedNodeWriter.forStreamWriter(nodeWriter);
        try {
            normalizedNodeWriter.write(data);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } finally {
            closeAutoCloseableResource(normalizedNodeWriter);
            closeAutoCloseableResource(writer);
        }
        return writer.toString();
    }

    private static String createJsonWithNestedWriter(Inference inference, NormalizedNode data,
            JSONCodecFactory jsonCodecFactory) {
        Writer writer = new StringWriter();
        var jsonWriter = new JsonWriter(writer);
        XMLNamespace namespace = data.getIdentifier().getNodeType().getNamespace();
        NormalizedNodeStreamWriter nodeWriter = JSONNormalizedNodeStreamWriter
                .createNestedWriter(jsonCodecFactory, inference, namespace, jsonWriter);
        NormalizedNodeWriter normalizedNodeWriter = NormalizedNodeWriter.forStreamWriter(nodeWriter);
        try {
            normalizedNodeWriter.write(data);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } finally {
            closeAutoCloseableResource(normalizedNodeWriter);
            closeAutoCloseableResource(jsonWriter);
            closeAutoCloseableResource(writer);
        }
        return writer.toString();
    }

    @SuppressWarnings("IllegalCatch")
    private static void closeAutoCloseableResource(AutoCloseable resource) {
        try {
            resource.close();
        } catch (Exception e) {
            LOG.warn("Unable to close resource properly", e);
        }
    }

    private static NormalizedNode fromJson(String inputJson, Inference inference) {
        /*
         Write result into container builder with identifier (netconf:base)data. Makes possible to write multiple
          top level elements.
         */
        DataContainerNodeBuilder<NodeIdentifier, ContainerNode> resultBuilder = ImmutableContainerNodeBuilder
                .create()
                .withNodeIdentifier(YangInstanceIdentifier.NodeIdentifier.create(SchemaContext.NAME));

        NormalizedNodeStreamWriter streamWriter = ImmutableNormalizedNodeStreamWriter.from(resultBuilder);
        JSONCodecFactory jsonCodecFactory =
                JSONCodecFactorySupplier.RFC7951.createLazy(inference.getEffectiveModelContext());

        try (JsonParserStream jsonParser = JsonParserStream.create(streamWriter,
                jsonCodecFactory, inference)) {
            var reader = new JsonReader(new StringReader(inputJson));
            jsonParser.parse(reader);
        /*
         In a case when multiple values are present in result container that means we parsed multiple top elements,
          in that case return the container holding them.
         Otherwise (1 value) return that value only
         */
            ContainerNode resultContainer = resultBuilder.build();
            Collection<DataContainerChild> values = resultContainer.body();
            return values.size() == 1 ? values.iterator().next() : resultContainer;
        } catch (IOException e) {
            throw new RuntimeException("IO error while closing JsonParserStream", e);
        }
    }

    private static Inference toParentInference(YangInstanceIdentifier path, EffectiveModelContext ctx) {
        SchemaInferenceStack stack = DataSchemaContextTree.from(ctx)
                .enterPath(Objects.requireNonNull(path))
                .orElseThrow()
                .stack();
        if (!stack.isEmpty()) {
            stack.exit();
        }
        return stack.toInference();
    }

    private static Inference toInference(YangInstanceIdentifier path, EffectiveModelContext ctx) {
        return DataSchemaContextTree.from(ctx)
                .enterPath(Objects.requireNonNull(path))
                .orElseThrow()
                .stack()
                .toInference();
    }

    /**
     * Find module by the element-name in current converter's schema-context.
     * <p>It is necessary to specify module name as prefix, if there are multiple modules with same elements - the
     * element has to be specified uniquely.</p>
     * <ul>
     *      <li>Looking for module end correctly and return the module found, when:
     *      <ul>
     *           <li>exactly one module was found for element-name when module-name (as element-prefix) was NOT provided
     *           - there are no modules with same elements
     *           <dl>
     *                <dt>Example:</dt>
     *                <dd>schema-context contains single module: openconfig-interfaces</dd>
     *                <dd>entered element: interfaces</dd>
     *                <dd>found and returned: openconfig-interfaces:interfaces</dd>
     *           </dl></li>
     *           <li>exactly one module was found for element-name, there are modules with same elements, but the
     *           specific module-name was entered in form of element-prefix
     *           <dl>
     *                <dt>Example:</dt>
     *                <dd>schema-context contains two modules: openconfig-interfaces, ietf-interfaces</dd>
     *                <dd>entered element: openconfig-interfaces:interfaces</dd>
     *               <dd>found and returned: openconfig-interfaces:interfaces (skipped: ietf-interfaces:interfaces)</dd>
     *           </dl></li>
     *      </ul></li>
     * </ul>
     * <ul>
     *      <li>Looking for module ends with empty result in cases, when:
     *      <ul>
     *           <li>no module found for element-name</li>
     *           <li>multiple modules found for element-name but module-name in form of element-prefix was not provided
     *           <dl>
     *                <dt>Example:</dt>
     *                <dd>schema-context contains two modules: openconfig-interfaces, ietf-interfaces</dd>
     *                <dd>entered element: interfaces</dd>
     *                <dd>found and returned: openconfig-interfaces:interfaces, ietf-interfaces:interfaces</dd>
     *           </dl></li>
     *      </ul></li>
     * </ul>
     *
     * @param element the element-name (with or without module prefix)
     * @param context Schema context
     * @return YANG module containing the specific element
     */
    public static Optional<Module> findModuleByElement(@NonNull String element,
            @NonNull EffectiveModelContext context) {
        ElementNameWithModuleName elementWithModule = ElementNameWithModuleName.parseFromString(element);
        return context.getModules()
                .stream()
                .filter(module -> module.getChildNodes()
                        .stream()
                        .anyMatch(node -> elementWithModule.equals(node.getQName(), module)))
                .collect(Collectors.collectingAndThen(Collectors.toList(), modules -> {
                    if (modules.size() == 1) {
                        return Optional.of(modules.get(0));
                    } else {
                        LOG.warn("Found multiple modules for element {}: {}", element, modules);
                        return Optional.empty();
                    }
                }));
    }

    public static Optional<Module> findModuleByQName(@NonNull QName element,
            @NonNull EffectiveModelContext context) {
        return context.findModule(element.getModule());
    }

    public static Optional<DataSchemaNode> findAugmentationDataNode(@NonNull String element,
            @NonNull EffectiveModelContext context) {
        return context.getModules()
                .stream()
                .flatMap(module -> module.getAugmentations().stream())
                .flatMap(augmentation -> augmentation.getChildNodes().stream())
                .filter(childNode -> childNode.getQName().getLocalName().equals(element))
                .map(DataSchemaNode.class::cast)
                .findFirst();
    }

    private static boolean isListEntry(NormalizedNode node) {
        return node instanceof MapEntryNode;
    }
}
