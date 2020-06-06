package com.quakearts.auth.server.totp.test;

import static org.junit.Assert.*;
import static org.hamcrest.core.Is.*;
import static org.hamcrest.core.IsNull.*;

import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import com.quakearts.auth.server.totp.model.Administrator;
import com.quakearts.auth.server.totp.model.Alias;
import com.quakearts.auth.server.totp.model.Device;
import com.quakearts.auth.server.totp.model.Device.Status;
import com.quakearts.security.cryptography.CryptoResource;
import com.quakearts.security.cryptography.jpa.EncryptedValue;
import com.quakearts.webapp.security.util.HashPassword;

public class ModelTests {

	@Test
	public void testDeviceModel() {
		Device device1 = new Device();
		assertThat(device1.notTamperedWith(), is(false));		
		Set<Alias> aliases = new HashSet<>();
		aliases.add(new Alias());
		device1.setAliases(aliases);
		assertThat(device1.getAliases(), is(aliases));
		device1.setId("123456");
		assertThat(device1.getId(), is("123456"));
		assertThat(device1.notTamperedWith(), is(false));		
		device1.setInitialCounter(1);
		assertThat(device1.getInitialCounter(), is(1L));
		device1.setItemCount(1);
		assertThat(device1.getItemCount(), is(1L));
		EncryptedValue seed = new EncryptedValue();
		seed.setDataStoreName("dataStore");
		seed.setValue("value".getBytes());
		device1.setSeed(seed);
		assertThat(device1.getSeed(), is(seed));
		device1.setStatus(Status.INACTIVE);
		assertThat(device1.getStatus(), is(Status.INACTIVE));
		assertThat(device1.getCheckValue(), is(notNullValue()));
		assertThat(device1.getCheckValue().getStringValue(), 
				is(new HashPassword("123456","SHA-256",3,CryptoResource
						.byteAsHex("value".getBytes())).toString()));
		assertThat(device1.notTamperedWith(), is(true));
		
		device1.setId("78901");
		assertThat(device1.notTamperedWith(), is(false));		
		
		Device device2 = new Device();
		Set<Alias> aliases2 = new HashSet<>();
		aliases2.add(new Alias());
		device2.setAliases(aliases2);
		assertThat(device2.getAliases(), is(aliases2));
		device2.setInitialCounter(1);
		assertThat(device2.getInitialCounter(), is(1L));
		device2.setItemCount(1);
		assertThat(device2.getItemCount(), is(1L));
		EncryptedValue seed2 = new EncryptedValue();
		seed2.setDataStoreName("dataStore");
		seed2.setValue("value2".getBytes());
		device2.setSeed(seed2);
		device2.setId("78901");
		assertThat(device2.getId(), is("78901"));
		assertThat(device2.getSeed(), is(seed2));
		device2.setStatus(Status.INACTIVE);
		assertThat(device2.getStatus(), is(Status.INACTIVE));
		assertThat(device2.getCheckValue(), is(notNullValue()));
		assertThat(device2.getCheckValue().getStringValue(), 
				is(new HashPassword("78901","SHA-256",3,CryptoResource
						.byteAsHex("value2".getBytes())).toString()));
		assertThat(device2.notTamperedWith(), is(true));
		
		EncryptedValue checkValue = new EncryptedValue();
		checkValue.setDataStoreName("dataStore");
		checkValue.setStringValue(new HashPassword("34567","SHA-256",3,CryptoResource
						.byteAsHex("value3".getBytes())).toString());
		device2.setCheckValue(checkValue);
		assertThat(device2.notTamperedWith(), is(false));
	}

	@Test
	public void testAdministratorModel() throws Exception {
		Administrator administrator1 = new Administrator();
		assertThat(administrator1.notTamperedWith(), is(false));
		administrator1.setCommonName("test");
		assertThat(administrator1.getCommonName(), is("test"));
		Device device = new Device();
		device.setId("12345");
		administrator1.setDevice(device);
		assertThat(administrator1.notTamperedWith(), is(true));
		assertThat(administrator1.getCheckValue().getStringValue(), 
				is("test12345"));
		assertThat(administrator1.getDevice(), is(device));
		administrator1.setId(1);
		assertThat(administrator1.getId(), is(1l));
		
		Device device2 = new Device();
		device2.setId("67890");
		administrator1.setDevice(device2);
		assertThat(administrator1.notTamperedWith(), is(false));
		
		Administrator administrator2 = new Administrator();
		administrator2.setCommonName("test2");
		Device device3 = new Device();
		device3.setId("34567");
		administrator2.setDevice(device3);
		administrator2.setId(1);
		administrator2.setDevice(null);
		assertThat(administrator2.notTamperedWith(), is(false));
		administrator2.setDevice(device3);
		assertThat(administrator2.notTamperedWith(), is(true));
		EncryptedValue checkValue = new EncryptedValue();
		administrator2.setCheckValue(checkValue);
		assertThat(administrator2.getCheckValue(), is(checkValue));
		assertThat(administrator2.notTamperedWith(), is(false));
		
		Administrator administrator3 = new Administrator();
		Device device4 = new Device();
 		device4.setId("12345");
		administrator3.setDevice(device4);
		administrator3.setCommonName("test");
		administrator3.setId(1);
		assertThat(administrator3.notTamperedWith(), is(true));
	}
	
	@Test
	public void testAliasModel() throws Exception {
		Alias alias1 = new Alias();
		assertThat(alias1.notTamperedWith(), is(false));
		alias1.setName("alias1");
		assertThat(alias1.getName(), is("alias1"));
		assertThat(alias1.notTamperedWith(), is(false));
		Device device = new Device();
		device.setId("12345");
		alias1.setDevice(device);
		assertThat(alias1.getDevice(), is(device));
		assertThat(alias1.getCheckValue(), is(notNullValue()));
		assertThat(alias1.getCheckValue().getStringValue(), 
				is("alias112345"));
		assertThat(alias1.notTamperedWith(), is(true));
		
		Device device2 = new Device();
		device.setId("67890");
		alias1.setDevice(device2);
		assertThat(alias1.notTamperedWith(), is(false));
		
		Alias alias2 = new Alias();
		alias2.setName("alias1");
		Device device3 = new Device();
		device3.setId("12345");
		alias2.setDevice(device3);
		alias2.setName("alias2");
		assertThat(alias2.notTamperedWith(), is(false));
		
		Alias alias3 = new Alias();
		Device device4 = new Device();
		device4.setId("12345");
		alias3.setDevice(device4);
		alias3.setName("alias3");
		assertThat(alias3.notTamperedWith(), is(true));
		
		Alias alias4 = new Alias();
		alias4.setName("alias1");
		Device device5 = new Device();
		device5.setId("12345");
		alias4.setDevice(device5);
		EncryptedValue checkValue = new EncryptedValue();
		checkValue.setStringValue("alias267890");
		alias4.setCheckValue(checkValue);
		assertThat(alias4.notTamperedWith(), is(false));
	}
}
