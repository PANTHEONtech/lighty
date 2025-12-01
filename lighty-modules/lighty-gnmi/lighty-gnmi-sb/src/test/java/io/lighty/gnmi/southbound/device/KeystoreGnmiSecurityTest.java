/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.gnmi.southbound.device;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import io.grpc.ConnectivityState;
import io.grpc.ManagedChannel;
import io.lighty.gnmi.southbound.device.connection.DeviceConnection;
import io.lighty.gnmi.southbound.device.connection.DeviceConnectionInitializer;
import io.lighty.gnmi.southbound.device.session.security.KeystoreGnmiSecurityProvider;
import io.lighty.gnmi.southbound.schema.certstore.impl.CertificationStorageServiceImpl;
import io.lighty.modules.gnmi.connector.configuration.SessionConfiguration;
import io.lighty.modules.gnmi.connector.gnmi.session.impl.GnmiSessionFactoryImpl;
import io.lighty.modules.gnmi.connector.security.GnmiCallCredentials;
import io.lighty.modules.gnmi.connector.session.SessionManagerFactory;
import io.lighty.modules.gnmi.connector.session.SessionManagerImpl;
import io.lighty.modules.gnmi.connector.session.api.SessionProviderImpl;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.util.Base64;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opendaylight.aaa.encrypt.AAAEncryptionService;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.ReadTransaction;
import org.opendaylight.mdsal.binding.api.WriteTransaction;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Host;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.PortNumber;
import org.opendaylight.yang.gen.v1.urn.lighty.gnmi.certificate.storage.rev210504.Keystore;
import org.opendaylight.yang.gen.v1.urn.lighty.gnmi.certificate.storage.rev210504.KeystoreBuilder;
import org.opendaylight.yang.gen.v1.urn.lighty.gnmi.certificate.storage.rev210504.KeystoreKey;
import org.opendaylight.yang.gen.v1.urn.lighty.gnmi.topology.rev210316.GnmiNodeBuilder;
import org.opendaylight.yang.gen.v1.urn.lighty.gnmi.topology.rev210316.credentials.Credentials;
import org.opendaylight.yang.gen.v1.urn.lighty.gnmi.topology.rev210316.credentials.CredentialsBuilder;
import org.opendaylight.yang.gen.v1.urn.lighty.gnmi.topology.rev210316.gnmi.connection.parameters.ConnectionParametersBuilder;
import org.opendaylight.yang.gen.v1.urn.lighty.gnmi.topology.rev210316.security.SecurityChoice;
import org.opendaylight.yang.gen.v1.urn.lighty.gnmi.topology.rev210316.security.security.choice.InsecureDebugOnly;
import org.opendaylight.yang.gen.v1.urn.lighty.gnmi.topology.rev210316.security.security.choice.InsecureDebugOnlyBuilder;
import org.opendaylight.yang.gen.v1.urn.lighty.gnmi.topology.rev210316.security.security.choice.SecureBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yangtools.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.Uint16;

@ExtendWith(MockitoExtension.class)
public class KeystoreGnmiSecurityTest {

    private static final String KEYSTORE_PASSPHRASE_ID_1 = "KeystoreID_1";
    private static final String KEYSTORE_ID_2 = "KeystoreID_2";
    private static final int PORT = 9001;
    private static final String TEST_NODE = "test_node";
    private static final String TEST_USERNAME = "Test_Username";
    private static final String TEST_PASSWORD = "Test_Password";
    private static final String ADDRESS = "127.0.0.1";

    @Mock
    private AAAEncryptionService aaaEncryptionService;

    private static final String CA_CRT = "/certs/ca.crt";
    private static final String CLIENT_ENCRYPTED_CRT = "/certs/client.encrypted.crt";
    private static final String CLIENT_ENCRYPTED_KEY = "/certs/client.encrypted.key";
    private static final String PASSPHRASE = "password";
    private static final String CLIENT_CRT = "/certs/client.crt";
    private static final String CLIENT_KEY = "/certs/client.key";
    private DeviceConnectionInitializer connectionInitializer;

    @Spy
    private GnmiSessionFactoryImpl gnmiSessionFactorySpy;

    @BeforeEach
    public void setup() throws GeneralSecurityException {
        lenient().when(aaaEncryptionService.encrypt(any(byte[].class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(aaaEncryptionService.decrypt(any(byte[].class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        final DataBroker dataBrokerMock = mock(DataBroker.class);
        final WriteTransaction wtxMock = mock(WriteTransaction.class);
        final ReadTransaction rtxMock = mock(ReadTransaction.class);

        lenient().when(dataBrokerMock.newWriteOnlyTransaction()).thenReturn(wtxMock);
        lenient().when(dataBrokerMock.newReadOnlyTransaction()).thenReturn(rtxMock);

        lenient().doReturn(FluentFuture.from(Futures.immediateFuture(CommitInfo.empty())))
            .when(wtxMock).commit();

        final CertificationStorageServiceImpl certificationStorageService
            = new CertificationStorageServiceImpl(aaaEncryptionService, dataBrokerMock);
        final KeystoreGnmiSecurityProvider securityProvider
            = new KeystoreGnmiSecurityProvider(certificationStorageService);

        final ExecutorService gnmiExecutorService = Executors.newCachedThreadPool();

        final SessionManagerFactory sessionManagerFactoryMock = mock(SessionManagerFactory.class);
        final SessionManagerImpl sessionManagerMock = mock(SessionManagerImpl.class);

        when(sessionManagerMock.createSession(any()))
            .thenAnswer(invocation1 -> {
                final SessionConfiguration sessionConfig = invocation1.getArgument(0);

                ManagedChannel channelMock = mock(ManagedChannel.class);
                lenient().when(channelMock.getState(any(Boolean.class))).thenReturn(ConnectivityState.READY);

                return new SessionProviderImpl(
                    sessionConfig,
                    sessionManagerMock,
                    channelMock,
                    gnmiSessionFactorySpy.createGnmiSession(sessionConfig, channelMock)
                );
            });

        when(sessionManagerFactoryMock.createSessionManager(any())).thenReturn(sessionManagerMock);

        connectionInitializer = new DeviceConnectionInitializer(securityProvider,
            sessionManagerFactoryMock, dataBrokerMock, gnmiExecutorService);

        lenient().when(rtxMock.read(eq(LogicalDatastoreType.OPERATIONAL), eq(getKeystore1Identifier())))
            .thenAnswer(in -> getReadResult(getKeystore1WithPassResponse()));
        lenient().when(rtxMock.read(eq(LogicalDatastoreType.OPERATIONAL), eq(getKeystore2Identifier())))
            .thenAnswer(in -> getReadResult(getKeystore2Response()));
    }

    @AfterEach
    public void tearDown() throws Exception {
        if (connectionInitializer != null) {
            connectionInitializer.close();
        }
    }

    @Test
    public void testNoTls() throws Exception {
        final Node node = createNode(TEST_NODE, PORT, getNoTlsSecurityChoice());
        deviceInitializerDevicesConnecting(node);

        final ArgumentCaptor<SessionConfiguration> sessionConfigCaptor
            = ArgumentCaptor.forClass(SessionConfiguration.class);
        verify(gnmiSessionFactorySpy).createGnmiSession(sessionConfigCaptor.capture(), any(ManagedChannel.class));

        Assertions.assertEquals(1, sessionConfigCaptor.getAllValues().size());
        SessionConfiguration capturedConfiguration = sessionConfigCaptor.getValue();

        Assertions.assertTrue(capturedConfiguration.isUsePlainText());
        Assertions.assertEquals(ADDRESS, capturedConfiguration.getAddress().getHostString());
        Assertions.assertEquals(PORT, capturedConfiguration.getAddress().getPort());
        Assertions.assertNull(capturedConfiguration.getPassword());
        Assertions.assertNull(capturedConfiguration.getUsername());
    }

    @Test
    public void testInsecure() throws Exception {
        final Node node = createNode(TEST_NODE, PORT, getInsecureSecurityChoice());
        deviceInitializerDevicesConnecting(node);

        final ArgumentCaptor<SessionConfiguration> sessionConfigCaptor
            = ArgumentCaptor.forClass(SessionConfiguration.class);
        verify(gnmiSessionFactorySpy).createGnmiSession(sessionConfigCaptor.capture(), any(ManagedChannel.class));

        Assertions.assertEquals(1, sessionConfigCaptor.getAllValues().size());
        SessionConfiguration capturedConfiguration = sessionConfigCaptor.getValue();

        Assertions.assertFalse(capturedConfiguration.isUsePlainText());
        Assertions.assertEquals(ADDRESS, capturedConfiguration.getAddress().getHostString());
        Assertions.assertEquals(PORT, capturedConfiguration.getAddress().getPort());
        Assertions.assertNull(capturedConfiguration.getPassword());
        Assertions.assertNull(capturedConfiguration.getUsername());
    }

    @Test
    public void testCertificateWithPassphrase() throws Exception {
        final Node node = createNode(TEST_NODE, PORT, getTlsSecurityChoice(KEYSTORE_PASSPHRASE_ID_1));
        deviceInitializerDevicesConnecting(node);

        final ArgumentCaptor<SessionConfiguration> sessionConfigCaptor
            = ArgumentCaptor.forClass(SessionConfiguration.class);
        verify(gnmiSessionFactorySpy).createGnmiSession(sessionConfigCaptor.capture(), any(ManagedChannel.class));

        Assertions.assertEquals(1, sessionConfigCaptor.getAllValues().size());
        SessionConfiguration capturedConfiguration = sessionConfigCaptor.getValue();

        Assertions.assertFalse(capturedConfiguration.isUsePlainText());
        Assertions.assertEquals(ADDRESS, capturedConfiguration.getAddress().getHostString());
        Assertions.assertEquals(PORT, capturedConfiguration.getAddress().getPort());
        Assertions.assertNull(capturedConfiguration.getPassword());
        Assertions.assertNull(capturedConfiguration.getUsername());
    }

    @Test
    public void testCertificates() throws Exception {
        final Node node = createNode(TEST_NODE, PORT, getTlsSecurityChoice(KEYSTORE_ID_2));
        deviceInitializerDevicesConnecting(node);

        final ArgumentCaptor<SessionConfiguration> sessionConfigCaptor
            = ArgumentCaptor.forClass(SessionConfiguration.class);
        verify(gnmiSessionFactorySpy).createGnmiSession(sessionConfigCaptor.capture(), any(ManagedChannel.class));

        Assertions.assertEquals(1, sessionConfigCaptor.getAllValues().size());
        SessionConfiguration capturedConfiguration = sessionConfigCaptor.getValue();

        Assertions.assertFalse(capturedConfiguration.isUsePlainText());
        Assertions.assertEquals(ADDRESS, capturedConfiguration.getAddress().getHostString());
        Assertions.assertEquals(PORT, capturedConfiguration.getAddress().getPort());
        Assertions.assertNull(capturedConfiguration.getPassword());
        Assertions.assertNull(capturedConfiguration.getUsername());
    }

    @Test
    public void testNoTlsWithBasiAuth() throws Exception {
        final Node node = createNode(TEST_NODE, PORT, getNoTlsSecurityChoice(), getTestCredentials());
        deviceInitializerDevicesConnecting(node);

        verify(gnmiSessionFactorySpy, times(1))
            .createGnmiSession(any(ManagedChannel.class), any(GnmiCallCredentials.class));
        verify(gnmiSessionFactorySpy, times(0))
            .createGnmiSession(any(ManagedChannel.class));

        final ArgumentCaptor<SessionConfiguration> sessionConfigCaptor
            = ArgumentCaptor.forClass(SessionConfiguration.class);
        verify(gnmiSessionFactorySpy).createGnmiSession(sessionConfigCaptor.capture(), any(ManagedChannel.class));

        Assertions.assertEquals(1, sessionConfigCaptor.getAllValues().size());
        SessionConfiguration capturedConfiguration = sessionConfigCaptor.getValue();

        Assertions.assertTrue(capturedConfiguration.isUsePlainText());
        Assertions.assertEquals(ADDRESS, capturedConfiguration.getAddress().getHostString());
        Assertions.assertEquals(PORT, capturedConfiguration.getAddress().getPort());
        Assertions.assertEquals(TEST_USERNAME, capturedConfiguration.getPassword());
        Assertions.assertEquals(TEST_PASSWORD, capturedConfiguration.getUsername());
    }

    @Test
    public void testInsecureWithBasiAuth() throws Exception {
        final Node node = createNode(TEST_NODE, PORT, getInsecureSecurityChoice(), getTestCredentials());
        deviceInitializerDevicesConnecting(node);

        verify(gnmiSessionFactorySpy, times(1))
            .createGnmiSession(any(ManagedChannel.class), any(GnmiCallCredentials.class));
        verify(gnmiSessionFactorySpy, times(0))
            .createGnmiSession(any(ManagedChannel.class));

        final ArgumentCaptor<SessionConfiguration> sessionConfigCaptor
            = ArgumentCaptor.forClass(SessionConfiguration.class);
        verify(gnmiSessionFactorySpy).createGnmiSession(sessionConfigCaptor.capture(), any(ManagedChannel.class));

        Assertions.assertEquals(1, sessionConfigCaptor.getAllValues().size());
        SessionConfiguration capturedConfiguration = sessionConfigCaptor.getValue();

        Assertions.assertFalse(capturedConfiguration.isUsePlainText());
        Assertions.assertEquals(ADDRESS, capturedConfiguration.getAddress().getHostString());
        Assertions.assertEquals(PORT, capturedConfiguration.getAddress().getPort());
        Assertions.assertEquals(TEST_USERNAME, capturedConfiguration.getPassword());
        Assertions.assertEquals(TEST_PASSWORD, capturedConfiguration.getUsername());
    }

    @Test
    public void testCertificateWithPassphraseWithBasiAuth() throws Exception {
        final Node node
            = createNode(TEST_NODE, PORT, getTlsSecurityChoice(KEYSTORE_PASSPHRASE_ID_1), getTestCredentials());
        deviceInitializerDevicesConnecting(node);

        verify(gnmiSessionFactorySpy, times(1))
            .createGnmiSession(any(ManagedChannel.class), any(GnmiCallCredentials.class));
        verify(gnmiSessionFactorySpy, times(0))
            .createGnmiSession(any(ManagedChannel.class));

        final ArgumentCaptor<SessionConfiguration> sessionConfigCaptor
            = ArgumentCaptor.forClass(SessionConfiguration.class);
        verify(gnmiSessionFactorySpy).createGnmiSession(sessionConfigCaptor.capture(), any(ManagedChannel.class));

        Assertions.assertEquals(1, sessionConfigCaptor.getAllValues().size());
        SessionConfiguration capturedConfiguration = sessionConfigCaptor.getValue();

        Assertions.assertFalse(capturedConfiguration.isUsePlainText());
        Assertions.assertEquals(ADDRESS, capturedConfiguration.getAddress().getHostString());
        Assertions.assertEquals(PORT, capturedConfiguration.getAddress().getPort());
        Assertions.assertEquals(TEST_USERNAME, capturedConfiguration.getPassword());
        Assertions.assertEquals(TEST_PASSWORD, capturedConfiguration.getUsername());
    }

    @Test
    public void testCertificatesWithBasiAuth() throws Exception {
        final Node node = createNode(TEST_NODE, PORT, getTlsSecurityChoice(KEYSTORE_ID_2), getTestCredentials());
        deviceInitializerDevicesConnecting(node);

        final ArgumentCaptor<SessionConfiguration> sessionConfigCaptor
            = ArgumentCaptor.forClass(SessionConfiguration.class);
        verify(gnmiSessionFactorySpy).createGnmiSession(sessionConfigCaptor.capture(), any(ManagedChannel.class));
        verify(gnmiSessionFactorySpy, times(1))
            .createGnmiSession(any(ManagedChannel.class), any(GnmiCallCredentials.class));

        verify(gnmiSessionFactorySpy, times(0))
            .createGnmiSession(any(ManagedChannel.class));

        Assertions.assertEquals(1, sessionConfigCaptor.getAllValues().size());
        SessionConfiguration capturedConfiguration = sessionConfigCaptor.getValue();

        Assertions.assertFalse(capturedConfiguration.isUsePlainText());
        Assertions.assertEquals(ADDRESS, capturedConfiguration.getAddress().getHostString());
        Assertions.assertEquals(PORT, capturedConfiguration.getAddress().getPort());
        Assertions.assertEquals(TEST_USERNAME, capturedConfiguration.getPassword());
        Assertions.assertEquals(TEST_PASSWORD, capturedConfiguration.getUsername());
    }

    /*
        Tests behaviour of DevicesConnectionManager if nodes are not yet connected by DeviceSessionInitializerProvider.
     */
    public void deviceInitializerDevicesConnecting(Node node) throws Exception {
        final ListenableFuture<DeviceConnection> connect = connectionInitializer.initConnection(node);
        final DeviceConnection connection = connect.get(10, TimeUnit.SECONDS);
        Assertions.assertEquals(node.getNodeId(), connection.getIdentifier());
        connectionInitializer.close();
    }

    private static SecurityChoice getInsecureSecurityChoice() {
        return new InsecureDebugOnlyBuilder()
            .setConnectionType(InsecureDebugOnly.ConnectionType.INSECURE)
            .build();
    }

    private static SecurityChoice getNoTlsSecurityChoice() {
        return new InsecureDebugOnlyBuilder()
            .setConnectionType(InsecureDebugOnly.ConnectionType.PLAINTEXT)
            .build();
    }

    private static SecurityChoice getTlsSecurityChoice(String keystoreId) {
        return new SecureBuilder()
            .setKeystoreId(keystoreId)
            .build();
    }

    private static Node createNode(final String nameOfNode, final int port, final SecurityChoice securityChoice) {
        return createNode(nameOfNode, port, securityChoice, null);
    }

    static Node createNode(final String nameOfNode, final int port, final SecurityChoice securityChoice,
        final Credentials credentials) {
        ConnectionParametersBuilder connectionParametersBuilder = new ConnectionParametersBuilder()
            .setHost(new Host(new IpAddress(new Ipv4Address(ADDRESS))))
            .setPort(new PortNumber(Uint16.valueOf(port)))
            .setSecurityChoice(securityChoice);

        if (credentials != null) {
            connectionParametersBuilder.setCredentials(credentials);
        }

        return new NodeBuilder()
            .setNodeId(new NodeId(nameOfNode))
            .addAugmentation(new GnmiNodeBuilder()
                .setConnectionParameters(connectionParametersBuilder.build())
                .build())
            .build();
    }

    private static Credentials getTestCredentials() {
        return new CredentialsBuilder()
            .setPassword(TEST_USERNAME)
            .setUsername(TEST_PASSWORD)
            .build();
    }

    private static InstanceIdentifier<Keystore> getKeystore1Identifier() {
        return InstanceIdentifier
            .builder(Keystore.class, new KeystoreKey(KEYSTORE_PASSPHRASE_ID_1))
            .build();
    }

    private static InstanceIdentifier<Keystore> getKeystore2Identifier() {
        return InstanceIdentifier
            .builder(Keystore.class, new KeystoreKey(KEYSTORE_ID_2))
            .build();
    }

    private Keystore getKeystore1WithPassResponse() throws GeneralSecurityException {
        // Use java.util.Base64 instead of javax.xml.bind.DatatypeConverter
        return new KeystoreBuilder()
            .setCaCertificate(getResource(CA_CRT))
            .setClientCert(getResource(CLIENT_ENCRYPTED_CRT))
            .setClientKey(Base64.getEncoder().encodeToString(
                (aaaEncryptionService.encrypt(getResource(CLIENT_ENCRYPTED_KEY).getBytes(
                    Charset.defaultCharset())))))
            .setPassphrase(
                Base64.getEncoder().encodeToString(aaaEncryptionService.encrypt(PASSPHRASE.getBytes())))
            .setKeystoreId(KEYSTORE_PASSPHRASE_ID_1)
            .build();
    }

    private Keystore getKeystore2Response() throws GeneralSecurityException {
        // Use java.util.Base64 instead of javax.xml.bind.DatatypeConverter
        return new KeystoreBuilder().setCaCertificate(getResource(CA_CRT)).setClientCert(getResource(CLIENT_CRT))
            .setClientKey(Base64.getEncoder().encodeToString(
                (aaaEncryptionService.encrypt((getResource(CLIENT_KEY).getBytes())))))
            .setKeystoreId(KEYSTORE_ID_2).build();
    }

    private static <T extends DataObject> FluentFuture<Optional<T>> getReadResult(T data) {
        return FluentFuture.from(Futures.immediateFuture(Optional.of(data)));
    }

    private static String getResource(String path) {
        try {
            byte[] bytes = Files.readAllBytes(Paths.get(SessionInitializeTest.class.getResource(path).toURI()));
            return new String(bytes);
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException(String.format("Failed to read resources at path [%s]", path), e);
        }
    }
}