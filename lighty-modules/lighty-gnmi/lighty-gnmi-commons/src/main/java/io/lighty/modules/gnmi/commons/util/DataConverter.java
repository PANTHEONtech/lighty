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
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.yangtools.yang.common.QName;
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
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;
import org.opendaylight.yangtools.yang.model.api.EffectiveStatementInference;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.stmt.SchemaNodeIdentifier.Absolute;
import org.opendaylight.yangtools.yang.model.util.SchemaInferenceStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class DataConverter {
    private static final Logger LOG = LoggerFactory.getLogger(DataConverter.class);
    private static final int MAP_ENTRY_PARENT_OFFSET = 2;
    private static final int PARENT_OFFSET = 1;

    public static final YangInstanceIdentifier ROOT_IDENTIFIER = YangInstanceIdentifier.empty();

    private DataConverter() {
        // Utility class
    }

    public static String jsonStringFromNormalizedNodes(@NonNull final YangInstanceIdentifier identifier,
                                                       @NonNull final NormalizedNode data,
                                                       @NonNull final EffectiveModelContext context) {
        final JSONCodecFactory jsonCodecFactory = JSONCodecFactorySupplier.RFC7951.createSimple(context);
        if (isListEntry(data)) {
            return createJsonWithNestedWriter(toSchemaStack(identifier, context), data, jsonCodecFactory);
        } else {
            return createJsonWithExclusiveWriter(getParentStack(identifier, context), data, jsonCodecFactory);
        }
    }

    public static NormalizedNode nodeFromJsonString(@NonNull final YangInstanceIdentifier yangInstanceIdentifier,
                                                    @NonNull final String inputJson,
                                                    @NonNull final EffectiveModelContext context) {
        return fromJson(inputJson, getParentStack(yangInstanceIdentifier,context).toInference(), context);
    }

    private static String createJsonWithExclusiveWriter(final SchemaInferenceStack schemaStack,
                                                        final NormalizedNode data,
                                                        final JSONCodecFactory jsonCodecFactory) {
        final Writer writer = new StringWriter();
        final JsonWriter jsonWriter = new JsonWriter(writer);
        final var namespace = schemaStack.isEmpty() ? null
                : schemaStack.currentModule().localQNameModule().getNamespace();
        final NormalizedNodeStreamWriter nodeWriter = JSONNormalizedNodeStreamWriter
                .createExclusiveWriter(jsonCodecFactory, schemaStack.toInference(), namespace, jsonWriter);
        final NormalizedNodeWriter normalizedNodeWriter = NormalizedNodeWriter.forStreamWriter(nodeWriter);
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

    private static String createJsonWithNestedWriter(final SchemaInferenceStack schemaStack, NormalizedNode data,
                                                     final JSONCodecFactory jsonCodecFactory) {
        final Writer writer = new StringWriter();
        final JsonWriter jsonWriter = new JsonWriter(writer);
        final var namespace = schemaStack.isEmpty() ? null
                : schemaStack.currentModule().localQNameModule().getNamespace();
        final NormalizedNodeStreamWriter nodeWriter = JSONNormalizedNodeStreamWriter
                .createNestedWriter(jsonCodecFactory, schemaStack.toInference(), namespace, jsonWriter);
        final NormalizedNodeWriter normalizedNodeWriter = NormalizedNodeWriter.forStreamWriter(nodeWriter);
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
    private static void closeAutoCloseableResource(final AutoCloseable resource) {
        try {
            resource.close();
        } catch (Exception e) {
            LOG.warn("Unable to close resource properly", e);
        }
    }

    private static NormalizedNode fromJson(final String inputJson, final EffectiveStatementInference statementInference,
                                           final EffectiveModelContext context) {
        /*
         Write result into container builder with identifier (netconf:base)data. Makes possible to write multiple
          top level elements.
         */
        final DataContainerNodeBuilder<NodeIdentifier, ContainerNode> resultBuilder = ImmutableContainerNodeBuilder
            .create()
            .withNodeIdentifier(YangInstanceIdentifier.NodeIdentifier.create(SchemaContext.NAME));

        final NormalizedNodeStreamWriter streamWriter = ImmutableNormalizedNodeStreamWriter.from(resultBuilder);
        final JSONCodecFactory jsonCodecFactory =
                JSONCodecFactorySupplier.RFC7951.createLazy(context);

        try (JsonParserStream jsonParser = (statementInference != null)
                ? JsonParserStream.create(streamWriter, jsonCodecFactory, statementInference)
                : JsonParserStream.create(streamWriter, jsonCodecFactory)) {

            final JsonReader reader = new JsonReader(new StringReader(inputJson));
            jsonParser.parse(reader);
        /*
         In a case when multiple values are present in result container that means we parsed multiple top elements,
          in that case return the container holding them.
         Otherwise (1 value) return that value only
         */
            final ContainerNode resultContainer = resultBuilder.build();
            final Collection<DataContainerChild> values =
                    resultContainer.body();
            return values.size() == 1 ? values.iterator().next() : resultContainer;
        } catch (IOException e) {
            throw new RuntimeException("IO error while closing JsonParserStream", e);
        }
    }

    private static SchemaInferenceStack getParentStack(final YangInstanceIdentifier identifier,
                                                       final EffectiveModelContext context) {
        // In case of root
        if (identifier == ROOT_IDENTIFIER) {
            return SchemaInferenceStack.of(context);
        }

        final int offset = isMapEntryPath(identifier) ? MAP_ENTRY_PARENT_OFFSET : PARENT_OFFSET;
        final List<QName> schemaArgs = identifier.getPathArguments()
                .subList(0, identifier.getPathArguments().size() - offset).stream()
                .filter(arg -> !(arg instanceof YangInstanceIdentifier.NodeIdentifierWithPredicates))
                .filter(arg -> !(arg instanceof YangInstanceIdentifier.AugmentationIdentifier))
                .map(YangInstanceIdentifier.PathArgument::getNodeType)
                .collect(Collectors.toList());
        return schemaArgs.isEmpty() ? SchemaInferenceStack.of(context)
                : SchemaInferenceStack.of(context, Absolute.of(schemaArgs));
    }

    private static SchemaInferenceStack toSchemaStack(final YangInstanceIdentifier path,
                                                      final EffectiveModelContext context) {
        final List<QName> schemaArgs = path.getPathArguments()
                .stream()
                .filter(arg -> !(arg instanceof YangInstanceIdentifier.NodeIdentifierWithPredicates))
                .filter(arg -> !(arg instanceof YangInstanceIdentifier.AugmentationIdentifier))
                .map(YangInstanceIdentifier.PathArgument::getNodeType)
                .collect(Collectors.toList());
        return schemaArgs.isEmpty() ? SchemaInferenceStack.of(context)
                : SchemaInferenceStack.of(context, Absolute.of(schemaArgs));
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
     * @param element the element-name (with or without module prefix)
     * @param context Schema context
     * @return YANG module containing the specific element
     */
    public static Optional<Module> findModuleByElement(@NonNull final String element,
                                                       @NonNull final EffectiveModelContext context) {
        final ElementNameWithModuleName elementWithModule = ElementNameWithModuleName.parseFromString(element);
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

    public static Optional<Module> findModuleByQName(@NonNull final QName element,
                                                     @NonNull final EffectiveModelContext context) {
        return context.findModule(element.getModule());
    }

    public static Optional<DataSchemaNode> findAugmentationDataNode(@NonNull final String element,
                                                                    @NonNull final EffectiveModelContext context) {
        return context.getModules()
                .stream()
                .flatMap(module -> module.getAugmentations().stream())
                .flatMap(augmentation -> augmentation.getChildNodes().stream())
                .filter(childNode -> childNode.getQName().getLocalName().equals(element))
                .map(DataSchemaNode.class::cast)
                .findFirst();
    }

    private static boolean isListEntry(final NormalizedNode node) {
        return node instanceof MapEntryNode;
    }

    private static boolean isMapEntryPath(final YangInstanceIdentifier yid) {
        return yid.getLastPathArgument() instanceof YangInstanceIdentifier.NodeIdentifierWithPredicates;
    }
}
