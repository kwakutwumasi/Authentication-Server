package com.quakearts.auth.server.totp.alternatives;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import javax.annotation.Priority;
import javax.enterprise.inject.Alternative;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.interceptor.Interceptor;

import com.quakearts.auth.server.totp.device.DeviceManagementService;
import com.quakearts.auth.server.totp.device.impl.DeviceManagementServiceImpl;
import com.quakearts.auth.server.totp.exception.DuplicateAliasException;
import com.quakearts.auth.server.totp.exception.InvalidAliasException;
import com.quakearts.auth.server.totp.exception.InvalidDeviceStatusException;
import com.quakearts.auth.server.totp.exception.MissingNameException;
import com.quakearts.auth.server.totp.exception.TOTPException;
import com.quakearts.auth.server.totp.model.Administrator;
import com.quakearts.auth.server.totp.model.Device;
import com.quakearts.auth.server.totp.model.Device.Status;

@Alternative
@Priority(Interceptor.Priority.APPLICATION)
@Singleton
public class AlternativeDeviceManagementService implements DeviceManagementService {

	private static TestLockFunction returnLock;
	
	@FunctionalInterface
	public static interface TestLockFunction {
		boolean lock(Device device);
	}
	
	public static void returnLock(TestLockFunction returnLock) {
		AlternativeDeviceManagementService.returnLock = returnLock;
	}
	
	private static TestFindDeviceFunction returnDevice;
	
	@FunctionalInterface
	public static interface TestFindDeviceFunction {
		Optional<Device> findDevice(String id);
	}
	
	public static void returnDevice(TestFindDeviceFunction returnDevice) {
		AlternativeDeviceManagementService.returnDevice = returnDevice;
	}
	
	private static RuntimeException throwRuntimeException;
	
	public static void throwRuntimeException(RuntimeException newThrowException) {
		throwRuntimeException = newThrowException;
	}
	
	private static TOTPException throwTotpException;
	
	public static void throwTOTPException(TOTPException newThrowTotpException) {
		throwTotpException = newThrowTotpException;
	}

	@Inject
	private DeviceManagementServiceImpl deviceService;
	
	@Override
	public Optional<Device> findDevice(String id) {
		if(throwRuntimeException != null) {
			RuntimeException toThrow = throwRuntimeException;
			throwRuntimeException = null;
			throw toThrow;
		}
		if(returnDevice!=null){
			TestFindDeviceFunction toreturn = returnDevice;
			returnDevice = null;
			return toreturn.findDevice(id);
		}
		
		return deviceService.findDevice(id);
	}

	@Override
	public void assign(String name, Device device) 
			throws DuplicateAliasException, InvalidAliasException {
		deviceService.assign(name, device);
	}

	@Override
	public boolean unassign(String name) {
		return deviceService.unassign(name);
	}

	@Override
	public boolean lock(Device device) {
		if(returnLock!=null){
			TestLockFunction toreturn = returnLock;
			returnLock = null;
			return toreturn.lock(device);
		}
		
		return deviceService.lock(device);
	}

	@Override
	public boolean unlock(Device device) {
		return deviceService.unlock(device);
	}

	@Override
	public void addAsAdmin(String commonName, Device device)
			throws MissingNameException, InvalidDeviceStatusException {
		deviceService.addAsAdmin(commonName, device);
	}

	@Override
	public boolean removeAsAdmin(Device device) {
		return deviceService.removeAsAdmin(device);
	}

	@Override
	public Optional<Administrator> findAdministrator(String id) {
		return deviceService.findAdministrator(id);
	}

	@Override
	public List<Administrator> listAdministrators() {
		return deviceService.listAdministrators();
	}
	
	@Override
	public long deviceCount() {
		return deviceService.deviceCount();
	}
	
	@Override
	public boolean deactivate(Device device) {
		return deviceService.deactivate(device);
	}
	
	@Override
	public List<Device> fetchDevices(Status status, long lastId, int maxRows, String deviceString) {
		return deviceService.fetchDevices(status, lastId, maxRows, deviceString);
	}
	
	@Override
	public void isConnected(Device device, Consumer<Boolean> callback) throws TOTPException {
		if(throwTotpException!=null){
			TOTPException exception = throwTotpException;
			throwTotpException = null;
			throw exception;
		}
		deviceService.isConnected(device, callback);
	}
}
