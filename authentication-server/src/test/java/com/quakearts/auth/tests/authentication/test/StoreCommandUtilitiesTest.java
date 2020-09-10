package com.quakearts.auth.tests.authentication.test;

import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.InputStream;
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
		
		impl.getRegistryCache().clear();
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
		impl.getRegistryCache().put(registration1.getId(), registration1);
		impl.getRegistryCache().put(registration2.getId(), registration2);
		impl.getRegistryCache().put(registration3.getId(), registration3);
	}
	
	@Test
	public void test() throws Exception {
		String expectedOut;
		try(InputStream in = new FileInputStream("expected.txt")) {
			ByteArrayOutputStream stream = new ByteArrayOutputStream();
			int read;
			while((read = in.read())!=-1) {
				stream.write(read);
			}
			expectedOut = new String(stream.toByteArray());
		}
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
		impl.getRegistryCache().clear();		
	}
}
