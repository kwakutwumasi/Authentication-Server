package com.quakearts.auth.server.totp.loginmodule;

import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.security.auth.login.LoginException;

import com.quakearts.appbase.cdi.annotation.Transactional;
import com.quakearts.appbase.cdi.annotation.Transactional.TransactionType;
import com.quakearts.auth.server.totp.authentication.AuthenticationService;
import com.quakearts.auth.server.totp.device.DeviceService;
import com.quakearts.auth.server.totp.model.Device;
import com.quakearts.auth.server.totp.model.Device.Status;

@Singleton
public class LoginModuleService {
	
	@Inject
	private AuthenticationService authenticationService;
	
	@Inject
	private DeviceService deviceService;
	
	@Transactional(TransactionType.SINGLETON)
	public void login(String id, char[] otp) throws LoginException {
		Optional<Device> optionalDevice = deviceService.findDevice(id);
		if(optionalDevice.isPresent() && optionalDevice.get().getStatus() == Status.ACTIVE){
			Device device = optionalDevice.get();
			try {
				if(!authenticationService.authenticate(device, new String(otp))){
					throw new LoginException("OTP did not match");
				} 
			} finally {
				if(authenticationService.isLocked(device)){
					deviceService.lock(device);
				}
			}
		} else {
			throw new LoginException("Device with ID "+id+" not found");
		}
	}
}
