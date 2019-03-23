package com.quakearts.auth.server.totp.rest;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.security.URIParameter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.Configuration;
import javax.security.auth.login.LoginException;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.quakearts.auth.server.totp.authentication.AuthenticationService;
import com.quakearts.auth.server.totp.device.DeviceService;
import com.quakearts.auth.server.totp.model.Administrator;
import com.quakearts.auth.server.totp.options.TOTPOptions;
import com.quakearts.auth.server.totp.rest.model.AuthorizationRequest;
import com.quakearts.auth.server.totp.rest.model.ErrorResponse;
import com.quakearts.auth.server.totp.rest.model.TokenResponse;
import com.quakearts.webapp.security.auth.JWTLoginModule;
import com.quakearts.webapp.security.auth.UserPrincipal;

@Path("login")
@Singleton
public class LoginResource {
	
	private static final CallbackHandler DEFUALTCALLBACKHANDLER = callbacks->{};

	@Inject
	private DeviceService deviceService;
	
	@Inject
	private AuthenticationService authenticationService;

	private Configuration jaasConfig;
	private Map<String, ?> options;
	
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public TokenResponse login(AuthorizationRequest request) 
			throws URISyntaxException, NoSuchAlgorithmException, LoginException {
		if(request.getDeviceId()==null){
			throw new WebApplicationException(Response.status(403)
					.entity(new ErrorResponse().withMessageAs("AuthorizationRequest is required")).build());
		}
		
		Optional<Administrator> optionalAdministrator = deviceService.findAdministrator(request.getDeviceId());
		if(!optionalAdministrator.isPresent()){
			throw new WebApplicationException(Response.status(403)
					.entity(new ErrorResponse().withMessageAs("Device is not an administrator")).build());
		}
		
		if(!authenticationService.authenticate(optionalAdministrator.get().getDevice(), 
				request.getOtp())){
			throw new WebApplicationException(Response.status(403)
					.entity(new ErrorResponse().withMessageAs("Authentication failed")).build());			
		}
		
		if(jaasConfig == null){
				URL resource = Thread.currentThread().getContextClassLoader().
                        getResource("login.config");
                URI uri = resource.toURI();
                jaasConfig = Configuration.getInstance("JavaLoginConfig", 
                		new URIParameter(uri));
		}
		
		if(options==null){
			AppConfigurationEntry entry = jaasConfig.getAppConfigurationEntry(TOTPOptions.GlobalDefaults.LOGIN_MODULE)[0];
			options = entry.getOptions();
		}
		
		Subject subject = new Subject();

		UserPrincipal principal = new UserPrincipal(request.getDeviceId());
		Map<String, Object> sharedState = new HashMap<>();
		sharedState.put("javax.security.auth.login.name", principal);
		sharedState.put("com.quakearts.LoginOk", Boolean.TRUE);
		
		JWTLoginModule jwtLoginModule = new JWTLoginModule();
		jwtLoginModule.initialize(subject, DEFUALTCALLBACKHANDLER, sharedState, options);
		jwtLoginModule.login();
		return new TokenResponse().withTokenAs(jwtLoginModule.generateJWTToken(Collections.emptyList()));
	}
}
