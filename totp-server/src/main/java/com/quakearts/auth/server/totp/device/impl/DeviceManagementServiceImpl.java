package com.quakearts.auth.server.totp.device.impl;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.quakearts.auth.server.totp.device.DeviceManagementService;
import com.quakearts.auth.server.totp.exception.DuplicateAliasException;
import com.quakearts.auth.server.totp.exception.InvalidAliasException;
import com.quakearts.auth.server.totp.exception.InvalidDeviceStatusException;
import com.quakearts.auth.server.totp.exception.MissingNameException;
import com.quakearts.auth.server.totp.model.Administrator;
import com.quakearts.auth.server.totp.model.Alias;
import com.quakearts.auth.server.totp.model.Device;
import com.quakearts.auth.server.totp.model.Device.Status;
import com.quakearts.auth.server.totp.options.TOTPOptions;
import com.quakearts.webapp.orm.DataStore;
import com.quakearts.webapp.orm.DataStoreFactory;
import com.quakearts.webapp.orm.cdi.annotation.DataStoreFactoryHandle;
import com.quakearts.webapp.orm.exception.DataStoreException;
import com.quakearts.webapp.orm.query.ListBuilder;
import com.quakearts.webapp.orm.query.QueryOrder;

@Singleton
public class DeviceManagementServiceImpl implements DeviceManagementService {

	private static final String DEVICE_ITEM_COUNT = "device.itemCount";

	private static final String ITEM_COUNT = "itemCount";

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
	public void assign(String name, Device device) 
			throws DuplicateAliasException, InvalidAliasException {
		if(name==null || name.trim().isEmpty())
			throw new InvalidAliasException();
		
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
		administrator.setDevice(device);
		administrator.setCommonName(commonName);
		administrator.getCheckValue().setDataStoreName(totpOptions.getDataStoreName());
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
	public List<Device> fetchDevices(Status status, long lastId, int maxRows,
			String deviceFilter) {
		if(deviceFilter == null){
			ListBuilder<Device> builder = getTOTPDataStore()
					.find(Device.class)
					.filterBy(ITEM_COUNT).withValues().startingFrom(lastId+1)
					.useAResultLimitOf(maxRows)
					.orderBy(new QueryOrder(ITEM_COUNT, true));
			
			if(status!=null)
				builder.filterBy("status").withAValueEqualTo(status);

			return builder.thenList();
		} else {
			deviceFilter = "%"+deviceFilter+"%";
			ListBuilder<Alias> aliasBuilder = getTOTPDataStore()
					.find(Alias.class)
					.filterBy(DEVICE_ITEM_COUNT).withValues().startingFrom(lastId+1)
						.usingAnyMatchingFilter()
						.filterBy("name").withAValueLike(deviceFilter)
					.orderBy(new QueryOrder(DEVICE_ITEM_COUNT, true));
			
			if(status!=null)
				aliasBuilder.filterBy("device.status").withAValueEqualTo(status);
			
			List<Device> devices = aliasBuilder.thenList().stream().map(Alias::getDevice)
					.collect(Collectors.toList());
			
			ListBuilder<Device> deviceBuilder = getTOTPDataStore()
					.find(Device.class)
					.filterBy("id").withAValueLike(deviceFilter)
					.filterBy(ITEM_COUNT).withValues().startingFrom(lastId+1)
					.orderBy(new QueryOrder(ITEM_COUNT, true));
			
			if(status!=null)
				deviceBuilder.filterBy("status").withAValueEqualTo(status);
			
			devices.addAll(deviceBuilder.thenList());
			
			Collections.sort(devices, (d1,d2)->(int)(-1*(d1.getItemCount()-d2.getItemCount())));
			
			List<Device> trimmedDevices = new ArrayList<>(maxRows);
			
			long lastCount = -1;
			int count = 0;
			int index = 0;
			while(index<devices.size() && count<maxRows){
				try {
					Device device = devices.get(index);
					if(device.getItemCount()==lastCount)
						continue;
					
					trimmedDevices.add(device);
					lastCount = device.getItemCount();
				} finally {					
					count++;
					index++;
				}
			}
			
			return trimmedDevices;
		}
	}
}
