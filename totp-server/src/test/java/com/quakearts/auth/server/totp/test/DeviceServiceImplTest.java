package com.quakearts.auth.server.totp.test;

import static org.junit.Assert.*;
import static org.hamcrest.core.Is.*;
import static org.hamcrest.core.IsNull.*;

import java.util.List;
import java.util.Optional;

import javax.inject.Inject;
import javax.ws.rs.core.Response;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import com.quakearts.appbase.cdi.annotation.Transactional;
import com.quakearts.appbase.cdi.annotation.Transactional.TransactionType;
import com.quakearts.auth.server.totp.alternatives.AlternativeTOTPOptions;
import com.quakearts.auth.server.totp.device.impl.DeviceManagementServiceImpl;
import com.quakearts.auth.server.totp.exception.DuplicateAliasException;
import com.quakearts.auth.server.totp.exception.InvalidAliasException;
import com.quakearts.auth.server.totp.exception.InvalidDeviceStatusException;
import com.quakearts.auth.server.totp.exception.MissingNameException;
import com.quakearts.auth.server.totp.exception.TOTPException;
import com.quakearts.auth.server.totp.exception.TOTPExceptionMapper;
import com.quakearts.auth.server.totp.model.Administrator;
import com.quakearts.auth.server.totp.model.Device;
import com.quakearts.auth.server.totp.model.Device.Status;
import com.quakearts.auth.server.totp.options.TOTPOptions;
import com.quakearts.auth.server.totp.rest.model.ErrorResponse;
import com.quakearts.auth.server.totp.runner.TOTPDatabaseServiceRunner;
import com.quakearts.webapp.orm.DataStore;
import com.quakearts.webapp.orm.DataStoreFactory;
import com.quakearts.webapp.orm.cdi.annotation.DataStoreFactoryHandle;
import com.quakearts.webapp.orm.exception.DataStoreException;

@RunWith(TOTPDatabaseServiceRunner.class)
public class DeviceServiceImplTest {

	@Inject
	private DeviceManagementServiceImpl deviceService;
	
	@Inject @DataStoreFactoryHandle
	private DataStoreFactory factory;
	
	@Inject
	private TOTPOptions totpOptions;
	
	@Rule
	public ExpectedException expectedException = ExpectedException.none();
	
	@Test
	@Transactional(TransactionType.SINGLETON)
	public void testFindDevice() {
		Optional<Device> optionalDevice = deviceService.findDevice("testdevice1");
		assertThat(optionalDevice.isPresent(), is(true));
		Device device = optionalDevice.get();
		assertThat(device.getId(), is("testdevice1"));
		assertThat(device.getInitialCounter(), is(notNullValue()));
		assertThat(device.getSeed().getValue(), is(notNullValue()));
		assertThat(device.getSeed().getValue(), is(notNullValue()));
		assertThat(device.getStatus(), is(Status.ACTIVE));
		
		optionalDevice = deviceService.findDevice("testalias1");
		assertThat(optionalDevice.isPresent(), is(true));
		device = optionalDevice.get();
		assertThat(device.getId(), is("testdevice1"));
		assertThat(device.getInitialCounter(), is(notNullValue()));
		assertThat(device.getSeed().getValue(), is(notNullValue()));
		assertThat(device.getSeed().getValue(), is(notNullValue()));
		assertThat(device.getStatus(), is(Status.ACTIVE));
		
		optionalDevice = deviceService.findDevice("testnonexistentdevice");
		assertThat(optionalDevice.isPresent(), is(false));
		
		optionalDevice = deviceService.findDevice("testaliasTampered");
		assertThat(optionalDevice.isPresent(), is(false));
		
		optionalDevice = deviceService.findDevice("tampereddevice1");
		assertThat(optionalDevice.isPresent(), is(false));
	}

	@Test
	@Transactional(TransactionType.SINGLETON)
	public void testAssign() throws Exception {
		Optional<Device> optionalDevice = deviceService.findDevice("testdevice1");
		assertThat(optionalDevice.isPresent(), is(true));
		Device device = optionalDevice.get();
		
		deviceService.assign("testassign1", device);
		DataStore dataStore = factory.getDataStore(totpOptions.getDataStoreName());
		dataStore.flushBuffers();
		optionalDevice = deviceService.findDevice("testassign1");
		assertThat(optionalDevice.isPresent(), is(true));
		expectedException.expect(DuplicateAliasException.class);
		expectedException.expect(matchesTOTPException(new DuplicateAliasException()));
		deviceService.assign("testassign1", device);
	}

	@Test
	@Transactional(TransactionType.SINGLETON)
	public void testAssignWithNullName() throws Exception {
		Optional<Device> optionalDevice = deviceService.findDevice("testdevice1");
		assertThat(optionalDevice.isPresent(), is(true));
		Device device = optionalDevice.get();
		
		expectedException.expect(InvalidAliasException.class);
		deviceService.assign(null, device);
	}

	@Test
	@Transactional(TransactionType.SINGLETON)
	public void testAssignWithEmptyName() throws Exception {
		Optional<Device> optionalDevice = deviceService.findDevice("testdevice1");
		assertThat(optionalDevice.isPresent(), is(true));
		Device device = optionalDevice.get();
		
		expectedException.expect(InvalidAliasException.class);
		deviceService.assign("  ", device);
	}

	
	@Test
	@Transactional(TransactionType.SINGLETON)
	public void testUnassign() throws Exception {
		Optional<Device> optionalDevice = deviceService.findDevice("testdevice1");
		assertThat(optionalDevice.isPresent(), is(true));
		Device device = optionalDevice.get();
		
		deviceService.assign("testunassign1", device);
		DataStore dataStore = factory.getDataStore(totpOptions.getDataStoreName());
		dataStore.flushBuffers();
		optionalDevice = deviceService.findDevice("testunassign1");
		assertThat(optionalDevice.isPresent(), is(true));
		assertThat(deviceService.unassign("testunassign1"), is(true));
		dataStore.flushBuffers();
		optionalDevice = deviceService.findDevice("testunassign1");
		assertThat(optionalDevice.isPresent(), is(false));
		
		assertThat(deviceService.unassign("testunassign1"), is(false));
	}

	@Test
	@Transactional(TransactionType.SINGLETON)
	public void testLock() throws Exception {
		Optional<Device> optionalDevice = deviceService.findDevice("testunlockeddevice1");
		assertThat(optionalDevice.isPresent(), is(true));
		Device device = optionalDevice.get();
		
		deviceService.lock(device);
		optionalDevice = deviceService.findDevice("testunlockeddevice1");
		assertThat(optionalDevice.isPresent(), is(true));
		Device lockedDevice = optionalDevice.get();
		assertThat(lockedDevice.getStatus(), is(Status.LOCKED));
		
		optionalDevice = deviceService.findDevice("testinactivedevice1");
		assertThat(optionalDevice.isPresent(), is(true));
		device = optionalDevice.get();
		assertThat(deviceService.lock(device), is(false));
	}

	@Test
	@Transactional(TransactionType.SINGLETON)
	public void testUnlock() throws Exception {
		Optional<Device> optionalDevice = deviceService.findDevice("testlockeddevice1");
		assertThat(optionalDevice.isPresent(), is(true));
		Device device = optionalDevice.get();
		
		deviceService.unlock(device);
		optionalDevice = deviceService.findDevice("testlockeddevice1");
		assertThat(optionalDevice.isPresent(), is(true));
		Device unlockedDevice = optionalDevice.get();
		assertThat(unlockedDevice.getStatus(), is(Status.ACTIVE));
		
		optionalDevice = deviceService.findDevice("testinactivedevice1");
		assertThat(optionalDevice.isPresent(), is(true));
		device = optionalDevice.get();
		assertThat(deviceService.unlock(device), is(false));
	}

	@Test
	@Transactional(TransactionType.SINGLETON)
	public void testAddAsAdminFindAdministratorListAdministratorsAndRemoveAsAdmin() throws Exception {
		Optional<Device> optionalDevice = deviceService.findDevice("testdevice1");
		assertThat(optionalDevice.isPresent(), is(true));
		Device device = optionalDevice.get();
		
		deviceService.addAsAdmin("Administrator", device);
		DataStore dataStore = factory.getDataStore(totpOptions.getDataStoreName());
		dataStore.flushBuffers();
		
		Optional<Administrator> optionalAdministrator = deviceService.findAdministrator("testdevice1");
		
		assertThat(optionalAdministrator.isPresent(), is(true));
		Administrator administrator = optionalAdministrator.get();
		assertThat(administrator.getDevice().getId(), is(device.getId()));
		assertThat(administrator.getCheckValue(), is(notNullValue()));
		assertThat(administrator.getCommonName(), is("Administrator"));
		
		//Test tampered administrator
		optionalAdministrator = deviceService.findAdministrator("testdeactivatedevice1");
		assertThat(optionalAdministrator.isPresent(), is(false));

		optionalAdministrator = deviceService.findAdministrator("testinactivedevice1");
		assertThat(optionalAdministrator.isPresent(), is(false));
		
		List<Administrator> administrators = deviceService.listAdministrators();
		assertThat(administrators.isEmpty(), is(false));
		
		assertThat(deviceService.removeAsAdmin(device), is(true));
		dataStore.flushBuffers();
		
		optionalAdministrator = deviceService
				.findAdministrator("testdevice1");
		assertThat(optionalAdministrator.isPresent(), is(false));
	}
	
	@Test
	@Transactional(TransactionType.SINGLETON)
	public void testAddAsAdminWithMissingNameException() throws Exception {
		Optional<Device> optionalDevice = deviceService.findDevice("testdevice1");
		assertThat(optionalDevice.isPresent(), is(true));
		Device device = optionalDevice.get();
		
		expectedException.expect(MissingNameException.class);
		expectedException.expect(matchesTOTPException(new MissingNameException()));
		deviceService.addAsAdmin(null, device);
	}
	
	@Test
	@Transactional(TransactionType.SINGLETON)
	public void testAddAsAdminWithInvalidDeviceStatusExceptionAndLockedDevice() throws Exception {
		Optional<Device> optionalDevice = deviceService.findDevice("testlockeddevice1");
		assertThat(optionalDevice.isPresent(), is(true));
		Device device = optionalDevice.get();
		
		//Ensure it is locked
		device.setStatus(Status.LOCKED);
		
		expectedException.expect(InvalidDeviceStatusException.class);
		expectedException.expect(matchesTOTPException(new InvalidDeviceStatusException()));
		deviceService.addAsAdmin("lockedAdministrator", device);
	}
	
	@Test
	@Transactional(TransactionType.SINGLETON)
	public void testAddAsAdminWithInvalidDeviceStatusExceptionAndInactiveDevice() throws Exception {
		Optional<Device> optionalDevice = deviceService.findDevice("testinactivedevice1");
		assertThat(optionalDevice.isPresent(), is(true));
		Device device = optionalDevice.get();
				
		expectedException.expect(InvalidDeviceStatusException.class);
		deviceService.addAsAdmin("inactiveAdministrator", device);
	}
	
	@Test
	@Transactional(TransactionType.SINGLETON)
	public void testRemoveAsAdminWithNonAdminDevice() {
		Optional<Device> optionalDevice = deviceService.findDevice("testlockeddevice1");
		assertThat(optionalDevice.isPresent(), is(true));
		Device device = optionalDevice.get();
		
		assertThat(deviceService.removeAsAdmin(device), is(false));
	}

	@Test
	@Transactional(TransactionType.SINGLETON)
	public void deactivate() throws Exception {
		Optional<Device> optionalDevice = deviceService.findDevice("testdeactivatedevice1");
		assertThat(optionalDevice.isPresent(), is(true));
		Device device = optionalDevice.get();
		
		deviceService.deactivate(device);
		optionalDevice = deviceService.findDevice("testdeactivatedevice1");
		assertThat(optionalDevice.isPresent(), is(true));
		Device lockedDevice = optionalDevice.get();
		assertThat(lockedDevice.getStatus(), is(Status.INACTIVE));
		
		optionalDevice = deviceService.findDevice("testinactivedevice1");
		assertThat(optionalDevice.isPresent(), is(true));
		device = optionalDevice.get();
		assertThat(deviceService.deactivate(device), is(false));
	}
	
	@Test
	@Transactional(TransactionType.SINGLETON)
	public void testDeviceCount() throws Exception {
		AlternativeTOTPOptions.returnCountQuery("SELECT 5 AS TOTAL FROM DEVICE WHERE 1=1");
		assertThat(deviceService.deviceCount(), is(5l));
		AlternativeTOTPOptions.returnCountQuery("SELECT 0 AS TOTAL FROM DEVICE WHERE 1=2");
		assertThat(deviceService.deviceCount(), is(0l));
		
		expectedException.expect(DataStoreException.class);
		expectedException.expectMessage("Unable to run count query");
		AlternativeTOTPOptions.returnCountQuery(";");
		deviceService.deviceCount();
	}
	
	@Test
	@Transactional(TransactionType.SINGLETON)
	public void testFetchDevices() throws Exception {
		assertThat(deviceService.fetchDevices(Status.LOCKED, 0l, 1).size(), is(1));
		assertThat(deviceService.fetchDevices(null, 0l, 5).size(), is(5));
		assertThat(deviceService.fetchDevices(Status.INITIATED, 0l, 5).size(), is(1));
	}
	
	@Test
	@Transactional(TransactionType.SINGLETON)
	public void testFindTamperedDevice() throws Exception {
		Optional<Device> tamperedDeviceOptional = deviceService.findDevice("tampereddevice");
		assertThat(tamperedDeviceOptional.isPresent(), is(false));
	}
	
	static class TOTPExceptionMatch extends BaseMatcher<TOTPException> {
		TOTPException toMatch;
		
		@Override
		public boolean matches(Object item) {
			if(item instanceof TOTPException) {
				TOTPException totpException = (TOTPException) item;
				TOTPExceptionMapper mapper = new TOTPExceptionMapper();
				Response response = mapper.toResponse(totpException);
				Response compareResponse = mapper.toResponse(toMatch);
				return response.getStatus() == compareResponse.getStatus() && 
						((ErrorResponse)response.getEntity()).getMessage()
						.equals(((ErrorResponse)compareResponse.getEntity()).getMessage());
			}
			return false;
		}

		@Override
		public void describeTo(Description description) {
			TOTPExceptionMapper mapper = new TOTPExceptionMapper();
			Response response = mapper.toResponse(toMatch);
			description.appendText("Expected exception with message "
					+toMatch.getMessage()+" with httpCode "+response.getStatus());
		}
		
	}

	private static TOTPExceptionMatch matchesTOTPException(TOTPException toMatch) {
		TOTPExceptionMatch match = new TOTPExceptionMatch();
		match.toMatch = toMatch;
		return match;
	}
}
