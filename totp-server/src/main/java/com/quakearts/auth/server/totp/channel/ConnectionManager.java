package com.quakearts.auth.server.totp.channel;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import com.quakearts.auth.server.totp.exception.UnconnectedDeviceException;

public interface ConnectionManager {
	byte[] send(byte[] bites) throws UnconnectedDeviceException;
	void init() throws IOException, NoSuchAlgorithmException, KeyStoreException, NoSuchProviderException,
			CertificateException, UnrecoverableKeyException, KeyManagementException;
	void shutdown();
}
