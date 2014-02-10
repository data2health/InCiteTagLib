package edu.uiowa.slis.incite;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Observable;
import java.util.Observer;
import java.util.Properties;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import edu.uiowa.crawling.Crawler;
import edu.uiowa.crawling.CrawlerThreadFactory;
import edu.uiowa.crawling.URLRequest;
import edu.uiowa.crawling.filters.domainFilter;
import edu.uiowa.crawling.filters.levelFilter;
import edu.uiowa.crawling.filters.textFilter;
import edu.uiowa.lex.DocumentToken;
import edu.uiowa.lex.HTMLDocument;
import edu.uiowa.lex.token;

public class HTMLCrawler implements Observer {
    static Logger logger = Logger.getLogger(HTMLCrawler.class);
	static Crawler theCrawler = new Crawler(new CrawlerThreadFactory(CrawlerThreadFactory.HTML));
	static Connection conn = null;

	public static void main (String[] args) throws Exception {
		PropertyConfigurator.configure(args[0]);

        Class.forName("org.postgresql.Driver");
		Properties props = new Properties();
		props.setProperty("user", "eichmann");
		props.setProperty("password", "translational");
		conn = DriverManager.getConnection("jdbc:postgresql://localhost/incite", props);
		conn.setAutoCommit(false);

		new HTMLCrawler(args);
	}
	
	int docCount = 0;
	
	HTMLCrawler(String[] args) throws Exception {
		theCrawler.addFilter(new domainFilter(".edu"));
		theCrawler.addFilter(new textFilter());
//		theCrawler.addFilter(new levelFilter(3));
		theCrawler.addObserver(this);
		
		Thread.sleep(1000);
		for (int i = 1; i < args.length; i++)
			theCrawler.update(null, new URLRequest(args[i]));
	}
	
	public synchronized void update(Observable o, Object obj) {
		if (! (obj instanceof HTMLDocument))
			return;
		
		HTMLDocument theDoc = (HTMLDocument)obj;
		logger.info("HTMLCrawler updated: " + theDoc);
		try {
			storeDocument(theDoc);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	void storeDocument(HTMLDocument theDoc) throws SQLException {
		PreparedStatement insStmt = conn.prepareStatement("insert into web.document values (?,?,?)");
		insStmt.setInt(1, docCount);
		insStmt.setString(2, theDoc.getURL());
		insStmt.setString(3, theDoc.getTitle());
		insStmt.execute();
		insStmt.close();
		
		int linkCount = 0;
		for (token url : (Vector<token>)theDoc.getLinks()) {
			PreparedStatement linkStmt = conn.prepareStatement("insert into web.link values (?,?,?)");
			linkStmt.setInt(1, docCount);
			linkStmt.setInt(2, linkCount++);
			linkStmt.setString(3, url.value);
			linkStmt.execute();
			linkStmt.close();
		}
		
		for (DocumentToken token : (Vector<DocumentToken>)theDoc.getTokens()) {
			PreparedStatement tokenStmt = conn.prepareStatement("insert into web.token values (?,?,?,?)");
			tokenStmt.setInt(1, docCount);
			tokenStmt.setString(2, token.getToken());
			tokenStmt.setInt(3, token.getCount());
			tokenStmt.setDouble(4, token.getFrequency());
			tokenStmt.execute();
			tokenStmt.close();
		}
		
		conn.commit();
		docCount++;		
	}

}
