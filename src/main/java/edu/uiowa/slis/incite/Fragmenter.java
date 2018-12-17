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

import edu.uiowa.lex.basicLexerToken;
import edu.uiowa.lex.lexer;
import edu.uiowa.pos_tagging.POStagger;
import edu.uiowa.extraction.LocalProperties;
import edu.uiowa.extraction.PropertyLoader;

public class Fragmenter  {
    static Logger logger = Logger.getLogger(Fragmenter.class);
    static Connection conn = null;
    static LocalProperties prop_file = PropertyLoader.loadProperties("incite");

    int count = 0;
    lexer theLexer = null;
    POStagger theTagger = null;
    Vector<basicLexerToken> tokenVector = null;
    
    public static void main(String[] args) throws Exception {
	PropertyConfigurator.configure(args[0]);

	conn = getConnection();
	new Fragmenter();
    }
    
    static void loadConfiguration() {
	
    }

    static Connection getConnection() throws ClassNotFoundException, SQLException {
	Connection conn = null;
	
	Class.forName("org.postgresql.Driver");
	Properties props = new Properties();
	props.setProperty("user", prop_file.getProperty("jdbc.user"));
	props.setProperty("password", prop_file.getProperty("jdbc.password"));
	conn = DriverManager.getConnection("jdbc:postgresql://" + prop_file.getProperty("jdbc.host") + "/" + prop_file.getProperty("jdbc.database"), props);
	conn.setAutoCommit(false);

	return conn;
    }

    public Fragmenter() throws Exception {
	threadedGenerate();
    }

    void threadedGenerate() throws Exception {
	int maxCrawlerThreads = Runtime.getRuntime().availableProcessors();
	Vector<Integer> queue = new Vector<Integer>();
	Thread[] fragmenterThreads = new Thread[maxCrawlerThreads];
	
	logger.info("truncating fragment...");
	conn.prepareStatement("truncate " + prop_file.getProperty("jdbc.schema") + ".fragment").execute();
	conn.commit();

	logger.info("loading queue...");
	    PreparedStatement stmt = conn.prepareStatement("select distinct id from extraction.parse where not exists (select * from extraction.fragment where fragment.id=parse.id) order by id");
	    ResultSet rs = stmt.executeQuery();
	    while (rs.next()) {
		int ID = rs.getInt(1);
		queue.add(ID);
	}
	logger.info("queue loaded.");

	for (int i = 0; i < maxCrawlerThreads; i++) {
	    Thread theThread = new Thread(new FragmenterThread(i, queue, getConnection(), prop_file));
	    theThread.setPriority(Math.max(theThread.getPriority() - 2, Thread.MIN_PRIORITY));
	    theThread.start();
	    fragmenterThreads[i] = theThread;
	}
	for (int i = 0; i < maxCrawlerThreads; i++) {
	    fragmenterThreads[i].join();
	}

	logger.info("refreshing fragments...");
	conn.prepareStatement("refresh materialized view " + prop_file.getProperty("jdbc.schema") + ".fragments").execute();
	conn.commit();
	logger.info("done.");
    }

}
