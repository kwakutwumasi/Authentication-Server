package com.quakearts.auth.server.totp.rest.filter;

import java.io.IOException;

import javax.inject.Singleton;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.annotation.WebInitParam;

import com.quakearts.appbase.cdi.annotation.Transactional;
import com.quakearts.appbase.cdi.annotation.Transactional.TransactionType;
import com.quakearts.auth.server.totp.options.TOTPOptions;
import com.quakearts.webapp.security.rest.filter.AuthenticationFilter;

@Singleton
@WebFilter(urlPatterns="/totp/*", initParams = {
		@WebInitParam(name="requireAuthorization", value="false"),
		@WebInitParam(name="contextName", value=TOTPOptions.GlobalDefaults.LOGIN_MODULE),
		@WebInitParam(name="errorWriterClass", 
			value="com.quakearts.auth.server.totp.rest.filter.TOTPAuthenticationErrorWriter")
})
public class TOTPAuthenticationFilter extends AuthenticationFilter {

	@Override
	@Transactional(TransactionType.SINGLETON)
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		super.doFilter(request, response, chain);
	}
}
