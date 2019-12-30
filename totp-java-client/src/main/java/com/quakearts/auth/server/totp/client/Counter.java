package com.quakearts.auth.server.totp.client;

import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;

public class Counter {
	private boolean run;
	private Label label;
	private Runnable counterAction;
	private long initialCounter;
	
	public Counter(Label label, Runnable counterAction) {
		this.label = label;
		this.counterAction = counterAction;
		Runtime.getRuntime().addShutdownHook(new Thread(this::stop));
	}

	public synchronized void start(){
		if(run)
			return;
		if(initialCounter == 0)
			initialCounter = DeviceStorage.getInstance().getDevice().getInitialCounter();
		run = true;
		new Thread(this::runCounter).start();
	}
	
	private long lastTime;
	
	private void runCounter(){
		while (run) {
			long time = ((System.currentTimeMillis() - initialCounter) 
					% Options.getInstance().getTimeStep())/1000;
			Display.getDefault()
				.syncExec(()->{
				if(label.isDisposed())
					return;
				label.setText(String.format(":%02d", time));
				if(time < lastTime){
					counterAction.run();
				}
				lastTime = time;
			});
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				run = false;
			}
		}
	}
	
	public void reset(){
		Display.getDefault()
		.syncExec(()->{
			if(label.isDisposed())
				return;
			label.setText(":00");
		});
	}
	
	public synchronized void stop(){
		run = false;
	}
}
