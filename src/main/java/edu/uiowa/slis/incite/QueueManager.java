package edu.uiowa.slis.incite;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
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
    
    public QueueManager(Connection conn, String test) {
	this.conn = conn;
    }
    
    public QueueManager(Connection conn, Vector<String> domains) {
	this.conn = conn;
	monitorThread = new Thread(new QueueMonitor());
	monitorThread.start();
	for (String domain : domains) {
	    reloads.add(new QueueDomain(domain));
	}
	starting = false;
    }
    
    private synchronized void loadQueue() throws SQLException {
	logger.info("full initialization of QueueManager...");
	PreparedStatement filterStmt = conn.prepareStatement("select domain from jsoup.crawler_seed where domain not in (select domain from jsoup.domain_suppress) order by domain");
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
    
    public void robotScan(String domain) {
	String buffer = null;
	logger.info("scanning for new " + domain + " robot.txt files...");
	try {
	    PreparedStatement filterStmt = conn.prepareStatement("select host from jsoup.document_host where domain = ? and not exists (select host from jsoup.host_disallow where host_disallow.host = document_host.host)");
	    filterStmt.setString(1, domain);
	    
	    ResultSet filterRS = filterStmt.executeQuery();
	    while (filterRS.next()) {
		String host = filterRS.getString(1);
		logger.info("probing robots.txt for " + host);
		URL theURL = new URL(host + "/robots.txt");

		try {
		    URLConnection theConnection = theURL.openConnection();
		    theConnection.setConnectTimeout(30000);
		    theConnection.setReadTimeout(30000);
		    theConnection.setAllowUserInteraction(false);
		    theConnection.setDoOutput(true);
		    BufferedReader theReader = new BufferedReader(new InputStreamReader(theConnection.getInputStream()));

		    boolean inBlock = false;
		    boolean storedEntry = false;

		    while ((buffer = theReader.readLine()) != null) {
			buffer = buffer.trim();
		        logger.debug("\tbuffer: " + buffer);
			if (buffer.startsWith("User-agent:")) {
			    if (buffer.trim().endsWith(" *"))
				inBlock = true;
			    else
				inBlock = false;
			}

			if (inBlock && buffer.startsWith("Disallow:")) {
			    String prefix = buffer.substring(9).trim();
			    if (prefix.length() == 0)
				continue;
			    logger.info("\tprefix: " + prefix);
			    PreparedStatement stmt = conn.prepareStatement("insert into jsoup.host_disallow values(?,now(),?,?)");
			    stmt.setString(1, host);
			    stmt.setString(2, prefix);
			    stmt.setString(3, "^" + host + prefix.replaceAll("\\?", "\\\\?").replaceAll("\\*", ".*"));
			    stmt.execute();
			    stmt.close();
			    storedEntry = true;
			}
		    }
		    conn.commit();
		    if (!storedEntry)
			throw new Exception("no entries");
		} catch (Exception e) {
		    logger.info("\tno robots.txt " + e);
		    PreparedStatement stmt = conn.prepareStatement("insert into jsoup.host_disallow values(?,now(),null,null)");
		    stmt.setString(1, host);
		    stmt.execute();
		    stmt.close();
		}
	    }
	    filterStmt.close();
	} catch (Exception e) {
	    logger.error("exception raised processing robots.txt: " + e);
	}
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
	if (domain == null)
	    return null;
	logger.info("requesting url from " + domain);
	return domain.nextRequest();
    }
    
    public synchronized boolean completed() {
	return domains.isEmpty() && reloads.isEmpty();
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
