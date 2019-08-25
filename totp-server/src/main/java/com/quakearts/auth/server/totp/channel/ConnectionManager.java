package com.quakearts.auth.server.totp.channel;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import com.quakearts.auth.server.totp.exception.TOTPException;
import com.quakearts.auth.server.totp.function.CheckedConsumer;

public interface ConnectionManager {
	void send(byte[] bites, CheckedConsumer<byte[], TOTPException> callback) throws TOTPException;
	void init() throws IOException, NoSuchAlgorithmException, KeyStoreException, NoSuchProviderException,
			CertificateException, UnrecoverableKeyException, KeyManagementException;
	void shutdown();
}
