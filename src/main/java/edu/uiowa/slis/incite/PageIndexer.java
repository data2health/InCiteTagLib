package edu.uiowa.slis.incite;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;

import javax.servlet.jsp.JspTagException;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

public class PageIndexer {
    static Logger logger = Logger.getLogger(PageIndexer.class);
    static Connection wintermuteConn = null;
    static String pathPrefix = "/usr/local/CD2H/lucene/";

    public static void main(String[] args) throws IOException, ClassNotFoundException, SQLException, JspTagException {
        PropertyConfigurator.configure(args[0]);
	wintermuteConn = getConnection("wintermute.slis.uiowa.edu");

	Directory indexDir = FSDirectory.open(new File(pathPrefix + "web"));
	IndexWriter indexWriter = new IndexWriter(indexDir, new IndexWriterConfig(Version.LUCENE_43, new StandardAnalyzer(org.apache.lucene.util.Version.LUCENE_43)));

	indexPages(indexWriter);
	indexWriter.close();
    }
    
    @SuppressWarnings("deprecation")
    static void indexPages(IndexWriter indexWriter) throws IOException, SQLException {
	int count = 0;
	logger.info("indexing CTSA hub web content...");
	PreparedStatement stmt = wintermuteConn.prepareStatement("select id,url,title from jsoup.document where exists (select id from jsoup.segment where segment.id=document.id)");
	ResultSet rs = stmt.executeQuery();

	while (rs.next()) {
	    count++;
	    int ID = rs.getInt(1);
	    String url = rs.getString(2);
	    String title = rs.getString(3);

	    logger.info("document: " + ID + "\t" + title);

	    Document theDocument = new Document();

	    theDocument.add(new Field("url", url, Field.Store.YES, Field.Index.NOT_ANALYZED));
	    theDocument.add(new Field("id", ID + "", Field.Store.YES, Field.Index.NOT_ANALYZED));

	    if (title == null) {
		theDocument.add(new Field("label", url, Field.Store.YES, Field.Index.ANALYZED));
	    } else {
		theDocument.add(new Field("label", title, Field.Store.YES, Field.Index.ANALYZED));		
		theDocument.add(new Field("content", title, Field.Store.NO, Field.Index.ANALYZED));
	    }
	    
	    PreparedStatement segStmt = wintermuteConn.prepareStatement("select content from jsoup.segment where id = ?");
	    segStmt.setInt(1, ID);
	    ResultSet segRS = segStmt.executeQuery();
	    while (segRS.next()) {
		String segment = segRS.getString(1);
		logger.debug("\tsegment: " + segment);
		theDocument.add(new Field("content", segment, Field.Store.NO, Field.Index.ANALYZED));
	    }
	    segStmt.close();
		
	    indexWriter.addDocument(theDocument);
	}
	stmt.close();
	logger.info("\tpages indexed: " + count);
    }
    
    public static Connection getConnection(String host) throws SQLException, ClassNotFoundException {
	Class.forName("org.postgresql.Driver");
	Properties props = new Properties();
	props.setProperty("user", "eichmann");
	props.setProperty("password", "translational");
//	if (use_ssl.equals("true")) {
//	    props.setProperty("sslfactory", "org.postgresql.ssl.NonValidatingFactory");
//	    props.setProperty("ssl", "true");
//	}
	Connection conn = DriverManager.getConnection("jdbc:postgresql://"+host+"/incite", props);
	conn.setAutoCommit(false);
	return conn;
    }
}
