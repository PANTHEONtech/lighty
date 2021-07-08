/*
 * Copyright (c) 2021 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.gnmi.southbound.schema;

import io.lighty.gnmi.southbound.capabilities.GnmiDeviceCapability;
import io.lighty.gnmi.southbound.schema.impl.SchemaContextHolderImpl;
import io.lighty.gnmi.southbound.schema.impl.SchemaException;
import io.lighty.gnmi.southbound.schema.loader.api.YangLoadException;
import io.lighty.gnmi.southbound.schema.loader.impl.ByPathYangLoaderService;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opendaylight.yang.gen.v1.urn.lighty.gnmi.yang.storage.rev210331.gnmi.yang.models.GnmiYangModel;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;
import org.opendaylight.yangtools.yang.model.api.Module;

public class SchemaConstructTest {

    private static final String SCHEMA_PATH = "src/test/resources/test_schema";
    private static final String SYNTAX_ERROR_YANGS_PATH = "src/test/resources/syntax_error_yangs";
    private static final List<String> MODELS_TO_MISS = Arrays.asList("openconfig-alarms",
            "openconfig-platform");
    private static final List<String> CAPABILITIES_TO_MISS = Arrays.asList("openconfig-alarm-types",
            "openconfig-yang-types", "openconfig-if-aggregate", "openconfig-platform-types", "openconfig-extensions",
            "test-dependency", "test-dependency2");
    private static final List<String> REVISION_MODELS = Arrays.asList("iana-if-type",
            "openconfig-extensions");
    private static final List<String> NO_VERSION_MODELS = Arrays.asList("no-version", "test-dependency",
            "test-dependency2");
    private TestYangDataStoreService dataStoreService;
    private List<GnmiDeviceCapability> completeCapabilities;

    /*
        Creates and loads all models to TestYangDataStoreService.
     */
    @BeforeEach
    public void setup() throws YangLoadException {
        dataStoreService = new TestYangDataStoreService();
        completeCapabilities = new ByPathYangLoaderService(Path.of(SCHEMA_PATH), null)
                .load(dataStoreService);
        Assertions.assertFalse(completeCapabilities.isEmpty());
    }

    /*
        Test that all loaded models have expected version types (semver, revision, empty).
     */
    @Test
    public void schemaConstructRevisionTest() {
        // Get and assert that all models are read
        final List<GnmiYangModel> storedModels =
                completeCapabilities.stream().map(cap -> cap.getVersionString().isPresent()
                        ? dataStoreService.readYangModel(cap.getName(), cap.getVersionString().get())
                        : dataStoreService.readYangModel(cap.getName()))
                        .peek(model -> Assertions.assertTrue(model.isPresent()))
                        .map(Optional::get)
                        .collect(Collectors.toList());

        for (GnmiYangModel model : storedModels) {
            if (NO_VERSION_MODELS.contains(model.getName())) {
                Assertions.assertTrue(model.getVersion().getValue().isEmpty());
            } else if (REVISION_MODELS.contains(model.getName())) {
                Assertions.assertTrue(model.getVersion().getValue().matches(SchemaConstants.REVISION_REGEX));
            } else {
                Assertions.assertTrue(model.getVersion().getValue().matches(SchemaConstants.SEMVER_REGEX));
            }
        }
    }

    /*
        Test that schemaContext is correctly created if every module for successfully creating schema is present in
         requested capabilities. Version of modules in requested capabilities is specified as SemVer, for modules
          which do not specify SemVer, revision is present.
         Modules are stored in datastore with one of the following, based on it's presence in model:
            1. Semantic version
            2. Revision
            3. empty
     */
    @Test
    public void schemaConstructSemVerTest() throws SchemaException {
        final SchemaContextHolder schemaContextHolder = new SchemaContextHolderImpl(dataStoreService, null);
        final EffectiveModelContext schemaContext = schemaContextHolder.getSchemaContext(completeCapabilities);
        // Check that every module in requested capabilities is contained in resulting schema
        assertSchemaContainsModels(schemaContext, completeCapabilities);
    }

    /*
        Test that schemaContext is correctly created even if every module for successfully creating schema is not
         present in requested capabilities (missing models are imports of modules that are present in capabilities).
        Schema should be created successfully because of mechanism which determines imports and adds them to schema.
     */
    @Test
    public void schemaConstructDependenciesTest() throws SchemaException {
        final SchemaContextHolder schemaContextHolder = new SchemaContextHolderImpl(
                dataStoreService, null);

        // Remove some models which are required for building models from capabilities
        final List<GnmiDeviceCapability> requestedCapabilities = completeCapabilities.stream()
                .filter(cap -> !CAPABILITIES_TO_MISS.contains(cap.getName())).collect(Collectors.toList());
        Assertions.assertEquals(completeCapabilities.size() - CAPABILITIES_TO_MISS.size(),
                requestedCapabilities.size());

        final EffectiveModelContext schemaContext = schemaContextHolder.getSchemaContext(
                requestedCapabilities);
        // Check that every module in full set of capabilities are contained in schema
        assertSchemaContainsModels(schemaContext, completeCapabilities);
    }

    /*
        Test that SchemaContextHolderImpl correctly reports missing models if they are not found in datastore.
    */
    @Test
    public void schemaConstructionModelsMissingTest() {
        final SchemaContextHolder schemaContextHolder = new SchemaContextHolderImpl(
                dataStoreService, null);
        //Delete models so they should be reported as missing
        for (String name : MODELS_TO_MISS) {
            Assertions.assertTrue(dataStoreService.deleteYangModel(name, null));
        }

        try {
            schemaContextHolder.getSchemaContext(completeCapabilities);
            Assertions.fail("Schema context creation should fail!");
        } catch (SchemaException schemaException) {
            // Check that all missing models are reported in SchemaException
            Assertions.assertFalse(schemaException.getMissingModels().isEmpty());
            Assertions.assertEquals(MODELS_TO_MISS.size(), schemaException.getMissingModels().size());
            for (GnmiDeviceCapability missingCap : schemaException.getMissingModels()) {
                Assertions.assertTrue(MODELS_TO_MISS.contains(missingCap.getName()));
            }

        }
    }

    /*
        Test that SchemaContextHolderImpl correctly reports missing models if they are not found in datastore and also
        reports missing models which are imports of models provided in capabilities.
    */
    @Test
    public void schemaConstructionModelsImportsMissingTest() {
        final SchemaContextHolder schemaContextHolder = new SchemaContextHolderImpl(
                dataStoreService, null);
        //Delete models so they should be reported as missing
        final ArrayList<String> modelsToDelete = new ArrayList<>(MODELS_TO_MISS);
        modelsToDelete.addAll(CAPABILITIES_TO_MISS);
        for (String name : modelsToDelete) {
            Assertions.assertTrue(dataStoreService.deleteYangModel(name, null));
        }

        try {
            schemaContextHolder.getSchemaContext(completeCapabilities);
            Assertions.fail("Schema context creation should fail!");
        } catch (SchemaException schemaException) {
            // Check that all missing models are reported in SchemaException
            Assertions.assertFalse(schemaException.getMissingModels().isEmpty());
            Assertions.assertEquals(modelsToDelete.size(), schemaException.getMissingModels().size());
            for (GnmiDeviceCapability missingCap : schemaException.getMissingModels()) {
                Assertions.assertTrue(modelsToDelete.contains(missingCap.getName()));
            }

        }

    }

    /*
        Test behaviour of schema context creation if some requested modules contains yang syntax errors.
     */
    @Test
    public void schemaConstructWrongSyntaxTest() throws IOException {
        // Delete models with correct syntax and add them back with wrong syntax
        final List<File> filesInFolder = Files.walk(Path.of(SYNTAX_ERROR_YANGS_PATH))
                .filter(Files::isRegularFile)
                .map(Path::toFile)
                .collect(Collectors.toList());
        // Change body of model to syntax error one
        for (File file : filesInFolder) {
            final String body = IOUtils.toString(Files.newInputStream(file.toPath()), StandardCharsets.UTF_8);
            final GnmiYangModel model = dataStoreService.readYangModel(
                    FilenameUtils.removeExtension(file.getName())).orElseThrow();
            dataStoreService.deleteYangModel(model.getName(), null);
            dataStoreService.addYangModel(model.getName(), model.getVersion().getValue(), body);
        }
        final SchemaContextHolder schemaContextHolder = new SchemaContextHolderImpl(
                dataStoreService, null);
        try {
            schemaContextHolder.getSchemaContext(completeCapabilities);
            Assertions.fail("Schema context creation should fail!");
        } catch (SchemaException schemaException) {
            // Check that reported number of models with errors is equal to actual number of yangs with error syntax
            Assertions.assertEquals(filesInFolder.size(), schemaException.getErrorMessages().size());
        }

    }

    /*
        Test behaviour of schema context creation if some requested modules contains yang syntax errors and some
         modules are also missing.
     */
    @Test
    public void schemaConstructWrongSyntaxAndMissingModelsTest() throws IOException {
        // Delete models with correct syntax and add them back with wrong syntax
        final List<File> filesInFolder = Files.walk(Path.of(SYNTAX_ERROR_YANGS_PATH))
                .filter(Files::isRegularFile)
                .map(Path::toFile)
                .collect(Collectors.toList());
        // Change body of model to syntax error one
        for (File file : filesInFolder) {
            final String body = IOUtils.toString(Files.newInputStream(file.toPath()), StandardCharsets.UTF_8);
            final GnmiYangModel model = dataStoreService.readYangModel(
                    FilenameUtils.removeExtension(file.getName())).orElseThrow();
            dataStoreService.deleteYangModel(model.getName(), null);
            dataStoreService.addYangModel(model.getName(), model.getVersion().getValue(), body);
        }

        //Delete models so they should be reported as missing
        final ArrayList<String> modelsToDelete = new ArrayList<>(MODELS_TO_MISS);
        modelsToDelete.addAll(CAPABILITIES_TO_MISS);
        for (String name : modelsToDelete) {
            Assertions.assertTrue(dataStoreService.deleteYangModel(name, null));
        }

        final SchemaContextHolder schemaContextHolder = new SchemaContextHolderImpl(
                dataStoreService, null);
        try {
            schemaContextHolder.getSchemaContext(completeCapabilities);
            Assertions.fail("Schema context creation should fail!");
        } catch (SchemaException schemaException) {
            // Check that number of models with errors is equal to actual number of yangs with error syntax
            Assertions.assertEquals(filesInFolder.size(), schemaException.getErrorMessages().size());
            // Check that all missing models are reported in SchemaException
            Assertions.assertFalse(schemaException.getMissingModels().isEmpty());
            Assertions.assertEquals(modelsToDelete.size(), schemaException.getMissingModels().size());
            for (GnmiDeviceCapability missingCap : schemaException.getMissingModels()) {
                Assertions.assertTrue(modelsToDelete.contains(missingCap.getName()));
            }
        }

    }

    private static void assertSchemaContainsModels(final EffectiveModelContext schema,
                                                   final List<GnmiDeviceCapability> capsToCheck) {
        for (GnmiDeviceCapability capability : capsToCheck) {
            final Optional<Module> match =
                (Optional<Module>) schema.getModules().stream()
                        .filter(module -> module.getName().equals(capability.getName()))
                        .findAny();
            Assertions.assertTrue(match.isPresent());
        }
    }

}
