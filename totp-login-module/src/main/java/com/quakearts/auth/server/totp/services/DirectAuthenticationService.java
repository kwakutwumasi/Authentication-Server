package com.quakearts.auth.server.totp.services;

import java.security.SecureRandom;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class DirectAuthenticationService {
	private DirectAuthenticationService() {}
	
	private static final DirectAuthenticationService instance = new DirectAuthenticationService();
	
	public static DirectAuthenticationService getInstance() {
		return instance;
	}
	
	private static final Map<String, String> authenticationId = new ConcurrentHashMap<>();
	private static final Map<String, FallbackTokenBroker> fallbackTokenBrokers = new ConcurrentHashMap<>();
	
	public String generateID(String deviceId) {
		return authenticationId.computeIfAbsent(deviceId, key->generateRandomID());
	}
	
	public void removeID(String deviceId) {
		authenticationId.remove(deviceId);
	}
	
	private SecureRandom secureRandom = new SecureRandom();
	
	// 0 - 9 : 48
	// A - Z : 65
	
	private String generateRandomID() {
		char[] randomID = new char[getCodeLength()];
		for(int i=0;i<randomID.length;i++) {
			char c;
			if(secureRandom.nextInt()%2==0){
				c = (char) (48+(Math.abs(secureRandom.nextInt()%9)));
			} else {
				c = (char) (65+(Math.abs(secureRandom.nextInt()%25)));
			}
			randomID[i] = c;
		}
		return new String(randomID);
	}
	
	private Integer codeLength;
	
	public Integer getCodeLength() {
		if(codeLength == null) {
			try {
				codeLength = Integer.parseInt(System.getProperty("authentication.id.length", "6"));
			} catch (Exception e) {
				codeLength = 6;
			}
		}
		return codeLength;
	}

	public String getFallbackToken(String username) {
		return getFallbackTokenBroker(username)
				.fetch();
	}
	
	public void putFallbackToken(String username, String token){
		getFallbackTokenBroker(username).put(token);
	}

	public void setFallbackListener(String username, FallbackListener listener){
		getFallbackTokenBroker(username).setListener(listener);
	}

	private FallbackTokenBroker getFallbackTokenBroker(String username) {
		if(fallbackTokenBrokers.size()>Integer.parseInt(System.getProperty("fallback.token.broker.max.size", "100"))){
			CompletableFuture.runAsync(this::pruneFallbackTokenBrokers);
		}
		
		return fallbackTokenBrokers.computeIfAbsent(username, key->new FallbackTokenBroker());
	}
	
	private void pruneFallbackTokenBrokers(){
		fallbackTokenBrokers.entrySet().removeIf(entry->entry.getValue().hasExpired());
	}
	
	class FallbackTokenBroker {
		static final String DEFAULT = "XXXXXX";
		String token = DEFAULT;
		FallbackListener listener;
		long timestamp = System.currentTimeMillis();
		
		public void setListener(FallbackListener listener) {
			this.listener = listener;
		}
		
		synchronized void put(String token){
			this.token = token;
			notifyAll();
		}
		
		String fetch() {
			notifyListener();
			waitForTokenIfNeccessary();
			return andReset();
		}

		private void notifyListener() {
			if(listener != null) {
				listener.fallbackRequested();
			}
		}

		private synchronized void waitForTokenIfNeccessary() {
			long start = System.currentTimeMillis();
			while(token.equals(DEFAULT)
					&& System.currentTimeMillis()-start<getFallBackTimeout()){
				try {
					wait(getFallBackTimeout());
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			}
		}

		private String andReset() {
			String toReturn = token;
			token = DEFAULT;
			return toReturn;
		}
		
		private boolean hasExpired(){
			return System.currentTimeMillis()-timestamp>getFallBackTimeout();
		}
	}
	
	private Long fallBackTimeout;
	
	public Long getFallBackTimeout() {
		if(fallBackTimeout == null) {
			try {
				fallBackTimeout = Long.parseLong(System.getProperty("authentication.fallback.timeout", "60000"));
			} catch (Exception e) {
				fallBackTimeout = 20000l;
			}
		}
		return fallBackTimeout;
	}
}
