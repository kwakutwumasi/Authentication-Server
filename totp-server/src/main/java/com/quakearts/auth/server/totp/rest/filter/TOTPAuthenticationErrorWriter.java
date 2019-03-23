package com.quakearts.auth.server.totp.rest.filter;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import com.quakearts.webapp.security.rest.filter.AuthenticationErrorWriter;

public class TOTPAuthenticationErrorWriter implements AuthenticationErrorWriter {

	@Override
	public void sendError(int code, String message, HttpServletResponse httpResponse) throws IOException {
		httpResponse.setStatus(403);
		httpResponse.getWriter().write("{"
				+ "\"message\":\""
				+ message +"\""
				+ "}");
	}

}
