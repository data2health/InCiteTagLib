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

import com.ibm.tspaces.TupleSpace;
import com.ibm.tspaces.TupleSpaceException;

import edu.uiowa.lex.basicLexerToken;
import edu.uiowa.lex.lexer;
import edu.uiowa.pos_tagging.POStagger;
import edu.uiowa.extraction.LocalProperties;
import edu.uiowa.extraction.PropertyLoader;

public class Fragmenter {
	static Logger logger = Logger.getLogger(Fragmenter.class);
	static Connection conn = null;
	static LocalProperties prop_file = PropertyLoader.loadProperties("incite");
	static boolean useTSpace = true;
	static TupleSpace ts = null;
	static final String host = "deep-thought.slis.uiowa.edu";

	int count = 0;
	lexer theLexer = null;
	POStagger theTagger = null;
	Vector<basicLexerToken> tokenVector = null;

	public static void main(String[] args) throws Exception {
		PropertyConfigurator.configure(args[0]);

		if (useTSpace) {
			logger.info("initializing tspace...");
			try {
				ts = new TupleSpace("incite", host);
			} catch (TupleSpaceException tse) {
				logger.error("TSpace error: " + tse);
			}
		}

		conn = getConnection();
		new Fragmenter(args);
	}

	static void loadConfiguration() {

	}

	static Connection getConnection() throws ClassNotFoundException, SQLException {
		Connection conn = null;

		Class.forName("org.postgresql.Driver");
		Properties props = new Properties();
		props.setProperty("user", prop_file.getProperty("jdbc.user"));
		props.setProperty("password", prop_file.getProperty("jdbc.password"));
		conn = DriverManager.getConnection("jdbc:postgresql://" + prop_file.getProperty("jdbc.host") + "/"
				+ prop_file.getProperty("jdbc.database"), props);
		conn.setAutoCommit(false);

		return conn;
	}

	public Fragmenter(String[] args) throws Exception {
		threadedGenerate(args);
	}

	void threadedGenerate(String[] args) throws Exception {
		int maxCrawlerThreads = Runtime.getRuntime().availableProcessors();
		Vector<Integer> queue = new Vector<Integer>();
		Thread[] fragmenterThreads = new Thread[maxCrawlerThreads];

		if (!useTSpace || (useTSpace && args.length > 1 && args[1].equals("-hub"))) {
			logger.info("truncating fragment...");
			conn.prepareStatement("truncate " + prop_file.getProperty("jdbc.schema") + ".fragment").execute();
			conn.commit();

			logger.info("loading queue...");
			PreparedStatement stmt = conn.prepareStatement(
					"select distinct id from extraction.parse where not exists (select * from extraction.fragment where fragment.id=parse.id) order by id");
			ResultSet rs = stmt.executeQuery();
			while (rs.next()) {
				if (useTSpace) {
					try {
						ts.write("fragment_request", rs.getInt(1));
					} catch (Exception e) {
						logger.error("tspace exception raised: ", e);
					}
				} else
					queue.add(rs.getInt(1));
			}
			logger.info("queue loaded.");
			if (useTSpace)
				return;
		}

		for (int i = 0; i < maxCrawlerThreads; i++) {
			Thread theThread = new Thread(new FragmenterThread(i, queue, getConnection(), prop_file, useTSpace));
			theThread.setPriority(Math.max(theThread.getPriority() - 2, Thread.MIN_PRIORITY));
			theThread.start();
			fragmenterThreads[i] = theThread;
		}
		for (int i = 0; i < maxCrawlerThreads; i++) {
			fragmenterThreads[i].join();
		}

		if (!useTSpace || (useTSpace && args.length > 1 && args[1].equals("-hub"))) {
			logger.info("refreshing fragments...");
			conn.prepareStatement("refresh materialized view " + prop_file.getProperty("jdbc.schema") + ".fragment_frequency")
					.execute();
			conn.commit();
			logger.info("done.");
		}
	}

}
