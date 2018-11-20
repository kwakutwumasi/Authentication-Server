package com.quakearts.auth.tests.authentication.test;

import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.NoSuchFileException;

import javax.inject.Inject;
import static org.hamcrest.core.Is.*;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import com.quakearts.auth.server.rest.models.Registration;
import com.quakearts.auth.server.rest.services.impl.FileServiceImpl;

@RunWith(MainRunner.class)
public class FileServiceImplTest {

	@Inject
	private FileServiceImpl serviceImpl;
	
	@Test
	public void testCreateFile() throws Exception {
		File file = serviceImpl.createFile("etc", "default.ds.json");
		assertThat(file.exists(), is(true));
		assertThat(file.exists(), is(serviceImpl.fileExists(file)));
		file = File.createTempFile("test.delete.", ".json");
		assertThat(file.exists(), is(true));
		assertThat(file.exists(), is(serviceImpl.fileExists(file)));
		serviceImpl.deleteFile(file);
		assertThat(file.exists(), is(false));
		assertThat(file.exists(), is(serviceImpl.fileExists(file)));
	}

	@Test
	public void testSaveObjectToFile() throws Exception {
		File file = File.createTempFile("test.file.", ".json");
		Registration registration = new Registration().withIdAs("test");
		serviceImpl.saveObjectToFile(registration, file);
		
		StringBuilder builder = new StringBuilder();
		try(InputStream stream = new FileInputStream(file)){
			BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
			String line;
			while ((line = reader.readLine())!=null) {
				builder.append(line).append("\n");
			}
		}
		
		assertThat(builder.toString(), is("{\"id\":\"test\",\"configurations\":[],\"options\":{}}\n"));
	}

	@Rule
	public ExpectedException expectedException = ExpectedException.none();

	@Test
	public void testDeleteFileException() throws Exception {
		expectedException.expect(NoSuchFileException.class);
		expectedException.expectMessage(is("doesnotexist.txt"));

		serviceImpl.deleteFile(new File("doesnotexist.txt"));
	}

}
