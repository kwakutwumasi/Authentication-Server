package com.quakearts.auth.server.totp.rest.authorization;

import java.util.Optional;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

import com.quakearts.auth.server.totp.authentication.AuthenticationService;
import com.quakearts.auth.server.totp.device.DeviceManagementService;
import com.quakearts.auth.server.totp.model.Administrator;
import com.quakearts.auth.server.totp.rest.model.ManagementRequest;
import com.quakearts.webapp.security.auth.UserPrincipal;
import com.quakearts.webapp.security.rest.SecurityContext;
import com.quakearts.webapp.security.rest.exception.RestSecurityException;

@Interceptor @AuthorizeManagedRequest
@Priority(Interceptor.Priority.APPLICATION+1)
public class AuthorizeManagedRequestInterceptor {
	
	@Inject
	private DeviceManagementService deviceManagementService;
	
	@Inject
	private AuthenticationService authenticationService;

	@AroundInvoke
	public Object intercept(InvocationContext context) throws Exception {
		ManagementRequest request = (ManagementRequest) context.getParameters()[0];

		if(request==null
				|| request.getAuthorizationRequest().getDeviceId() == null
				|| request.getAuthorizationRequest().getOtp() == null)
			throw new RestSecurityException("AuthenticationRequest is required");
		
		if(!SecurityContext.getCurrentSecurityContext().isAuthenicated()
				|| SecurityContext.getCurrentSecurityContext()
					.getSubject().getPrincipals(UserPrincipal.class)
					.contains(new UserPrincipal(request.getAuthorizationRequest().getDeviceId()))){
			throw new RestSecurityException("Dual administrator authorization is required");
		}
		
		Optional<Administrator> optionalAdministrator = deviceManagementService
				.findAdministrator(request.getAuthorizationRequest().getDeviceId());
		if(optionalAdministrator.isPresent()
				&& authenticationService.authenticate(optionalAdministrator.get().getDevice(), request.getAuthorizationRequest().getOtp())){
			return context.proceed();
		}
		
		throw new RestSecurityException("Cannot authorize this request");
	}
}
