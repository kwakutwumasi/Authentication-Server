package com.quakearts.auth.server.totp.rest;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.MediaType;
import com.quakearts.auth.server.totp.device.DeviceConnectionExecutorService;
import com.quakearts.auth.server.totp.device.DeviceManagementService;
import com.quakearts.auth.server.totp.exception.TOTPException;
import com.quakearts.auth.server.totp.exception.UnconnectedDeviceException;
import com.quakearts.auth.server.totp.model.Device;
import com.quakearts.auth.server.totp.model.Device.Status;
import com.quakearts.auth.server.totp.options.TOTPOptions;
import com.quakearts.auth.server.totp.rest.model.AliasCheckResponse;

@Path("alias")
public class AliasResource {
	
	@Inject
	private DeviceManagementService deviceManagementService;
	
	@Inject
	private DeviceConnectionExecutorService deviceConnectionExecutorService;
	
	@Inject
	private TOTPOptions totpOptions;

	
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("check/{aliasOrDeviceId}")
	public void checkAlias(@PathParam("aliasOrDeviceId") String aliasOrDeviceId, 
			@Suspended AsyncResponse asyncResponse) {
		asyncResponse.setTimeout(totpOptions.getDeviceConnectionRequestTimeout(), TimeUnit.MILLISECONDS);
		asyncResponse.setTimeoutHandler(this::handleTimeout);
		CompletableFuture.runAsync(()->{
			Optional<Device> optionalDevice = deviceManagementService.findDevice(aliasOrDeviceId);
			if(optionalDevice.isPresent()){
				try {
					deviceManagementService.isConnected(optionalDevice.get(), connected->
						asyncResponse.resume(new AliasCheckResponse()
								.withActiveAs(optionalDevice.get().getStatus() == Status.ACTIVE)
								.withConnectedAs(connected)));
				} catch (TOTPException e) {
					asyncResponse.resume(e);
				}
			} else {
				asyncResponse.resume(new AliasCheckResponse()
						.withActiveAs(false)
						.withConnectedAs(false));
			}
		}, deviceConnectionExecutorService.getExecutorService());
	}
	
	private void handleTimeout(AsyncResponse asyncResponse) {
		asyncResponse.resume(new UnconnectedDeviceException("Connection timed out"));
	}
}
