package com.quakearts.auth.server.rest.services;

import java.io.File;
import java.io.IOException;

public interface FileService {
	File createFile(String root, String filename);
	void saveObjectToFile(Object value, File file) throws IOException;
	void deleteFile(File file) throws IOException;
	boolean fileExists(File file);
}