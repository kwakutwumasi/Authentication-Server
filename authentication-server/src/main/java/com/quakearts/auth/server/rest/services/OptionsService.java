package com.quakearts.auth.server.rest.services;

import java.util.Map;

public interface OptionsService {

	Map<String, String> buildOptions(Map<String, String> options);
	void resolveSecrets(Map<String, String> options);

}