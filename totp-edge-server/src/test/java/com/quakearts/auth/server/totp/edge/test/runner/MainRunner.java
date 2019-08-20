package com.quakearts.auth.server.totp.edge.test.runner;

import javax.enterprise.inject.spi.CDI;

import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.InitializationError;

import com.quakearts.auth.server.totp.edge.Main;

public class MainRunner extends BlockJUnit4ClassRunner {

	private static boolean started = false;
	
	public MainRunner(Class<?> klass) throws InitializationError {
		super(klass);
		if(!started) {
			Main.main(new String[0]);
			
			Runtime.getRuntime().addShutdownHook(new Thread(()->Main.stop(new String[0])));
			started = true;
		}
	}
	
	@Override
	protected Object createTest() throws Exception {
		return CDI.current().select(getTestClass().getJavaClass()).get();
	}

}
