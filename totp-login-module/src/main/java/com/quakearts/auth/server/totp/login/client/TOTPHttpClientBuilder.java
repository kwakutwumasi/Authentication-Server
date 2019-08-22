package com.quakearts.auth.server.totp.login.client;

import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.quakearts.rest.client.HttpClientBuilder;

public class TOTPHttpClientBuilder 
	extends HttpClientBuilder<TOTPHttpClient> {
	
	public TOTPHttpClientBuilder() {
		httpClient = new TOTPHttpClient();
	}
	
	public HttpClientBuilder<TOTPHttpClient> setFileAs(String file) {
		httpClient.file = file;
		return this;
	}
	
	@Override
	public HttpClientBuilder<TOTPHttpClient> setURLAs(URL url) {
		setFileAs(url.getFile());
		return super.setURLAs(url);
	}
	
	@Override
	public TOTPHttpClient thenBuild() {
		return httpClient;
	}
	
	private static Map<String, TOTPHttpClient> produced = new ConcurrentHashMap<>();
	
	public static TOTPHttpClient createTOTPServerHttpClient(String url) {
		if(produced.containsKey(url)) {
			return produced.get(url);
		}
		
		TOTPHttpClient client = new TOTPHttpClientBuilder()
				.setURLAs(url)
				.thenBuild();
		
		produced.put(url, client);
		
		return client;
	}
}
