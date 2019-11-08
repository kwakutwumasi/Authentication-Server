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
		Registration registration1 = new Registration().withIdAs("YB293SJ")
					.withAliasAs("Test1")
					.createConfiguration()
						.setNameAs("Test-Config1")
						.createEntry()
							.addOption("option1", "value1")
							.setModuleClassnameAs("TestClass")
							.setModuleFlagAs(ModuleFlag.REQUIRED)
						.thenAdd()
					.thenAdd();
		
		Registration registration2 = new Registration().withIdAs("YB283SJ")
				.withAliasAs("Test1")
				.createConfiguration()
					.setNameAs("Test-Config1")
					.createEntry()
						.addOption("option1", "value1")
						.setModuleClassnameAs("TestClass")
						.setModuleFlagAs(ModuleFlag.REQUIRED)
					.thenAdd()
				.thenAdd();
		
		Registration registration3 = new Registration().withIdAs("YB593SJ")
				.withAliasAs("Test1")
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
	
	private String expectedOut = "Registrations:\n" + 
			"	Size: 3\n" + 
			"Aliases:\n" + 
			"	Size: 2\n" + 
			"Secrets:\n" + 
			"	Size: 2\n" + 
			"Size: 3\n" + 
			"Size: 2\n" + 
			"Size: 2\n" + 
			"Registrations:\n" + 
			"	ImmortalCacheEntry{key=YB593SJ, value=Registration [id=YB593SJ, alias=Test1, configurations=[LoginConfiguration [name=Test-Config1, entries=[LoginConfigurationEntry [moduleClassname=TestClass, moduleFlag=REQUIRED, options={option1=value1}]]]], options={}]}\n" + 
			"	ImmortalCacheEntry{key=YB283SJ, value=Registration [id=YB283SJ, alias=Test1, configurations=[LoginConfiguration [name=Test-Config1, entries=[LoginConfigurationEntry [moduleClassname=TestClass, moduleFlag=REQUIRED, options={option1=value1}]]]], options={}]}\n" + 
			"	ImmortalCacheEntry{key=YB293SJ, value=Registration [id=YB293SJ, alias=Test1, configurations=[LoginConfiguration [name=Test-Config1, entries=[LoginConfigurationEntry [moduleClassname=TestClass, moduleFlag=REQUIRED, options={option1=value1}]]]], options={}]}\n" + 
			"Aliases:\n" + 
			"	ImmortalCacheEntry{key=Test2, value=4cf5dc0}\n" + 
			"	ImmortalCacheEntry{key=Test1, value=4cf5dbf}\n" + 
			"Secrets:\n" + 
			"	ImmortalCacheEntry{key=Secrets1, value=3c3cd4ee}\n" + 
			"	ImmortalCacheEntry{key=Secrets2, value=3c3cd4ef}\n" + 
			"Registrations:\n" + 
			"	YB593SJ\n" + 
			"	YB283SJ\n" + 
			"	YB293SJ\n" + 
			"Aliases:\n" + 
			"	Test2\n" + 
			"	Test1\n" + 
			"Secrets:\n" + 
			"	Secrets1\n" + 
			"	Secrets2\n" + 
			"Registrations:\n" + 
			"	Registration [id=YB593SJ, alias=Test1, configurations=[LoginConfiguration [name=Test-Config1, entries=[LoginConfigurationEntry [moduleClassname=TestClass, moduleFlag=REQUIRED, options={option1=value1}]]]], options={}]\n" + 
			"	Registration [id=YB283SJ, alias=Test1, configurations=[LoginConfiguration [name=Test-Config1, entries=[LoginConfigurationEntry [moduleClassname=TestClass, moduleFlag=REQUIRED, options={option1=value1}]]]], options={}]\n" + 
			"	Registration [id=YB293SJ, alias=Test1, configurations=[LoginConfiguration [name=Test-Config1, entries=[LoginConfigurationEntry [moduleClassname=TestClass, moduleFlag=REQUIRED, options={option1=value1}]]]], options={}]\n" + 
			"Aliases:\n" + 
			"	4cf5dc0\n" + 
			"	4cf5dbf\n" + 
			"Secrets:\n" + 
			"	3c3cd4ee\n" + 
			"	3c3cd4ef\n" + 
			"ImmortalCacheEntry{key=YB593SJ, value=Registration [id=YB593SJ, alias=Test1, configurations=[LoginConfiguration [name=Test-Config1, entries=[LoginConfigurationEntry [moduleClassname=TestClass, moduleFlag=REQUIRED, options={option1=value1}]]]], options={}]}\n" + 
			"ImmortalCacheEntry{key=YB283SJ, value=Registration [id=YB283SJ, alias=Test1, configurations=[LoginConfiguration [name=Test-Config1, entries=[LoginConfigurationEntry [moduleClassname=TestClass, moduleFlag=REQUIRED, options={option1=value1}]]]], options={}]}\n" + 
			"ImmortalCacheEntry{key=YB293SJ, value=Registration [id=YB293SJ, alias=Test1, configurations=[LoginConfiguration [name=Test-Config1, entries=[LoginConfigurationEntry [moduleClassname=TestClass, moduleFlag=REQUIRED, options={option1=value1}]]]], options={}]}\n" + 
			"ImmortalCacheEntry{key=Test2, value=4cf5dc0}\n" + 
			"ImmortalCacheEntry{key=Test1, value=4cf5dbf}\n" + 
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
