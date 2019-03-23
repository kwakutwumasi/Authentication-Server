package com.quakearts.auth.server.totp.model;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import com.quakearts.security.cryptography.jpa.EncryptedValue;
import com.quakearts.security.cryptography.jpa.EncryptedValueStringConverter;

@Entity
public class Administrator implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -5761570268167719558L;
	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	private long id;
	@Convert(converter = EncryptedValueStringConverter.class)
	@Column(unique=true, nullable=false)
	private EncryptedValue checkValue;
	@Column(unique=true, nullable=false)
	private String commonName;
	@ManyToOne(optional = false, fetch = FetchType.EAGER)
	private Device device;

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}
	
	public EncryptedValue getCheckValue() {
		return checkValue;
	}
	
	public void setCheckValue(EncryptedValue checkValue) {
		this.checkValue = checkValue;
	}

	public String getCommonName() {
		return commonName;
	}

	public void setCommonName(String commonName) {
		this.commonName = commonName;
	}

	public Device getDevice() {
		return device;
	}

	public void setDevice(Device device) {
		this.device = device;
	}
	
	public boolean notTamperedWith(){
		return device!=null && checkValue!=null
				&& checkValue.getStringValue()
				.equals(device.getId());
	}
}
