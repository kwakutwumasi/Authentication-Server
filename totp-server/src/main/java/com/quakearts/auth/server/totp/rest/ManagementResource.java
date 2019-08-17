package com.quakearts.auth.server.totp.rest;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.quakearts.auth.server.totp.device.DeviceManagementService;
import com.quakearts.auth.server.totp.exception.DuplicateAliasException;
import com.quakearts.auth.server.totp.exception.InvalidAliasException;
import com.quakearts.auth.server.totp.exception.InvalidDeviceStatusException;
import com.quakearts.auth.server.totp.exception.ManagementException;
import com.quakearts.auth.server.totp.exception.MissingNameException;
import com.quakearts.auth.server.totp.model.Device;
import com.quakearts.auth.server.totp.model.Device.Status;
import com.quakearts.auth.server.totp.rest.authorization.AuthorizeManagedRequest;
import com.quakearts.auth.server.totp.rest.model.AdministratorResponse;
import com.quakearts.auth.server.totp.rest.model.CountResponse;
import com.quakearts.auth.server.totp.rest.model.DeviceRequest;
import com.quakearts.auth.server.totp.rest.model.DeviceResponse;
import com.quakearts.auth.server.totp.rest.model.ManagementRequest;
import com.quakearts.auth.server.totp.rest.model.ManagementResponseEntry;
import com.quakearts.auth.server.totp.rest.model.ManagementResponse;
import com.quakearts.webapp.security.rest.cdi.RequireAuthorization;

@Path("manage")
@Singleton
@RequireAuthorization(allow="Administrator")
public class ManagementResource {
	
	private static final String THE_ALIAS = "The alias ";

	private static final String DEVICE_WAS_NOT_FOUND = "Device was not found";

	private static final String THE_DEVICE = "The device ";
	
	@Inject
	private DeviceManagementService deviceManagementService;
	
	@FunctionalInterface
	private interface RequestProcessor {
		void process(String alias, Optional<Device> deviceOptional) throws Throwable;
	}
	
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@AuthorizeManagedRequest
	@Path("assign-alias")
	public ManagementResponse assignAliases(ManagementRequest managementRequest){
		return execute(managementRequest, this::assignAlias);
	}
	
	private void assignAlias(String alias, Optional<Device> deviceOptional) throws ManagementException {
		if(deviceOptional.isPresent()){
			Device device = deviceOptional.get();
			try {
				deviceManagementService.assign(alias, device);
			} catch (DuplicateAliasException e) {
				throw new ManagementException(THE_ALIAS+alias+" has already been assigned");
			} catch (InvalidAliasException e) {
				throw new ManagementException(THE_ALIAS+alias+" is not valid");
			}
		} else {
			throw new ManagementException(DEVICE_WAS_NOT_FOUND);
		}
	}

	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@AuthorizeManagedRequest
	@Path("unassign-alias")
	public ManagementResponse unassignAliases(ManagementRequest managementRequest){
		return execute(managementRequest, this::unassignAlias);
	}	

	private void unassignAlias(String alias, Optional<Device> deviceOptional) throws ManagementException {
		if(!deviceManagementService.unassign(alias)){
			throw new ManagementException(THE_ALIAS+alias+" was not assigned");
		}
	}
	
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@AuthorizeManagedRequest
	@Path("lock")
	public ManagementResponse lock(ManagementRequest managementRequest){
		return execute(managementRequest, this::lock);
	}

	private void lock(String alias, Optional<Device> deviceOptional) throws ManagementException {
		if(deviceOptional.isPresent()){
			Device device = deviceOptional.get();
			if(!deviceManagementService.lock(device)){
				throw new ManagementException(THE_DEVICE+device.getId()+" was not locked");
			}
		} else {
			throw new ManagementException(DEVICE_WAS_NOT_FOUND);
		}
	}

	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@AuthorizeManagedRequest
	@Path("unlock")
	public ManagementResponse unlock(ManagementRequest managementRequest){
		return execute(managementRequest, this::unlock);
	}
	
	private void unlock(String alias, Optional<Device> deviceOptional) throws ManagementException {
		if(deviceOptional.isPresent()){
			Device device = deviceOptional.get();
			if(!deviceManagementService.unlock(device)){
				throw new ManagementException(THE_DEVICE+device.getId()+" cannot be unlocked. It is "+device.getStatus());
			}
		} else {
			throw new ManagementException(DEVICE_WAS_NOT_FOUND);
		}
	}
	
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@AuthorizeManagedRequest
	@Path("add-as-admin")
	public ManagementResponse addAsAdmin(ManagementRequest managementRequest){
		return execute(managementRequest, this::addAsAdmin);
	}
	
	private void addAsAdmin(String alias, Optional<Device> deviceOptional) throws ManagementException {
		if(deviceOptional.isPresent()){
			Device device = deviceOptional.get();
			if(deviceManagementService.findAdministrator(device.getId()).isPresent()){
				throw new ManagementException(THE_DEVICE+device.getId()+" is already an administrator device");
			}
			try {
				deviceManagementService.addAsAdmin(alias, device);
			} catch (MissingNameException e) {
				throw new ManagementException("Common name was missing");
			} catch (InvalidDeviceStatusException e) {
				throw new ManagementException("The device is not in a valid state. Status is "+device.getStatus());
			}
		} else {
			throw new ManagementException(DEVICE_WAS_NOT_FOUND);
		}
	}
	
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@AuthorizeManagedRequest
	@Path("remove-as-admin")
	public ManagementResponse removeAsAdmin(ManagementRequest managementRequest){
		return execute(managementRequest, this::removeAsAdmin);
	}
	
	private void removeAsAdmin(String alias, Optional<Device> deviceOptional) throws ManagementException {
		if(deviceOptional.isPresent()){
			Device device = deviceOptional.get();
			if(!deviceManagementService.removeAsAdmin(device)){
				throw new ManagementException(THE_DEVICE+device.getId()+" is not an administrator device");
			}
		} else {
			throw new ManagementException(DEVICE_WAS_NOT_FOUND);
		}
	}
	
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@AuthorizeManagedRequest
	@Path("deactivate")
	public ManagementResponse deactivate(ManagementRequest managementRequest){
		return execute(managementRequest, this::deactivate);
	}
	
	private void deactivate(String alias, Optional<Device> deviceOptional) throws ManagementException {
		if(deviceOptional.isPresent()){
			Device device = deviceOptional.get();
			if(!deviceManagementService.deactivate(device)){
				throw new ManagementException(THE_DEVICE+device.getId()+" cannot be deactivated. It is "+device.getStatus());
			}
		} else {
			throw new ManagementException(DEVICE_WAS_NOT_FOUND);
		}
	}

	private ManagementResponse execute(ManagementRequest managementRequest, RequestProcessor processor){
		ManagementResponseEntry[] response = new ManagementResponseEntry[managementRequest.getRequests().length];
		int index = 0;
		for(DeviceRequest deviceRequest:managementRequest.getRequests()){
			Optional<Device> optionalDevice = deviceRequest.getDeviceId()!=null? deviceManagementService.findDevice(deviceRequest.getDeviceId())
					: Optional.empty();
			try {
				processor.process(deviceRequest.getAlias(), optionalDevice);					
				response[index] = new ManagementResponseEntry().withMessageAs("Success");					
			} catch (Throwable e) {
				response[index] = new ManagementResponseEntry().withMessageAs("Error: "+ e.getMessage())
						.withErrorAs(true);
			}
			index++;
		}
		
		ManagementResponse managementResponse = new ManagementResponse();
		managementResponse.setResponses(response);
		return managementResponse;
	}
	
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("list-administrators")
	public List<AdministratorResponse> listAdministrators(){
		return deviceManagementService.listAdministrators()
				.stream().map(AdministratorResponse::new)
				.collect(Collectors.toList());
	}
	
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("count-devices")
	public CountResponse countDevices(){
		return new CountResponse().withCountAs(deviceManagementService.deviceCount());
	}
	
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("get-devices")
	public List<DeviceResponse> getDevices(@QueryParam("status")Status status, @QueryParam("lastid") long lastId, @QueryParam("maxrows") int maxRows){
		return deviceManagementService.fetchDevices(status, lastId, maxRows)
				.stream().map(DeviceResponse::new)
				.collect(Collectors.toList());
	}
}
