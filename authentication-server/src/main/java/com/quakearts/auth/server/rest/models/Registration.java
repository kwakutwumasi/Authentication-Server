package com.quakearts.auth.server.rest.models;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.validation.Valid;

import com.quakearts.auth.server.rest.models.LoginConfigurationEntry.ModuleFlag;
import com.quakearts.auth.server.rest.validators.annotation.ValidLoginConfiguration;
import com.quakearts.auth.server.rest.validators.annotation.ValidRegistrationOption;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

public class Registration implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -1665076079629454098L;
	@Schema(description="A unique ID for registration. This should be as long as possible. "
					  + "It should be kept secret to prevent malicious users from seeing "
					  + "sensitive security information such as database passwords and secret keys",
					  required=true)
	private String id;
	@Schema(description="A unique name to use when identifying the registration during authentication. "
					  + "It can be the domain name of the server, or any other unique name",
					  required=true)
	private String alias;
	
	@ArraySchema(minItems=1, 
				schema=@Schema(description="A list of authentication modules and their configuration",
				required=true,
				implementation=LoginConfiguration.class))
	@Valid
	private List<@ValidLoginConfiguration LoginConfiguration> configurations = new ArrayList<>();
	@Schema(description="A list of options for use during token generation.", 
			type="object",
			example="\"options\":{\n" + 
					"      \"audience\":\"https://demo.quakearts.com\",\n" + 
					"      \"validity.period\":\"1 Day\",\n" + 
					"      \"secret\":\"W@h8237HksIhfmsd2Nl94WNCA\",\n" + 
					"      \"issuer\":\"https://quakearts.com\"\n" + 
					"   }")
	@Valid
	private Map<@ValidRegistrationOption String, String> options = new HashMap<>();

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
		if(options!=null)
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

	@Override
	public String toString() {
		return "Registration [id=" + id + ", alias=" + alias + ", configurations=" + configurations + ", options="
				+ options + "]";
	}
}
