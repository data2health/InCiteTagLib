package edu.uiowa.slis.incite;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import edu.uiowa.NLP_grammar.FragmentGenerator;
import edu.uiowa.NLP_grammar.ParseFragment;
import edu.uiowa.NLP_grammar.linkGrammarParserBridge;
import edu.uiowa.UMLS.Concept;
import edu.uiowa.extraction.LocalProperties;
import edu.uiowa.extraction.PropertyLoader;
import edu.uiowa.extraction.TemplatePromoter;

public class HTMLFragmenter implements Runnable {
    static Logger logger = Logger.getLogger(HTMLFragmenter.class);
    protected static LocalProperties prop_file = null;
    static int parseCount = 6;
    static Connection mainConn = null;
    static int documentCounter = 1;
    static int maxCounter = 0;

    public static void main(String[] args) throws Exception {
	PropertyConfigurator.configure(args[0]);
	prop_file = PropertyLoader.loadProperties("incite");
	mainConn = getConnection();

	PreparedStatement mainStmt = mainConn.prepareStatement("select max(id) from extraction.parse");
	ResultSet mainRS = mainStmt.executeQuery();
	while (mainRS.next())
	    maxCounter = mainRS.getInt(1);
	mainStmt.close();
	logger.info("max document ID: " + maxCounter);
	
	logger.info("truncating fragment...");
	mainStmt = mainConn.prepareStatement("truncate extraction.fragment");
	mainStmt.execute();
	mainStmt.close();
	mainConn.commit();

	int maxCrawlerThreads = Runtime.getRuntime().availableProcessors();
	Thread[] scannerThreads = new Thread[maxCrawlerThreads];
	for (int i = 0; i < maxCrawlerThreads; i++) {
	    logger.info("starting thread " + i);
	    Thread theThread = new Thread(new HTMLFragmenter(i));
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
    
    static Connection getPMCConnection() throws Exception {
	Connection conn = null;
	Class.forName("org.postgresql.Driver");
	Properties props = new Properties();
	props.setProperty("user", "eichmann");
	props.setProperty("password", "translational");
//	props.setProperty("sslfactory", "org.postgresql.ssl.NonValidatingFactory");
//	props.setProperty("ssl", "true");
//	conn = DriverManager.getConnection("jdbc:postgresql://neuromancer.icts.uiowa.edu/incite", props);
	conn = DriverManager.getConnection("jdbc:postgresql://localhost/loki", props);
	conn.setAutoCommit(false);
	return conn;
    }
    
    int threadID = 0;
    Connection conn = null;
    Connection pmcConn = null;
    
    public HTMLFragmenter(int threadID) throws Exception {
	this.threadID = threadID;
	conn = getConnection();
	pmcConn = getPMCConnection();
    }
    
    @Override
    public void run() {
	for (int ID = getID(); ID != 0; ID = getID()) {
	    try {
		fragmentDocument(ID);
	    } catch (Exception e) {
		logger.error("[" + threadID + "] " + "Exception raised: ", e);
	    }
	}
    }

    void fragmentDocument(int id) throws ClassNotFoundException, Exception {
	logger.info("[" + threadID + "] " + "document: " + id);
//	Concept.initialize(conn);
	FragmentGenerator theGenerator = new FragmentGenerator(new InCiteDecorator(pmcConn), new InCiteInstantiator(prop_file, conn), new TemplatePromoter(conn));
	PreparedStatement sourceStmt = conn.prepareStatement("select seqnum, parse, parsed from extraction.parse where id = ?");
	sourceStmt.setInt(1, id);
	ResultSet sourceRS = sourceStmt.executeQuery();
	while (sourceRS.next()) {
	    int seqnum = sourceRS.getInt(1);
	    int parsenum = sourceRS.getInt(2);
	    String parseString = sourceRS.getString(3);
	    logger.debug("[" + threadID + "] : " + id + " : " + seqnum + " : " + parsenum + " : " + parseString);
	    for (ParseFragment fragment : theGenerator.fragments(id, parseString)) {
		logger.debug("\tfragment: " + fragment.getFragmentString());
		logger.debug("\t\tparse: " + fragment.getFragmentParse());

		PreparedStatement fragStmt = conn.prepareStatement("insert into extraction.fragment values(?,?,?,?)");
		fragStmt.setInt(1, id);
		fragStmt.setInt(2, seqnum);
		fragStmt.setString(3, fragment.getFragmentString());
		fragStmt.setString(4, fragment.getFragmentParse());
		fragStmt.execute();
		fragStmt.close();
	    }
	}
	conn.commit();
    }
}
