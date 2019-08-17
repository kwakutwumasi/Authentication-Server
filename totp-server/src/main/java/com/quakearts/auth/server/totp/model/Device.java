package com.quakearts.auth.server.totp.model;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.OneToMany;

import com.quakearts.security.cryptography.CryptoResource;
import com.quakearts.security.cryptography.jpa.EncryptedValue;
import com.quakearts.security.cryptography.jpa.EncryptedValueConverter;
import com.quakearts.security.cryptography.jpa.EncryptedValueStringConverter;
import com.quakearts.webapp.security.util.HashPassword;

@Entity
public class Device implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -688112438713243083L;
	@Id
	@Column(length=250)
	private String id;
	@Column(nullable = false)
	private long initialCounter;
	@Column(nullable = false, length=250)
	@Convert(converter = EncryptedValueConverter.class)
	private EncryptedValue seed;
	@Column(length=250, nullable=false)
	@Convert(converter = EncryptedValueStringConverter.class)
	private EncryptedValue checkValue;
	@Column(nullable = false)
	private Status status;
	@Column(nullable = false, unique=true, insertable=false, updatable=false)
	private long itemCount;
	@OneToMany(mappedBy="device", fetch=FetchType.LAZY)
	private Set<Alias> aliases = new HashSet<>();

	public enum Status {
		INITIATED, ACTIVE, INACTIVE, LOCKED
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
		addCheck();
	}

	public long getInitialCounter() {
		return initialCounter;
	}

	public void setInitialCounter(long initialCounter) {
		this.initialCounter = initialCounter;
	}

	public EncryptedValue getSeed() {
		return seed;
	}

	public void setSeed(EncryptedValue seed) {
		this.seed = seed;
		addCheck();
	}
	
	public EncryptedValue getCheckValue() {
		return checkValue;
	}

	public void setCheckValue(EncryptedValue checkValue) {
		this.checkValue = checkValue;
	}

	public Status getStatus() {
		return status;
	}

	public void setStatus(Status status) {
		this.status = status;
	}
	
	public long getItemCount() {
		return itemCount;
	}
	
	public void setItemCount(long index) {
		this.itemCount = index;
	}
	
	private void addCheck() {
		if(checkValue ==null && id!=null && seed!=null){
			checkValue = generateCheck();
			checkValue.setDataStoreName(seed.getDataStoreName());
		}
	}
	
	public boolean notTamperedWith(){
		return checkValue!=null && id!=null && seed!=null
				&& generateCheck().getStringValue()
					.equals(checkValue.getStringValue());
	}
	
	private EncryptedValue generateCheck() {
		EncryptedValue encryptedValue = new EncryptedValue();
		encryptedValue.setStringValue(new HashPassword(id, "SHA-256", 3, 
				CryptoResource.byteAsHex(seed.getValue())).toString());
		return encryptedValue;
	}

	public Set<Alias> getAliases() {
		return aliases;
	}
	
	public void setAliases(Set<Alias> aliases) {
		this.aliases = aliases;
	}
}
