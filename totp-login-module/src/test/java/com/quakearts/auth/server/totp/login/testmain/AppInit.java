package com.quakearts.auth.server.totp.login.testmain;

import java.util.Map;
import java.util.Random;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quakearts.appbase.Main;
import com.quakearts.tools.test.mockserver.MockServer;
import com.quakearts.tools.test.mockserver.MockServerFactory;
import com.quakearts.tools.test.mockserver.configuration.Configuration.MockingMode;
import com.quakearts.tools.test.mockserver.configuration.impl.ConfigurationBuilder;
import com.quakearts.tools.test.mockserver.model.impl.HttpMessageBuilder;
import com.quakearts.tools.test.mockserver.model.impl.MockActionBuilder;

public class AppInit {
	public void init(){
		TypeReference<Map<String, Object>> typeReference = new TypeReference<Map<String,Object>>() {};
		ObjectMapper mapper = new ObjectMapper();

		Random random = new Random();
		
		MockServer mockServer = MockServerFactory
				.getInstance()
				.getMockServer()
				.configure(ConfigurationBuilder
						.newConfiguration().
						setMockingModeAs(MockingMode.MOCK)
						.setPortAs(8081)
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
						try {
							Map<String, Object> jsonRequest = mapper
									.readValue(request.getContentBytes(), typeReference);
							if(!jsonRequest.containsKey("otp") || 
									!jsonRequest.get("otp").equals("123456")) {
								return HttpMessageBuilder
										.createNewHttpResponse().setResponseCodeAs(403)
										.thenBuild();
							}
							
							return response;
						} catch (Exception e) {
							Main.log.error("Processing error", e);
							return HttpMessageBuilder
									.createNewHttpResponse().setResponseCodeAs(500)
									.thenBuild();
						}
					}).thenBuild())
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
								try {
									Map<String, Object> jsonRequest = mapper
											.readValue(request.getContentBytes(), typeReference);
									String deviceId = jsonRequest.get("deviceId").toString();
								
									if (deviceId.endsWith("-fall")){
										randomSleep(random);
										return HttpMessageBuilder
												.createNewHttpResponse().setResponseCodeAs(404)
												.setContentBytes("{\"message\":\"Error-not-found\"}".getBytes())
												.thenBuild();
									} else if (deviceId.endsWith("-fail")){
										return HttpMessageBuilder
												.createNewHttpResponse().setResponseCodeAs(403)
												.setContentBytes("{\"message\":\"Error-fail\"}".getBytes())
												.thenBuild();
									} else {
										randomSleep(random);
										return response;
									}
								} catch (Exception e) {
									Main.log.error("Processing error", e);
									return HttpMessageBuilder
											.createNewHttpResponse().setResponseCodeAs(500)
											.thenBuild();
								}
							})
					.thenBuild());
		
		mockServer.start();
		Runtime.getRuntime().addShutdownHook(new Thread(mockServer::stop));
	}

	private void randomSleep(Random random) {
		try {
			Thread.sleep((1+(Math.abs(random.nextInt())%10))*1000l);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}
}
