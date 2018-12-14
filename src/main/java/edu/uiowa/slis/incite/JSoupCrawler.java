package edu.uiowa.slis.incite;

import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Hashtable;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;

import edu.uiowa.crawling.Excluder;
import edu.uiowa.crawling.URLRequest;
import edu.uiowa.crawling.filters.domainFilter;
import edu.uiowa.crawling.filters.domainPoolFilter;
import edu.uiowa.crawling.filters.domainPrefixFilter;
import edu.uiowa.lex.HTMLDocument;
import edu.uiowa.lex.HTMLLink;

public class JSoupCrawler {
    static Logger logger = Logger.getLogger(JSoupCrawler.class);
    static Connection conn = null;
    static Hashtable<String, Integer> domainHash = new Hashtable<String, Integer>();
    static Hashtable<String, Integer> urlHash = new Hashtable<String, Integer>();

    static Connection getConnection() throws Exception {
	Connection conn = null;
	Class.forName("org.postgresql.Driver");
	Properties props = new Properties();
	props.setProperty("user", "eichmann");
	props.setProperty("password", "translational");
	// props.setProperty("sslfactory",
	// "org.postgresql.ssl.NonValidatingFactory");
	// props.setProperty("ssl", "true");
	// conn =
	// DriverManager.getConnection("jdbc:postgresql://neuromancer.icts.uiowa.edu/incite",
	// props);
	conn = DriverManager.getConnection("jdbc:postgresql://localhost/incite", props);
	conn.setAutoCommit(false);
	// execute(conn, "set session enable_seqscan = off");
	// execute(conn, "set session random_page_cost = 1");
	return conn;
    }

    public static void main(String[] args) throws Exception {
	PropertyConfigurator.configure(args[0]);
	conn = getConnection();
	// testing(args[1]);
	new JSoupCrawler(args);
	// (new HTMLLexer()).process(new URL(args[1]));
    }

    Pattern suffixPat = Pattern.compile(".*?(\\.[^./]+(\\.gz)?)$");

    JSoupCrawler(String[] args) throws Exception {
	initializeSeeds();
	PreparedStatement filterStmt = conn.prepareStatement("select id,url from jsoup.document where indexed is null");
	ResultSet filterRS = filterStmt.executeQuery();
	while (filterRS.next()) {
	    int id = filterRS.getInt(1);
	    String url = filterRS.getString(2);

	    org.jsoup.Connection connection = null;
	    try {
		connection = Jsoup.connect(url).timeout(5000);
		Document document = connection.get();
		storeURLs(document);
		storeDocument(id, document);

	    } catch (Exception e) {
		logger.error("exception raised: ", e);
		String contentType = connection.response().contentType();
		int statusCode = connection.response().statusCode();
		logger.info("status code: " + statusCode + "\tcontent type: " + contentType);
		PreparedStatement tagStmt = conn.prepareStatement("update jsoup.document set indexed = now() where id = ?");
		tagStmt.setInt(1, id);
		tagStmt.execute();
		tagStmt.close();
		conn.commit();
	    }

	}
	filterStmt.close();
    }

    void initializeSeeds() throws SQLException {
	PreparedStatement filterStmt = conn.prepareStatement("select institution,domain,prefix,seed from jsoup.crawler_seed");
	ResultSet filterRS = filterStmt.executeQuery();
	while (filterRS.next()) {
	    String institution = filterRS.getString(1);
	    String domain = filterRS.getString(2);
	    String prefix = filterRS.getString(3);
	    String seed = filterRS.getString(4);

	    logger.info("seed: " + seed);
	    int did = 0;
	    PreparedStatement insert = conn.prepareStatement("insert into jsoup.institution(domain) values(?)", Statement.RETURN_GENERATED_KEYS);
	    insert.setString(1, institution);
	    insert.execute();
	    ResultSet rs = insert.getGeneratedKeys();
	    while (rs.next()) {
		did = rs.getInt(1);
		logger.info("institution: " + institution + "\tdid: " + did);
	    }
	    insert.close();

	    PreparedStatement docStmt = conn.prepareStatement("insert into jsoup.document(url,did) values (?, ?)");
	    docStmt.setString(1, seed);
	    docStmt.setInt(2, did);
	    docStmt.execute();
	    docStmt.close();
	}
	conn.commit();

    }

    void storeURLs(Document document) throws SQLException, MalformedURLException {
	Elements links = document.getElementsByTag("a");
	for (Element linkElement : links) {
	    logger.info("linkElement: " + linkElement);
	    URL theURL = null;
	    try {
		theURL = new URL(linkElement.absUrl("href"));
	    } catch (Exception e1) {
		continue;
	    }
	    String theURLString = theURL.toString();
	    URLRequest link = new URLRequest(theURLString);
	    String hostname = theURL.getHost();
	    logger.debug("host name: " + hostname);

	    if (hostname == null)
		continue;

	    String part1 = null;
	    String part2 = null;
	    StringTokenizer theTokenizer = new StringTokenizer(hostname, ".");
	    while (theTokenizer.hasMoreTokens()) {
		part1 = part2;
		part2 = theTokenizer.nextToken();
	    }

	    if (part1 == null || part2 == null)
		continue;

	    String domainname = part1 + "." + part2;
	    int did = 0;
	    String suffix = null;

	    Matcher theMatcher = suffixPat.matcher(theURL.getPath());
	    if (theMatcher.find()) {
		suffix = theMatcher.group(1);
	    }
	    if (suffix != null && suffix.indexOf(' ') > 0)
		suffix = suffix.substring(0, suffix.indexOf(' '));
	    if (suffix != null && suffix.indexOf(';') > 0)
		suffix = suffix.substring(0, suffix.indexOf(';'));
	    if (suffix != null && suffix.length() > 10)
		suffix = null;

	    logger.debug("domain name: " + domainname);
	    logger.debug("suffix: " + suffix);

	    if (domainHash.containsKey(domainname)) {
		did = domainHash.get(domainname);
		logger.debug("\tcached did: " + did);
	    } else {
		try {
		    PreparedStatement insert = conn.prepareStatement("insert into jsoup.institution(domain) values(?)", Statement.RETURN_GENERATED_KEYS);
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
			PreparedStatement select = conn.prepareStatement("select did from jsoup.institution where domain = ?");
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

	    logger.debug("link url: " + theURLString);
	    if (urlHash.containsKey(theURLString)) {
		link.setID(urlHash.get(theURLString));
		logger.debug("\tcached id: " + theURLString);
	    } else {
		try {
		    PreparedStatement insert = conn.prepareStatement("insert into jsoup.document(url,did,suffix) values(?,?,?)",
			    Statement.RETURN_GENERATED_KEYS);
		    insert.setString(1, theURLString);
		    insert.setInt(2, did);
		    insert.setString(3, suffix);
		    insert.execute();
		    ResultSet rs = insert.getGeneratedKeys();
		    while (rs.next()) {
			link.setID(rs.getInt(1));
			logger.debug("\tid: " + theURLString);
		    }
		} catch (SQLException e) {
		    if (e.getSQLState().equals("23505")) {
			conn.rollback();
			PreparedStatement select = conn.prepareStatement("select id from jsoup.document where url = ?");
			select.setString(1, theURLString);
			ResultSet rs = select.executeQuery();
			while (rs.next()) {
			    link.setID(rs.getInt(1));
			    logger.debug("\texisting id: " + theURLString);
			}

		    } else {
			e.printStackTrace();
		    }
		} finally {
		    conn.commit();
		}

		urlHash.put(theURLString, link.getID());

	    }
	}
    }
    
    void getTextBlocks(Vector<String> blocks, Element element) {
	if (element.children().isEmpty()) {
	    String elementText = element.text().trim();
	    if (elementText.length() > 0)
		blocks.add(elementText);
	}
	for (Element child : element.children()) {
	    getTextBlocks(blocks, child);
	}

    }

    void storeDocument(int id, Document document) throws SQLException {
	PreparedStatement insStmt = conn.prepareStatement("update jsoup.document set title = ?, length = ?, indexed = now() where id = ?");
	insStmt.setString(1, document.title());
	insStmt.setInt(2, document.toString().length());
	insStmt.setInt(3, id);
	insStmt.execute();
	insStmt.close();
	
	logger.info("document text: " + document.text());
	Vector<String> blocks = new Vector<String>();
	getTextBlocks(blocks, document);
	for (String block : blocks) {
	    logger.info("text block: " + block);
	    
	}

//	if (HTMLCrawler.cleanup) {
//	    PreparedStatement delStmt = conn.prepareStatement("delete from web.hyperlink where id = ?");
//	    delStmt.setInt(1, id);
//	    delStmt.execute();
//	    delStmt.close();
//
//	    delStmt = conn.prepareStatement("delete from web.token where id = ?");
//	    delStmt.setInt(1, id);
//	    delStmt.execute();
//	    delStmt.close();
//
//	    delStmt = conn.prepareStatement("delete from web.doi where id = ?");
//	    delStmt.setInt(1, id);
//	    delStmt.execute();
//	    delStmt.close();
//
//	    delStmt = conn.prepareStatement("delete from web.pmid where id = ?");
//	    delStmt.setInt(1, id);
//	    delStmt.execute();
//	    delStmt.close();
//	}

	PreparedStatement linkStmt = conn.prepareStatement("insert into jsoup.hyperlink values (?,?,?,?)");
	PreparedStatement mailStmt = conn.prepareStatement("insert into jsoup.email values (?,?,?,?)");
	int linkCount = 0;
	int mailCount = 0;
	Elements links = document.getElementsByTag("a");
	for (Element link : links) {
	    String linkHref = link.absUrl("href");
	    String linkText = link.text();
	    logger.info("\tlink: " + linkHref + "\tanchor: " + linkText);
	    
	    if (linkHref.startsWith("mailto:")) {
		mailStmt.setInt(1, id);
		mailStmt.setInt(2, mailCount++);
		mailStmt.setString(3, linkHref);
		mailStmt.setString(4, linkText);
		mailStmt.addBatch();
	    } else if (urlHash.containsKey(linkHref)) {
		int linkID = urlHash.get(linkHref);
		logger.debug("doc id: " + id + "\tlink id: " + linkID + "\tanchor: " + linkText);
		linkStmt.setInt(1, id);
		linkStmt.setInt(2, linkCount++);
		linkStmt.setInt(3, linkID);
		linkStmt.setString(4, linkText);
		linkStmt.addBatch();
	    }
	}
	linkStmt.executeBatch();
	linkStmt.close();
	mailStmt.executeBatch();
	mailStmt.close();
	conn.commit();
    }

    class URLnode {

    }
}
