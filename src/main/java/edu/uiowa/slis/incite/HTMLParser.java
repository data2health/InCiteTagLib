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

import edu.stanford.nlp.parser.Parser;
import edu.uiowa.NLP_grammar.SegmentParser;
import edu.uiowa.NLP_grammar.StanfordParserBridge;
import edu.uiowa.NLP_grammar.TextSegment;
import edu.uiowa.NLP_grammar.TextSegmentElement;
import edu.uiowa.NLP_grammar.linkGrammarParserBridge;
import edu.uiowa.NLP_grammar.parser;
import edu.uiowa.NLP_grammar.syntaxTree;
import edu.uiowa.lex.Sentence;

public class HTMLParser implements Runnable {
    static Logger logger = Logger.getLogger(HTMLParser.class);
    static int parseCount = 6;
    static Connection mainConn = null;
    static int documentCounter = 1;
    static int maxCounter = 0;

    public static void main(String[] args) throws Exception {
	PropertyConfigurator.configure(args[0]);
	mainConn = getConnection();

	PreparedStatement mainStmt = mainConn.prepareStatement("select max(id) from web.document");
	ResultSet mainRS = mainStmt.executeQuery();
	while (mainRS.next())
	    maxCounter = mainRS.getInt(1);
	mainStmt.close();
	logger.info("max document ID: " + maxCounter);

	int maxCrawlerThreads = Runtime.getRuntime().availableProcessors();
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
	if (documentCounter > maxCounter)
	    return 0;
	return documentCounter++;
    }

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
	return conn;
    }
    
    int threadID = 0;
    Connection conn = null;
    SegmentParser theParser = null;
    
    public HTMLParser(int threadID) throws Exception {
	this.threadID = threadID;
	conn = getConnection();
	theParser = new SegmentParser();
	theParser.setParseCount(parseCount);
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
    }

    @SuppressWarnings("unchecked")
    void parseDocument(int id) throws SQLException {
	logger.info("[" + threadID + "] " + "document: " + id);
	PreparedStatement segStmt = conn.prepareStatement("select seqnum,sentence from web.sentence where id=? order by seqnum");
	segStmt.setInt(1, id);
	ResultSet segRS = segStmt.executeQuery();
	while (segRS.next()) {
	    int seqnum = segRS.getInt(1);
	    String segString = segRS.getString(2);
	    logger.info("[" + threadID + "] " + "\tseqnum: " + seqnum + "\t" + segString);

	    try {
		TextSegment segment = theParser.parse(segString);
		    
		    if (segment.exceptionRaised) {
			PreparedStatement exStmt = conn.prepareStatement("insert into extraction.ignore values(?)");
			exStmt.setInt(1, id);
			exStmt.execute();
			exStmt.close();
		    }
		    
		    if (theParser.isPunctuationLimitExceeded()) {
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
