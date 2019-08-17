package com.quakearts.auth.server.totp;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import javax.inject.Inject;

import com.quakearts.appbase.exception.ConfigurationException;
import com.quakearts.auth.server.totp.channel.ConnectionManager;

public class InitMain {
	
	@Inject
	private ConnectionManager connectionManager;
	
	public void init() {
		try {
			connectionManager.init();
		} catch (IOException | UnrecoverableKeyException | KeyManagementException | NoSuchAlgorithmException
				| KeyStoreException | NoSuchProviderException | CertificateException e) {
			throw new ConfigurationException(e);
		}
	}
}
