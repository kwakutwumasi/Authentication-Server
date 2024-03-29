package com.quakearts.auth.server.totp;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import javax.inject.Inject;

import com.quakearts.appbase.Main;
import com.quakearts.appbase.exception.ConfigurationException;
import com.quakearts.auth.server.totp.channel.ConnectionManager;

public class InitMain {
	
	@Inject
	private ConnectionManager connectionManager;
	
	public void init() {
		if(Boolean.parseBoolean(System.getProperty("totp.edge.server.active","true"))){
			try {
				Main.log.debug("Connection Manager starting...");
				connectionManager.init();
				Main.log.debug("Connection Manager started");
			} catch (IOException | UnrecoverableKeyException | KeyManagementException | NoSuchAlgorithmException
					| KeyStoreException | NoSuchProviderException | CertificateException e) {
				throw new ConfigurationException(e);
			}
		}
	}
}
