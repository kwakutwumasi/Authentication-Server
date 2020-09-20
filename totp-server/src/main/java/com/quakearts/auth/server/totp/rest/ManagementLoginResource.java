package com.quakearts.auth.server.totp.rest;

import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.security.auth.login.LoginException;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.quakearts.auth.server.totp.authentication.AuthenticationService;
import com.quakearts.auth.server.totp.device.DeviceManagementService;
import com.quakearts.auth.server.totp.generator.JWTGenerator;
import com.quakearts.auth.server.totp.model.Administrator;
import com.quakearts.auth.server.totp.rest.model.AuthenticationRequest;
import com.quakearts.auth.server.totp.rest.model.ErrorResponse;
import com.quakearts.auth.server.totp.rest.model.TokenResponse;

@Path("management-login")
@Singleton
public class ManagementLoginResource {
	
	@Inject
	private DeviceManagementService deviceManagementService;
	
	@Inject
	private AuthenticationService authenticationService;

	@Inject 
	private JWTGenerator jwtGenerator;
	
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public TokenResponse login(AuthenticationRequest request) 
			throws URISyntaxException, NoSuchAlgorithmException, LoginException {
		if(request.getDeviceId()==null){
			throw new WebApplicationException(Response.status(403)
					.entity(new ErrorResponse().withMessageAs("AuthenticationRequest is required")).build());
		}
		
		Optional<Administrator> optionalAdministrator = deviceManagementService.findAdministrator(request.getDeviceId());
		if(!optionalAdministrator.isPresent()){
			throw new WebApplicationException(Response.status(403)
					.entity(new ErrorResponse().withMessageAs("Device is not an administrator")).build());
		}
		
		if(!authenticationService.authenticate(optionalAdministrator.get().getDevice(), 
				request.getOtp())){
			throw new WebApplicationException(Response.status(403)
					.entity(new ErrorResponse().withMessageAs("Authentication failed")).build());			
		}
		return new TokenResponse().withTokenAs(jwtGenerator.login(optionalAdministrator.get()));
	}
}
