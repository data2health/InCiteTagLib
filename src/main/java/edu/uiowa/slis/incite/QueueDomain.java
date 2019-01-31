package edu.uiowa.slis.incite;

import java.util.Date;
import java.util.Vector;

import org.apache.log4j.Logger;

public class QueueDomain {
    static Logger logger = Logger.getLogger(QueueDomain.class);
    int deferInterval = 2000;
    Date lastAccess = new Date(0);
    String domain = null;
    Vector<QueueRequest> queue = new Vector<QueueRequest>();

    QueueDomain(String domain) {
	this.domain = domain;
    }
    
    public String toString() {
	return domain + " (" + queue.size() + " urls)";
    }
    
    synchronized boolean isEmpty() {
	return queue.isEmpty();
    }
    
    synchronized void addRequest(QueueRequest request) {
	queue.addElement(request);
    }
    
    synchronized QueueRequest nextRequest() {
	Date now = new Date();
	if (now.getTime() - lastAccess.getTime() < deferInterval)
	    try {
		logger.info("throttling domain " + domain);
		Thread.sleep(deferInterval);
	    } catch (InterruptedException e) { }
	lastAccess = new Date();
	
	if (queue.isEmpty())
	    return null;

	return queue.remove(0);
    }
}
