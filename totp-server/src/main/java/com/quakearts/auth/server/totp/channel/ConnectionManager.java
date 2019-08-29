package com.quakearts.auth.server.totp.channel;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.function.Consumer;

import com.quakearts.auth.server.totp.exception.TOTPException;

public interface ConnectionManager {
	void send(byte[] bites, Consumer<byte[]> callback) throws TOTPException;
	void init() throws IOException, NoSuchAlgorithmException, KeyStoreException, NoSuchProviderException,
			CertificateException, UnrecoverableKeyException, KeyManagementException;
	void shutdown();
}
