package io.lighty.core.controller.springboot.restconf;

import org.opendaylight.mdsal.dom.api.DOMDataBroker;
import org.opendaylight.mdsal.dom.api.DOMMountPointService;
import org.opendaylight.mdsal.dom.api.DOMNotificationService;
import org.opendaylight.mdsal.dom.api.DOMRpcService;
import org.opendaylight.mdsal.dom.api.DOMSchemaService;
import org.opendaylight.mdsal.dom.api.DOMYangTextSourceProvider;
import org.opendaylight.restconf.nb.rfc8040.RestconfApplication;
import org.opendaylight.restconf.nb.rfc8040.handlers.DOMDataBrokerHandler;
import org.opendaylight.restconf.nb.rfc8040.handlers.DOMMountPointServiceHandler;
import org.opendaylight.restconf.nb.rfc8040.handlers.NotificationServiceHandler;
import org.opendaylight.restconf.nb.rfc8040.handlers.RpcServiceHandler;
import org.opendaylight.restconf.nb.rfc8040.handlers.SchemaContextHandler;
import org.opendaylight.restconf.nb.rfc8040.handlers.TransactionChainHandler;
import org.opendaylight.restconf.nb.rfc8040.jersey.providers.JsonNormalizedNodeBodyReader;
import org.opendaylight.restconf.nb.rfc8040.jersey.providers.NormalizedNodeJsonBodyWriter;
import org.opendaylight.restconf.nb.rfc8040.jersey.providers.NormalizedNodeXmlBodyWriter;
import org.opendaylight.restconf.nb.rfc8040.jersey.providers.XmlNormalizedNodeBodyReader;
import org.opendaylight.restconf.nb.rfc8040.jersey.providers.patch.JsonToPatchBodyReader;
import org.opendaylight.restconf.nb.rfc8040.jersey.providers.patch.PatchJsonBodyWriter;
import org.opendaylight.restconf.nb.rfc8040.jersey.providers.patch.PatchXmlBodyWriter;
import org.opendaylight.restconf.nb.rfc8040.jersey.providers.patch.XmlToPatchBodyReader;
import org.opendaylight.restconf.nb.rfc8040.jersey.providers.schema.SchemaExportContentYangBodyWriter;
import org.opendaylight.restconf.nb.rfc8040.jersey.providers.schema.SchemaExportContentYinBodyWriter;
import org.opendaylight.restconf.nb.rfc8040.rests.services.api.JSONRestconfService;
import org.opendaylight.restconf.nb.rfc8040.rests.services.api.RestconfDataService;
import org.opendaylight.restconf.nb.rfc8040.rests.services.api.RestconfInvokeOperationsService;
import org.opendaylight.restconf.nb.rfc8040.rests.services.api.RestconfStreamsSubscriptionService;
import org.opendaylight.restconf.nb.rfc8040.rests.services.impl.JSONRestconfServiceRfc8040Impl;
import org.opendaylight.restconf.nb.rfc8040.rests.services.impl.RestconfDataServiceImpl;
import org.opendaylight.restconf.nb.rfc8040.rests.services.impl.RestconfInvokeOperationsServiceImpl;
import org.opendaylight.restconf.nb.rfc8040.rests.services.impl.RestconfStreamsSubscriptionServiceImpl;
import org.opendaylight.restconf.nb.rfc8040.services.simple.api.RestconfOperationsService;
import org.opendaylight.restconf.nb.rfc8040.services.simple.api.RestconfSchemaService;
import org.opendaylight.restconf.nb.rfc8040.services.simple.api.RestconfService;
import org.opendaylight.restconf.nb.rfc8040.services.simple.impl.RestconfImpl;
import org.opendaylight.restconf.nb.rfc8040.services.simple.impl.RestconfOperationsServiceImpl;
import org.opendaylight.restconf.nb.rfc8040.services.simple.impl.RestconfSchemaServiceImpl;
import org.opendaylight.restconf.nb.rfc8040.services.wrapper.ServicesWrapper;
import org.opendaylight.yangtools.yang.model.api.SchemaContextProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.web.embedded.jetty.JettyServletWebServerFactory;
import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RestconfConfiguration {
	
	@Autowired
	@Qualifier("getDOMYangTextSourceProvider")
	private DOMYangTextSourceProvider domYangTextSourceProvider;

	@Autowired
	private SchemaContextProvider schemaContextProvider;
	
	@Bean
	public RpcServiceHandler rpcServiceHandler(DOMRpcService rpcService)
	{
		return new RpcServiceHandler(rpcService);
	}

	@Bean
	public TransactionChainHandler transactionChainHandler(@Qualifier("ClusteredDOMDataBroker") DOMDataBroker domDataBroker)
	{
		return new TransactionChainHandler(domDataBroker);
	}
	
	@Bean
	public SchemaContextHandler schemaContextHandler(TransactionChainHandler transactionChainHandler, DOMSchemaService domSchemaService)
	{
		SchemaContextHandler schemaContextHandler = SchemaContextHandler.newInstance(transactionChainHandler, domSchemaService);
		schemaContextHandler.onGlobalContextUpdated(schemaContextProvider.getSchemaContext());
		return schemaContextHandler;
	}

	@Bean
	public DOMMountPointServiceHandler domMountPointServiceHandler(DOMMountPointService domMountPointService)
	{
		return DOMMountPointServiceHandler.newInstance(domMountPointService);
	}

	@Bean
	public DOMDataBrokerHandler domDataBrokerHandler(@Qualifier("ClusteredDOMDataBroker") DOMDataBroker domDataBroker)
	{
		return new DOMDataBrokerHandler(domDataBroker);
	}
	
	@Bean
	public NotificationServiceHandler notificationServiceHandler(@Qualifier("getDOMNotificationService") DOMNotificationService domNotificationService)
	{
		return new NotificationServiceHandler(domNotificationService);
	}
	
	@Bean
	public RestconfStreamsSubscriptionService restconfStreamsSubscriptionService(DOMDataBrokerHandler domDataBrokerHandler, NotificationServiceHandler notificationServiceHandler, 
											SchemaContextHandler schemaContextHandler, TransactionChainHandler transactionChainHandler)
	{
		return new RestconfStreamsSubscriptionServiceImpl(domDataBrokerHandler, notificationServiceHandler, schemaContextHandler, transactionChainHandler);
	}

	@Bean
	public RestconfDataService restconfDataService(SchemaContextHandler schemaContextHandler, TransactionChainHandler transactionChainHandler, 
							DOMMountPointServiceHandler domMountPointServiceHandler,
							RestconfStreamsSubscriptionService restconfStreamsSubscriptionService)
	{
		return new RestconfDataServiceImpl(schemaContextHandler, transactionChainHandler, domMountPointServiceHandler, restconfStreamsSubscriptionService);
	}

	@Bean
	public RestconfInvokeOperationsService restconfInvokeOperationsService(RpcServiceHandler rpcServiceHandler, SchemaContextHandler schemaContextHandler) {
		return new RestconfInvokeOperationsServiceImpl(rpcServiceHandler, schemaContextHandler);
	}

	@Bean
	public RestconfOperationsService restconfOperationsService(SchemaContextHandler schemaContextHandler, DOMMountPointServiceHandler domMountPointServiceHandler) {
		return new RestconfOperationsServiceImpl(schemaContextHandler, domMountPointServiceHandler);
	}
	
	@Bean
	public RestconfSchemaService restconfSchemaService(SchemaContextHandler schemaContextHandler, DOMMountPointServiceHandler domMountPointServiceHandler) {
		return new RestconfSchemaServiceImpl(schemaContextHandler, domMountPointServiceHandler, domYangTextSourceProvider);
	}

	@Bean
	public RestconfService restconfService(SchemaContextHandler schemaContextHandler) {
        	return new RestconfImpl(schemaContextHandler);
	}


	@Bean
	public ServicesWrapper servicesWrapper(SchemaContextHandler schemaContextHandler, DOMMountPointServiceHandler domMountPointServiceHandler, TransactionChainHandler transactionChainHandler,
											DOMDataBrokerHandler domDataBrokerHandler, RpcServiceHandler rpcServiceHandler, NotificationServiceHandler notificationServiceHandler, 
											DOMSchemaService domSchemaService)
	{
		return ServicesWrapper.newInstance(schemaContextHandler, domMountPointServiceHandler, transactionChainHandler, domDataBrokerHandler, rpcServiceHandler, notificationServiceHandler, domSchemaService);
	}
	
	@Bean
	public RestconfApplication restconfApplication(SchemaContextHandler schemaContextHandler, DOMMountPointServiceHandler mountPointServiceHandler, ServicesWrapper servicesWrapper)
	{
		return new RestconfApplication(schemaContextHandler, mountPointServiceHandler, servicesWrapper);
	}
	
	@Bean
	public JSONRestconfService jsonRestconfService(ServicesWrapper servicesWrapper, DOMMountPointServiceHandler domMountPointServiceHandler, SchemaContextHandler schemaContextHandler)
	{
		return new JSONRestconfServiceRfc8040Impl(servicesWrapper, domMountPointServiceHandler, schemaContextHandler);
	}
	

	@Bean
	public JsonNormalizedNodeBodyReader jsonNormalizedNodeBodyReader(SchemaContextHandler schemaContextHandler, DOMMountPointServiceHandler domMountPointServiceHandler)
	{
		return new JsonNormalizedNodeBodyReader(schemaContextHandler, domMountPointServiceHandler);
	}

	@Bean
	public JsonToPatchBodyReader jsonToPatchBodyReader(SchemaContextHandler schemaContextHandler, DOMMountPointServiceHandler domMountPointServiceHandler)
	{
        	return new JsonToPatchBodyReader(schemaContextHandler, domMountPointServiceHandler);
	}

	@Bean
	public XmlNormalizedNodeBodyReader xmlNormalizedNodeBodyReader(SchemaContextHandler schemaContextHandler, DOMMountPointServiceHandler domMountPointServiceHandler)
	{
        	return new XmlNormalizedNodeBodyReader(schemaContextHandler, domMountPointServiceHandler);
	}

	@Bean
	public XmlToPatchBodyReader xmlToPatchBodyReader(SchemaContextHandler schemaContextHandler, DOMMountPointServiceHandler domMountPointServiceHandler)
	{
		return new XmlToPatchBodyReader(schemaContextHandler, domMountPointServiceHandler);
	}


	@Bean
	public NormalizedNodeJsonBodyWriter normalizedNodeJsonBodyWriter()
	{
		return new NormalizedNodeJsonBodyWriter();
	}

	@Bean
	public NormalizedNodeXmlBodyWriter normalizedNodeXmlBodyWriter()
	{
		return new NormalizedNodeXmlBodyWriter();
	}

	@Bean
        public SchemaExportContentYinBodyWriter schemaExportContentYinBodyWriter()
	{
		return new SchemaExportContentYinBodyWriter();
	}

	@Bean
	public SchemaExportContentYangBodyWriter schemaExportContentYangBodyWriter()
	{
		return new SchemaExportContentYangBodyWriter();
	}

	@Bean
	public PatchJsonBodyWriter patchJsonBodyWriter()
	{
		return new PatchJsonBodyWriter();
	}

	@Bean
	public PatchXmlBodyWriter patchXmlBodyWriter()
	{
		return new PatchXmlBodyWriter();
	}

	@Bean
	public ConfigurableServletWebServerFactory restConfJettyConfig(RestConfJettyServerCustomizer customizer)
	{
		JettyServletWebServerFactory factory = new JettyServletWebServerFactory();
		factory.addServerCustomizers(customizer);
		return factory;
	}

}
