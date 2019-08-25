package com.quakearts.auth.server.totp.alternatives;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.function.Function;

import javax.annotation.Priority;
import javax.enterprise.inject.Alternative;
import javax.inject.Singleton;
import javax.interceptor.Interceptor;

import com.quakearts.auth.server.totp.channel.ConnectionManager;
import com.quakearts.auth.server.totp.exception.TOTPException;
import com.quakearts.auth.server.totp.function.CheckedConsumer;

@Alternative
@Priority(Interceptor.Priority.APPLICATION)
@Singleton
public class AlternativeConnectionManager implements ConnectionManager {

	private static Function<byte[], byte[]> execute = b->new byte[0];
	
	public static void run(Function<byte[], byte[]> newExecute) {
		execute = newExecute;
	}
	
	@Override
	public void send(byte[] bites, CheckedConsumer<byte[], TOTPException> callback) throws TOTPException {
		callback.accept(execute.apply(bites));
	}

	@Override
	public void init() throws IOException, NoSuchAlgorithmException, KeyStoreException, NoSuchProviderException,
			CertificateException, UnrecoverableKeyException, KeyManagementException {}

	@Override
	public void shutdown() {}

}
