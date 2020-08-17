package com.quakearts.auth.server.totp.login.testmain;

import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;

import com.quakearts.auth.server.totp.services.DirectAuthenticationService;

@ServerEndpoint("/fallbacktoken/{username}")
public class FallbackTokenEndpoint {
	
	private DirectAuthenticationService service = DirectAuthenticationService.getInstance();
	
	@OnOpen
	public void opened(Session session, @PathParam("username") String username){
		session.getAsyncRemote().sendText(service.generateID(username));
		session.getUserProperties().put("fallback.token.username", username);
		service.setFallbackListener(username, ()->{
			session.getAsyncRemote().sendText("fallback-required");
		});
	}
	
	@OnMessage
	public void received(Session session, String token) {
		service.putFallbackToken(session.getUserProperties()
				.getOrDefault("fallback.token.username", "").toString(), token);
	}
}
