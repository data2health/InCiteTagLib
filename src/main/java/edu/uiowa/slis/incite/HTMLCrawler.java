package edu.uiowa.slis.incite;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
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
import edu.uiowa.crawling.filters.domainPoolFilter;
import edu.uiowa.crawling.filters.domainPrefixFilter;
import edu.uiowa.crawling.filters.pathLengthFilter;
import edu.uiowa.crawling.filters.textFilter;
import edu.uiowa.lex.HTMLDocument;
import edu.uiowa.lex.HTMLLexer;
import edu.uiowa.util.PooledGenerator;

public class HTMLCrawler implements Observer {
    static Logger logger = Logger.getLogger(HTMLCrawler.class);
    static Crawler theCrawler = null;
    static Connection conn = null;

    static boolean cleanup = false;

    static Connection getConnection() throws Exception {
	Connection conn = null;
	Class.forName("org.postgresql.Driver");
	Properties props = new Properties();
	props.setProperty("user", "eichmann");
	props.setProperty("password", "translational");
//	props.setProperty("sslfactory", "org.postgresql.ssl.NonValidatingFactory");
//	props.setProperty("ssl", "true");
//	conn = DriverManager.getConnection("jdbc:postgresql://neuromancer.icts.uiowa.edu/incite", props);
	conn = DriverManager.getConnection("jdbc:postgresql://localhost/incite", props);
	conn.setAutoCommit(false);
	// execute(conn, "set session enable_seqscan = off");
	// execute(conn, "set session random_page_cost = 1");
	return conn;
    }

    static void execute(Connection conn, String statement) throws SQLException {
	logger.debug("executing " + statement + "...");
	PreparedStatement stmt = conn.prepareStatement(statement);
	stmt.executeUpdate();
	stmt.close();
    }

    public static void main(String[] args) throws Exception {
	PropertyConfigurator.configure(args[0]);
	conn = getConnection();
	// testing(args[1]);
	new HTMLCrawler(args);
	// (new HTMLLexer()).process(new URL(args[1]));
    }

    static void testing(String name) throws SQLException {
	try {
	    PreparedStatement insert = conn.prepareStatement("insert into testing(label) values(?)", Statement.RETURN_GENERATED_KEYS);
	    insert.setString(1, name);
	    insert.execute();
	    ResultSet rs = insert.getGeneratedKeys();
	    while (rs.next()) {
		int id = rs.getInt(1);
		System.out.println("id: " + id);
	    }
	} catch (SQLException e) {
	    if (e.getSQLState().equals("23505")) {
		conn.rollback();
		PreparedStatement select = conn.prepareStatement("select id from testing where label = ?");
		select.setString(1, name);
		ResultSet rs = select.executeQuery();
		while (rs.next()) {
		    int id = rs.getInt(1);
		    System.out.println("existing id: " + id);
		}

	    } else {
		e.printStackTrace();
	    }
	} finally {
	    conn.commit();
	}
    }
    
    Vector<Seed> seeds = new Vector<Seed>();

    HTMLCrawler(String[] args) throws Exception {
	theCrawler = new Crawler(new CrawlerThreadFactory(CrawlerThreadFactory.HTML), false);

	PreparedStatement filterStmt = conn.prepareStatement("select institution,domain,prefix,seed from web.crawler_seed");
	domainPoolFilter poolFilter = new domainPoolFilter();
	ResultSet filterRS = filterStmt.executeQuery();
	while (filterRS.next()) {
	    String institution = filterRS.getString(1);
	    String domain = filterRS.getString(2);
	    String prefix = filterRS.getString(3);
	    String seed = filterRS.getString(4);

	    if (domain != null) {
		logger.info("adding domain filter: " + domain);
		Excluder.addFilter(new domainFilter(domain));
		poolFilter.addDomainFilter(new domainFilter(domain));
	    } else if (prefix != null) {
		logger.info("adding prefiex filter: " + prefix);
		poolFilter.addDomainFilter(new domainPrefixFilter(prefix));
	    }
	    seeds.add(new Seed(institution, domain, prefix, seed));
	}
	filterStmt.close();

	if (args[1].equals("-rescan")) {
	    rescan(args[2]);
	} else if (args[1].equals("-reset")) {
	    reset();
	} else if (args[1].equals("-pmids")) {
	    rescanPMIDS();
	} else if (args[1].equals("-dois")) {
	    rescanDOIS();
	} else {
	    PooledGenerator theStorer = new PooledGenerator(new ConnectionFactory());
	    
	    theCrawler.addFilter(poolFilter);
	    theCrawler.addFilter(new textFilter(true));
	    theCrawler.addFilter(new pathLengthFilter(10));
//	    theCrawler.addFilter(new queryLengthFilter());
//	    theCrawler.addFilter(new levelFilter(3));
	    theCrawler.addObserver(theStorer);

	    theCrawler.setVisitMonitor(new MemoizedVisitedMonitor(getConnection()));
	    Thread cacheMonitorThread = new Thread(new ConnectionCacheMonitor());
	    cacheMonitorThread.start();

	    // reloadVisited();

	    Thread.sleep(1000);
	    for (int i = 1; i < args.length; i++)
		if (args[1].equals("-restart")) {
		    reloadQueue(2000000, true);
		    reloadQueue(2000000, false);
		} else if (args[1].equals("-cleanup")) {
		    cleanup = true;
		    cleanup();
		} else
		    theCrawler.update(null, new URLRequest(args[i]));

	    theCrawler.initiate();

	    // for (int i = 1; i < args.length; i++)
	    // if (args[1].equals("-restart"))
	    // reloadQueue(1000000);
	}
    }

    void reset() throws SQLException {
	logger.info("resetting database");
	PreparedStatement resetStmt = conn.prepareStatement("truncate web.institution cascade");
	resetStmt.execute();
	resetStmt.close();

	PreparedStatement resetInstStmt = conn.prepareStatement("alter sequence web.institution_did_seq restart with 1");
	resetInstStmt.execute();
	resetInstStmt.close();

	PreparedStatement resetDocStmt = conn.prepareStatement("alter sequence web.document_id_seq restart with 1");
	resetDocStmt.execute();
	resetDocStmt.close();

	for (Seed seed : seeds) {
	    PreparedStatement insert = conn.prepareStatement("insert into web.institution(domain) values(?)", Statement.RETURN_GENERATED_KEYS);
	    insert.setString(1, seed.institution);
	    insert.execute();
	    ResultSet rs = insert.getGeneratedKeys();
	    while (rs.next()) {
		seed.did = rs.getInt(1);
		logger.info("institution: " + seed.institution + "\tdid: " + seed.did);
	    }
	    insert.close();

	    PreparedStatement docStmt = conn.prepareStatement("insert into web.document(url,did) values (?, ?)");
	    docStmt.setString(1, seed.seed);
	    docStmt.setInt(2, seed.did);
	    docStmt.execute();
	    docStmt.close();
	}
	
	conn.commit();
    }

    void rescan(String token) throws SQLException, MalformedURLException, IOException {
	HTMLLexer theLexer = new HTMLLexer();
	PreparedStatement scanStmt = conn.prepareStatement(
		"select document.id,document.url from web.document natural join web.token where token = ? and not exists (select id from web.pmid where pmid.id=document.id) and document.id > 343796 order by document.id");
	scanStmt.setString(1, token);
	ResultSet scanRS = scanStmt.executeQuery();
	while (scanRS.next()) {
	    int docCount = scanRS.getInt(1);
	    String url = scanRS.getString(2);

	    deleteDocument(docCount);

	    logger.info("scanning " + docCount + ": " + url);
	    try {
		theLexer.process(new URL(url));
		// storeDocument(new HTMLDocument(docCount, url,
		// theLexer.getTitle(), theLexer.getTokens(),
		// theLexer.wordCount(), theLexer.getURLs(),
		// theLexer.getContentLength(), theLexer.getLastModified(),
		// theLexer.getDois(), theLexer.getPmids()));
	    } catch (Exception e) {
	    }
	}
    }

    static Pattern pmidPat = Pattern.compile("[^0-9]*([0-9]+).*$");

    void rescanPMIDS() throws SQLException, MalformedURLException, IOException {
	PreparedStatement scanStmt = conn
		.prepareStatement("select distinct id from web.link where url like 'http://www.ncbi.nlm.nih.gov/pubmed/%' order by id");
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

	    PreparedStatement urlStmt = conn
		    .prepareStatement("select url from web.link where id = ? and url like 'http://www.ncbi.nlm.nih.gov/pubmed/%' order by url");
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
	if (!(obj instanceof HTMLDocument))
	    return;

	HTMLDocument theDoc = (HTMLDocument) obj;
	logger.info("HTMLCrawler updated: " + theDoc);
	// try {
	// storeURLs(theDoc);
	// storeDocument(theDoc);
	// } catch (SQLException e) {
	// logger.error("SQL error storing document " + theDoc.getURL() + " : "
	// + e);
	// try {
	// conn.rollback();
	// } catch (Exception e1) {
	// logger.error("SQL error aborting transaction: " + e1);
	// } finally {
	// try {
	// conn = getConnection();
	// } catch (Exception e2) {
	// logger.error("SQL error resetting connection: " + e2);
	// }
	// }
	// }
    }

    void reloadVisited() throws SQLException, MalformedURLException {
	PreparedStatement stmt = conn.prepareStatement("select url from web.document where indexed is not null order by url");
	ResultSet rs = stmt.executeQuery();
	while (rs.next()) {
	    String url = rs.getString(1);
	    logger.debug("visited: " + url);
	    URLRequest theRequest = new URLRequest(url);
	    theCrawler.addVisited(theRequest);
	}
	stmt.close();
    }

    void cleanup() throws SQLException, MalformedURLException {
	PreparedStatement stmt = conn.prepareStatement("select id, url from web.document where response_code = 0 ");

	ResultSet rs = stmt.executeQuery();
	while (rs.next()) {
	    int ID = rs.getInt(1);
	    String url = rs.getString(2);
	    logger.info("queueing: " + url);
	    URLRequest theRequest = new URLRequest(ID, url);
	    theCrawler.initialURL(theRequest);
	}
	stmt.close();
    }

    void reloadQueue(int amount, boolean mode) throws SQLException, MalformedURLException {
	// PreparedStatement stmt = conn.prepareStatement("select distinct url,
	// length(url) from web.link where url ~ '^http:.*\\.edu.*\\.html$' and
	// not exists (select url from web.document where document.url =
	// link.url) and length(url) < 200 order by length(url) limit 200");
	// PreparedStatement stmt = conn.prepareStatement("select id, url from
	// web.document where url ~ '^https?://w.*\\.edu.*/$' and indexed is
	// null and response_code is null and length(url)<60 limit 500");
	// PreparedStatement stmt = conn.prepareStatement("select id, url from
	// web.document,web.document_type where
	// document.suffix=document_type.suffix and type='hypertext' and indexed
	// is null and did != 6729 and url ~ '^https?://[^/]*\\.edu.*' limit " +
	// amount);
	PreparedStatement stmt = null;
	if (mode) {
	    // stmt = conn.prepareStatement("select id, url from web.document
	    // where document.suffix ='.html' and indexed is null and url !~
	    // 'https?://.*/calendar(/|\\.)' and url ~ '^https?://[^/]*\\.edu.*'
	    // limit " + amount);
	    stmt = conn.prepareStatement(
		    "select id, url from web.document where document.suffix ='.html' and indexed is null and url !~ 'https?://.*/calendar(/|\\.)' and url ~ '^https?://.*' and length(url) < 150 order by length(url) ");
	} else {
	    // stmt = conn.prepareStatement("select id, url from web.document
	    // where document.suffix is null and indexed is null and url !~
	    // 'https?://.*/calendar(/|\\.)' and url ~ '^https?://[^/]*\\.edu.*'
	    // limit " + amount);
	    stmt = conn.prepareStatement(
		    "select id, url from web.document where document.suffix is null and indexed is null and url !~ 'https?://.*/calendar(/|\\.)' and url ~ '^https?://.*' and length(url) < 150 order by length(url) ");
	}
	ResultSet rs = stmt.executeQuery();
	while (rs.next()) {
	    int ID = rs.getInt(1);
	    String url = rs.getString(2);
	    logger.info("queueing: " + url);
	    URLRequest theRequest = new URLRequest(ID, url);
	    theCrawler.initialURL(theRequest);
	}
	stmt.close();
    }
    
    class Seed {
	String institution = null;
	String domain = null;
	int did = 0;
	String prefix = null;
	String seed = null;
	
	Seed(String institution, String domain, String prefix, String seed) {
	    this.institution = institution;
	    this.domain = domain;
	    this.prefix = prefix;
	    this.seed = seed;
	}
    }
}
