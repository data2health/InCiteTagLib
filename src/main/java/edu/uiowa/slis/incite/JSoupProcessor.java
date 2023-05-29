package edu.uiowa.slis.incite;

import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Hashtable;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import edu.uiowa.crawling.URLRequest;

public class JSoupProcessor {
	static Logger logger = Logger.getLogger(JSoupProcessor.class);
	Connection conn = null;
	int threadID = 0;
	Hashtable<String, Integer> domainHash = null;
	Hashtable<String, Integer> urlHash = null;
	Pattern suffixPat = Pattern.compile(".*?(\\.[^./]+(\\.gz)?)$");
	
	public JSoupProcessor(Connection conn, int threadID, Hashtable<String, Integer> domainHash, Hashtable<String, Integer> urlHash) {
		this.conn = conn;
		this.threadID = threadID;
		this.domainHash = domainHash;
		this.urlHash = urlHash;
	}

	void storeURLs(Document document) throws SQLException, MalformedURLException {
		Elements links = document.getElementsByTag("a");
		for (Element linkElement : links) {
			logger.debug("[" + threadID + "] linkElement: " + linkElement);
			URL theURL = null;
			try {
				theURL = URLRequest.canonicalURL(null, null, linkElement.absUrl("href")); // mostly to clean up # mentions
			} catch (Exception e1) {
				continue;
			}
			String theURLString = theURL.toString();
			URLRequest link = new URLRequest(theURLString);
			String hostname = theURL.getHost();
			logger.debug("[" + threadID + "] host name: " + hostname);

			if (hostname == null)
				continue;

			String part1 = null;
			String part2 = null;
			StringTokenizer theTokenizer = new StringTokenizer(hostname, ".");
			while (theTokenizer.hasMoreTokens()) {
				part1 = part2;
				part2 = theTokenizer.nextToken();
			}

			if (part1 == null || part2 == null)
				continue;

			String domainname = part1 + "." + part2;
			int did = 0;
			String suffix = null;

			Matcher theMatcher = suffixPat.matcher(theURL.getPath());
			if (theMatcher.find()) {
				suffix = theMatcher.group(1);
			}
			if (suffix != null && suffix.indexOf(' ') > 0)
				suffix = suffix.substring(0, suffix.indexOf(' '));
			if (suffix != null && suffix.indexOf(';') > 0)
				suffix = suffix.substring(0, suffix.indexOf(';'));
			if (suffix != null && suffix.length() > 10)
				suffix = null;

			logger.debug("[" + threadID + "] domain name: " + domainname);
			logger.debug("[" + threadID + "] suffix: " + suffix);

			if (domainHash.containsKey(domainname)) {
				did = domainHash.get(domainname);
				logger.debug("[" + threadID + "]\tcached did: " + did);
			} else {
				try {
					PreparedStatement insert = conn.prepareStatement("insert into jsoup.institution(domain) values(?)",	Statement.RETURN_GENERATED_KEYS);
					insert.setString(1, domainname);
					insert.execute();
					ResultSet rs = insert.getGeneratedKeys();
					while (rs.next()) {
						did = rs.getInt(1);
						logger.debug("[" + threadID + "]\tdid: " + did);
					}
				} catch (SQLException e) {
					if (e.getSQLState().equals("23505")) {
						conn.rollback();
						PreparedStatement select = conn.prepareStatement("select did from jsoup.institution where domain = ?");
						select.setString(1, domainname);
						ResultSet rs = select.executeQuery();
						while (rs.next()) {
							did = rs.getInt(1);
							logger.debug("[" + threadID + "]\texisting id: " + did);
						}

					} else {
						e.printStackTrace();
					}
				} finally {
					conn.commit();
				}

				domainHash.put(domainname, did);
			}

			logger.debug("[" + threadID + "] link url: " + theURLString);
			if (urlHash.containsKey(theURLString)) {
				link.setID(urlHash.get(theURLString));
				logger.debug("[" + threadID + "]\tcached id: " + theURLString);
			} else {
				try {
					PreparedStatement insert = conn.prepareStatement("insert into jsoup.document(url,did,suffix) values(?,?,?)", Statement.RETURN_GENERATED_KEYS);
					insert.setString(1, theURLString);
					insert.setInt(2, did);
					insert.setString(3, suffix);
					insert.execute();
					ResultSet rs = insert.getGeneratedKeys();
					while (rs.next()) {
						link.setID(rs.getInt(1));
						logger.debug("[" + threadID + "]\tid: " + theURLString);
					}
				} catch (SQLException e) {
					if (e.getSQLState().equals("23505")) {
						conn.rollback();
						PreparedStatement select = conn.prepareStatement("select id from jsoup.document where url = ?");
						select.setString(1, theURLString);
						ResultSet rs = select.executeQuery();
						while (rs.next()) {
							link.setID(rs.getInt(1));
							logger.debug("[" + threadID + "]\texisting id: " + theURLString);
						}

					} else {
						e.printStackTrace();
					}
				} finally {
					conn.commit();
				}

				urlHash.put(theURLString, link.getID());

			}
		}
	}

	void getTextBlocks(Vector<TextBlock> blocks, Element element) {
		if (element.children().isEmpty()) {
			String elementText = element.text().trim();
			if (elementText.length() > 0)
				blocks.add(new TextBlock(element.tagName(), elementText));
		}
		for (Element child : element.children()) {
			getTextBlocks(blocks, child);
		}

	}

	void storeMeta(int id, String name, String content) throws SQLException {
		logger.debug("[" + threadID + "] meta name: " + name + "\tcontent: " + content);
		PreparedStatement metaStmt = conn.prepareStatement("insert into jsoup.meta values(?,?,?)");
		metaStmt.setInt(1, id);
		metaStmt.setString(2, name);
		metaStmt.setString(3, content);
		metaStmt.execute();
		metaStmt.close();
	}

	void storeDocument(int id, Document document) throws SQLException {
		PreparedStatement insStmt = conn.prepareStatement("update jsoup.document set title = ?, length = ?, indexed = now() where id = ?");
		insStmt.setString(1, document.title());
		insStmt.setInt(2, document.toString().length());
		insStmt.setInt(3, id);
		insStmt.execute();
		insStmt.close();

		for (Element metaTag : document.getElementsByTag("meta")) {
			String content = metaTag.attr("content");
			String name = metaTag.attr("name").trim();
			String property = metaTag.attr("property").trim();
			String itemprop = metaTag.attr("itemprop").trim();
			if (name != null && name.length() > 0)
				storeMeta(id, name, content);
			else if (property != null && property.length() > 0)
				storeMeta(id, property, content);
			else if (itemprop != null && itemprop.length() > 0)
				storeMeta(id, itemprop, content);
			else
				logger.debug("[" + threadID + "] meta ***: " + metaTag);
		}

		logger.debug("[" + threadID + "] document text: " + document.text());
		int blockCount = 0;
		Vector<TextBlock> blocks = new Vector<TextBlock>();
		getTextBlocks(blocks, document);
		for (TextBlock block : blocks) {
			logger.debug("[" + threadID + "] text block: " + block);
			PreparedStatement blockStmt = conn.prepareStatement("insert into jsoup.segment values(?,?,?,?)");
			blockStmt.setInt(1, id);
			blockStmt.setInt(2, blockCount++);
			blockStmt.setString(3, block.tag);
			blockStmt.setString(4, block.content);
			blockStmt.execute();
			blockStmt.close();
		}

		PreparedStatement linkStmt = conn.prepareStatement("insert into jsoup.hyperlink values (?,?,?,?)");
		PreparedStatement mailStmt = conn.prepareStatement("insert into jsoup.email values (?,?,?,?)");
		int linkCount = 0;
		int mailCount = 0;
		Elements links = document.getElementsByTag("a");
		for (Element link : links) {
			String linkHref = link.absUrl("href");
			String linkText = link.text();
			logger.debug("[" + threadID + "]\tlink: " + linkHref + "\tanchor: " + linkText);

			if (linkHref.startsWith("mailto:")) {
				mailStmt.setInt(1, id);
				mailStmt.setInt(2, mailCount++);
				mailStmt.setString(3, linkHref);
				mailStmt.setString(4, linkText);
				mailStmt.addBatch();
			} else if (urlHash.containsKey(linkHref)) {
				int linkID = urlHash.get(linkHref);
				logger.debug("[" + threadID + "] doc id: " + id + "\tlink id: " + linkID + "\tanchor: " + linkText);
				linkStmt.setInt(1, id);
				linkStmt.setInt(2, linkCount++);
				linkStmt.setInt(3, linkID);
				linkStmt.setString(4, linkText);
				linkStmt.addBatch();
			}
		}
		linkStmt.executeBatch();
		linkStmt.close();
		mailStmt.executeBatch();
		mailStmt.close();
		conn.commit();
	}

	class TextBlock {
		String tag = null;
		String content = null;

		public TextBlock(String tag, String content) {
			this.tag = tag;
			this.content = content;
		}

		public String toString() {
			return tag + ": " + content;
		}
	}

}
