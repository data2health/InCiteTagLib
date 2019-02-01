package edu.uiowa.slis.incite;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import com.ibm.tspaces.Field;
import com.ibm.tspaces.Tuple;
import com.ibm.tspaces.TupleSpace;
import com.ibm.tspaces.TupleSpaceException;

import edu.uiowa.NLP_grammar.SegmentParser;
import edu.uiowa.NLP_grammar.SimpleStanfordParserBridge;
import edu.uiowa.NLP_grammar.TextSegment;
import edu.uiowa.NLP_grammar.TextSegmentElement;
import edu.uiowa.NLP_grammar.syntaxTree;
import edu.uiowa.lex.biomedicalLexerMod;

public class HTMLParser implements Runnable {
    static Logger logger = Logger.getLogger(HTMLParser.class);
    static int parseCount = 6;
    static Connection mainConn = null;
    static int documentCounter = 1;
    static int maxCounter = 0;
    static Vector<Integer> queue = new Vector<Integer>();
    static boolean useTSpace = false;
    static TupleSpace ts = null;
    static final String host = "deep-thought.slis.uiowa.edu";
    
    static boolean big = false;

    public static void main(String[] args) throws Exception {
	PropertyConfigurator.configure(args[0]);
	mainConn = getConnection();
	
	if (useTSpace) {
	    logger.info("initializing tspace...");
	    try {
		ts = new TupleSpace("incite", host);
	    } catch (TupleSpaceException tse) {
		logger.error("TSpace error: " + tse);
	    }
	}
	
//	if (big) {
//	    PreparedStatement mainStmt = mainConn.prepareStatement("truncate extraction.ignore");
//	    mainStmt.execute();
//	    mainStmt.close();
//	    mainConn.commit();
//	}

	if (!useTSpace || (useTSpace && args.length > 1 && args[1].equals("-hub"))) {
	    PreparedStatement mainStmt = mainConn.prepareStatement("select distinct id from jsoup.segment where not exists (select * from extraction.sentence where segment.id=sentence.id) and not exists (select * from extraction.ignore where segment.id=ignore.id) order by id desc");
	    ResultSet mainRS = mainStmt.executeQuery();
	    while (mainRS.next()) {
		if (useTSpace) {
		    try {
			ts.write("parse_request", mainRS.getInt(1));
		    } catch (Exception e) {
			logger.error("tspace exception raised: ", e);
		    }
		} else
		    queue.add(mainRS.getInt(1));
	    }
	    mainStmt.close();
	    logger.info("queue size: " + queue.size());
	    mainConn.commit();
	    if (useTSpace)
		return;
	}
	
	int maxCrawlerThreads = big ? 2 : Runtime.getRuntime().availableProcessors();
	Thread[] scannerThreads = new Thread[maxCrawlerThreads];
	for (int i = 0; i < maxCrawlerThreads; i++) {
	    logger.info("starting thread " + i);
	    Thread theThread = new Thread(new HTMLParser(i));
	    theThread.setPriority(Math.max(theThread.getPriority() - 2, Thread.MIN_PRIORITY));
	    theThread.start();
	    scannerThreads[i] = theThread;
	}
	for (int i = 0; i < maxCrawlerThreads; i++) {
	    scannerThreads[i].join();
	}
    }

    static synchronized int getID() {
	if (useTSpace) {
	    Tuple theTuple = null;

	    try {
		theTuple = ts.take("parse_request", new Field(Integer.class));
		if (theTuple != null) {
		    return (Integer) theTuple.getField(1).getValue();
		}
	    } catch (TupleSpaceException e1) {
		// TODO Auto-generated catch block
		e1.printStackTrace();
	    }
	    return 0;
	} else {
	    Integer result = null;
	    try {
		result = queue.remove(0);
	    } catch (Exception e) { }
	    if (result != null)
		return result;
	    return 0;
	}
    }

    static Connection getConnection() throws Exception {
	Connection conn = null;
	Class.forName("org.postgresql.Driver");
	Properties props = new Properties();
	props.setProperty("user", "eichmann");
	props.setProperty("password", "translational");
	// props.setProperty("sslfactory", "org.postgresql.ssl.NonValidatingFactory");
	// props.setProperty("ssl", "true");
	// conn = DriverManager.getConnection("jdbc:postgresql://neuromancer.icts.uiowa.edu/incite", props);
	conn = DriverManager.getConnection("jdbc:postgresql://localhost/incite", props);
	conn.setAutoCommit(false);
	return conn;
    }

    int threadID = 0;
    Connection conn = null;
    SegmentParser theParser = null;

    public HTMLParser(int threadID) throws Exception {
	this.threadID = threadID;
	conn = getConnection();
	theParser = new SegmentParser(new biomedicalLexerMod(), new SimpleStanfordParserBridge(), new BiomedicalSentenceGenerator(conn));
	if (!big)
	    theParser.setPunctuationLimit(20);
	theParser.setParseCount(parseCount);
	theParser.setPunctuationLimitNotification(true);
	theParser.setTokenLimit(200);
	theParser.setTokenLimitNotification(true);
	theParser.setTokenLimitSuppression(true);
    }

    @Override
    public void run() {
	for (int ID = getID(); ID != 0; ID = getID()) {
	    try {
		parseDocument(ID);
	    } catch (SQLException e) {
		logger.error("[" + threadID + "] " + "Exception raised: ", e);
	    }
	}
	logger.info("[" + threadID + "] terminating");
    }

    void parseDocument(int id) throws SQLException {
	logger.info("[" + threadID + "] " + "document: " + id);
	PreparedStatement segStmt = conn.prepareStatement("select distinct seqnum,content from jsoup.segment where id=? order by seqnum");
	segStmt.setInt(1, id);
	ResultSet segRS = segStmt.executeQuery();
	while (segRS.next()) {
	    int seqnum = segRS.getInt(1);
	    String segString = segRS.getString(2).trim();
	    
	    if (segString.length() == 0 || segString.startsWith("[vc_") || segString.startsWith("[/vc_") || segString.startsWith("BEGIN:VCARD") || segString.startsWith("BEGIN:VCALENDAR"))
		continue;
	    
	    logger.debug("[" + threadID + "] " + "\tseqnum: " + seqnum + "\t" + segString);

	    try {
		TextSegment segment = theParser.parse(segString);

		if (segment.exceptionRaised) {
		    PreparedStatement exStmt = conn.prepareStatement("insert into extraction.ignore values(?)");
		    exStmt.setInt(1, id);
		    exStmt.execute();
		    exStmt.close();
		}

		if (theParser.isPunctuationLimitExceeded() || theParser.isTokenLimitExceeded()) {
		    conn.rollback();
		    PreparedStatement exStmt = conn.prepareStatement("insert into extraction.ignore values(?)");
		    exStmt.setInt(1, id);
		    exStmt.execute();
		    exStmt.close();
		    break;
		}

		int sentenceCount = 0;
		for (TextSegmentElement element : segment.getElementVector()) {
		    logger.debug("\tsentence: " + element.getSentence());

		    PreparedStatement sentStmt = conn.prepareStatement("insert into extraction.sentence values(?,?,?,?,?)");
		    sentStmt.setInt(1, id);
		    sentStmt.setInt(2, seqnum);
		    sentStmt.setInt(3, ++sentenceCount);
		    sentStmt.setString(4, element.getSentence().getText());
		    sentStmt.setString(5, element.getSentence().toString());
		    sentStmt.execute();
		    sentStmt.close();

		    int parseCount = 0;
		    for (syntaxTree theTree : element.getParseVector()) {
			logger.debug("\t\tparse: " + theTree.treeString());

			PreparedStatement parseStmt = conn.prepareStatement("insert into extraction.parse values(?,?,?,?,?)");
			parseStmt.setInt(1, id);
			parseStmt.setInt(2, seqnum);
			parseStmt.setInt(3, sentenceCount);
			parseStmt.setInt(4, ++parseCount);
			parseStmt.setString(5, theTree.treeString());
			parseStmt.execute();
			parseStmt.close();
		    }
		}
	    } catch (Exception e) {
		logger.error("Error parsing: ", e);
	    }
	}
	segStmt.close();

	conn.commit();
    }
}
