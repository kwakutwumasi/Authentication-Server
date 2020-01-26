package com.quakearts.auth.server.totp.client;

import java.io.IOException;
import java.net.URI;
import java.security.GeneralSecurityException;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quakearts.auth.server.totp.client.http.model.Payload;

@WebSocket
public class DeviceConnection {
	private static final String ERROR = "error";
	private static Logger log = LoggerFactory.getLogger(DeviceConnection.class);
	
	private DeviceConnection() {}
	private static DeviceConnection instance = new DeviceConnection();
	private WebSocketClient client;
	private ObjectMapper mapper = new ObjectMapper();
	private Session session;
	private Device device;
	private Shell shell;

	public static DeviceConnection getInstance() {
		return instance;
	}
	
	public void init(Device device, Shell shell) throws Exception {
		this.device = device;
		this.shell = shell;
		client = new WebSocketClient();
		client.start();
		client.connect(this, new URI(MessageFormat.format(Options.getInstance().getTotpWsUrl(), device.getId(), device.generateOTP())));
		Runtime.getRuntime().addShutdownHook(new Thread(this::stop));
	}
	
	private void stop(){
		try {
			client.stop();
		} catch (Exception e) {
			//Do nothing
		}
	}

	@OnWebSocketConnect
	public void onConnect(Session session){
		this.session = session;
	}
	
	@OnWebSocketMessage
	public void onMessage(String message){
		CompletableFuture.runAsync(()->handleMessage(message));
	}

	@OnWebSocketClose
	public void sockectClosed(int statusCode, String reason){
		Display.getDefault().syncExec(()->{
			MessageBox approveReconnection = new MessageBox(shell, 
					SWT.ICON_ERROR | SWT.OK | SWT.CANCEL);
			approveReconnection.setText("Connection closed");
			approveReconnection.setMessage("The web socket connection to the server has unexpectedly terminated with a status code: "+statusCode+
					". Reason: "+ reason+".\n Do you want to reconnect?");
			if(approveReconnection.open() == SWT.OK){
				try {
					init(device, shell);
				} catch (Exception e) {
					log.error("Error processing reconnection", e);
				}
			}
		});
	}
	
	private synchronized void handleMessage(String message) {
		Payload response = new Payload();
		response.setMessage(new HashMap<>());
		try {
			Payload payload = mapper.readValue(message, Payload.class);
			String requestType = payload.getMessage().get("requestType");
			response.setId(payload.getId());
			response.setTimestamp(payload.getTimestamp());
			if("otp".equals(requestType)){
				displayOTPRequest(response);
			} else if("otp-signing".equals(requestType)){
				displayOTPSigningRequest(payload, response);
			} else {
				response.getMessage().put(ERROR, "Missing request type");			
			}
		} catch (IOException e) {
			handleException(response, e);			
		}
		send(response);
	}

	private void displayOTPRequest(Payload response) {
		Display.getDefault().syncExec(()->{
			MessageBox approveSignIn = new MessageBox(shell, 
					SWT.ICON_QUESTION | SWT.OK| SWT.CANCEL);
			approveSignIn.setText("Sign In Request");
			approveSignIn.setMessage("A login request has been received. Do you want to approve it?");
			if(approveSignIn.open() == SWT.OK){
				generateOTPAndStoreIn(response);
			} else {
				response.getMessage().put(ERROR, "Request rejected");
			}
		});
	}
	
	private void generateOTPAndStoreIn(Payload response) {
		try {
			response.getMessage().put("otp", device.generateOTP());
		} catch (GeneralSecurityException e) {
			log.error("Unable to generate response to server", e);
			response.getMessage().put(ERROR, e.getMessage());
		}
	}

	private void displayOTPSigningRequest(Payload request, Payload response) {
		Display.getDefault().syncExec(()->{
			MessageBox approveRequestSigning = new MessageBox(shell, 
					SWT.ICON_QUESTION | SWT.OK| SWT.CANCEL);
			approveRequestSigning.setText("Sign In Request");
			String message = request.getMessage().entrySet()
					.stream().filter(e->!e.getKey().equals("iat") 
							&& !e.getKey().equals("deviceId")
							&& !e.getKey().equals("requestType"))
					.map(e->e.getKey()+":"+e.getValue()).collect(Collectors.joining("\n"));
			approveRequestSigning.setMessage("A request has been received for signing. Do you want to approve it?\nThe details:\n"
					+message);
			if(approveRequestSigning.open() == SWT.OK){
				generateSigningTOTPandStoreIn(response);
			} else {
				response.getMessage().put(ERROR, "Request rejected");
			}
		});
	}

	private void generateSigningTOTPandStoreIn(Payload response) {
		long totpTimestamp = System.currentTimeMillis();
		try {
			response.getMessage().put("otp", device.generateOTPForTimestamp(totpTimestamp));
			response.getMessage().put("totp-timestamp", Long.toString(totpTimestamp));
		} catch (GeneralSecurityException e) {
			response.getMessage().put(ERROR, e.getMessage());
		}
	}

	private void handleException(Payload response, IOException e) {
		log.error("Unable to read message from server", e);
		response.getMessage().put(ERROR, e.getMessage());
	}

	private void send(Payload response) {
		try {
			session.getRemote().sendString(mapper.writeValueAsString(response));
		} catch (IOException e) {
			log.error("Unable to send message to server", e);
		}
	}
}
