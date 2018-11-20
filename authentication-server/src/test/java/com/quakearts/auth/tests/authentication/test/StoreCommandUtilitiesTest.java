package com.quakearts.auth.tests.authentication.test;

import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.quakearts.auth.server.rest.models.LoginConfigurationEntry.ModuleFlag;
import com.quakearts.auth.server.rest.models.Registration;
import com.quakearts.auth.server.store.impl.RegistryStoreManagerImpl;
import com.quakearts.auth.server.store.utilities.StoreCommandUtilities;
import com.quakearts.utilities.CommandMain;

public class StoreCommandUtilitiesTest {

	@BeforeClass
	public static void populateCache() {
		RegistryStoreManagerImpl impl = new RegistryStoreManagerImpl();
		impl.getAliasCache().clear();
		impl.getAliasCache().put("Test1", Integer.toHexString("Test1".hashCode()));
		impl.getAliasCache().put("Test2", Integer.toHexString("Test2".hashCode()));
		
		impl.getSecretsCache().clear();
		impl.getSecretsCache().put("Secrets1", Integer.toHexString("Secrets1".hashCode()));
		impl.getSecretsCache().put("Secrets2", Integer.toHexString("Secrets2".hashCode()));
		
		impl.getCache().clear();
		Registration registration1 = new Registration().setIdAs("YB293SJ")
					.setAliasAs("Test1")
					.createConfiguration()
						.setNameAs("Test-Config1")
						.createEntry()
							.addOption("option1", "value1")
							.setModuleClassnameAs("TestClass")
							.setModuleFlagAs(ModuleFlag.REQUIRED)
						.thenAdd()
					.thenAdd();
		
		Registration registration2 = new Registration().setIdAs("YB283SJ")
				.setAliasAs("Test1")
				.createConfiguration()
					.setNameAs("Test-Config1")
					.createEntry()
						.addOption("option1", "value1")
						.setModuleClassnameAs("TestClass")
						.setModuleFlagAs(ModuleFlag.REQUIRED)
					.thenAdd()
				.thenAdd();
		
		Registration registration3 = new Registration().setIdAs("YB593SJ")
				.setAliasAs("Test1")
				.createConfiguration()
					.setNameAs("Test-Config1")
					.createEntry()
						.addOption("option1", "value1")
						.setModuleClassnameAs("TestClass")
						.setModuleFlagAs(ModuleFlag.REQUIRED)
					.thenAdd()
				.thenAdd();
		impl.getCache().put(registration1.getId(), registration1);
		impl.getCache().put(registration2.getId(), registration2);
		impl.getCache().put(registration3.getId(), registration3);
	}
	
	private String expectedOut = "Registrations:\n\tSize: 3\n" + 
			"Aliases:\n\tSize: 2\n" + 
			"Secrets:\n\tSize: 2\n" +
			"Size: 3\n"+
			"Size: 2\n"+
			"Size: 2\n"+
			"Registrations:\n\tImmortalCacheEntry{key=YB593SJ, value=Registration [id=YB593SJ, alias=Test1, configurations=[LoginConfiguration [name=Test-Config1, entries=[LoginConfigurationEntry [moduleClassname=TestClass, moduleFlag=REQUIRED, options={option1=value1}]]]], options={}]}\n" + 
			"\tImmortalCacheEntry{key=YB283SJ, value=Registration [id=YB283SJ, alias=Test1, configurations=[LoginConfiguration [name=Test-Config1, entries=[LoginConfigurationEntry [moduleClassname=TestClass, moduleFlag=REQUIRED, options={option1=value1}]]]], options={}]}\n" + 
			"\tImmortalCacheEntry{key=YB293SJ, value=Registration [id=YB293SJ, alias=Test1, configurations=[LoginConfiguration [name=Test-Config1, entries=[LoginConfigurationEntry [moduleClassname=TestClass, moduleFlag=REQUIRED, options={option1=value1}]]]], options={}]}\n" + 
			"Aliases:\n\tImmortalCacheEntry{key=Test1, value=4cf5dbf}\n" + 
			"\tImmortalCacheEntry{key=Test2, value=4cf5dc0}\n" + 
			"Secrets:\n\tImmortalCacheEntry{key=Secrets1, value=3c3cd4ee}\n" + 
			"\tImmortalCacheEntry{key=Secrets2, value=3c3cd4ef}\n" + 
			"Registrations:\n\tYB593SJ\n" + 
			"\tYB283SJ\n" + 
			"\tYB293SJ\n" + 
			"Aliases:\n\tTest1\n" + 
			"\tTest2\n" + 
			"Secrets:\n\tSecrets1\n" + 
			"\tSecrets2\n" + 
			"Registrations:\n\tRegistration [id=YB593SJ, alias=Test1, configurations=[LoginConfiguration [name=Test-Config1, entries=[LoginConfigurationEntry [moduleClassname=TestClass, moduleFlag=REQUIRED, options={option1=value1}]]]], options={}]\n" + 
			"\tRegistration [id=YB283SJ, alias=Test1, configurations=[LoginConfiguration [name=Test-Config1, entries=[LoginConfigurationEntry [moduleClassname=TestClass, moduleFlag=REQUIRED, options={option1=value1}]]]], options={}]\n" + 
			"\tRegistration [id=YB293SJ, alias=Test1, configurations=[LoginConfiguration [name=Test-Config1, entries=[LoginConfigurationEntry [moduleClassname=TestClass, moduleFlag=REQUIRED, options={option1=value1}]]]], options={}]\n" + 
			"Aliases:\n\t4cf5dbf\n" + 
			"\t4cf5dc0\n" + 
			"Secrets:\n\t3c3cd4ee\n" + 
			"\t3c3cd4ef\n" + 
			"ImmortalCacheEntry{key=YB593SJ, value=Registration [id=YB593SJ, alias=Test1, configurations=[LoginConfiguration [name=Test-Config1, entries=[LoginConfigurationEntry [moduleClassname=TestClass, moduleFlag=REQUIRED, options={option1=value1}]]]], options={}]}\n" + 
			"ImmortalCacheEntry{key=YB283SJ, value=Registration [id=YB283SJ, alias=Test1, configurations=[LoginConfiguration [name=Test-Config1, entries=[LoginConfigurationEntry [moduleClassname=TestClass, moduleFlag=REQUIRED, options={option1=value1}]]]], options={}]}\n" + 
			"ImmortalCacheEntry{key=YB293SJ, value=Registration [id=YB293SJ, alias=Test1, configurations=[LoginConfiguration [name=Test-Config1, entries=[LoginConfigurationEntry [moduleClassname=TestClass, moduleFlag=REQUIRED, options={option1=value1}]]]], options={}]}\n" + 
			"ImmortalCacheEntry{key=Test1, value=4cf5dbf}\n" + 
			"ImmortalCacheEntry{key=Test2, value=4cf5dc0}\n" + 
			"ImmortalCacheEntry{key=Secrets1, value=3c3cd4ee}\n" + 
			"ImmortalCacheEntry{key=Secrets2, value=3c3cd4ef}\n" + 
			"Registration [id=YB293SJ, alias=Test1, configurations=[LoginConfiguration [name=Test-Config1, entries=[LoginConfigurationEntry [moduleClassname=TestClass, moduleFlag=REQUIRED, options={option1=value1}]]]], options={}]\n" + 
			"4cf5dbf\n" + 
			"3c3cd4ee\n";
	
	@Test
	public void test() {
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		PrintStream printWriter = new PrintStream(stream);
		PrintStream oldOut = System.out;
		System.setOut(printWriter);
		try {
			CommandMain.main(new String[] {StoreCommandUtilities.class.getName()});
			CommandMain.main(new String[] {StoreCommandUtilities.class.getName(),"-store","Registrations"});
			CommandMain.main(new String[] {StoreCommandUtilities.class.getName(),"-store","Aliases"});
			CommandMain.main(new String[] {StoreCommandUtilities.class.getName(),"-store","Secrets"});
			CommandMain.main(new String[] {StoreCommandUtilities.class.getName(),"-list","all"});
			CommandMain.main(new String[] {StoreCommandUtilities.class.getName(),"-list","key"});
			CommandMain.main(new String[] {StoreCommandUtilities.class.getName(),"-list","value"});
			CommandMain.main(new String[] {StoreCommandUtilities.class.getName(),"-list","all","-store","Registrations"});
			CommandMain.main(new String[] {StoreCommandUtilities.class.getName(),"-list","all","-store","Aliases"});
			CommandMain.main(new String[] {StoreCommandUtilities.class.getName(),"-list","all","-store","Secrets"});
			CommandMain.main(new String[] {StoreCommandUtilities.class.getName(),"-key","YB293SJ","-store","Registrations"});
			CommandMain.main(new String[] {StoreCommandUtilities.class.getName(),"-key","Test1","-store","Aliases"});
			CommandMain.main(new String[] {StoreCommandUtilities.class.getName(),"-key","Secrets1","-store","Secrets"});
			CommandMain.main(new String[] {StoreCommandUtilities.class.getName(),"-key","Secrets1"});
			
			assertEquals(expectedOut, new String(stream.toByteArray()));
		} finally {
			System.setOut(oldOut);
		}
	}

	@AfterClass
	public static void clearCache() {
		RegistryStoreManagerImpl impl = new RegistryStoreManagerImpl();
		impl.getAliasCache().clear();
		impl.getSecretsCache().clear();
		impl.getCache().clear();		
	}
}
