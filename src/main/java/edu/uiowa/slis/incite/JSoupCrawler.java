package edu.uiowa.slis.incite;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Hashtable;
import java.util.Properties;
import java.util.Vector;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import com.ibm.tspaces.Field;
import com.ibm.tspaces.Tuple;
import com.ibm.tspaces.TupleSpaceException;

public class JSoupCrawler implements Runnable {
	static Logger logger = Logger.getLogger(JSoupCrawler.class);
	static Connection mainConn = null;
	static Hashtable<String, Integer> domainHash = new Hashtable<String, Integer>();
	static Hashtable<String, Integer> urlHash = new Hashtable<String, Integer>();
	static QueueManager queueManager = null; // for others
	static Vector<QueueRequest> requestQueue = new Vector<QueueRequest>(); // for binaries
	static boolean doBinaries = false;
	static int binaryStatusCode = -1;

	static Connection getConnection() throws Exception {
		Connection conn = null;
		Class.forName("org.postgresql.Driver");
		Properties props = new Properties();
		LocalProperties prop_file = PropertyLoader.loadProperties("incite");
		props.setProperty("user", prop_file.getProperty("jdbc.user"));
		props.setProperty("password", prop_file.getProperty("jdbc.password"));
		conn = DriverManager.getConnection(prop_file.getProperty("jdbc.url"), props);
		conn.setAutoCommit(false);
		return conn;
	}

	public static void main(String[] args) throws Exception {
		PropertyConfigurator.configure(args[0]);
		mainConn = getConnection();

		if (args.length == 1) {
			queueManager = new QueueManager(mainConn);
		} else if (args.length == 2 && args[1].equals("seeds")) {
			initializeSeeds();
			return;
		} else if (args.length == 2 && args[1].equals("binaries")) {
			doBinaries = true;
			trustAllCerts();
			harvestBinaries();
		} else if (args.length == 3 && args[1].equals("binaries")) {
			doBinaries = true;
			binaryStatusCode = Integer.parseInt(args[2]);
			trustAllCerts();
			harvestBinaries();
		} else if (args.length == 2 && args[1].equals("robots")) {
			queueManager = new QueueManager(mainConn, "test");
			PreparedStatement stmt = mainConn.prepareStatement("select domain from jsoup.crawler_seed order by 1");
			ResultSet rs = stmt.executeQuery();
			while (rs.next()) {
				String domain = rs.getString(1);
				logger.info("domain: " + domain);
				queueManager.robotScan(domain);
			}
			stmt.close();
			return;
		} else {
			Vector<String> domains = new Vector<String>();
			for (int i = 1; i < args.length; i++)
				domains.add(args[i]);
			queueManager = new QueueManager(mainConn, domains);
		}

		int maxCrawlerThreads = Math.min((doBinaries ? requestQueue.size() : queueManager.domainCount()), Runtime.getRuntime().availableProcessors() * 2);
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
		if (!doBinaries)
			queueManager.monitorThread.join();
	}
	
	static void harvestBinaries() throws Exception {
		harvestBinaries(".pdf");
		harvestBinaries(".docx");
		harvestBinaries(".pptx");
		harvestBinaries(".doc");
		harvestBinaries(".ppt");
		harvestBinaries(".xlsx");
		harvestBinaries(".xls");
		logger.info("queue size : " + requestQueue.size());
	}
	
	static void harvestBinaries(String suffix) throws SQLException, ClientProtocolException, IOException {
		PreparedStatement stmt = mainConn.prepareStatement(
				binaryStatusCode >= 0 ?
						"select id,url from jsoup.document where lower(suffix) = ? and id in (select id from jsoup.binary_document where response_code = ?)"
						:
						"select id,url from jsoup.document where lower(suffix) = ? and id not in (select id from jsoup.binary_document)");
		stmt.setString(1, suffix);
		if (binaryStatusCode >= 0)
			stmt.setInt(2, binaryStatusCode);
		ResultSet rs = stmt.executeQuery();
		while (rs.next()) {
			int id = rs.getInt(1);
			String url = rs.getString(2).replaceAll(" ", "%20");
			requestQueue.add(new QueueRequest(id, url));
		}
		stmt.close();
	}

	void harvestBinaries(QueueRequest request) throws SQLException, ClientProtocolException, IOException {
			logger.info("[" + threadID + "] id: " + request.ID + " url: " + request.url);
			if (binaryStatusCode >= 0 ) {
				PreparedStatement insStmt = conn.prepareStatement("delete from jsoup.binary_document where id = ?");
				insStmt.setInt(1, request.ID);
				insStmt.execute();
				insStmt.close();
			}
			if (request.url.startsWith("ftp:")) {
				try {
					URLConnection urlConnection = new URL(request.url).openConnection();
					InputStream inputStream = urlConnection.getInputStream();
					PreparedStatement insStmt = conn.prepareStatement("insert into jsoup.binary_document values(?,?,?)");
					insStmt.setInt(1, request.ID);
					insStmt.setBinaryStream(2, inputStream);
					insStmt.setInt(3, 200);
					insStmt.execute();
					insStmt.close();
				} catch (Exception e) {
					logger.error("[" + threadID + "] error downloading: " + request.url + " " + e);
					PreparedStatement insStmt = conn.prepareStatement("insert into jsoup.binary_document values(?,?,?)");
					insStmt.setInt(1, request.ID);
					insStmt.setBinaryStream(2, null);
					insStmt.setInt(3, 0);
					insStmt.execute();
					insStmt.close();
				}
			} else {
				RequestConfig config = RequestConfig.custom()
						.setConnectTimeout(30*1000)
						.setConnectionRequestTimeout(30*1000)
						.setSocketTimeout(30*1000).build();
				CloseableHttpClient httpclient = HttpClientBuilder.create().setDefaultRequestConfig(config).build();
				MyResponseHandler responseHandler = new MyResponseHandler();
				int statusCode = 0;
				try {
					HttpGet httpget = new HttpGet(request.url);
					responseHandler.url = request.url;
					statusCode = httpclient.execute(httpget, responseHandler);
				} catch (Exception e) {
					logger.error("[" + threadID + "] error downloading: " + request.url + " " + e);
				}
				try {
					PreparedStatement insStmt = conn.prepareStatement("insert into jsoup.binary_document values(?,?,?)");
					insStmt.setInt(1, request.ID);
					insStmt.setBinaryStream(2, responseHandler.getInputStream());
					insStmt.setInt(3, statusCode);
					insStmt.execute();
					insStmt.close();
				} catch (Exception e) {
					logger.error("[" + threadID + "] error downloading: " + request.url + " " + e);
					PreparedStatement insStmt = conn.prepareStatement("insert into jsoup.binary_document values(?,?,?)");
					insStmt.setInt(1, request.ID);
					insStmt.setBinaryStream(2, null);
					insStmt.setInt(3, 0);
					insStmt.execute();
					insStmt.close();
				}
			}
			conn.commit();
	}

	class MyResponseHandler implements ResponseHandler<Integer> {
		InputStream resultStream = null;
		String url = null;

		public Integer handleResponse(HttpResponse response) throws IOException {

			// Get the status of the response
			int status = response.getStatusLine().getStatusCode();
			if (status >= 200 && status < 300) {
				resultStream = (new URL(url)).openStream();
				return status;
			} else {
				logger.info("[" + threadID + "] failing status code: " + status + "\t" + response.getStatusLine().getReasonPhrase());
				resultStream = null;
				return status;
			}
		}
		
		void resetInputStream() {
			resultStream = null;
		}
		
		InputStream getInputStream() {
			return resultStream;
		}

	}

	JSoupProcessor processor = null;
	int threadID = 0;
	Connection conn = null;

	JSoupCrawler(int threadID) throws Exception {
		this.threadID = threadID;
		conn = getConnection();
		processor = new JSoupProcessor(conn, threadID, domainHash, urlHash);
	}

	public void run() {
		if (doBinaries)
			runBinaries();
		else
			runOthers();
	}

	public void runOthers() {
		QueueRequest request = null;
		do {
			request = queueManager.nextRequest();
			if (request != null) {
				logger.info("[" + threadID + "] request: " + request);
				processURL(request);
			} else {
				logger.info("[" + threadID + "] terminating");
				return;
			}
		} while (true);
	}

	public void runBinaries() {
		QueueRequest request = null;
		do {
			request = getRequest();
			if (request != null) {
				try {
					harvestBinaries(request);
				} catch (Exception e) {
					logger.error("error processing " + request.url + "\t" + e);
				}
			} else {
				logger.info("[" + threadID + "] terminating");
				return;
			}
		} while (true);
	}

	static synchronized QueueRequest getRequest() {
		QueueRequest result = null;
			try {
				result = requestQueue.remove(0);
			} catch (Exception e) {
			}
			if (result != null)
				return result;
			return null;
	}

	void processURL(QueueRequest request) {
		if (request.url.length() > 300) {
			setIndexed(request);
			return;
		}
		org.jsoup.Connection connection = null;
		try {
			connection = Jsoup.connect(request.url).timeout(15000);
			Document document = connection.get();

			processor.storeURLs(document);
			processor.storeDocument(request.ID, document);

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
		executeStatement("alter sequence jsoup.institution_did_seq restart with 1");
		executeStatement("alter sequence jsoup.document_id_seq restart with 1");

		PreparedStatement filterStmt = mainConn.prepareStatement("select institution,domain,prefix,seed from jsoup.crawler_seed");
		ResultSet filterRS = filterStmt.executeQuery();
		while (filterRS.next()) {
			String institution = filterRS.getString(1);
			String domain = filterRS.getString(2);
			String prefix = filterRS.getString(3);
			String seed = filterRS.getString(4);

			logger.info("[++] seed: " + seed);
			int did = 0;
			PreparedStatement insert = mainConn.prepareStatement("insert into jsoup.institution(domain) values(?)",	Statement.RETURN_GENERATED_KEYS);
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

	static void trustAllCerts() throws NoSuchAlgorithmException, KeyManagementException {
		/*
		 * fix for Exception in thread "main" javax.net.ssl.SSLHandshakeException:
		 * sun.security.validator.ValidatorException: PKIX path building failed:
		 * sun.security.provider.certpath.SunCertPathBuilderException: unable to find
		 * valid certification path to requested target
		 */
		TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
			public java.security.cert.X509Certificate[] getAcceptedIssuers() {
				return null;
			}

			public void checkClientTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
			}

			public void checkServerTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
			}

		} };

		SSLContext sc = SSLContext.getInstance("SSL");
		sc.init(null, trustAllCerts, new java.security.SecureRandom());
		HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

		// Create all-trusting host name verifier
		HostnameVerifier allHostsValid = new HostnameVerifier() {
			@SuppressWarnings("unused")
			public boolean verify1(String hostname, SSLSession session) {
				return true;
			}

			public boolean verify(String arg0, SSLSession arg1) {
				return true;
			}
		};
		// Install the all-trusting host verifier
		HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);

	}
}
