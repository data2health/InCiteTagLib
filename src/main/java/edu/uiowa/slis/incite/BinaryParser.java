package edu.uiowa.slis.incite;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Hashtable;
import java.util.Properties;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.xml.sax.ContentHandler;
import org.apache.tika.sax.ToXMLContentHandler;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import com.ibm.tspaces.Field;
import com.ibm.tspaces.Tuple;
import com.ibm.tspaces.TupleSpace;
import com.ibm.tspaces.TupleSpaceException;

public class BinaryParser implements Runnable {
	static Logger logger = Logger.getLogger(BinaryParser.class);
	static int parseCount = 6;
	static Connection mainConn = null;
	static int documentCounter = 1;
	static int maxCounter = 0;
	static Vector<Integer> queue = new Vector<Integer>();
	static boolean useTSpace = false;
	static TupleSpace ts = null;
	static final String host = "deep-thought.slis.uiowa.edu";
	static LocalProperties prop_file = null;
	static boolean big = false;
	static Hashtable<String, Integer> domainHash = new Hashtable<String, Integer>();
	static Hashtable<String, Integer> urlHash = new Hashtable<String, Integer>();

	public static void main_test(String[] args) throws Exception {
		PropertyConfigurator.configure(args[0]);
		prop_file = PropertyLoader.loadProperties("incite");
		mainConn = getConnection();
		
		PreparedStatement stmt = mainConn.prepareStatement("select id,content from jsoup.binary_document where content is not null and id not in (select id from jsoup.binary_snapshot) limit 5");
		ResultSet rs = stmt.executeQuery();
		while (rs.next()) {
			int id = rs.getInt(1);
			logger.info("processing: " + id);
		    try {
				ContentHandler handler = new ToXMLContentHandler();
				
				AutoDetectParser parser = new AutoDetectParser();
				Metadata metadata = new Metadata();
				try (InputStream stream = rs.getBinaryStream(2)) {
				    parser.parse(stream, handler, metadata);
				    logger.info("content: " +handler.toString());
				    Document document = Jsoup.parse(handler.toString());
					logger.info("document text: " + document.text());
				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
	}
	
	public static void main(String[] args) throws Exception {
		PropertyConfigurator.configure(args[0]);
		prop_file = PropertyLoader.loadProperties("incite");
		mainConn = getConnection();
		
		if (useTSpace) {
			logger.info("initializing tspace...");
			try {
				ts = new TupleSpace("incite", host);
			} catch (TupleSpaceException tse) {
				logger.error("TSpace error: " + tse);
			}
		}

		if (!useTSpace || (useTSpace && args.length > 1 && args[1].equals("-hub"))) {
			PreparedStatement mainStmt = mainConn.prepareStatement("select id from jsoup.binary_document where content is not null and length(content) > 0 and id not in (select id from jsoup.binary_document_suppress) and id not in (select id from jsoup.segment) order by id");
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
			Thread theThread = new Thread(new BinaryParser(i));
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
		// props.setProperty("sslfactory", "org.postgresql.ssl.NonValidatingFactory");
		// props.setProperty("ssl", "true");
		// conn =
		// DriverManager.getConnection("jdbc:postgresql://neuromancer.icts.uiowa.edu/incite",
		// props);
		conn = DriverManager.getConnection("jdbc:postgresql://localhost/incite", props);
		conn.setAutoCommit(false);
		return conn;
	}

	int threadID = 0;
	Connection conn = null;
	JSoupProcessor processor = null;

	public BinaryParser(int threadID) throws Exception {
		this.threadID = threadID;
		conn = getConnection();
		processor = new JSoupProcessor(conn, threadID, domainHash, urlHash);
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
		PreparedStatement segStmt = conn.prepareStatement("select content from jsoup.binary_document where id = ?");
		segStmt.setInt(1, id);
		ResultSet segRS = segStmt.executeQuery();
		while (segRS.next()) {
			try {
				ContentHandler handler = new ToXMLContentHandler();

				AutoDetectParser parser = new AutoDetectParser();
				Metadata metadata = new Metadata();
				InputStream stream = segRS.getBinaryStream(1);
				parser.parse(stream, handler, metadata);
				logger.debug("content: " + handler.toString());
				Document document = Jsoup.parse(handler.toString());

				processor.storeURLs(document);
				processor.storeDocument(id, document);
			} catch (java.lang.NoSuchMethodError e) {
				errorDocument(id, e.toString());
			} catch (java.lang.NoClassDefFoundError e) {
				errorDocument(id, e.toString());
			} catch (java.lang.IllegalAccessError e) {
				errorDocument(id, e.toString());
			} catch (java.lang.ClassCastException e) {
				errorDocument(id, e.toString());
			} catch (Exception e) {
				errorDocument(id, e.toString());
			}
		}
		segStmt.close();

		conn.commit();
	}
	
	void errorDocument(int id, String error) throws SQLException {
		logger.error("id: " + id + "\terror: " + error);
		PreparedStatement stmt = conn.prepareStatement("insert into jsoup.binary_document_suppress values (?,?)");
		stmt.setInt(1, id);
		stmt.setString(2, error);
		stmt.execute();
	}
}
