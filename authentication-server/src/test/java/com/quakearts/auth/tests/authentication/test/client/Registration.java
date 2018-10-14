package com.quakearts.auth.tests.authentication.test.client;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.validation.Valid;

import com.quakearts.auth.server.rest.models.LoginConfiguration;
import com.quakearts.auth.server.rest.models.LoginConfigurationEntry;
import com.quakearts.auth.server.rest.models.LoginConfigurationEntry.ModuleFlag;

public class Registration implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -1665076079629454098L;
	private String id;
	private String alias;
	private List<LoginConfiguration> configurations = new ArrayList<>();
	@Valid
	private Map<String, String> options = new HashMap<>();

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public Registration setIdAs(String id) {
		setId(id);
		return this;
	}
	
	public String getAlias() {
		return alias;
	}

	public void setAlias(String alias) {
		this.alias = alias;
	}

	public Registration setAliasAs(String alias) {
		setAlias(alias);
		return this;
	}
	
	public List<LoginConfiguration> getConfigurations() {
		return configurations;
	}

	public void setConfigurations(List<LoginConfiguration> configurations) {
		this.configurations = configurations;
	}
	
	public Map<String, String> getOptions() {
		return options;
	}

	public void setOptions(Map<String, String> options) {
		this.options = options;
	}

	public Registration addOption(String key, String value) {
		options.put(key, value);
		return this;
	}
	
	public LoginConfigurationBuilder createConfiguration() {
		return new LoginConfigurationBuilder(new LoginConfiguration());
	}
	
	public class LoginConfigurationBuilder {
		private LoginConfiguration configuration;
		
		public LoginConfigurationBuilder(LoginConfiguration configuration) {
			this.configuration = configuration;
		}
		
		public LoginConfigurationBuilder setNameAs(String name) {
			configuration.setName(name);
			return this;
		}
		
		public LoginConfigurationEntryBuilder createEntry() {
			return new LoginConfigurationEntryBuilder(new LoginConfigurationEntry(), this);
		}
		
		public Registration thenAdd() {
			configurations.add(configuration);
			return Registration.this;
		}
	}

	public class LoginConfigurationEntryBuilder {
		private LoginConfigurationEntry entry;
		private LoginConfigurationBuilder configurationBuilder;
		
		public LoginConfigurationEntryBuilder(LoginConfigurationEntry entry,
				LoginConfigurationBuilder configurationBuilder) {
			this.entry = entry;
			this.configurationBuilder = configurationBuilder;
		}
	
		public LoginConfigurationEntryBuilder setModuleClassnameAs(String moduleClassname) {
			entry.setModuleClassname(moduleClassname);
			return this;
		}
		
		public LoginConfigurationEntryBuilder addOption(String key, String value) {
			entry.getOptions().put(key, value);
			return this;
		}
		
		public LoginConfigurationEntryBuilder setModuleFlagAs(ModuleFlag moduleFlag) {
			entry.setModuleFlag(moduleFlag);
			return this;
		}
		
		public LoginConfigurationBuilder thenAdd() {
			configurationBuilder.configuration.getEntries().add(entry);
			return configurationBuilder;
		}
	}

}
