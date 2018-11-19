package com.quakearts.auth.server.rest.models;

import java.util.HashMap;
import java.util.Map;
import javax.security.auth.login.AppConfigurationEntry;

public class Configuration 
	extends javax.security.auth.login.Configuration {
	Map<String, AppConfigurationEntry[]> entriesMap = new HashMap<>();
	
	public Configuration(Registration registration) {
		for(LoginConfiguration configuration:registration.getConfigurations()) {
			AppConfigurationEntry[] entries = new AppConfigurationEntry[configuration.getEntries().size()];
			int index = 0;
			for(LoginConfigurationEntry entry:configuration.getEntries()) {
				entries[index] = new AppConfigurationEntry(entry.getModuleClassname(), 
									entry.getModuleFlag().getFlag(), 
									entry.getOptions());
				index++;
			}
			
			entriesMap.put(configuration.getName(), entries);
		}
	}
	
	@Override
	public AppConfigurationEntry[] getAppConfigurationEntry(String name) {
		return entriesMap.get(name);
	}

}
