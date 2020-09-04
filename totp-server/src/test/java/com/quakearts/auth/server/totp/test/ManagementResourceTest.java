package com.quakearts.auth.server.totp.test;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.core.Is.*;
import static org.junit.Assert.*;

import java.lang.reflect.Field;
import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.security.auth.Subject;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.TimeoutHandler;

import org.awaitility.Duration;
import org.hamcrest.core.IsNull;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.quakearts.auth.server.totp.alternatives.AlternativeConnectionManager;
import com.quakearts.auth.server.totp.alternatives.AlternativeDeviceManagementService;
import com.quakearts.auth.server.totp.generator.impl.JWTGeneratorImpl;
import com.quakearts.auth.server.totp.model.Device;
import com.quakearts.auth.server.totp.model.Device.Status;
import com.quakearts.auth.server.totp.rest.ManagementResource;
import com.quakearts.auth.server.totp.rest.model.ConnectedResponse;
import com.quakearts.tools.test.mocking.MockedImplementation;
import com.quakearts.tools.test.mocking.proxy.MockingProxyBuilder;
import com.quakearts.webapp.security.auth.OtherPrincipal;
import com.quakearts.webapp.security.jwt.JWTClaims;
import com.quakearts.webapp.security.jwt.exception.JWTException;
import com.quakearts.webapp.security.rest.SecurityContext;
import com.quakearts.webtools.test.AllServicesRunner;

@RunWith(AllServicesRunner.class)
public class ManagementResourceTest {

	@Inject
	private JWTGeneratorImpl jwtGenerator;

	@Inject
	private ManagementResource managementResource;
	
	@Test
	public void testCheckConnectionOK() throws Exception {
		SecurityContext context = SecurityContext.getCurrentSecurityContext();
		
		Subject subject = new Subject();
		subject.getPrincipals().add(new OtherPrincipal("Administrator"));
		
		Field subjectField = SecurityContext.class.getDeclaredField("subject");
		subjectField.setAccessible(true);
		subjectField.set(context, subject);
		try {
			Device device = new Device();
			device.setStatus(Status.ACTIVE);
			device.setId("testConnectedDevice1");
			AlternativeDeviceManagementService.returnDevice(id-> {
				assertThat(id, is("testConnectedDevice1"));
				return Optional.of(device);
			});
	
			AlternativeConnectionManager.run(bite->{
				 try {
					JWTClaims claims = jwtGenerator.verifyJWT(bite);
					assertThat(claims.getPrivateClaim("ping"), is("ping"));
				} catch (Exception e1) {
					fail("Exception thrown: "+e1.getMessage());
				}
	
				Map<String, String> responseMap = new HashMap<>();
				responseMap.put("connected", "true");
				try {
					return jwtGenerator.generateJWT(responseMap).getBytes();
				} catch (NoSuchAlgorithmException | URISyntaxException | JWTException e) {
					throw new AssertionError(e);
				}
			});
	
			class ResponseHolder {
				Object value = new Object();
			}
			
			ResponseHolder response = new ResponseHolder();
		
			managementResource.checkConnection("testConnectedDevice1", mockAsyncResponse(arguments->{
				response.value = arguments.get(0); 
				return true;
			}));
			
			await().atMost(Duration.ONE_SECOND).until(()->{
				return response.value != null
						&& ((ConnectedResponse) response.value).isConnected();
			});
		
			assertThat(time, is(1000l));
			assertThat(timeUnit, is(TimeUnit.MILLISECONDS));
			assertThat(handler, is(IsNull.notNullValue()));
		} finally {
			subjectField.set(context, null);
		}
	}

	private TimeoutHandler handler;
	private long time;
	private TimeUnit timeUnit;
	
	public AsyncResponse mockAsyncResponse(MockedImplementation implementation) {
		return MockingProxyBuilder
				.createMockingInvocationHandlerFor(AsyncResponse.class)
				.mock("resume").with(implementation)
				.mock("setTimeout")
				.with(arguments->{
					time = arguments.get(0);
					timeUnit = arguments.get(1);
					return true;
				})
				.mock("setTimeoutHandler")
				.withVoidMethod(arguments->{
					handler = arguments.get(0);
				})
				.thenBuild();
	}

}
