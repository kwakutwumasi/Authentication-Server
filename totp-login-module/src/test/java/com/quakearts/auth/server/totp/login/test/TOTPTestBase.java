package com.quakearts.auth.server.totp.login.test;

import java.io.IOException;
import java.security.Principal;
import java.security.acl.Group;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Map;

import org.junit.BeforeClass;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quakearts.auth.server.totp.services.DirectAuthenticationService;
import com.quakearts.tools.test.mockserver.MockServer;
import com.quakearts.tools.test.mockserver.MockServerFactory;
import com.quakearts.tools.test.mockserver.configuration.Configuration.MockingMode;
import com.quakearts.tools.test.mockserver.configuration.impl.ConfigurationBuilder;
import com.quakearts.tools.test.mockserver.model.impl.HttpMessageBuilder;
import com.quakearts.tools.test.mockserver.model.impl.MockActionBuilder;

public class TOTPTestBase {
	
	private static MockServer mockServer;
	private static TypeReference<Map<String, Object>> typeReference = new TypeReference<Map<String,Object>>() {};
	
	private static ObjectMapper mapper = new ObjectMapper();
	
	@BeforeClass
	public static void startServer() {
		if(mockServer == null){
			mockServer = MockServerFactory
					.getInstance()
					.getMockServer()
					.configure(ConfigurationBuilder
							.newConfiguration().
							setMockingModeAs(MockingMode.MOCK)
							.thenBuild())
					.add(MockActionBuilder.createNewMockAction()
						.setRequestAs(HttpMessageBuilder.createNewHttpRequest()
							.setId("otp-authentication")
							.setMethodAs("POST")
							.setResourceAs("/totp/authenticate")
								.setResponseAs(HttpMessageBuilder.createNewHttpResponse()
									.setResponseCodeAs(204)
									.thenBuild())
								.thenBuild())
						.setResponseActionAs((request, response)->{
							if(request.getContentBytes() == null) {
								return HttpMessageBuilder
										.createNewHttpResponse().setResponseCodeAs(500)
										.thenBuild();
							}
							
							try {
								Map<String, Object> jsonRequest = mapper
										.readValue(request.getContentBytes(), typeReference);
								if(!jsonRequest.containsKey("otp") || 
										!jsonRequest.get("otp").equals("123456")) {
									return HttpMessageBuilder
											.createNewHttpResponse().setResponseCodeAs(500)
											.thenBuild();
								}
								
								switch (jsonRequest.getOrDefault("deviceId","").toString()) {
								case "testdevice-ok":
								case "testdevice-ok-fallback":
								case "testdevice-ok-fallback-2":
									return response;
								case "testdevice-not-found":
									return HttpMessageBuilder
											.createNewHttpResponse().setResponseCodeAs(404)
											.setContentBytes("{\"message\":\"Error-not-found\"}".getBytes())
											.thenBuild();
								case "testdevice-deserialize-error":
									return HttpMessageBuilder
											.createNewHttpResponse().setResponseCodeAs(404)
											.setContentBytes("{\"message\":\"Error-deserialize-error".getBytes())
											.thenBuild();
								default:
									return HttpMessageBuilder
											.createNewHttpResponse().setResponseCodeAs(403)
											.setContentBytes("{\"message\":\"Error-uknown\"}".getBytes())
											.thenBuild();
								}
							} catch (IOException e) {
								return HttpMessageBuilder
										.createNewHttpResponse().setResponseCodeAs(500)
										.thenBuild();
							}
						})
						.thenBuild())
					.add(MockActionBuilder.createNewMockAction()
							.setRequestAs(HttpMessageBuilder.createNewHttpRequest()
									.setId("direct-authentication")
									.setMethodAs("POST")
									.setResourceAs("/totp/authenticate/direct")
										.setResponseAs(HttpMessageBuilder.createNewHttpResponse()
											.setResponseCodeAs(204)
											.thenBuild())
										.thenBuild())
								.setResponseActionAs((request, response)->{
									if(request.getContentBytes() == null) {
										return HttpMessageBuilder
												.createNewHttpResponse().setResponseCodeAs(500)
												.thenBuild();
									}
									
									try {
										Map<String, Object> jsonRequest = mapper
												.readValue(request.getContentBytes(), typeReference);
										String deviceId = jsonRequest.getOrDefault("deviceId","").toString();
										if(!(jsonRequest.get("authenticationData") instanceof Map)) {
											return HttpMessageBuilder
													.createNewHttpResponse().setResponseCodeAs(500)
													.thenBuild();
										}
										
										@SuppressWarnings("unchecked")
										Map<String, String> authenticationData = (Map<String, String>) jsonRequest.get("authenticationData");
										if(!"TestApplication".equals(authenticationData.get("Application Name"))
												&& !"Transaction Processor".equals(authenticationData.get("Application Name"))) {
											return HttpMessageBuilder
													.createNewHttpResponse().setResponseCodeAs(500)
													.thenBuild();
										}
										
										if(!DirectAuthenticationService.getInstance().generateID(deviceId)
												.equals(authenticationData.get("Authentication ID"))){
											return HttpMessageBuilder
													.createNewHttpResponse().setResponseCodeAs(500)
													.thenBuild();
										}
										
										switch (deviceId) {
										case "testdevice-ok":
											return response;
										case "testdevice-ok-fallback":
										case "testdevice-ok-fallback-2":
										case "testdevice-not-found":
											return HttpMessageBuilder
													.createNewHttpResponse().setResponseCodeAs(404)
													.setContentBytes("{\"message\":\"Error-not-found\"}".getBytes())
													.thenBuild();
										case "testdevice-deserialize-error":
											return HttpMessageBuilder
													.createNewHttpResponse().setResponseCodeAs(404)
													.setContentBytes("{\"message\":\"Error-deserialize-error".getBytes())
													.thenBuild();
										default:
											return HttpMessageBuilder
													.createNewHttpResponse().setResponseCodeAs(403)
													.setContentBytes("{\"message\":\"Error-uknown\"}".getBytes())
													.thenBuild();
										}
									} catch (IOException e) {
										return HttpMessageBuilder
												.createNewHttpResponse().setResponseCodeAs(500)
												.thenBuild();
									}
								})
						.thenBuild());
			
			mockServer.start();
			Runtime.getRuntime().addShutdownHook(new Thread(mockServer::stop));
		}
	}

	protected Group getOtherGroup() {
		Group otherGroup = new Group() {
			@Override
			public String getName() {return "Other";}
			
			@Override
			public boolean removeMember(Principal user) {
				return false;
			}
			
			@Override
			public Enumeration<? extends Principal> members() {
				return Collections.enumeration(Collections.emptyList());
			}
			
			@Override
			public boolean isMember(Principal member) {
				return false;
			}
			
			@Override
			public boolean addMember(Principal user) {
				return false;
			}
		};
		return otherGroup;
	}
		
}
