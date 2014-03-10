package edu.uiowa.slis.incite;

import java.net.MalformedURLException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Hashtable;
import java.util.Observable;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import edu.uiowa.crawling.URLRequest;
import edu.uiowa.lex.DocumentToken;
import edu.uiowa.lex.HTMLDocument;
import edu.uiowa.lex.HTMLLink;
import edu.uiowa.util.Generator;

public class ConnectionGenerator extends Generator {
    static Logger logger = Logger.getLogger(HTMLCrawler.class);

	static Hashtable<String,Integer> domainHash = new Hashtable<String, Integer>();
	static Hashtable<String,Integer> urlHash = new Hashtable<String, Integer>();

    Connection conn = null;
	
	public ConnectionGenerator(Connection conn) {
		this.conn = conn;
	}

	public synchronized void update(Observable o, Object obj) {
		if (! (obj instanceof HTMLDocument))
			return;
		
		HTMLDocument theDoc = (HTMLDocument)obj;
		logger.info("HTMLCrawler updated: " + theDoc);
		try {
			storeURLs(theDoc);
			storeDocument(theDoc);

			for (HTMLLink newLink : theDoc.getLinks()) {
				try {
					HTMLCrawler.theCrawler.update(this, new URLRequest(newLink.getID(), newLink.getUrl(), theDoc.getLevel() + 1));
				} catch (MalformedURLException e) {
					logger.debug("malformed url for queue: " + newLink.getUrl());
				}
			}	
		} catch (SQLException e) {
			logger.error("SQL error storing document " + theDoc.getID() + " : " + theDoc.getURL() + " : " + e);
			try {
				conn.rollback();
			} catch (Exception e1) {
				logger.error("SQL error aborting transaction: " + e1);
			} finally {
				try {
					conn.close();
					conn = HTMLCrawler.getConnection();
				} catch (Exception e2) {
					logger.error("SQL error resetting connection: " + e2);
					try {
						conn = HTMLCrawler.getConnection();
					} catch (Exception e1) {
						logger.error("SQL error resetting connection: " + e1);
					}
				}				
			}
		}
	}
	
	Pattern suffixPat = Pattern.compile(".*?(\\.[^./]+(\\.gz)?)$");
	void storeURLs(HTMLDocument theDoc) throws SQLException {
		for (HTMLLink link : theDoc.getLinks()) {
			String hostname = link.getHostname();
			logger.debug("host name: " + hostname);
			
			if (hostname == null)
				continue;
			
			String part1 = null;
			String part2 = null;
			StringTokenizer theTokenizer = new StringTokenizer(hostname,".");
			while (theTokenizer.hasMoreTokens()) {
				part1 = part2;
				part2 = theTokenizer.nextToken();
			}
			
			if (part1 == null || part2 == null)
				continue;
			
			String domainname = part1 + "." + part2;
			int did = 0;
			String suffix = null;
			
			Matcher theMatcher = suffixPat.matcher(link.getPath());
			if (theMatcher.find()) {
				suffix = theMatcher.group(1);
			}

			logger.debug("domain name: " + domainname);
			logger.debug("suffix: " + suffix);
			
			if (domainHash.containsKey(domainname)) {
				did = domainHash.get(domainname);
				logger.debug("\tcached did: " + did);
			} else {
				try {
					PreparedStatement insert = conn.prepareStatement("insert into web.institution(domain) values(?)", Statement.RETURN_GENERATED_KEYS);
					insert.setString(1, domainname);
					insert.execute();
					ResultSet rs = insert.getGeneratedKeys();
					while (rs.next()) {
						did = rs.getInt(1);
						logger.debug("\tdid: " + did);
					}
				} catch (SQLException e) {
					if (e.getSQLState().equals("23505")) {
						conn.rollback();
						PreparedStatement select = conn.prepareStatement("select did from web.institution where domain = ?");
						select.setString(1, domainname);
						ResultSet rs = select.executeQuery();
						while (rs.next()) {
							did = rs.getInt(1);
							logger.debug("\texisting id: " + did);
						}
						
					} else {
						e.printStackTrace();				
					}
				} finally {
					conn.commit();
				}

				domainHash.put(domainname, did);
			}

			logger.debug("link url: " + link.getUrl());
			if (urlHash.containsKey(link.getUrl())) {
				link.setID(urlHash.get(link.getUrl()));
				logger.debug("\tcached id: " + link.getID());
			} else {
				try {
					PreparedStatement insert = conn.prepareStatement("insert into web.document(url,did,suffix) values(?,?,?)", Statement.RETURN_GENERATED_KEYS);
					insert.setString(1, link.getUrl());
					insert.setInt(2, did);
					insert.setString(3, suffix);
					insert.execute();
					ResultSet rs = insert.getGeneratedKeys();
					while (rs.next()) {
						link.setID(rs.getInt(1));
						logger.debug("\tid: " + link.getID());
					}
				} catch (SQLException e) {
					if (e.getSQLState().equals("23505")) {
						conn.rollback();
						PreparedStatement select = conn.prepareStatement("select id from web.document where url = ?");
						select.setString(1, link.getUrl());
						ResultSet rs = select.executeQuery();
						while (rs.next()) {
							link.setID(rs.getInt(1));
							logger.debug("\texisting id: " + link.getID());
						}
						
					} else {
						e.printStackTrace();				
					}
				} finally {
					conn.commit();
				}

				urlHash.put(link.getUrl(), link.getID());
			}
		}
	}
	
	void storeDocument(HTMLDocument theDoc) throws SQLException {
		PreparedStatement insStmt = conn.prepareStatement("update web.document set title = ?, length = ?, modified = ?, indexed = now(), response_code = ? where id = ?");
		insStmt.setString(1, theDoc.getTitle());
		insStmt.setInt(2, theDoc.getContentLength());
		insStmt.setTimestamp(3, new Timestamp(theDoc.getLastModified()));
		insStmt.setInt(4, theDoc.getResponseCode());
		insStmt.setInt(5, theDoc.getID());
		insStmt.execute();
		insStmt.close();
		
		PreparedStatement linkStmt = conn.prepareStatement("insert into web.hyperlink values (?,?,?,?)");
		int linkCount = 0;
		for (HTMLLink link : theDoc.getLinks()) {
			logger.debug("doc id: " + theDoc.getID() + "\tlink id: " + link.getID() + "\tanchor: " + link.getAnchor());
			linkStmt.setInt(1, theDoc.getID());
			linkStmt.setInt(2, linkCount++);
			linkStmt.setInt(3, link.getID());
			linkStmt.setString(4, link.getAnchor());
			linkStmt.addBatch();
		}
		linkStmt.executeBatch();
		linkStmt.close();
		
		PreparedStatement tokenStmt = conn.prepareStatement("insert into web.token values (?,?,?,?)");
		for (DocumentToken token : (Vector<DocumentToken>)theDoc.getTokens()) {
			tokenStmt.setInt(1, theDoc.getID());
			tokenStmt.setString(2, token.getToken());
			tokenStmt.setInt(3, token.getCount());
			tokenStmt.setDouble(4, token.getFrequency());
			tokenStmt.addBatch();
		}
		tokenStmt.executeBatch();
		tokenStmt.close();
		
		PreparedStatement doiStmt = conn.prepareStatement("insert into web.doi values (?,?,?)");
		int doiCount = 0;
		for (String doi : (Vector<String>)theDoc.getDois()) {
			doiStmt.setInt(1, theDoc.getID());
			doiStmt.setInt(2, doiCount++);
			doiStmt.setString(3, doi);
			doiStmt.addBatch();
		}
		doiStmt.executeBatch();
		doiStmt.close();
		
		PreparedStatement pmidStmt = conn.prepareStatement("insert into web.pmid values (?,?,?)");
		int pmidCount = 0;
		for (int pmid : (Vector<Integer>)theDoc.getPmids()) {
			pmidStmt.setInt(1, theDoc.getID());
			pmidStmt.setInt(2, pmidCount++);
			pmidStmt.setInt(3, pmid);
			pmidStmt.addBatch();
		}
		pmidStmt.executeBatch();
		pmidStmt.close();
		
		conn.commit();
	}
	
}
