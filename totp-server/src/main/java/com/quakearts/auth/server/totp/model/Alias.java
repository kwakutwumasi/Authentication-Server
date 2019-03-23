package com.quakearts.auth.server.totp.model;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import com.quakearts.security.cryptography.jpa.EncryptedValue;
import com.quakearts.security.cryptography.jpa.EncryptedValueStringConverter;
import com.quakearts.webapp.security.util.HashPassword;

@Entity
public class Alias implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -5644993216169476713L;
	@Id
	@Column(length=250)
	private String name;
	@Column(length=250, nullable=false)
	@Convert(converter = EncryptedValueStringConverter.class)
	private EncryptedValue checkValue;
	@ManyToOne(optional = false, fetch = FetchType.EAGER)
	private Device device;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
		addCheck();
	}

	public Device getDevice() {
		return device;
	}

	public void setDevice(Device device) {
		this.device = device;
		addCheck();
	}

	private void addCheck() {
		if(checkValue==null && device!=null && name!=null){
			checkValue = generateCheck();
		}
	}

	public EncryptedValue getCheckValue() {
		return checkValue;
	}

	public void setCheckValue(EncryptedValue check) {
		this.checkValue = check;
	}
	
	public boolean notTamperedWith(){
		return name!=null && device!=null && checkValue!=null
				&& checkValue.getStringValue()
				.equals(generateCheck().getStringValue());
	}

	private EncryptedValue generateCheck() {
		EncryptedValue encryptedValue = new EncryptedValue();
		encryptedValue.setStringValue(new HashPassword(name, "SHA-256", 3, device.getId()).toString());
		return encryptedValue;
	}
}
