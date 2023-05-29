package edu.uiowa.slis.incite;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Hashtable;

import org.apache.log4j.Logger;

import edu.uiowa.crawling.URLRequest;
import edu.uiowa.crawling.VisitedMonitor;

public class MemoizedVisitedMonitor extends VisitedMonitor {
	static Logger logger = Logger.getLogger(MemoizedVisitedMonitor.class);
	static Hashtable<String, Integer> urlHash = new Hashtable<String, Integer>();

	Connection conn = null;

	public MemoizedVisitedMonitor() {
		Thread cacheMonitorThread = new Thread(new CacheMonitor());
		cacheMonitorThread.start();
	}

	public MemoizedVisitedMonitor(Connection conn) {
		this();
		this.conn = conn;
	}

	@Override
	public void visit(URLRequest theURLRequest) {
		urlHash.put(theURLRequest.getURLstring(), theURLRequest.getID());
	}

	@Override
	public boolean visited(String urlString) {
		int id = 0;

		if (urlHash.containsKey(urlString)) {
			logger.debug("################# visited cache hit: " + urlHash.get(urlString));
			return true;
		}

		try {
			PreparedStatement stmt = conn
					.prepareStatement("select id from web.document where url = ? and indexed is not null");
			stmt.setString(1, urlString);
			ResultSet rs = stmt.executeQuery();
			while (rs.next()) {
				id = rs.getInt(1);
				logger.debug("################# visited cache retrieval: " + id);
			}
			stmt.close();
			conn.commit();
		} catch (SQLException e) {
			logger.error("Error processing visit monitor: " + e);
			try {
				conn.rollback();
			} catch (SQLException e1) {
				try {
					conn = HTMLCrawler.getConnection();
				} catch (Exception e2) {
					logger.error("Failed to reacquire visit monitor connection: " + e2);
				}
			}
		}

		if (id > 0) {
			urlHash.put(urlString, id);
			return true;
		}

		return false;
	}

	class CacheMonitor implements Runnable {
		int interval = 30 * 60 * 1000;

		CacheMonitor() {
			logger.info("initializing memoized visited monitor...");
		}

		CacheMonitor(int interval) {
			this.interval = interval;
			logger.info("initializing memoized visited monitor...");
		}

		public void run() {
			while (true) {
				try {
					Thread.sleep(interval);
				} catch (InterruptedException e) {
				}

				logger.info("############## resetting visited cache");
				urlHash = new Hashtable<String, Integer>();
			}
		}

	}
}
