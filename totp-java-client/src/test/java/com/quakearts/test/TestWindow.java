package com.quakearts.test;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.widgets.Label;
import org.eclipse.wb.swt.SWTResourceManager;

import com.quakearts.auth.server.totp.client.Counter;

public class TestWindow {
	private Text text;
	public static void main(String[] args) {
		TestWindow window = new TestWindow();
		window.open();
	}
	
	int index = 123456;
	
	public void open(){
		Display display = Display.getDefault();
		Shell shell = new Shell();
		shell.setMinimumSize(new Point(300, 600));
		shell.setSize(450, 300);
		shell.setImage(new Image(shell.getDisplay(), "favicon.ico"));
		shell.setText("Symbolus Demo Application");
		shell.setLayout(new FormLayout());
		
		text = new Text(shell, SWT.BORDER | SWT.CENTER);
		text.setText("\n123456");
		text.setFont(SWTResourceManager.getFont("Segoe UI", 32, SWT.NORMAL));
		text.setEditable(false);
		FormData fd_text = new FormData();
		fd_text.top = new FormAttachment(0, 52);
		fd_text.left = new FormAttachment(0,10);
		fd_text.bottom = new FormAttachment(0, 207);
		fd_text.right = new FormAttachment(100,-10);
		text.setLayoutData(fd_text);
		
		Label label = new Label(shell, SWT.NONE);
		label.setFont(SWTResourceManager.getFont("Segoe UI", 16, SWT.NORMAL));
		FormData fd_label = new FormData();
		fd_label.top = new FormAttachment(text, 6);
		fd_label.right = new FormAttachment(100, -10);
		label.setLayoutData(fd_label);
		label.setText(":00");
		
		Counter counter = new Counter(label, ()->{
			index++;
			text.setText("\n"+index);
		});
		counter.start();
		
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
}
