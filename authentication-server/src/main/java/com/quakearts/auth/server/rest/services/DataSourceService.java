package com.quakearts.auth.server.rest.services;

import java.io.File;
import java.util.List;

public interface DataSourceService {
	List<String> getAllDataSources();
	void createUsing(File propertyFile);
	void removeIdentifiedBy(File dataSourceFile);
}
