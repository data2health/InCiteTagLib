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

public class JSoupCrawler implements Runnable {
    static Logger logger = Logger.getLogger(JSoupCrawler.class);
    static Connection mainConn = null;
    static Hashtable<String, Integer> domainHash = new Hashtable<String, Integer>();
    static Hashtable<String, Integer> urlHash = new Hashtable<String, Integer>();
    static QueueManager queueManager = null;

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
	mainConn = getConnection();
//	initializeSeeds();
	
	if (args.length == 1)
	    queueManager = new QueueManager(mainConn);
	else {
	    Vector<String> domains = new Vector<String>();
	    for (int i = 1; i < args.length; i++)
		domains.add(args[i]);
	    queueManager = new QueueManager(mainConn, domains);
	}

	int maxCrawlerThreads = Runtime.getRuntime().availableProcessors();
	Thread[] scannerThreads = new Thread[maxCrawlerThreads];
	for (int i = 0; i < maxCrawlerThreads; i++) {
	    logger.info("[" + i + "] initiating");
	    Thread theThread = new Thread(new JSoupCrawler(i));
	    theThread.setPriority(Math.max(theThread.getPriority() - 2, Thread.MIN_PRIORITY));
	    theThread.start();
	    scannerThreads[i] = theThread;
	}
	for (int i = 0; i < maxCrawlerThreads; i++) {
	    scannerThreads[i].join();
	}
	queueManager.monitorThread.join();

	// testing(args[1]);
//	new JSoupCrawler(args);
	// (new HTMLLexer()).process(new URL(args[1]));
    }

    Pattern suffixPat = Pattern.compile(".*?(\\.[^./]+(\\.gz)?)$");
    int threadID = 0;
    Connection conn = null;

    JSoupCrawler(int threadID) throws Exception {
	this.threadID = threadID;
	conn = getConnection();
    }
    
    public void run() {
	QueueRequest request = null;
	do {
	    request = queueManager.nextRequest();
	    if (request != null) {
		logger.info("[" + threadID + "] request: " + request);
		processURL(request);
	    } else {
		if (queueManager.completed()) {
		    logger.info("[" + threadID + "] terminating");
		    return;
		}
		try {
		    Thread.sleep(5 * 1000);
		} catch (InterruptedException e) { }
	    }
	} while (true);
    }
    
    void processURL(QueueRequest request) {
	if (request.url.length() > 300) {
	    setIndexed(request);
	    return;
	}
	org.jsoup.Connection connection = null;
	try {
	    connection = Jsoup.connect(request.url).timeout(5000);
	    Document document = connection.get();

	    storeURLs(document);
	    storeDocument(request.ID, document);

	} catch (Exception e) {
	    logger.error("[" + threadID + "] exception raised: " + e);
	    String contentType = connection.response().contentType();
	    int statusCode = connection.response().statusCode();
	    logger.info("[" + threadID + "] status code: " + statusCode + "\tcontent type: " + contentType);
	    setIndexed(request);
	}
    }
    
    void setIndexed(QueueRequest request) {
	try {
	    PreparedStatement tagStmt = conn.prepareStatement("update jsoup.document set indexed = now() where id = ?");
	    tagStmt.setInt(1, request.ID);
	    tagStmt.execute();
	    tagStmt.close();
	    conn.commit();
	} catch (SQLException e) {
	    logger.error("[" + threadID + "] sql exception raised: ", e);
	}
    }
    
    static void executeStatement(String statement) throws SQLException {
	logger.info("executing: " + statement);
	PreparedStatement resetInstStmt = mainConn.prepareStatement(statement);
	resetInstStmt.execute();
	resetInstStmt.close();
    }

    static void initializeSeeds() throws SQLException {
	executeStatement("truncate jsoup.institution,jsoup.document,jsoup.hyperlink,jsoup.email,jsoup.segment,jsoup.meta");
	executeStatement("alter sequence web.institution_did_seq restart with 1");
	executeStatement("alter sequence web.document_id_seq restart with 1");

	PreparedStatement filterStmt = mainConn.prepareStatement("select institution,domain,prefix,seed from jsoup.crawler_seed");
	ResultSet filterRS = filterStmt.executeQuery();
	while (filterRS.next()) {
	    String institution = filterRS.getString(1);
	    String domain = filterRS.getString(2);
	    String prefix = filterRS.getString(3);
	    String seed = filterRS.getString(4);

	    logger.info("[++] seed: " + seed);
	    int did = 0;
	    PreparedStatement insert = mainConn.prepareStatement("insert into jsoup.institution(domain) values(?)", Statement.RETURN_GENERATED_KEYS);
	    insert.setString(1, institution);
	    insert.execute();
	    ResultSet rs = insert.getGeneratedKeys();
	    while (rs.next()) {
		did = rs.getInt(1);
		logger.info("[++] institution: " + institution + "\tdid: " + did);
	    }
	    insert.close();

	    PreparedStatement docStmt = mainConn.prepareStatement("insert into jsoup.document(url,did) values (?, ?)");
	    docStmt.setString(1, seed);
	    docStmt.setInt(2, did);
	    docStmt.execute();
	    docStmt.close();
	}
	mainConn.commit();

    }

    void storeURLs(Document document) throws SQLException, MalformedURLException {
	Elements links = document.getElementsByTag("a");
	for (Element linkElement : links) {
	    logger.debug("[" + threadID + "] linkElement: " + linkElement);
	    URL theURL = null;
	    try {
		theURL = new URL(linkElement.absUrl("href"));
	    } catch (Exception e1) {
		continue;
	    }
	    String theURLString = theURL.toString();
	    URLRequest link = new URLRequest(theURLString);
	    String hostname = theURL.getHost();
	    logger.debug("[" + threadID + "] host name: " + hostname);

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

	    logger.debug("[" + threadID + "] domain name: " + domainname);
	    logger.debug("[" + threadID + "] suffix: " + suffix);

	    if (domainHash.containsKey(domainname)) {
		did = domainHash.get(domainname);
		logger.debug("[" + threadID + "]\tcached did: " + did);
	    } else {
		try {
		    PreparedStatement insert = conn.prepareStatement("insert into jsoup.institution(domain) values(?)", Statement.RETURN_GENERATED_KEYS);
		    insert.setString(1, domainname);
		    insert.execute();
		    ResultSet rs = insert.getGeneratedKeys();
		    while (rs.next()) {
			did = rs.getInt(1);
			logger.debug("[" + threadID + "]\tdid: " + did);
		    }
		} catch (SQLException e) {
		    if (e.getSQLState().equals("23505")) {
			conn.rollback();
			PreparedStatement select = conn.prepareStatement("select did from jsoup.institution where domain = ?");
			select.setString(1, domainname);
			ResultSet rs = select.executeQuery();
			while (rs.next()) {
			    did = rs.getInt(1);
			    logger.debug("[" + threadID + "]\texisting id: " + did);
			}

		    } else {
			e.printStackTrace();
		    }
		} finally {
		    conn.commit();
		}

		domainHash.put(domainname, did);
	    }

	    logger.debug("[" + threadID + "] link url: " + theURLString);
	    if (urlHash.containsKey(theURLString)) {
		link.setID(urlHash.get(theURLString));
		logger.debug("[" + threadID + "]\tcached id: " + theURLString);
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
			logger.debug("[" + threadID + "]\tid: " + theURLString);
		    }
		} catch (SQLException e) {
		    if (e.getSQLState().equals("23505")) {
			conn.rollback();
			PreparedStatement select = conn.prepareStatement("select id from jsoup.document where url = ?");
			select.setString(1, theURLString);
			ResultSet rs = select.executeQuery();
			while (rs.next()) {
			    link.setID(rs.getInt(1));
			    logger.debug("[" + threadID + "]\texisting id: " + theURLString);
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
    
    void getTextBlocks(Vector<TextBlock> blocks, Element element) {
	if (element.children().isEmpty()) {
	    String elementText = element.text().trim();
	    if (elementText.length() > 0)
		blocks.add(new TextBlock(element.tagName(), elementText));
	}
	for (Element child : element.children()) {
	    getTextBlocks(blocks, child);
	}

    }
    
    void storeMeta(int id, String name, String content) throws SQLException {
	logger.debug("[" + threadID + "] meta name: " + name + "\tcontent: " + content);
	PreparedStatement metaStmt = conn.prepareStatement("insert into jsoup.meta values(?,?,?)");
	metaStmt.setInt(1, id);
	metaStmt.setString(2, name);
	metaStmt.setString(3, content);
	metaStmt.execute();
	metaStmt.close();
    }

    void storeDocument(int id, Document document) throws SQLException {
	PreparedStatement insStmt = conn.prepareStatement("update jsoup.document set title = ?, length = ?, indexed = now() where id = ?");
	insStmt.setString(1, document.title());
	insStmt.setInt(2, document.toString().length());
	insStmt.setInt(3, id);
	insStmt.execute();
	insStmt.close();

	for (Element metaTag : document.getElementsByTag("meta")) {
	    String content = metaTag.attr("content");
	    String name = metaTag.attr("name").trim();
	    String property = metaTag.attr("property").trim();
	    String itemprop = metaTag.attr("itemprop").trim();
	    if (name != null && name.length() > 0)
		storeMeta(id, name, content);
	    else if (property != null && property.length() > 0)
		storeMeta(id, property, content);
	    else if (itemprop != null && itemprop.length() > 0)
		storeMeta(id, itemprop, content);
	    else
		logger.debug("[" + threadID + "] meta ***: " + metaTag);
	}
	
	logger.debug("[" + threadID + "] document text: " + document.text());
	int blockCount = 0;
	Vector<TextBlock> blocks = new Vector<TextBlock>();
	getTextBlocks(blocks, document);
	for (TextBlock block : blocks) {
	    logger.debug("[" + threadID + "] text block: " + block);
	    PreparedStatement blockStmt = conn.prepareStatement("insert into jsoup.segment values(?,?,?,?)");
	    blockStmt.setInt(1, id);
	    blockStmt.setInt(2, blockCount++);
	    blockStmt.setString(3, block.tag);
	    blockStmt.setString(4, block.content);
	    blockStmt.execute();
	    blockStmt.close();	    
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
	    logger.debug("[" + threadID + "]\tlink: " + linkHref + "\tanchor: " + linkText);
	    
	    if (linkHref.startsWith("mailto:")) {
		mailStmt.setInt(1, id);
		mailStmt.setInt(2, mailCount++);
		mailStmt.setString(3, linkHref);
		mailStmt.setString(4, linkText);
		mailStmt.addBatch();
	    } else if (urlHash.containsKey(linkHref)) {
		int linkID = urlHash.get(linkHref);
		logger.debug("[" + threadID + "] doc id: " + id + "\tlink id: " + linkID + "\tanchor: " + linkText);
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

    class TextBlock {
	String tag = null;
	String content = null;
	
	public TextBlock(String tag, String content) {
	    this.tag = tag;
	    this.content = content;
	}
	
	public String toString() {
	    return tag + ": " + content;
	}
    }
}
