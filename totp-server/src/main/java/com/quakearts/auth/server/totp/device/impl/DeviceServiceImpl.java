package com.quakearts.auth.server.totp.device.impl;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.quakearts.auth.server.totp.device.DeviceService;
import com.quakearts.auth.server.totp.exception.DuplicateAliasException;
import com.quakearts.auth.server.totp.exception.InvalidDeviceStatusException;
import com.quakearts.auth.server.totp.exception.MissingNameException;
import com.quakearts.auth.server.totp.model.Administrator;
import com.quakearts.auth.server.totp.model.Alias;
import com.quakearts.auth.server.totp.model.Device;
import com.quakearts.auth.server.totp.model.Device.Status;
import com.quakearts.auth.server.totp.options.TOTPOptions;
import com.quakearts.security.cryptography.jpa.EncryptedValue;
import com.quakearts.webapp.orm.DataStore;
import com.quakearts.webapp.orm.DataStoreFactory;
import com.quakearts.webapp.orm.cdi.annotation.DataStoreFactoryHandle;
import com.quakearts.webapp.orm.exception.DataStoreException;
import com.quakearts.webapp.orm.query.ListBuilder;

@Singleton
public class DeviceServiceImpl implements DeviceService {

	@Inject @DataStoreFactoryHandle
	private DataStoreFactory factory;

	@Inject
	private TOTPOptions totpOptions;
	
	@Override
	public Optional<Device> findDevice(String id) {
		DataStore dataStore = getTOTPDataStore();
		Device device = dataStore.get(Device.class, id);
		if(device == null){
			Alias alias = dataStore.get(Alias.class, id);
			if(alias!=null && alias.notTamperedWith()){
				device = alias.getDevice();
			}
		}
		
		if(device!=null && device.notTamperedWith())
			return Optional.of(device);
		else
			return Optional.empty();
	}

	private DataStore getTOTPDataStore() {
		return factory.getDataStore(totpOptions.getDataStoreName());
	}

	@Override
	public void assign(String name, Device device) throws DuplicateAliasException {
		DataStore dataStore = getTOTPDataStore();
		if(dataStore.get(Alias.class, name) == null){
			Alias alias = new Alias();
			alias.setName(name);
			alias.setDevice(device);
			alias.getCheckValue().setDataStoreName(totpOptions.getDataStoreName());
			dataStore.save(alias);
		} else {
			throw new DuplicateAliasException();
		}
	}

	@Override
	public boolean unassign(String name) {
		DataStore dataStore = getTOTPDataStore();
		Alias alias = dataStore.get(Alias.class, name);
		if(alias!=null){
			dataStore.delete(alias);
			return true;
		} else {
			return false;
		}
			
	}

	@Override
	public boolean lock(Device device) {
		if(device.getStatus()==Status.ACTIVE){
			device.setStatus(Status.LOCKED);
			getTOTPDataStore().update(device);
			return true;
		}
		return false;
	}

	@Override
	public boolean unlock(Device device) {
		if(device.getStatus()==Status.LOCKED){
			device.setStatus(Status.ACTIVE);
			getTOTPDataStore().update(device);
			return true;
		}
		return false;
	}

	@Override
	public void addAsAdmin(String commonName, Device device) 
			throws MissingNameException, InvalidDeviceStatusException {
		if(commonName == null || commonName.trim().isEmpty())
			throw new MissingNameException();
		
		if(device.getStatus() == Status.LOCKED
				|| device.getStatus() == Status.INACTIVE)
			throw new InvalidDeviceStatusException();
		
		Administrator administrator = new Administrator();
		EncryptedValue id = new EncryptedValue();
		id.setDataStoreName(totpOptions.getDataStoreName());
		id.setStringValue(device.getId());
		administrator.setCheckValue(id);
		administrator.setDevice(device);
		administrator.setCommonName(commonName);
		getTOTPDataStore().save(administrator);
	}
	
	@Override
	public boolean removeAsAdmin(Device device) {
		DataStore dataStore = getTOTPDataStore();
		Optional<Administrator> optionalAdministrator = dataStore.find(Administrator.class)
				.filterBy("device").withAValueEqualTo(device)
				.thenGetFirst();
		if(optionalAdministrator.isPresent()){
			dataStore.delete(optionalAdministrator.get());
		}
		
		return optionalAdministrator.isPresent();
	}
	
	@Override
	public Optional<Administrator> findAdministrator(String id) {
		Optional<Administrator> optionalAdministrator = getTOTPDataStore().find(Administrator.class)
				.filterBy("device.id").withAValueEqualTo(id)
				.thenGetFirst();
		if(optionalAdministrator.isPresent() && !optionalAdministrator.get().notTamperedWith()){
			return Optional.empty();
		}
		
		return optionalAdministrator;
	}
	
	@Override
	public List<Administrator> listAdministrators() {
		return getTOTPDataStore().find(Administrator.class)
				.thenList();
	}
	
	@Override
	public long deviceCount() {
		long[] result = new long[1];
		getTOTPDataStore().executeFunction(dsCon->{
			Connection conn = dsCon.getConnection(Connection.class);
			try(Statement statement = conn.createStatement()){
				ResultSet rs = statement.executeQuery(totpOptions.getCountQuery());
				if(rs.next()){
					result[0] = rs.getLong(1);
				}
			} catch (SQLException e) {
				throw new DataStoreException("Unable to run count query", e);
			}
		});
		return result[0];
	}
	
	@Override
	public boolean deactivate(Device device) {
		if(device.getStatus()==Status.ACTIVE){
			device.setStatus(Status.INACTIVE);
			getTOTPDataStore().update(device);
			return true;
		}
		return false;
	}
	
	@Override
	public List<Device> fetchDevices(Status status, long lastId, int maxRows) {
		ListBuilder<Device> builder = getTOTPDataStore()
				.find(Device.class)
				.filterBy("itemCount").withValues().startingFrom(lastId)
				.useAResultLimitOf(maxRows);
		
		if(status!=null)
			builder.filterBy("status").withAValueEqualTo(status);
		
		return builder.thenList();
	}
}