package edu.uiowa.slis.incite;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Hashtable;
import java.util.Vector;

import org.apache.log4j.Logger;

public class QueueManager {
    static Logger logger = Logger.getLogger(QueueManager.class);
    Connection conn = null;
    Vector<QueueDomain> domains = new Vector<QueueDomain>();
    Hashtable<String,QueueDomain> domainHash = new Hashtable<String,QueueDomain>();
    Vector<QueueDomain> reloads = new Vector<QueueDomain>();
    boolean starting = true;
    Thread monitorThread = null;
    
    public QueueManager(Connection conn) {
	this.conn = conn;
	monitorThread = new Thread(new QueueMonitor());
	monitorThread.start();
	try {
	    loadQueue();
	    starting = false;
	} catch (SQLException e) {
	   logger.error("errur initializing QueueManager:", e);
	}
    }
    
    private synchronized void loadQueue() throws SQLException {
	logger.info("full initialization of QueueManager...");
	PreparedStatement filterStmt = conn.prepareStatement("select domain from jsoup.crawler_seed order by domain");
	ResultSet filterRS = filterStmt.executeQuery();
	while (filterRS.next()) {
	    String domain = filterRS.getString(1);	    
	    QueueDomain queueDomain = new QueueDomain(domain);
	    reloads.add(queueDomain);
	}
	filterStmt.close();
	logger.info("queued " + domains.size() + " domains");
	reloadQueue();
    }
    
    private synchronized boolean reloadQueue() {
	logger.info("\tstarting: " + starting);
	logger.info("\tdomains: " + domains);
	logger.info("\treloads: " + reloads);
	while (!reloads.isEmpty()) {
	    QueueDomain queueDomain = reloads.remove(0);
	    logger.info("reloading domain " + queueDomain);
	    int urlCount = 0;
	    try {
		PreparedStatement filterStmt = conn.prepareStatement(
			"select id,url"
			+ " from jsoup.document,jsoup.pattern"
			+ " where domain = ?"
			+ "  and url~pattern"
			+ "  and url!~'/calendar/'"
			+ "  and url!~'/login/'"
			+ "  and url!~'/directory/listing/'"
			+ "  and url!~'all-in-one-event-calendar'"
			+ "  and indexed is null"
			+ "  and (suffix is null or suffix not in (select suffix from jsoup.suffix))");
		filterStmt.setString(1, queueDomain.domain);
		ResultSet filterRS = filterStmt.executeQuery();
		while (filterRS.next()) {
		    String domain = filterRS.getString(1);
		    int id = filterRS.getInt(1);
		    String url = filterRS.getString(2);
		    queueDomain.addRequest(new QueueRequest(id, url));
		    logger.info("[--] reloadQueue: " + id + "\t" + url);
		    urlCount++;
		}
		filterStmt.close();
		conn.commit();
	    } catch (Exception e) {
		logger.error("exception raised in reloadQueue: ", e);
		System.exit(1);
	    }
	    if (urlCount > 0)
		domains.add(queueDomain);
	}
	return starting || !domains.isEmpty();
    }
    
    private QueueDomain nextDomain() {
	if (domains.isEmpty())
	    return null;
	
	QueueDomain domain = domains.remove(0);
	while (domain != null) {
	    if (domain.isEmpty()) {
		logger.info("requeueing empty domain: " + domain);
		reloads.add(domain);
	    } else {
		domains.addElement(domain);
		return domain;
	    }
	    if (domains.isEmpty())
		domain = null;
	    else
		domain = domains.remove(0);
	}
	return domain;
    }
    
    public synchronized QueueRequest nextRequest() {
	QueueDomain domain = nextDomain();
	logger.info("requesting url from " + domain);
	if (domain == null)
	    return null;
	return domain.nextRequest();
    }
    
    class QueueMonitor implements Runnable {

	public void run() {
	    logger.info("queue monitor initialized");
	    while (reloadQueue()) {
		try {
		    Thread.sleep(30 * 1000);
		} catch (InterruptedException e) {
		    
		} catch (Exception e) {
		    logger.error("exception raised in queue monitor: ", e);
		}
		logger.info("queue monitor polling...");
	    }
	    logger.info("queue monitor terminating");
	}
	
    }
}
