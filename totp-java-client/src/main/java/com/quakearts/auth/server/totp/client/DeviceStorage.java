package com.quakearts.auth.server.totp.client;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.spec.InvalidKeySpecException;
import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.util.Properties;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;
import javax.security.auth.login.LoginException;

public class DeviceStorage {
	private static final String ETC = "etc";
	private static final String DEVICE_PROPERTIES = "device.properties";
	private static final String PROV = "BC";
	private static final String ALG = "PBEWithSHAAnd128BitAES-CBC-BC";
	private static final DeviceStorage instance = new DeviceStorage();
	
	public static DeviceStorage getInstance() {
		return instance;
	}
	
	private Device device;
	
	private DeviceStorage() {}
	
	public boolean hasBeenProvisioned(){
		File etc = new File(ETC);
		if(!etc.exists()){
			if(!etc.mkdir())
				throw new IllegalStateException("Unable to create required folder");
			
			return false;
		}
		return new File(ETC+File.separator+DEVICE_PROPERTIES).exists();
	}
	
	public void loadDevice(String password) throws LoginException{
		try(InputStream in = new CipherInputStream(new FileInputStream(ETC+File.separator+DEVICE_PROPERTIES), 
				initCipher(password, Cipher.DECRYPT_MODE))) {
			Properties properties = new Properties();
			properties.load(in);
			
			long initialCounter = Long.parseLong(properties.getProperty("initialCounter"));
			byte[] seed = HexTool.hexAsByte(properties.getProperty("seed"));
			String id = properties.getProperty("id");
			
			device = new Device(id, seed, initialCounter);
		} catch (GeneralSecurityException | IOException e) {
			throw new LoginException("The device could not be loaded: "+e.getMessage());
		}
	}

	public Device getDevice() {
		return device;
	}
	
	public void storeDevice(Device device, String password) throws LoginException{
		this.device = device;
		
		Properties properties = new Properties();
		properties.setProperty("initialCounter", Long.toString(device.getInitialCounter()));
		properties.setProperty("seed", HexTool.byteAsHex(device.getSeed()));
		properties.setProperty("id", device.getId());
	
		try(OutputStream out = new FileOutputStream(ETC+File.separator+DEVICE_PROPERTIES)){
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			properties.store(bos, MessageFormat.format("Saved on {0}", LocalDateTime.now()));
			Cipher cipher = initCipher(password, Cipher.ENCRYPT_MODE);
			out.write(cipher.doFinal(bos.toByteArray()));
			out.flush();
		} catch (GeneralSecurityException | IOException e) {
			throw new LoginException("The device could not b installed");
		}		
	}

	private Cipher initCipher(String password, int mode) throws NoSuchAlgorithmException, NoSuchProviderException,
			InvalidKeySpecException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException {
		SecretKeyFactory keyFactory = SecretKeyFactory.getInstance(ALG, PROV);
		PBEKeySpec pbeKeySpec = new PBEKeySpec(password.toCharArray());
		SecretKey secretKey = keyFactory.generateSecret(pbeKeySpec);
		Options options = Options.getInstance();
		PBEParameterSpec pbeParameterSpec = new PBEParameterSpec(options.getPbeSalt(), options.getPbeIterations());
		Cipher cipher = Cipher.getInstance(ALG, PROV);
		cipher.init(mode, secretKey, pbeParameterSpec);
		return cipher;
	}
}
