package com.quakearts.auth.server.totp.setup;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.quakearts.appbase.cdi.annotation.Transactional;
import com.quakearts.appbase.cdi.annotation.Transactional.TransactionType;
import com.quakearts.auth.server.totp.generator.KeyGenerator;
import com.quakearts.auth.server.totp.model.Administrator;
import com.quakearts.auth.server.totp.model.Alias;
import com.quakearts.auth.server.totp.model.Device;
import com.quakearts.auth.server.totp.model.Device.Status;
import com.quakearts.auth.server.totp.options.TOTPOptions;
import com.quakearts.security.cryptography.jpa.EncryptedValue;
import com.quakearts.webapp.orm.DataStore;
import com.quakearts.webapp.orm.DataStoreConnection;
import com.quakearts.webapp.orm.DataStoreFactory;
import com.quakearts.webapp.orm.cdi.annotation.DataStoreFactoryHandle;

@Singleton
public class CreatorService {
	private static final Logger log = LoggerFactory.getLogger(CreatorService.class);
	
	@Inject @DataStoreFactoryHandle
	private DataStoreFactory factory;
	
	@Inject
	private TOTPOptions totpOptions;
	
	@Inject
	private KeyGenerator keyGenerator;
	
	private boolean databaseCreated;
	private boolean entitiesCreated;

	@Transactional(TransactionType.SINGLETON)
	public void dropAndCreateDatabase(){
		if(!databaseCreated){
			DataStore dataStore = factory.getDataStore(totpOptions.getDataStoreName());
			
			dataStore.executeFunction(this::createAndDropDatabase);
			databaseCreated = true;
		}
	}
	
	private void createAndDropDatabase(DataStoreConnection connection){
		Connection sqlConnection = connection.getConnection(Connection.class);
		executeStatement("ALTER TABLE ADMINISTRATOR DROP CONSTRAINT FKADMINISTRATORDEVICE", sqlConnection);
		executeStatement("ALTER TABLE ALIAS DROP CONSTRAINT FKALIASDEVICE", sqlConnection);
		executeStatement("ALTER TABLE DEVICE DROP CONSTRAINT PKDEVICE", sqlConnection);
		executeStatement("ALTER TABLE ADMINISTRATOR DROP CONSTRAINT PKADMINISTRATOR", sqlConnection);
		executeStatement("ALTER TABLE ALIAS DROP CONSTRAINT PKALIAS", sqlConnection);
		executeStatement("DROP TABLE ADMINISTRATOR", sqlConnection);
		executeStatement("DROP TABLE ALIAS", sqlConnection);
		executeStatement("DROP TABLE DEVICE", sqlConnection);
		executeStatement("CREATE TABLE ADMINISTRATOR (ID BIGINT NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1), CHECKVALUE VARCHAR(255) NOT NULL, DEVICE_ID VARCHAR(250) NOT NULL, COMMONNAME VARCHAR(250) NOT NULL)", sqlConnection);
		executeStatement("CREATE TABLE ALIAS (NAME VARCHAR(250) NOT NULL, CHECKVALUE VARCHAR(250) NOT NULL, DEVICE_ID VARCHAR(250) NOT NULL)", sqlConnection);
		executeStatement("CREATE TABLE DEVICE (ID VARCHAR(250) NOT NULL, INITIALCOUNTER BIGINT NOT NULL, SEED LONG VARCHAR FOR BIT DATA NOT NULL, CHECKVALUE VARCHAR(250) NOT NULL, STATUS INTEGER NOT NULL, ITEMCOUNT BIGINT NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1))", sqlConnection);
		executeStatement("CREATE UNIQUE INDEX IDXADMINISTRATORID ON ADMINISTRATOR (ID ASC)", sqlConnection);
		executeStatement("CREATE INDEX IDXADMINISTRATORIDDEVICEID ON ADMINISTRATOR (DEVICE_ID ASC)", sqlConnection);
		executeStatement("CREATE UNIQUE INDEX IDXALIASNAME ON ALIAS (NAME ASC)", sqlConnection);
		executeStatement("CREATE INDEX IDXALIASDEVICEID ON ALIAS (DEVICE_ID ASC)", sqlConnection);
		executeStatement("CREATE UNIQUE INDEX IDXDEVICEID ON DEVICE (ID ASC)", sqlConnection);
		executeStatement("ALTER TABLE DEVICE ADD CONSTRAINT PKDEVICE PRIMARY KEY (ID)", sqlConnection);
		executeStatement("ALTER TABLE ADMINISTRATOR ADD CONSTRAINT PKADMINISTRATOR PRIMARY KEY (ID)", sqlConnection);
		executeStatement("ALTER TABLE ADMINISTRATOR ADD CONSTRAINT FKADMINISTRATORDEVICE FOREIGN KEY (DEVICE_ID) REFERENCES DEVICE (ID)", sqlConnection);
		executeStatement("ALTER TABLE ADMINISTRATOR ADD CONSTRAINT UKADMINISTRATOR UNIQUE (CHECKVALUE)", sqlConnection);
		executeStatement("ALTER TABLE ALIAS ADD CONSTRAINT PKALIAS PRIMARY KEY (NAME)", sqlConnection);
		executeStatement("ALTER TABLE ALIAS ADD CONSTRAINT FKALIASDEVICE FOREIGN KEY (DEVICE_ID) REFERENCES DEVICE (ID)", sqlConnection);
	}
	
	private void executeStatement(String statement, Connection connection){
		try(Statement sqlstatement = connection.createStatement()) {
			sqlstatement.execute(statement);
		} catch (SQLException e) {
			log.error("Unable to exeute query '{}'\n{};{}",statement, e.getMessage(), e.getCause()!=null?e.getCause().getMessage():""); 
		}
	}
	
	@Transactional(TransactionType.SINGLETON)
	public void createEntitiesForTest(){
		if(!entitiesCreated){
			Device device = new Device();
			device.setId("testdevice1");
			keyGenerator.generateAndStoreIn(device);
			device.setInitialCounter(System.currentTimeMillis());
			device.setStatus(Status.ACTIVE);
			DataStore dataStore = factory.getDataStore(totpOptions.getDataStoreName());
			dataStore.save(device);
			dataStore.flushBuffers();
			
			Alias alias = new Alias();
			alias.setName("testalias1");
			alias.setDevice(device);
			alias.getCheckValue().setDataStoreName(totpOptions.getDataStoreName());
			dataStore.save(alias);
			
			alias = new Alias();
			alias.setName("testaliasTampered");
			alias.setDevice(device);
			alias.getCheckValue().setDataStoreName(totpOptions.getDataStoreName());
			alias.getCheckValue().setStringValue("TAMPERED");
			dataStore.save(alias);
			
			device = new Device();
			device.setId("testinactivedevice1");
			keyGenerator.generateAndStoreIn(device);
			device.setInitialCounter(System.currentTimeMillis());
			device.setStatus(Status.INACTIVE);
			dataStore.save(device);
			dataStore.flushBuffers();
			
			alias = new Alias();
			alias.setName("testinactivedevice1alias");
			alias.setDevice(device);
			alias.getCheckValue().setDataStoreName(totpOptions.getDataStoreName());
			dataStore.save(alias);
			
			device = new Device();
			device.setId("testlockeddevice1");
			keyGenerator.generateAndStoreIn(device);
			device.setInitialCounter(System.currentTimeMillis());
			device.setStatus(Status.LOCKED);
			dataStore.save(device);
			dataStore.flushBuffers();
			
			device = new Device();
			device.setId("testunlockeddevice1");
			keyGenerator.generateAndStoreIn(device);
			device.setInitialCounter(System.currentTimeMillis());
			device.setStatus(Status.ACTIVE);
			dataStore.save(device);
			dataStore.flushBuffers();
			
			device = new Device();
			device.setId("testunlockeddevice2");
			keyGenerator.generateAndStoreIn(device);
			device.setInitialCounter(System.currentTimeMillis());
			device.setStatus(Status.ACTIVE);
			dataStore.save(device);
			dataStore.flushBuffers();
			
			device = new Device();
			device.setId("testadmindevice1");
			keyGenerator.generateAndStoreIn(device);
			device.setInitialCounter(System.currentTimeMillis());
			device.setStatus(Status.INITIATED);
			dataStore.save(device);
			dataStore.flushBuffers();
			
			Administrator administrator = new Administrator();
			administrator.setDevice(device);
			administrator.setCommonName("Adminstrator 3");
			administrator.getCheckValue().setDataStoreName(totpOptions.getDataStoreName());
			dataStore.save(administrator);
			
			device = new Device();
			device.setId("testdeactivatedevice1");
			keyGenerator.generateAndStoreIn(device);
			device.setStatus(Status.ACTIVE);
			device.setInitialCounter(System.currentTimeMillis());
			dataStore.save(device);
			dataStore.flushBuffers();
			
			administrator = new Administrator();
			administrator.setDevice(device);
			EncryptedValue checkValue = new EncryptedValue();
			checkValue.setStringValue("testtampereddevice");
			checkValue.setDataStoreName(totpOptions.getDataStoreName());
			administrator.setCheckValue(checkValue);
			administrator.setCommonName("Adminstrator 4");
			dataStore.save(administrator);
			entitiesCreated=true;
		}
	}
}
