package com.quakearts.auth.server.totp.rest;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.quakearts.auth.server.totp.device.DeviceConnectionExecutorService;
import com.quakearts.auth.server.totp.device.DeviceManagementService;
import com.quakearts.auth.server.totp.exception.InvalidSignatureException;
import com.quakearts.auth.server.totp.exception.TOTPException;
import com.quakearts.auth.server.totp.exception.UnconnectedDeviceException;
import com.quakearts.auth.server.totp.model.Device;
import com.quakearts.auth.server.totp.model.Device.Status;
import com.quakearts.auth.server.totp.options.TOTPOptions;
import com.quakearts.auth.server.totp.rest.model.ErrorResponse;
import com.quakearts.auth.server.totp.rest.model.TokenResponse;
import com.quakearts.auth.server.totp.signing.DeviceRequestSigningService;

@Path("request")
@Singleton
public class RequestSigningResource {

	@Inject
	private DeviceManagementService deviceManagementService;

	@Inject
	private DeviceRequestSigningService deviceRequestSigningService;
	
	@Inject
	private DeviceConnectionExecutorService deviceConnectionExecutorService;
	
	@Inject
	private TOTPOptions totpOptions;

	@POST
	@Path("sign/device/{deviceId}")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public void signRequest(@PathParam("deviceId") String deviceId,
			@Suspended AsyncResponse asyncResponse,
			Map<String, String> requestMap) {
		if(requestMap == null || requestMap.size() == 0) {
			throw new WebApplicationException(Response.status(403)
					.entity(new ErrorResponse().withMessageAs("Request map is required"))
					.build()); 
		}
		
		Optional<Device> optionalDevice = deviceManagementService.findDevice(deviceId);
		if(optionalDevice.isPresent() && optionalDevice.get().getStatus() == Status.ACTIVE){
			CompletableFuture.runAsync(()->{
				Device device = optionalDevice.get();
				try {
					deviceRequestSigningService.signRequest(device, requestMap,
						signedMessage->asyncResponse.resume(new TokenResponse().withTokenAs(signedMessage)), 
						error->asyncResponse.resume(new WebApplicationException(Response.status(417)
								.entity(new ErrorResponse().withMessageAs(error)).build())));
				} catch (TOTPException e) {
					asyncResponse.resume(e);
				}
			}, deviceConnectionExecutorService.getExecutorService());
			asyncResponse.setTimeout(totpOptions.getDeviceAuthenticationTimeout(), TimeUnit.MILLISECONDS);
			asyncResponse.setTimeoutHandler(this::handleTimeout);			
		} else {
			throw notFound(deviceId);
		}
	}

	private void handleTimeout(AsyncResponse asyncResponse) {
		asyncResponse.resume(new UnconnectedDeviceException("Connection timed out"));
	}
	
	@GET
	@Path("verify/signature/{signature}/device/{deviceId}")
	public void verifySignedRequest(@PathParam("deviceId")String deviceId, 
			@PathParam("signature")String signature) throws InvalidSignatureException {
		Optional<Device> optionalDevice = deviceManagementService.findDevice(deviceId);
		if(optionalDevice.isPresent()){
			deviceRequestSigningService.verifySignedRequest(optionalDevice.get(), signature);
		} else {
			throw notFound(deviceId);
		}
	}

	private WebApplicationException notFound(String deviceId) {
		return new WebApplicationException(Response.status(404)
				.entity(new ErrorResponse().withMessageAs("Device with ID "+deviceId+" not found"))
				.build());
	}
}
