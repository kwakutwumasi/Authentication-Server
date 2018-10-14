package com.quakearts.auth.server.rest.models;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import javax.security.auth.login.AppConfigurationEntry.LoginModuleControlFlag;
import io.swagger.v3.oas.annotations.media.Schema;

public class LoginConfigurationEntry implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 9187062519254714504L;

	@Schema(description="The full class name of the login module implementation",
			required=true)
	private String moduleClassname;
	@Schema(description="The module flag. See JAAS documentation for more information on module flags and their implications",
			required=true)
	private ModuleFlag moduleFlag;
	public enum ModuleFlag {
		REQUIRED(LoginModuleControlFlag.REQUIRED), 
		REQUISITE(LoginModuleControlFlag.REQUISITE), 
		SUFFICIENT(LoginModuleControlFlag.SUFFICIENT), 
		OPTIONAL(LoginModuleControlFlag.OPTIONAL);

		private LoginModuleControlFlag flag;

		public LoginModuleControlFlag getFlag() {
			return flag;
		}

		private ModuleFlag(LoginModuleControlFlag flag) {
			this.flag = flag;
		}
	}

	private Map<String, String> options = new HashMap<>();

	public String getModuleClassname() {
		return moduleClassname;
	}

	public void setModuleClassname(String moduleClassname) {
		this.moduleClassname = moduleClassname;
	}

	public ModuleFlag getModuleFlag() {
		return moduleFlag;
	}

	public void setModuleFlag(ModuleFlag moduleFlag) {
		this.moduleFlag = moduleFlag;
	}

	public Map<String, String> getOptions() {
		return options;
	}

	public void setOptions(Map<String, String> options) {
		this.options = options;
	}
}