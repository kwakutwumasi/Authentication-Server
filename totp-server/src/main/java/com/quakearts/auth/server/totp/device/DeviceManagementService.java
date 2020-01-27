package com.quakearts.auth.server.totp.device;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import com.quakearts.auth.server.totp.exception.DuplicateAliasException;
import com.quakearts.auth.server.totp.exception.InstalledAdministratorDeactivationException;
import com.quakearts.auth.server.totp.exception.InstalledAdministratorRemovalException;
import com.quakearts.auth.server.totp.exception.InvalidAliasException;
import com.quakearts.auth.server.totp.exception.InvalidDeviceStatusException;
import com.quakearts.auth.server.totp.exception.MissingNameException;
import com.quakearts.auth.server.totp.exception.TOTPException;
import com.quakearts.auth.server.totp.model.Administrator;
import com.quakearts.auth.server.totp.model.Device;
import com.quakearts.auth.server.totp.model.Device.Status;

public interface DeviceManagementService {
	Optional<Device> findDevice(String id);
	void assign(String name, Device device) 
			throws DuplicateAliasException, InvalidAliasException;
	boolean unassign(String name);
	boolean lock(Device device);
	boolean unlock(Device device);
	void addAsAdmin(String name, Device device) 
			throws MissingNameException, InvalidDeviceStatusException;
	boolean removeAsAdmin(Device device) throws InstalledAdministratorRemovalException;
	Optional<Administrator> findAdministrator(String id);
	List<Administrator> listAdministrators();
	long deviceCount();
	boolean deactivate(Device device) throws InstalledAdministratorDeactivationException;
	List<Device> fetchDevices(Status status, long lastId, int maxRows, String deviceFilter);
	void isConnected(Device device, Consumer<Boolean> callback) throws TOTPException;
}
