package com.quakearts.auth.server.totp.client;

import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FormLayout;

import java.security.GeneralSecurityException;
import java.security.Security;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.wb.swt.SWTResourceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TOTPApplication {
	private static final String GENERATE = "Generate";
	private static final String DEFAULT_FONT = "Segoe UI";
	/**
	 * Launch the application.
	 * @param args
	 */
	
	private static final Logger log = LoggerFactory.getLogger(TOTPApplication.class);
	
	enum Mode {
		PROVISION,
		AUTHENTICATE;
	}
	
	private static String format = "%0"+Options.getInstance().getOtpLength()+"d";
	
	public static void main(String[] args) {
		try {
			Security.addProvider(new BouncyCastleProvider());
			TOTPApplication window = new TOTPApplication();
			window.mode = DeviceStorage.getInstance().hasBeenProvisioned()?
					Mode.AUTHENTICATE:Mode.PROVISION;
			window.open();
		} catch (Exception e) {
			log.error("Unable too start application", e);
		}
	}

	private Mode mode = Mode.PROVISION;
	private Runnable currentAction = this::authenticateAndLoad;
	private Text txtPin;
	private Shell shell;
	private Composite[] pages;
	private Counter counter;
	private Text txtTOTP;
	private Text txtAlias;
	private Label lblDeviceId;
	private Button btnAction;
	private Cursor cursor;
	
	/**
	 * Open the window.
	 */
	public void open() {
		Display display = Display.getDefault();
		shell = new Shell();
		shell.setMinimumSize(new Point(300, 600));
		shell.setSize(450, 300);
		shell.setImage(new Image(shell.getDisplay(), "favicon.ico"));
		shell.setText("Symbolus Demo Application");
		shell.setLayout(new FormLayout());
		
		Label label = new Label(shell, SWT.SEPARATOR | SWT.HORIZONTAL);
		FormData fdLabel = new FormData();
		fdLabel.top = new FormAttachment(0, 65);
		fdLabel.left = new FormAttachment(0);
		fdLabel.bottom = new FormAttachment(0, 67);
		fdLabel.right = new FormAttachment(100);
		label.setLayoutData(fdLabel);
		
		btnAction = new Button(shell, SWT.NONE);
		btnAction.setFont(SWTResourceManager.getFont(DEFAULT_FONT, 13, SWT.NORMAL));
		FormData fdBtnAction = new FormData();
		fdBtnAction.right = new FormAttachment(label, 0, SWT.RIGHT);
		fdBtnAction.bottom = new FormAttachment(100);
		fdBtnAction.left = new FormAttachment(label, 0, SWT.LEFT);
		fdBtnAction.top = new FormAttachment(100, -65);
		btnAction.setLayoutData(fdBtnAction);
		btnAction.setText(mode == Mode.AUTHENTICATE?"Unlock":"Create PIN");
		btnAction.addMouseListener(MouseListener.mouseUpAdapter(this::runAction));

		pages = new Composite[3];
		pages[0] = createPasswordComposite(shell, label);
		pages[1] = createTOTPComposite(shell, label);
		pages[1].setVisible(false);
		if(mode == Mode.PROVISION){
			pages[2] = createProvisioningComposite(shell, label);
			pages[2].setVisible(false);
		}
		lblDeviceId = new Label(shell, SWT.NONE);
		FormData fdLblDeviceId = new FormData();
		fdLblDeviceId.top = new FormAttachment(0, 10);
		fdLblDeviceId.left = new FormAttachment(0, 10);
		lblDeviceId.setLayoutData(fdLblDeviceId);
		
		shell.open();
		shell.layout();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
		counter.stop();
		System.exit(0);
	}

	private Composite createPasswordComposite(Shell shlSymbolusDemoApplication, Label label) {
		Composite passwordComposite = new Composite(shlSymbolusDemoApplication, SWT.NONE);
		passwordComposite.setLayout(new FormLayout());
		FormData fdPasswordComposite = new FormData();
		fdPasswordComposite.left = new FormAttachment(label, 10, SWT.LEFT);
		fdPasswordComposite.right = new FormAttachment(label, -10, SWT.RIGHT);
		fdPasswordComposite.top = new FormAttachment(0, 70);
		fdPasswordComposite.bottom = new FormAttachment(100, -70);
		passwordComposite.setLayoutData(fdPasswordComposite);
		
		txtPin = new Text(passwordComposite, SWT.BORDER | SWT.CENTER | SWT.SINGLE | SWT.PASSWORD);
		txtPin.setFont(SWTResourceManager.getFont(DEFAULT_FONT, 32, SWT.NORMAL));
		txtPin.setText("");
		FormData fdPinText = new FormData();
		fdPinText.bottom = new FormAttachment(0, 125);
		fdPinText.right = new FormAttachment(0, 412);
		fdPinText.top = new FormAttachment(0);
		fdPinText.left = new FormAttachment(0);
		txtPin.setLayoutData(fdPinText);
		
		Button button7 = new Button(passwordComposite, SWT.NONE);
		button7.setText("7");
		button7.setFont(SWTResourceManager.getFont(DEFAULT_FONT, 18, SWT.NORMAL));
		FormData fdButton7 = new FormData();
		fdButton7.top = new FormAttachment(txtPin, 14);
		fdButton7.left = new FormAttachment(0, 129);
		fdButton7.right = new FormAttachment(100, -240);
		button7.setLayoutData(fdButton7);
		button7.addMouseListener(createMouseListener(7));
		
		Button button8 = new Button(passwordComposite, SWT.NONE);
		button8.setText("8");
		button8.setFont(SWTResourceManager.getFont(DEFAULT_FONT, 18, SWT.NORMAL));
		FormData fdButton8 = new FormData();
		fdButton8.top = new FormAttachment(button7, 0, SWT.TOP);
		fdButton8.left = new FormAttachment(button7, 6);
		fdButton8.right = new FormAttachment(100, -191);
		button8.setLayoutData(fdButton8);
		button8.addMouseListener(createMouseListener(8));
		
		Button button9 = new Button(passwordComposite, SWT.NONE);
		button9.setText("9");
		button9.setFont(SWTResourceManager.getFont(DEFAULT_FONT, 18, SWT.NORMAL));
		FormData fdButton9 = new FormData();
		fdButton9.top = new FormAttachment(button7, 0, SWT.TOP);
		fdButton9.left = new FormAttachment(button8, 6);
		fdButton9.right = new FormAttachment(100, -142);
		button9.setLayoutData(fdButton9);
		button9.addMouseListener(createMouseListener(9));
		
		Button button4 = new Button(passwordComposite, SWT.NONE);
		button4.setText("4");
		button4.setFont(SWTResourceManager.getFont(DEFAULT_FONT, 18, SWT.NORMAL));
		FormData fdButton4 = new FormData();
		fdButton4.top = new FormAttachment(button7, 6);
		fdButton4.left = new FormAttachment(button7, 0, SWT.LEFT);
		fdButton4.right = new FormAttachment(button7, 0, SWT.RIGHT);
		button4.setLayoutData(fdButton4);
		button4.addMouseListener(createMouseListener(4));
		
		Button button5 = new Button(passwordComposite, SWT.NONE);
		button5.setText("5");
		button5.setFont(SWTResourceManager.getFont(DEFAULT_FONT, 18, SWT.NORMAL));
		FormData fdButton5 = new FormData();
		fdButton5.top = new FormAttachment(button8, 6);
		fdButton5.left = new FormAttachment(button8, 0, SWT.LEFT);
		fdButton5.right = new FormAttachment(button8, 0, SWT.RIGHT);
		button5.setLayoutData(fdButton5);
		button5.addMouseListener(createMouseListener(5));
		
		Button button6 = new Button(passwordComposite, SWT.NONE);
		button6.setText("6");
		button6.setFont(SWTResourceManager.getFont(DEFAULT_FONT, 18, SWT.NORMAL));
		FormData fdButton6 = new FormData();
		fdButton6.top = new FormAttachment(button9, 6);
		fdButton6.left = new FormAttachment(button9, 0, SWT.LEFT);
		fdButton6.right = new FormAttachment(button9, 0, SWT.RIGHT);
		button6.setLayoutData(fdButton6);
		button6.addMouseListener(createMouseListener(6));
		
		Button button1 = new Button(passwordComposite, SWT.NONE);
		button1.setText("1");
		button1.setFont(SWTResourceManager.getFont(DEFAULT_FONT, 18, SWT.NORMAL));
		FormData fdButton1 = new FormData();
		fdButton1.top = new FormAttachment(button4, 5);
		fdButton1.left = new FormAttachment(button7, 0, SWT.LEFT);
		fdButton1.right = new FormAttachment(button7, 0, SWT.RIGHT);
		button1.setLayoutData(fdButton1);
		button1.addMouseListener(createMouseListener(1));
		
		Button button2 = new Button(passwordComposite, SWT.NONE);
		button2.setText("2");
		button2.setFont(SWTResourceManager.getFont(DEFAULT_FONT, 18, SWT.NORMAL));
		FormData fdButton2 = new FormData();
		fdButton2.top = new FormAttachment(button5, 6);
		fdButton2.left = new FormAttachment(button8, 0, SWT.LEFT);
		fdButton2.right = new FormAttachment(button8, 0, SWT.RIGHT);
		button2.setLayoutData(fdButton2);
		button2.addMouseListener(createMouseListener(2));
		
		Button button3 = new Button(passwordComposite, SWT.NONE);
		button3.setText("3");
		button3.setFont(SWTResourceManager.getFont(DEFAULT_FONT, 18, SWT.NORMAL));
		FormData fdButton3 = new FormData();
		fdButton3.top = new FormAttachment(button6, 6);
		fdButton3.left = new FormAttachment(button9, 0, SWT.LEFT);
		fdButton3.right = new FormAttachment(button9, 0, SWT.RIGHT);
		button3.setLayoutData(fdButton3);
		button3.addMouseListener(createMouseListener(3));
		
		Button btnClear = new Button(passwordComposite, SWT.CENTER);
		btnClear.setText("\u25C4");
		btnClear.setFont(SWTResourceManager.getFont(DEFAULT_FONT, 18, SWT.NORMAL));
		FormData fdBtnClear = new FormData();
		fdBtnClear.top = new FormAttachment(button2, 6);
		fdBtnClear.left = new FormAttachment(button8, 0, SWT.LEFT);
		fdBtnClear.right = new FormAttachment(button9, 0, SWT.RIGHT);
		btnClear.setLayoutData(fdBtnClear);
		btnClear.addMouseListener(MouseListener
				.mouseUpAdapter(event->txtPin.setText("")));
		
		Button button0 = new Button(passwordComposite, SWT.NONE);
		button0.setText("0");
		button0.setFont(SWTResourceManager.getFont(DEFAULT_FONT, 18, SWT.NORMAL));
		FormData fdButton0 = new FormData();
		fdButton0.top = new FormAttachment(button1, 6);
		fdButton0.left = new FormAttachment(button7, 0, SWT.LEFT);
		fdButton0.right = new FormAttachment(button7, 0, SWT.RIGHT);
		button0.setLayoutData(fdButton0);
		button0.addMouseListener(createMouseListener(0));
		
		return passwordComposite;
	}

	private MouseListener createMouseListener(int code){
		return MouseListener.mouseUpAdapter(event->txtPin.setText(txtPin.getText()+code));
	}
	
	private Composite createTOTPComposite(Shell shlSymbolusDemoApplication, Label label) {
		Composite totpComposite = new Composite(shlSymbolusDemoApplication, SWT.NONE);
		totpComposite.setLayoutData(new FormData());
		totpComposite.setLayout(new FormLayout());
		FormData fdTotpComposite = new FormData();
		fdTotpComposite.left = new FormAttachment(label, 10, SWT.LEFT);
		fdTotpComposite.right = new FormAttachment(label, -10, SWT.RIGHT);
		fdTotpComposite.top = new FormAttachment(0, 70);
		fdTotpComposite.bottom = new FormAttachment(100, -70);
		totpComposite.setLayoutData(fdTotpComposite);
		totpComposite.setVisible(false);
		
		txtTOTP = new Text(totpComposite, SWT.BORDER | SWT.CENTER | SWT.MULTI);
		
		txtTOTP.setText(String.format(format, 0));
		txtTOTP.setFont(SWTResourceManager.getFont(DEFAULT_FONT, 50, SWT.NORMAL));
		FormData fdTOTP = new FormData();
		fdTOTP.left = new FormAttachment(totpComposite, 0, SWT.LEFT);
		fdTOTP.bottom = new FormAttachment(totpComposite, 208);
		fdTOTP.right = new FormAttachment(100);
		fdTOTP.top = new FormAttachment(0);
		txtTOTP.setLayoutData(fdTOTP);
		txtTOTP.setEditable(false);
		
		Label lblCounter = new Label(totpComposite, SWT.NONE);
		lblCounter.setFont(SWTResourceManager.getFont(DEFAULT_FONT, 14, SWT.NORMAL));
		FormData fdCounter = new FormData();
		fdCounter.top = new FormAttachment(txtTOTP, 6);
		fdCounter.right = new FormAttachment(100, -10);
		lblCounter.setLayoutData(fdCounter);
		lblCounter.setText(":00");
		
		counter = new Counter(lblCounter, this::regenerateOTP);
		return totpComposite;
	}

	private Composite createProvisioningComposite(Shell shlSymbolusDemoApplication, Label label) {
		Composite totpProvisioningComposite = new Composite(shlSymbolusDemoApplication, SWT.NONE);
		totpProvisioningComposite.setLayoutData(new FormData());
		totpProvisioningComposite.setLayout(new FormLayout());
		FormData fdTotpProvisioningComposite = new FormData();
		fdTotpProvisioningComposite.left = new FormAttachment(label, 10, SWT.LEFT);
		fdTotpProvisioningComposite.right = new FormAttachment(label, -10, SWT.RIGHT);
		fdTotpProvisioningComposite.top = new FormAttachment(0, 70);
		fdTotpProvisioningComposite.bottom = new FormAttachment(100, -70);
		totpProvisioningComposite.setLayoutData(fdTotpProvisioningComposite);
		
		Label lblAlias = new Label(totpProvisioningComposite, SWT.NONE);
		FormData fdLblAlias = new FormData();
		lblAlias.setLayoutData(fdLblAlias);
		lblAlias.setText("Alias");
		
		txtAlias = new Text(totpProvisioningComposite, SWT.BORDER);
		fdLblAlias.bottom = new FormAttachment(txtAlias, -6);
		fdLblAlias.left = new FormAttachment(txtAlias, 0, SWT.LEFT);
		txtAlias.setFont(SWTResourceManager.getFont(DEFAULT_FONT, 11, SWT.NORMAL));
		txtAlias.setText("alias");
		FormData fdTxtAlias = new FormData();
		fdTxtAlias.bottom = new FormAttachment(100, -281);
		fdTxtAlias.top = new FormAttachment(0, 98);
		fdTxtAlias.left = new FormAttachment(0);
		fdTxtAlias.right = new FormAttachment(100);
		txtAlias.setLayoutData(fdTxtAlias);
		
		Label lblInstructions = new Label(totpProvisioningComposite, SWT.NONE);
		FormData fdLblInstructions = new FormData();
		fdLblInstructions.top = new FormAttachment(0);
		fdLblInstructions.left = new FormAttachment(lblAlias, 0, SWT.LEFT);
		lblInstructions.setLayoutData(fdLblInstructions);
		lblInstructions.setText("Provision a device for totp\r\nOptionally enter an alias to associate with this device.\r\nThe Device ID is:");
		return totpProvisioningComposite;
	}
	
	private void runAction(MouseEvent event){
		btnAction.setEnabled(false);
		showCursor(SWT.CURSOR_WAIT);
		currentAction.run();
	}

	private void showCursor(int type) {
		if(cursor!=null)
			cursor.dispose();
		
		cursor = new Cursor(Display.getDefault(), type);
		shell.setCursor(cursor);
	}
	
	private void authenticateAndLoad(){
		CompletableFuture.runAsync(this::doAuthenticateAndLoad);
	}
	
	private void doAuthenticateAndLoad(){
		Runnable action;
		if(mode == Mode.AUTHENTICATE){
			action = ()->{
				try {
					DeviceStorage.getInstance().loadDevice(txtPin.getText());
					Device device = DeviceStorage.getInstance().getDevice();
					DeviceConnection.getInstance().init(device, shell);
					lblDeviceId.setText("Device ID: "+device.getId());
					lblDeviceId.getParent().layout();
					pages[0].setVisible(false);
					pages[1].setVisible(true);
					currentAction = this::generateAndDisplayOTP;
					btnAction.setText(GENERATE);
				} catch (Throwable e) {
					MessageBox box = new MessageBox(shell, SWT.ICON_WARNING | SWT.OK);
					box.setText("Login error");
					box.setMessage(e.getMessage()!=null?e.getMessage()
							:e.getClass().getName());
					box.open();
				} finally {
					btnAction.setEnabled(true);
					showCursor(SWT.CURSOR_ARROW);
				}
			};
		} else {
			action = ()->{
				pages[0].setVisible(false);
				pages[2].setVisible(true);
				currentAction = this::provisionDevice;
				btnAction.setText("Provision");
				btnAction.setEnabled(true);
				showCursor(SWT.CURSOR_ARROW);
			};
		}
		
		Display.getDefault().asyncExec(action);
	}
	
	private void generateAndDisplayOTP(){
		CompletableFuture.runAsync(this::doGenerateAndDisplayOTP);
	}
	
	private void doGenerateAndDisplayOTP(){
		Display.getDefault().asyncExec(()->{
			try {
				String otp = DeviceStorage.getInstance().getDevice().generateOTP();
				txtTOTP.setText(otp);
				currentAction = this::hideOTP;
				btnAction.setText("Hide");
				counter.start();
			} catch (GeneralSecurityException e) {
				showError(e);
			} finally {
				btnAction.setEnabled(true);
				showCursor(SWT.CURSOR_ARROW);
			}
		});
	}

	private void hideOTP(){
		counter.stop();
		counter.reset();
		currentAction = this::generateAndDisplayOTP;
		btnAction.setText(GENERATE);
		btnAction.setEnabled(true);
		txtTOTP.setText(String.format(format, 0));
		showCursor(SWT.CURSOR_ARROW);
	}
	
	private void showError(Throwable e) {
		MessageBox errorMessage = new MessageBox(shell, 
				SWT.ICON_ERROR | SWT.OK);
		errorMessage.setText("Error");
		errorMessage.setMessage(e.getMessage());
		errorMessage.open();
	}
	
	private void regenerateOTP(){
		try {
			txtTOTP.setText(DeviceStorage.getInstance().getDevice()
					.generateOTP());
		} catch (GeneralSecurityException e) {
			showError(e);
		}
	}
	
	private void provisionDevice() {
		CompletableFuture.runAsync(this::doProvisionDevice);
	}
	
	private void doProvisionDevice() {
		Display.getDefault().asyncExec(()->{
			try {
				Device device = DeviceProvisioner.getInstance()
						.provision(UUID.randomUUID().toString().toUpperCase(), txtAlias.getText());
				DeviceStorage.getInstance().storeDevice(device, txtPin.getText());
				DeviceConnection.getInstance().init(device, shell);
				lblDeviceId.setText("Device ID: "
						+device.getId());
				lblDeviceId.getParent().layout();
				pages[2].setVisible(false);
				pages[1].setVisible(true);
				currentAction = this::generateAndDisplayOTP;
				btnAction.setText(GENERATE);
				
				MessageBox errorMessage = new MessageBox(shell, 
						SWT.ICON_INFORMATION | SWT.OK);
				errorMessage.setText("Device Provisioned");
				errorMessage.setMessage("The device has been provisioned for Time Based One Time Passwords");
				errorMessage.open();
			} catch (Exception e) {
				showError(e);
			} finally {
				btnAction.setEnabled(true);
				showCursor(SWT.CURSOR_ARROW);
			}
		});
	}
}
