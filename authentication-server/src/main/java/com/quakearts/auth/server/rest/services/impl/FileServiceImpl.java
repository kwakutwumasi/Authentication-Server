package com.quakearts.auth.server.rest.services.impl;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import javax.inject.Singleton;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quakearts.auth.server.rest.services.FileService;

@Singleton
public class FileServiceImpl implements FileService {

	private ObjectMapper mapper = new ObjectMapper()
				.setSerializationInclusion(Include.NON_NULL);
	
	@Override
	public File createFile(String root, String filename) {
		File dsLocation = new File(root);
		File dataSourceFile = new File(dsLocation, filename);
		return dataSourceFile;
	}

	@Override
	public void saveObjectToFile(Object value, File file) throws IOException {
		try(OutputStream os = new FileOutputStream(file)) {
			mapper.writeValue(os, value);
		}
	}

	@Override
	public void deleteFile(File file) throws IOException {
		if(!file.delete())
			throw new IOException("File "+file.getName()+" could not be deleted");
	}

	@Override
	public boolean fileExists(File file) {
		return file.exists();
	}
}
