package edu.uiowa.slis.incite;

import java.util.Hashtable;

import org.apache.log4j.Logger;

public class ConnectionCacheMonitor implements Runnable {
    static Logger logger = Logger.getLogger(ConnectionCacheMonitor.class);

	int interval = 30*60*1000;
	
	ConnectionCacheMonitor() {
		logger.info("initializing cache monitor...");
	}
	
	ConnectionCacheMonitor(int interval) {
		this.interval = interval;
		logger.info("initializing cache monitor...");
	}
	
	public void run() {
		while (true) {
			try {
				Thread.sleep(interval);
			} catch (InterruptedException e) {
			}

			ConnectionGenerator.domainHash = new Hashtable<String, Integer>();
			ConnectionGenerator.urlHash = new Hashtable<String, Integer>();
		}
	}
	
}
