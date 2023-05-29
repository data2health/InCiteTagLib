package edu.uiowa.slis.incite;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Properties;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import com.ibm.tspaces.Field;
import com.ibm.tspaces.Tuple;
import com.ibm.tspaces.TupleSpace;
import com.ibm.tspaces.TupleSpaceException;

import edu.uiowa.NLP_grammar.FragmentGenerator;
import edu.uiowa.NLP_grammar.ParseFragment;
import edu.uiowa.PubMedCentral.AcknowledgementInstantiator;
import edu.uiowa.extraction.EnsembleDecorator;
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
	static Vector<Integer> queue = new Vector<Integer>();
	static boolean useTSpace = true;
	static TupleSpace ts = null;

	public static void main(String[] args) throws Exception {
		PropertyConfigurator.configure(args[0]);
		prop_file = PropertyLoader.loadProperties("incite");
		mainConn = getConnection();

		if (useTSpace) {
			logger.info("initializing tspace...");
			try {
				ts = new TupleSpace("incite", prop_file.getProperty("tspace.host"));
			} catch (TupleSpaceException tse) {
				logger.error("TSpace error: " + tse);
			}
		}

		if (!useTSpace || (useTSpace && args.length > 1 && args[1].equals("-hub"))) {
			PreparedStatement mainStmt = mainConn.prepareStatement(
					"select distinct id from extraction.sentence where not exists (select id from extraction.fragment where sentence.id=fragment.id) limit 20000");
			ResultSet mainRS = mainStmt.executeQuery();
			while (mainRS.next()) {
				if (useTSpace) {
					try {
						ts.write("fragment_request", mainRS.getInt(1));
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
		if (useTSpace) {
			Tuple theTuple = null;

			try {
				theTuple = ts.take("fragment_request", new Field(Integer.class));
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
			} catch (Exception e) {
			}
			if (result != null)
				return result;
			return 0;
		}
	}

	static Connection getConnection() throws Exception {
		Connection conn = null;
		Class.forName("org.postgresql.Driver");
		Properties props = new Properties();
		props.setProperty("user", prop_file.getProperty("jdbc.user"));
		props.setProperty("password", prop_file.getProperty("jdbc.password"));
//	props.setProperty("sslfactory", "org.postgresql.ssl.NonValidatingFactory");
//	props.setProperty("ssl", "true");
//	conn = DriverManager.getConnection("jdbc:postgresql://neuromancer.icts.uiowa.edu/incite", props);
		conn = DriverManager.getConnection("jdbc:postgresql://hal.local/incite", props);
		PreparedStatement stmt = conn.prepareStatement("set search_path to extraction");
		stmt.execute();
		stmt.close();
		conn.setAutoCommit(false);
		conn.setNetworkTimeout(null, 0);
		return conn;
	}

	static Connection getPMCConnection() throws Exception {
		Connection conn = null;
		Class.forName("org.postgresql.Driver");
		Properties props = new Properties();
		props.setProperty("user", prop_file.getProperty("jdbc.user"));
		props.setProperty("password", prop_file.getProperty("jdbc.password"));
//	props.setProperty("sslfactory", "org.postgresql.ssl.NonValidatingFactory");
//	props.setProperty("ssl", "true");
//	conn = DriverManager.getConnection("jdbc:postgresql://neuromancer.icts.uiowa.edu/incite", props);
		conn = DriverManager.getConnection("jdbc:postgresql://hal.local/incite", props);
		conn.setAutoCommit(false);
		return conn;
	}

	int threadID = 0;
	Connection conn = null;
	Connection pmcConn = null;
	FragmentGenerator theGenerator = null;

	public HTMLFragmenter(int threadID) throws Exception {
		this.threadID = threadID;
		conn = getConnection();
		pmcConn = getPMCConnection();
		theGenerator = new FragmentGenerator(new EnsembleDecorator(conn), new AcknowledgementInstantiator(prop_file, conn, threadID), new TemplatePromoter(conn));
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

		theGenerator.resetContext();

		PreparedStatement sourceStmt = conn
				.prepareStatement("select seqnum, sentnum, parsenum, parse from extraction.parse where id = ?");
		sourceStmt.setInt(1, id);
		ResultSet sourceRS = sourceStmt.executeQuery();
		while (sourceRS.next()) {
			int seqnum = sourceRS.getInt(1);
			int sentnum = sourceRS.getInt(2);
			int parsenum = sourceRS.getInt(3);
			String parseString = sourceRS.getString(4);
			
			theGenerator.resetContext(seqnum,sentnum);
			
			logger.debug("[" + threadID + "] : " + id + " : " + seqnum + " : " + parsenum + " : " + parseString);
			for (ParseFragment fragment : theGenerator.fragments(id, parseString)) {
				logger.debug("\tfragment: " + fragment.getFragmentString());
				logger.debug("\t\tparse: " + fragment.getFragmentParse());

				PreparedStatement fragStmt = conn.prepareStatement("insert into extraction.fragment values(?,?,?,?,?)");
				fragStmt.setInt(1, id);
				fragStmt.setInt(2, seqnum);
				fragStmt.setInt(3, sentnum);
				fragStmt.setString(4, fragment.getFragmentString());
				fragStmt.setString(5, fragment.getFragmentParse());
				fragStmt.execute();
				fragStmt.close();
			}
		}
		conn.commit();
	}
}
