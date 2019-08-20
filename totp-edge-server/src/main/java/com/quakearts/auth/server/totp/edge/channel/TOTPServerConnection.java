package com.quakearts.auth.server.totp.edge.channel;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

public interface TOTPServerConnection {
	void init() throws IOException, KeyStoreException, 
		NoSuchAlgorithmException, CertificateException, UnrecoverableKeyException, KeyManagementException;
	void shutdown();
}
