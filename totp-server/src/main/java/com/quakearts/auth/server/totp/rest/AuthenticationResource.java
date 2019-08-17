package com.quakearts.auth.server.totp.rest;

import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.quakearts.auth.server.totp.authentication.AuthenticationService;
import com.quakearts.auth.server.totp.device.DeviceManagementService;
import com.quakearts.auth.server.totp.exception.MessageGenerationException;
import com.quakearts.auth.server.totp.exception.UnconnectedDeviceException;
import com.quakearts.auth.server.totp.model.Device;
import com.quakearts.auth.server.totp.model.Device.Status;
import com.quakearts.auth.server.totp.rest.authorization.DeviceConnectionService;
import com.quakearts.auth.server.totp.rest.model.AuthenticationRequest;
import com.quakearts.auth.server.totp.rest.model.ErrorResponse;

@Path("authenticate")
@Singleton
public class AuthenticationResource {
	
	@Inject
	private AuthenticationService authenticationService;
	
	@Inject
	private DeviceManagementService deviceManagementService;
	
	@Inject
	private DeviceConnectionService deviceConnectionService;
	
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	public void authenticate(AuthenticationRequest request) {
		if(request.getDeviceId()==null){
			throw new WebApplicationException(Response.status(403)
					.entity(new ErrorResponse().withMessageAs("AuthenticationRequest is required")).build());
		}

		String deviceId = request.getDeviceId();
		String otp = request.getOtp();
		
		Optional<Device> optionalDevice = deviceManagementService.findDevice(deviceId);
		if(optionalDevice.isPresent() && optionalDevice.get().getStatus() == Status.ACTIVE){
			Device device = optionalDevice.get();
			authenticate(device, otp);
		} else {
			throw new WebApplicationException(Response.status(400)
					.entity(new ErrorResponse().withMessageAs("Device with ID "+deviceId+" not found"))
					.type(MediaType.APPLICATION_JSON_TYPE)
					.build());
		}
	}

	private void authenticate(Device device, String otp) {
		try {
			if(!authenticationService.authenticate(device, otp)){
				throw new WebApplicationException(Response.status(403)
						.entity(new ErrorResponse().withMessageAs("OTP did not match"))
						.type(MediaType.APPLICATION_JSON_TYPE)
						.build());
			} 
		} finally {
			if(authenticationService.isLocked(device)){
				deviceManagementService.lock(device);
			}
		}
	}
	
	@GET
	@Path("device/{deviceId}")
	public void authenticateDirect(@PathParam("deviceId") String deviceId) throws UnconnectedDeviceException, MessageGenerationException {
		Optional<Device> optionalDevice = deviceManagementService.findDevice(deviceId);
		if(optionalDevice.isPresent() && optionalDevice.get().getStatus() == Status.ACTIVE){
			Device device = optionalDevice.get();
			String otp = deviceConnectionService.requestOTPCode(deviceId);
			authenticate(device, otp);
		} else {
			throw new WebApplicationException(Response.status(403)
					.entity(new ErrorResponse().withMessageAs("Device with ID "+deviceId+" not found"))
					.type(MediaType.APPLICATION_JSON_TYPE)
					.build());
		}
	}
}
