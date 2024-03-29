package com.quakearts.auth.server.totp.rest;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.quakearts.auth.server.totp.authentication.AuthenticationService;
import com.quakearts.auth.server.totp.device.DeviceConnectionExecutorService;
import com.quakearts.auth.server.totp.device.DeviceManagementService;
import com.quakearts.auth.server.totp.exception.AuthenticationException;
import com.quakearts.auth.server.totp.exception.TOTPException;
import com.quakearts.auth.server.totp.exception.UnconnectedDeviceException;
import com.quakearts.auth.server.totp.model.Device;
import com.quakearts.auth.server.totp.model.Device.Status;
import com.quakearts.auth.server.totp.options.TOTPOptions;
import com.quakearts.auth.server.totp.rest.authorization.DeviceAuthorizationService;
import com.quakearts.auth.server.totp.rest.model.AuthenticationRequest;
import com.quakearts.auth.server.totp.rest.model.DirectAuthenticationRequest;
import com.quakearts.auth.server.totp.rest.model.ErrorResponse;

@Path("authenticate")
@Singleton
public class AuthenticationResource {
	
	private static final Logger log = LoggerFactory.getLogger(AuthenticationResource.class);
	
	@Inject
	private AuthenticationService authenticationService;
	
	@Inject
	private DeviceManagementService deviceManagementService;
	
	@Inject
	private DeviceAuthorizationService deviceAuthorizationService;
	
	@Inject
	private TOTPOptions totpOptions;
	
	@Inject
	private DeviceConnectionExecutorService deviceConnectionExecutorService;
	
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	public void authenticate(AuthenticationRequest request) throws AuthenticationException {
		if(request.getDeviceId()==null){
			throw new AuthenticationException("deviceId is required");
		}

		String deviceId = request.getDeviceId();
		
		Optional<Device> optionalDevice = deviceManagementService.findDevice(deviceId);
		if(optionalDevice.isPresent() && optionalDevice.get().getStatus() == Status.ACTIVE){
			Device device = optionalDevice.get();
			authenticate(device, request.getOtp());
		} else {
			throw new AuthenticationException("Device with ID "+deviceId+" not found");
		}
	}

	private void authenticate(Device device, String otp) throws AuthenticationException {
		try {
			if(!authenticationService.authenticate(device, otp)){
				throw new AuthenticationException("OTP did not match");
			} 
		} finally {
			if(authenticationService.isLocked(device)){
				deviceManagementService.lock(device);
			}
		}
	}
	
	@POST
	@Path("direct")
	@Consumes(MediaType.APPLICATION_JSON)
	public void authenticateDirect(DirectAuthenticationRequest request, @Suspended AsyncResponse asyncResponse) 
			throws AuthenticationException {
		if(request.getDeviceId()==null){
			throw new AuthenticationException("deviceId is required");
		}
		
		String deviceId = request.getDeviceId();
		Optional<Device> optionalDevice = deviceManagementService.findDevice(deviceId);
		if(optionalDevice.isPresent() && optionalDevice.get().getStatus() == Status.ACTIVE){
			CompletableFuture.runAsync(processRequest(request, asyncResponse, optionalDevice), 
					deviceConnectionExecutorService.getExecutorService());
			asyncResponse.setTimeout(totpOptions.getDeviceConnectionRequestTimeout(), TimeUnit.MILLISECONDS);
			asyncResponse.setTimeoutHandler(this::handleTimeout);
		} else {
			throw new WebApplicationException(Response.status(404)
					.entity(new ErrorResponse().withMessageAs("Device with ID "+deviceId+" not found"))
					.type(MediaType.APPLICATION_JSON_TYPE)
					.build());
		}
	}

	private Runnable processRequest(DirectAuthenticationRequest request, AsyncResponse asyncResponse,
			Optional<Device> optionalDevice) {
		return ()->{
			Device device = optionalDevice.get();
			if(log.isDebugEnabled())
				log.debug("Sending Authentication Request for device with itemCount: {}", 
						device.getItemCount());
			try {
				deviceAuthorizationService.requestOTPCode(device, request.getAuthenticationData(), otp->{
					try {
						authenticate(device, otp);
						asyncResponse.resume(Response.noContent().build());
					} catch (AuthenticationException e) {
						asyncResponse.resume(e);
					}
				}, error->asyncResponse.resume("Request rejected".equals(error)?
						new AuthenticationException(error):
							new UnconnectedDeviceException(error)));
			} catch (TOTPException e) {
				asyncResponse.resume(e);
			}
		};
	}
	
	private void handleTimeout(AsyncResponse asyncResponse) {
		asyncResponse.resume(new UnconnectedDeviceException("Connection timed out"));
	}

}
