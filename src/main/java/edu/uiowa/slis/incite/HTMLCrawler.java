package edu.uiowa.slis.incite;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Observable;
import java.util.Observer;
import java.util.Properties;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import edu.uiowa.crawling.Crawler;
import edu.uiowa.crawling.CrawlerThreadFactory;
import edu.uiowa.crawling.Excluder;
import edu.uiowa.crawling.URLRequest;
import edu.uiowa.crawling.filters.domainFilter;
import edu.uiowa.crawling.filters.textFilter;
import edu.uiowa.lex.DocumentToken;
import edu.uiowa.lex.HTMLDocument;
import edu.uiowa.lex.HTMLLexer;
import edu.uiowa.lex.token;

public class HTMLCrawler implements Observer {
    static Logger logger = Logger.getLogger(HTMLCrawler.class);
	static Crawler theCrawler = null;
	static Connection conn = null;

	static void resetConnection() throws Exception {
        Class.forName("org.postgresql.Driver");
		Properties props = new Properties();
		props.setProperty("user", "eichmann");
		props.setProperty("password", "translational");
        props.setProperty("sslfactory", "org.postgresql.ssl.NonValidatingFactory");
        props.setProperty("ssl", "true");
		conn = DriverManager.getConnection("jdbc:postgresql://neuromancer.icts.uiowa.edu/incite", props);
//		conn = DriverManager.getConnection("jdbc:postgresql://localhost/incite", props);
		conn.setAutoCommit(false);
	}
	
	public static void main (String[] args) throws Exception {
		PropertyConfigurator.configure(args[0]);
		resetConnection();
		new HTMLCrawler(args);
//		(new HTMLLexer()).process(new URL(args[1]));
	}
	
	int docCount = 0;
	
	HTMLCrawler(String[] args) throws Exception {
		if (args[1].equals("-rescan")) {
				rescan(args[2]);
		} else if (args[1].equals("-pmids")) {
			rescanPMIDS();
		} else if (args[1].equals("-dois")) {
				rescanDOIS();
		} else {
			theCrawler = new Crawler(new CrawlerThreadFactory(CrawlerThreadFactory.HTML));
			Excluder.addFilter(new domainFilter(".edu"));
			theCrawler.addFilter(new domainFilter(".edu"));
			theCrawler.addFilter(new textFilter());
//			theCrawler.addFilter(new levelFilter(3));
			theCrawler.addObserver(this);

			reloadVisited();
			resetDocCount();
			
			Thread.sleep(1000);
			for (int i = 1; i < args.length; i++)
				if (args[1].equals("-restart"))
					reloadQueue();
				else 
					theCrawler.update(null, new URLRequest(args[i]));
		}
	}
	
	void rescan(String token) throws SQLException, MalformedURLException, IOException {
		HTMLLexer theLexer = new HTMLLexer();
		PreparedStatement scanStmt = conn.prepareStatement("select document.id,document.url from web.document natural join web.token where token = ? and not exists (select id from web.pmid where pmid.id=document.id) and document.id > 343796 order by document.id");
		scanStmt.setString(1, token);
		ResultSet scanRS = scanStmt.executeQuery();
		while (scanRS.next()) {
			docCount = scanRS.getInt(1);
			String url = scanRS.getString(2);
			
			deleteDocument(docCount);
			
			logger.info("scanning " + docCount + ": " + url);
			try {
				theLexer.process(new URL(url));
				storeDocument(new HTMLDocument(url, theLexer.getTitle(), theLexer.getTokens(), theLexer.wordCount(), theLexer.getURLs(), theLexer.getContentLength(), theLexer.getLastModified(), theLexer.getDois(), theLexer.getPmids()));
			} catch (Exception e) {
			}
		}
	}
	
    static Pattern pmidPat = Pattern.compile("[^0-9]*([0-9]+).*$");

    void rescanPMIDS() throws SQLException, MalformedURLException, IOException {
		HTMLLexer theLexer = new HTMLLexer();
		PreparedStatement scanStmt = conn.prepareStatement("select distinct id from web.link where url like 'http://www.ncbi.nlm.nih.gov/pubmed/%' order by id");
		ResultSet scanRS = scanStmt.executeQuery();
		while (scanRS.next()) {
			int id = scanRS.getInt(1);
			int seqnum = -1;
			logger.info("scanning id: " + id);
			
			PreparedStatement seqStmt = conn.prepareStatement("select count(*) from web.pmid where id=?");
			seqStmt.setInt(1, id);
			ResultSet seqRS = seqStmt.executeQuery();
			while (seqRS.next()) {
				seqnum = seqRS.getInt(1);
			}
			seqStmt.close();
			// seqnum now set as next to be assigned
			
			PreparedStatement urlStmt = conn.prepareStatement("select url from web.link where id = ? and url like 'http://www.ncbi.nlm.nih.gov/pubmed/%' order by url");
			urlStmt.setInt(1, id);
			ResultSet urlRS = urlStmt.executeQuery();
			while (urlRS.next()) {
				String url = urlRS.getString(1);
            	Matcher theMatcher = pmidPat.matcher(url);
            	if (theMatcher.find()) {
            		String pmidString = theMatcher.group(1).trim();
                    if (pmidString != null && pmidString.length() > 0) {
                    	try {
                        	int pmid = Integer.parseInt(theMatcher.group(1));
    	                	logger.info("\tpmid link: " + pmid + " : " + url);
    	                	
    	                	int currentSeq = -1;
    	        			PreparedStatement seq2Stmt = conn.prepareStatement("select seqnum from web.pmid where id=? and pmid=?");
    	        			seq2Stmt.setInt(1, id);
    	        			seq2Stmt.setInt(2, pmid);
    	        			ResultSet seq2RS = seq2Stmt.executeQuery();
    	        			while (seq2RS.next()) {
    	        				currentSeq = seq2RS.getInt(1);
    	        			}
    	        			seqStmt.close();
    	        			
    	        			if (currentSeq == -1) {
    	        				logger.info("\t\tadd at " + seqnum);
    	        				
    	        				PreparedStatement insStmt = conn.prepareStatement("insert into web.pmid values (?,?,?)");
    	        				insStmt.setInt(1, id);
    	        				insStmt.setInt(2, seqnum);
    	        				insStmt.setInt(3, pmid);
    	        				insStmt.execute();
    	        				insStmt.close();
    	        				
    	        				seqnum++;
    	        			}
    					} catch (NumberFormatException e) {
    					}            		
                    }
            	}
			}
			urlStmt.close();
			conn.commit();
		}
		scanStmt.close();
	}
	
    void rescanDOIS() throws SQLException, MalformedURLException, IOException {
		HTMLLexer theLexer = new HTMLLexer();
		PreparedStatement scanStmt = conn.prepareStatement("select distinct id from web.link where url like 'http://dx.doi.org/%' order by id");
		ResultSet scanRS = scanStmt.executeQuery();
		while (scanRS.next()) {
			int id = scanRS.getInt(1);
			int seqnum = -1;
			logger.info("scanning id: " + id);
			
			PreparedStatement seqStmt = conn.prepareStatement("select count(*) from web.doi where id=?");
			seqStmt.setInt(1, id);
			ResultSet seqRS = seqStmt.executeQuery();
			while (seqRS.next()) {
				seqnum = seqRS.getInt(1);
			}
			seqStmt.close();
			// seqnum now set as next to be assigned
			
			PreparedStatement urlStmt = conn.prepareStatement("select url from web.link where id = ? and url like 'http://dx.doi.org/%' order by url");
			urlStmt.setInt(1, id);
			ResultSet urlRS = urlStmt.executeQuery();
			while (urlRS.next()) {
				String url = urlRS.getString(1);
            	String doi = url.substring(18);
            	logger.info("\tdoi link: " + doi + " : " + url);

            	int currentSeq = -1;
    			PreparedStatement seq2Stmt = conn.prepareStatement("select seqnum from web.doi where id=? and doi=?");
    			seq2Stmt.setInt(1, id);
    			seq2Stmt.setString(2, doi);
    			ResultSet seq2RS = seq2Stmt.executeQuery();
    			while (seq2RS.next()) {
    				currentSeq = seq2RS.getInt(1);
    			}
    			seqStmt.close();
    			
    			if (currentSeq == -1) {
    				logger.info("\t\tadd at " + seqnum);
    				
    				PreparedStatement insStmt = conn.prepareStatement("insert into web.doi values (?,?,?)");
    				insStmt.setInt(1, id);
    				insStmt.setInt(2, seqnum);
    				insStmt.setString(3, doi);
    				insStmt.execute();
    				insStmt.close();
    				
    				seqnum++;
    			}
			}
			urlStmt.close();
			conn.commit();
		}
		scanStmt.close();
	}
	
	void deleteDocument(int id) throws SQLException {
		PreparedStatement scanStmt = conn.prepareStatement("delete from web.document where id = ?");
		scanStmt.setInt(1, id);
		scanStmt.execute();
		scanStmt.close();
	}
	
	public synchronized void update(Observable o, Object obj) {
		if (! (obj instanceof HTMLDocument))
			return;
		
		HTMLDocument theDoc = (HTMLDocument)obj;
		logger.info("HTMLCrawler updated: " + theDoc);
		try {
			storeDocument(theDoc);
		} catch (SQLException e) {
			logger.error("SQL error storing document " + theDoc.getURL() + " : " + e);
			try {
				conn.rollback();
			} catch (Exception e1) {
				logger.error("SQL error aborting transaction: " + e1);
			} finally {
				try {
					resetConnection();
				} catch (Exception e2) {
					logger.error("SQL error resetting connection: " + e2);
				}				
			}
		}
	}
	
	void storeDocument(HTMLDocument theDoc) throws SQLException {
		PreparedStatement insStmt = conn.prepareStatement("insert into web.document values (?,?,?,?,?)");
		insStmt.setInt(1, docCount);
		insStmt.setString(2, theDoc.getURL());
		insStmt.setString(3, theDoc.getTitle());
		insStmt.setInt(4, theDoc.getContentLength());
		insStmt.setTimestamp(5, new Timestamp(theDoc.getLastModified()));
		insStmt.execute();
		insStmt.close();
		
		PreparedStatement linkStmt = conn.prepareStatement("insert into web.link values (?,?,?)");
		int linkCount = 0;
		for (token url : (Vector<token>)theDoc.getLinks()) {
			linkStmt.setInt(1, docCount);
			linkStmt.setInt(2, linkCount++);
			linkStmt.setString(3, url.value);
			linkStmt.addBatch();
		}
		linkStmt.executeBatch();
		linkStmt.close();
		
		PreparedStatement tokenStmt = conn.prepareStatement("insert into web.token values (?,?,?,?)");
		for (DocumentToken token : (Vector<DocumentToken>)theDoc.getTokens()) {
			tokenStmt.setInt(1, docCount);
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
			doiStmt.setInt(1, docCount);
			doiStmt.setInt(2, doiCount++);
			doiStmt.setString(3, doi);
			doiStmt.addBatch();
		}
		doiStmt.executeBatch();
		doiStmt.close();
		
		PreparedStatement pmidStmt = conn.prepareStatement("insert into web.pmid values (?,?,?)");
		int pmidCount = 0;
		for (int pmid : (Vector<Integer>)theDoc.getPmids()) {
			pmidStmt.setInt(1, docCount);
			pmidStmt.setInt(2, pmidCount++);
			pmidStmt.setInt(3, pmid);
			pmidStmt.addBatch();
		}
		pmidStmt.executeBatch();
		pmidStmt.close();
		
		if (docCount % 100 == 0)
			conn.commit();
		docCount++;		
	}
	
	void reloadVisited() throws SQLException, MalformedURLException {
		PreparedStatement stmt = conn.prepareStatement("select url from web.document order by url");
		ResultSet rs = stmt.executeQuery();
		while (rs.next()) {
			String url = rs.getString(1);
			logger.debug("visited: " + url);
			URLRequest theRequest = new URLRequest(url);
			theCrawler.addVisited(theRequest);
		}
		stmt.close();
	}

	void resetDocCount() throws SQLException, MalformedURLException {
		PreparedStatement stmt = conn.prepareStatement("select max(id) from web.document");
		ResultSet rs = stmt.executeQuery();
		while (rs.next()) {
			docCount = rs.getInt(1) + 1;
			logger.info("resetting docCount to: " + docCount);
		}
		stmt.close();
	}

	void reloadQueue() throws SQLException, MalformedURLException {
//		PreparedStatement stmt = conn.prepareStatement("select distinct url, length(url) from web.link where url ~ '^http:.*\\.edu.*\\.html$' and not exists (select url from web.document where document.url = link.url) and length(url) < 200 order by length(url) limit 200");
		PreparedStatement stmt = conn.prepareStatement("select url from web.link where url ~ '^http://[w].*/$' and length(url)<60 limit 1000");
		ResultSet rs = stmt.executeQuery();
		while (rs.next()) {
			String url = rs.getString(1);
			logger.info("queueing: " + url);
			URLRequest theRequest = new URLRequest(url);
			theCrawler.addURL(theRequest);
		}
		stmt.close();
	}

}
