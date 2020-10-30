package com.quakearts.auth.server.totp.edge.test;

import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.util.Map;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.BeforeClass;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quakearts.auth.server.totp.edge.exception.ConnectorException;
import com.quakearts.tools.test.mockserver.MockServer;
import com.quakearts.tools.test.mockserver.MockServerFactory;
import com.quakearts.tools.test.mockserver.configuration.Configuration.MockingMode;
import com.quakearts.tools.test.mockserver.configuration.impl.ConfigurationBuilder;
import com.quakearts.tools.test.mockserver.exception.ConfigurationException;
import com.quakearts.tools.test.mockserver.fi.HttpRequestMatcher;
import com.quakearts.tools.test.mockserver.model.impl.HttpMessageBuilder;
import com.quakearts.tools.test.mockserver.model.impl.MockActionBuilder;
import com.quakearts.tools.test.mockserver.store.HttpMessageStore;
import com.quakearts.tools.test.mockserver.store.exception.HttpMessageStoreException;
import com.quakearts.tools.test.mockserver.store.impl.MockServletHttpMessageStore;
import static org.hamcrest.core.Is.is;

public abstract class TestServerTest {

	private static MockServer mockServer;
	private static ObjectMapper objectMapper = new ObjectMapper();
	private static TypeReference<Map<String, String>> typeReference = new TypeReference<Map<String,String>>() {};

	@BeforeClass
	public static void createServer() throws ConfigurationException, HttpMessageStoreException {
		if(mockServer == null) {
			HttpMessageStore messageStore = MockServletHttpMessageStore.getInstance();
			
			HttpRequestMatcher fullmatcher = (request, incoming)->{
				boolean matches = request.getMethod().equals(incoming.getMethod())
						&& request.getResource().equals(incoming.getResource())
						&& incoming.getContentBytes()!=null;
				if(matches) {
					try {
						Map<String, String> incomingJsonMap = objectMapper.readValue(incoming.getContentBytes(), typeReference);
						Map<String, String> jsonMap = objectMapper.readValue(request.getContentBytes(), typeReference);
						matches = jsonMap.equals(incomingJsonMap);
					} catch (IOException e) {
						matches = false;
					}
				}
				
				return matches;
			};
			
			mockServer = MockServerFactory
				.getInstance().getMockServer()
				.configure(ConfigurationBuilder.newConfiguration()
						.setMockingModeAs(MockingMode.MOCK)
						.setPortAs(8081).thenBuild())
				.add(MockActionBuilder.createNewMockAction()
						.setRequestAs(messageStore.findRequestIdentifiedBy("GET-http---localhost-8080-totp-sync"))
						.thenBuild())
				.add(MockActionBuilder.createNewMockAction()
						.setRequestAs(messageStore.findRequestIdentifiedBy("POST-http---localhost-8080-totp-authenticate"))
						.setMatcherAs(fullmatcher)
						.thenBuild())
				.add(MockActionBuilder.createNewMockAction()
						.setRequestAs(messageStore.findRequestIdentifiedBy("POST-http---localhost-8080-totp-provisioning-NBV7-ST2R-3U47-6HFE-CSAQ-K9XC-NCJC-QZ4B"))
						.thenBuild())
				.add(MockActionBuilder.createNewMockAction()
						.setRequestAs(messageStore.findRequestIdentifiedBy("PUT-http---localhost-8080-totp-provisioning-NBV7-ST2R-3U47-6HFE-CSAQ-K9XC-NCJC-QZ4B"))
						.setMatcherAs(fullmatcher)
						.thenBuild())
				.add(MockActionBuilder.createNewMockAction()
						.setRequestAs(HttpMessageBuilder.createNewHttpRequest()
								.setId("no-device-id-test")
								.setMethodAs("POST")
								.setResourceAs("/totp/provisioning/")
								.setResponseAs(HttpMessageBuilder.createNewHttpResponse()
										.setContentBytes("{\"message\":\"Error message test\"}".getBytes())
										.setResponseCodeAs(404)
										.thenBuild())
							.thenBuild())
					.thenBuild())
				.add(MockActionBuilder.createNewMockAction()
						.setRequestAs(HttpMessageBuilder.createNewHttpRequest()
								.setId("activate-with-extra-data")
								.setMethodAs("PUT")
								.setResourceAs("/totp/provisioning/FAAF-16C0-1ABD-11EB-ADC1-0242-AC12-0002")
								.setResponseAs(HttpMessageBuilder.createNewHttpResponse()
										.setResponseCodeAs(204)
										.thenBuild())
							.thenBuild())
						.setMatcherAs((request, incoming)->{
							boolean matches = request.getMethod().equals(incoming.getMethod())
									&& request.getResource().equals(incoming.getResource())
									&& incoming.getContentBytes()!=null;
							if(matches) {
								Map<String, String> incomingJsonMap;
								try {
									incomingJsonMap = objectMapper.readValue(incoming.getContentBytes(), typeReference);
								} catch (IOException e) {
									return false;
								}
								assertThat(incomingJsonMap.get("other"), is("attribute"));
							}
							return matches;
						})
					.thenBuild());
			mockServer.start();
			Runtime.getRuntime().addShutdownHook(new Thread(()->mockServer.stop()));
		}
	}

	public TestServerTest() {
		super();
	}

	protected Matcher<?> messageIs(String string) {
		return new BaseMatcher<Exception>() {
	
			@Override
			public boolean matches(Object item) {
				ConnectorException e = (ConnectorException) item;
				return string.equals(e.getResponse().getMessage());
			}
	
			@Override
			public void describeTo(Description description) {
				description.appendText("Expected message "+string);
			}
			
		};
	}

}