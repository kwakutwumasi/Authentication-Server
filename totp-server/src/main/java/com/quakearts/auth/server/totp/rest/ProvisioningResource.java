package com.quakearts.auth.server.totp.rest;

import java.time.LocalDateTime;

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
import com.quakearts.auth.server.totp.device.DeviceManagementService;
import com.quakearts.auth.server.totp.exception.DuplicateAliasException;
import com.quakearts.auth.server.totp.exception.InvalidAliasException;
import com.quakearts.auth.server.totp.exception.InvalidDeviceStatusException;
import com.quakearts.auth.server.totp.exception.MissingNameException;
import com.quakearts.auth.server.totp.generator.KeyGenerator;
import com.quakearts.auth.server.totp.model.Alias;
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

@Path("provisioning/{deviceId}")
@Singleton
public class ProvisioningResource {
	@Inject
	private KeyGenerator keyGenerator;
	
	@Inject
	private AuthenticationService authenticationService;
	
	@Inject
	private DeviceManagementService deviceManagementService;
	
	@Inject
	private TOTPOptions totpOptions;
	
	@Inject @DataStoreFactoryHandle
	private DataStoreFactory factory;
	
	@POST
	@Produces(MediaType.APPLICATION_JSON)
	public ProvisioningResponse provision(@PathParam("deviceId") String deviceId) {
		DataStore dataStore = factory.getDataStore(totpOptions.getDataStoreName());
		Device device = dataStore.get(Device.class, deviceId);
		if(device == null) {
			device = createDevice(deviceId, dataStore);			
			return new ProvisioningResponse()
					.withSeedAs(CryptoResource.byteAsHex(device.getSeed().getValue()))
					.withInitialCounterAs(device.getInitialCounter());
		} else {
			throw new WebApplicationException(Response.status(403)
					.entity(new ErrorResponse()
							.withMessageAs("The device cannot be provisioned")).build());
		}
	}

	private Device createDevice(String deviceId, DataStore dataStore) {
		Device device;
		device = new Device();
		device.setId(deviceId);
		device.setInitialCounter(System.currentTimeMillis());
		keyGenerator.generateAndStoreIn(device);
		device.setStatus(Status.INITIATED);
		device.setCreatedOn(LocalDateTime.now());
		dataStore.save(device);
		return device;
	}
	
	@PUT
	@Consumes(MediaType.APPLICATION_JSON)
	public void activate(@PathParam("deviceId") String deviceId, ActivationRequest request) 
			throws MissingNameException, InvalidDeviceStatusException, 
				InvalidAliasException, DuplicateAliasException {
		DataStore dataStore = factory.getDataStore(totpOptions.getDataStoreName());
		checkAlias(request, dataStore);
		
		Device device = dataStore.get(Device.class, deviceId);
		if(device != null && device.getStatus() == Status.INITIATED) {
			doActivation(deviceId, request, dataStore, device);
		} else {
			throw new WebApplicationException(Response.status(404)
					.type(MediaType.APPLICATION_JSON)
					.entity(new ErrorResponse()
							.withMessageAs("The device cannot be activated")).build());
		}
	}

	private void checkAlias(ActivationRequest request, DataStore dataStore) throws InvalidAliasException {
		if(request.getAlias()!=null 
				&& dataStore.get(Alias.class, request.getAlias()) != null) {
			throw new InvalidAliasException();
		}
	}

	private void doActivation(String deviceId, ActivationRequest request, DataStore dataStore, Device device)
			throws MissingNameException, InvalidDeviceStatusException, DuplicateAliasException, InvalidAliasException {
		if(authenticationService.authenticate(device, request.getToken())){
			device.setStatus(Status.ACTIVE);
			dataStore.update(device);
			
			if(totpOptions.getInstalledAdministrators().containsKey(deviceId)){
				String name = totpOptions.getInstalledAdministrators().get(deviceId);
				deviceManagementService.addAsAdmin(name, device);
			}
			
			if(request.getAlias()!=null) {
				deviceManagementService.assign(request.getAlias(), device);
			}
		} else {
			throw new WebApplicationException(Response.status(403)
					.type(MediaType.APPLICATION_JSON)
					.entity(new ErrorResponse()
							.withMessageAs("The device cannot be activated")).build());
		}
	}
}
