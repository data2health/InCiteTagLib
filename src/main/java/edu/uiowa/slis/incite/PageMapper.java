package edu.uiowa.slis.incite;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.servlet.jsp.JspTagException;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.facet.index.FacetFields;
import org.apache.lucene.facet.taxonomy.CategoryPath;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.queries.mlt.MoreLikeThis;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

public class PageMapper {
	static Logger logger = Logger.getLogger(PageMapper.class);
	static Connection wintermuteConn = null;
	static String lucenePath = "/usr/local/CD2H/lucene/web";

	static IndexReader reader = null;
	static IndexSearcher theSearcher = null;

	public static void main(String[] args) throws IOException, ClassNotFoundException, SQLException, JspTagException {
		PropertyConfigurator.configure(args[0]);
		wintermuteConn = getConnection("localhost", "loki");

		reader = DirectoryReader.open(FSDirectory.open(new File(lucenePath)));
		theSearcher = new IndexSearcher(reader);
		Query theQuery = null;
		MoreLikeThis mlt = new MoreLikeThis(reader);
		mlt.setMinDocFreq(1);
		mlt.setMinTermFreq(1);
		mlt.setMaxQueryTerms(100);
		mlt.setFieldNames(new String[] { "content" });
		mlt.setAnalyzer(new StandardAnalyzer(Version.LUCENE_43));

		PreparedStatement stmt = wintermuteConn.prepareStatement(
				"select fid,path,coalesce(slot5,coalesce(slot4,coalesce(slot3,coalesce(slot2,coalesce(slot1,slot0))))) from ctsa_services.facet where hub = 'CD2H' order by path");
		ResultSet rs = stmt.executeQuery();
		while (rs.next()) {
			int fid = rs.getInt(1);
			String path = rs.getString(2);
			String slot = rs.getString(3);
			logger.info("CD2H node: " + fid + "\t" + path);

			PreparedStatement bindingStmt = wintermuteConn.prepareStatement(
					"select fid,hub,path,coalesce(slot5,coalesce(slot4,coalesce(slot3,coalesce(slot2,coalesce(slot1,slot0))))) from ctsa_services.facet,ctsa_services.binding where facet.fid = binding.hub_fid and binding.cd2h_fid = ? order by hub,path");
			bindingStmt.setInt(1, fid);
			ResultSet bindingRS = bindingStmt.executeQuery();
			while (bindingRS.next()) {
				int hid = bindingRS.getInt(1);
				String hub = bindingRS.getString(2);
				String hub_path = bindingRS.getString(3);
				String hub_slot = bindingRS.getString(4);
				logger.info("\tHub: " + hub + ":\t" + hid + "\t" + hub_slot + "\t" + hub_path);

				theQuery = mlt.like(new StringReader(hub_slot), "content");
				TopDocs theHits = theSearcher.search(theQuery, 10000);
				logger.info(theHits.totalHits);
				int count = 0;
				for (ScoreDoc theHit : theHits.scoreDocs) {
					Document document = theSearcher.doc(theHit.doc);
					String targetHub = document.get("domain");
					String url = document.get("url");
					if (!hub.endsWith(targetHub) || url.matches(".*#[-_a-zA-Z0-9]*"))
						continue;
					if (++count > 10)
						break;
					logger.info("\t\thit: " + theHit.score + "\t" + targetHub + "\t" + url);
					PreparedStatement insertStmt = wintermuteConn
							.prepareStatement("insert into ctsa_services.mapping values(?,?,?,?)");
					insertStmt.setInt(1, fid);
					insertStmt.setInt(2, hid);
					;
					insertStmt.setFloat(3, theHit.score);
					insertStmt.setString(4, url);
					insertStmt.execute();
					insertStmt.close();
				}
			}
			bindingStmt.close();
		}
		stmt.close();

//	Directory indexDir = FSDirectory.open(new File(pathPrefix + "web"));
//	Directory taxoDir = FSDirectory.open(new File(pathPrefix + "web_tax"));
//	IndexWriter indexWriter = new IndexWriter(indexDir, new IndexWriterConfig(Version.LUCENE_43, new StandardAnalyzer(org.apache.lucene.util.Version.LUCENE_43)));
//	DirectoryTaxonomyWriter taxoWriter = new DirectoryTaxonomyWriter(taxoDir);
//	FacetFields facetFields = new FacetFields(taxoWriter);
//
//	indexPages(indexWriter, facetFields);
//	taxoWriter.close();
//	indexWriter.close();
	}

	@SuppressWarnings("deprecation")
	static void indexPages(IndexWriter indexWriter, FacetFields facetFields) throws IOException, SQLException {
		int count = 0;
		logger.info("indexing CTSA hub web content...");
		PreparedStatement stmt = wintermuteConn.prepareStatement(
				"select domain,id,url,title from jsoup.document,jsoup.institution where document.did=institution.did and  exists (select id from jsoup.segment where segment.id=document.id)");
		ResultSet rs = stmt.executeQuery();

		while (rs.next()) {
			count++;
			String domain = rs.getString(1);
			int ID = rs.getInt(2);
			String url = rs.getString(3);
			String title = rs.getString(4);

			logger.info("document: " + ID + "\t" + title);

			Document theDocument = new Document();
			List<CategoryPath> paths = new ArrayList<CategoryPath>();

			paths.add(new CategoryPath("Source/CTSA web", '/'));
			paths.add(new CategoryPath("Entity/Web page", '/'));
			paths.add(new CategoryPath("CTSA/" + domain, '/'));
			theDocument.add(new Field("url", url, Field.Store.YES, Field.Index.NOT_ANALYZED));
			theDocument.add(new Field("id", ID + "", Field.Store.YES, Field.Index.NOT_ANALYZED));

			if (title == null) {
				theDocument.add(new Field("label", url, Field.Store.YES, Field.Index.ANALYZED));
			} else {
				theDocument.add(new Field("label", title, Field.Store.YES, Field.Index.ANALYZED));
				theDocument.add(new Field("content", title, Field.Store.NO, Field.Index.ANALYZED));
			}

			PreparedStatement segStmt = wintermuteConn
					.prepareStatement("select content from jsoup.segment where id = ?");
			segStmt.setInt(1, ID);
			ResultSet segRS = segStmt.executeQuery();
			while (segRS.next()) {
				String segment = segRS.getString(1);
				logger.debug("\tsegment: " + segment);
				theDocument.add(new Field("content", segment, Field.Store.NO, Field.Index.ANALYZED));
			}
			segStmt.close();

			facetFields.addFields(theDocument, paths);
			indexWriter.addDocument(theDocument);
		}
		stmt.close();
		logger.info("\tpages indexed: " + count);
	}

	public static Connection getConnection(String host, String database) throws SQLException, ClassNotFoundException {
		Class.forName("org.postgresql.Driver");
		Properties props = new Properties();
		LocalProperties prop_file = PropertyLoader.loadProperties("incite");
		props.setProperty("user", prop_file.getProperty("jdbc.user"));
		props.setProperty("password", prop_file.getProperty("jdbc.password"));
//	if (use_ssl.equals("true")) {
//	    props.setProperty("sslfactory", "org.postgresql.ssl.NonValidatingFactory");
//	    props.setProperty("ssl", "true");
//	}
		Connection conn = DriverManager.getConnection("jdbc:postgresql://" + host + "/" + database, props);
		conn.setAutoCommit(true);
		return conn;
	}
}
