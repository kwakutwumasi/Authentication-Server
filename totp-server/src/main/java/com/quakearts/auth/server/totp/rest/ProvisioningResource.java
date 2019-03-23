package com.quakearts.auth.server.totp.rest;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.quakearts.auth.server.totp.authentication.AuthenticationService;
import com.quakearts.auth.server.totp.device.DeviceService;
import com.quakearts.auth.server.totp.exception.InvalidDeviceStatusException;
import com.quakearts.auth.server.totp.exception.MissingNameException;
import com.quakearts.auth.server.totp.generator.KeyGenerator;
import com.quakearts.auth.server.totp.model.Device;
import com.quakearts.auth.server.totp.model.Device.Status;
import com.quakearts.auth.server.totp.options.TOTPOptions;
import com.quakearts.auth.server.totp.rest.model.ActivationRequest;
import com.quakearts.auth.server.totp.rest.model.ErrorResponse;
import com.quakearts.auth.server.totp.rest.model.ProvisioningResponse;
import com.quakearts.security.cryptography.CryptoResource;
import com.quakearts.webapp.orm.DataStore;
import com.quakearts.webapp.orm.DataStoreFactory;
import com.quakearts.webapp.orm.cdi.annotation.DataStoreFactoryHandle;

@Path("provisioning/{deviceid}")
@Singleton
public class ProvisioningResource {
	@Inject
	private KeyGenerator keyGenerator;
	
	@Inject
	private AuthenticationService authenticationService;
	
	@Inject
	private DeviceService deviceService;
	
	@Inject
	private TOTPOptions totpOptions;
	
	@Inject @DataStoreFactoryHandle
	private DataStoreFactory factory;
	
	@POST
	@Produces(MediaType.APPLICATION_JSON)
	public ProvisioningResponse provision(@PathParam("deviceid") String deviceid) {
		DataStore dataStore = factory.getDataStore(totpOptions.getDataStoreName());
		Device device = dataStore.get(Device.class, deviceid);
		if(device == null) {
			device = new Device();
			device.setId(deviceid);
			device.setInitialCounter(System.currentTimeMillis());
			keyGenerator.generateAndStoreIn(device);
			device.setStatus(Status.INITIATED);
			dataStore.save(device);
			
			return new ProvisioningResponse()
					.withSeedAs(CryptoResource.byteAsHex(device.getSeed().getValue()))
					.withInitialCounterAs(device.getInitialCounter());
		} else {
			throw new WebApplicationException(Response.status(403)
					.entity(new ErrorResponse()
							.withMessageAs("The device cannot be provisioned")).build());
		}
	}
	
	@PUT
	@Consumes(MediaType.APPLICATION_JSON)
	public void activate(@PathParam("deviceid") String deviceid, ActivationRequest request) 
			throws MissingNameException, InvalidDeviceStatusException {
		DataStore dataStore = factory.getDataStore(totpOptions.getDataStoreName());
		Device device = dataStore.get(Device.class, deviceid);
		if(device != null && device.getStatus() == Status.INITIATED) {
			if(authenticationService.authenticate(device, request.getToken())){
				device.setStatus(Status.ACTIVE);
				dataStore.update(device);
				
				if(totpOptions.getInstalledAdministrators().containsKey(deviceid)){
					String name = totpOptions.getInstalledAdministrators().get(deviceid);
					deviceService.addAsAdmin(name, device);
				}
			} else {
				throw new WebApplicationException(Response.status(403)
						.type(MediaType.APPLICATION_JSON)
						.entity(new ErrorResponse()
								.withMessageAs("The device cannot be activated")).build());
			}
		} else {
			throw new WebApplicationException(Response.status(404)
					.type(MediaType.APPLICATION_JSON)
					.entity(new ErrorResponse()
							.withMessageAs("The device cannot be activated")).build());
		}
	}
}
