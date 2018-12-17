package edu.uiowa.slis.incite;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.DecimalFormat;
import java.util.Properties;
import java.util.Vector;

import org.apache.log4j.Logger;

import edu.uiowa.NLP_grammar.FragmentGenerator;
import edu.uiowa.NLP_grammar.ParseFragment;
import edu.uiowa.PubMedCentral.AcknowledgementDecorator;
import edu.uiowa.PubMedCentral.AcknowledgementInstantiator;
import edu.uiowa.extraction.Decorator;
import edu.uiowa.extraction.LocalProperties;
import edu.uiowa.extraction.TemplatePromoter;

public class FragmenterThread implements Runnable {
    static Logger logger = Logger.getLogger(FragmenterThread.class);

    int threadID = 0;
    Connection pmcConn = null;
    Connection conn = null;
    LocalProperties prop_file = null;
    Vector<Integer> queue = null;
    Decorator theDecorator = null;
    DecimalFormat formatter = new DecimalFormat("#00.###");

    public FragmenterThread(int threadID, Vector<Integer> queue, Connection conn, LocalProperties prop_file) throws Exception {
	this.threadID = threadID;
	this.conn = conn;
	this.queue = queue;
	this.prop_file = prop_file;
	pmcConn = getPMCConnection();
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

    public void run() {
	while (queue.size() > 0) {
	    try {
		int id = queue.remove(0);
		generate(id);
	    } catch (Exception e) {
	    }
	}
    }

    void generate(int ID) throws Exception {
	FragmentGenerator theGenerator = new FragmentGenerator(new InCiteDecorator(conn), new AcknowledgementInstantiator(prop_file, conn), new TemplatePromoter(conn));
	logger.info("[" + formatter.format(threadID) + "] really fragmenting " + ID);
	PreparedStatement stmt = conn.prepareStatement("select seqnum,sentnum,parse from extraction.parse where id = ? order by seqnum,sentnum");
	stmt.setInt(1, ID);
	ResultSet rs = stmt.executeQuery();
	while (rs.next()) {
	    int seqnum = rs.getInt(1);
	    int sentnum = rs.getInt(2);
	    String parseString = rs.getString(3);
	    logger.debug("[" + formatter.format(threadID) + "] : " + ID + " : " + seqnum + " : " + parseString);
	    for (ParseFragment fragment : theGenerator.fragments(ID, parseString)) {
		logger.debug("[" + formatter.format(threadID) + "]\tfragment: " + fragment.getFragmentString());
		logger.debug("[" + formatter.format(threadID) + "] \t\tparse: " + fragment.getFragmentParse());

		PreparedStatement insertStmt = conn.prepareStatement("insert into " + Fragmenter.prop_file.getProperty("jdbc.schema") + ".fragment values (?,?,?,?,?)");
		insertStmt.setInt(1, ID);
		insertStmt.setInt(2, seqnum);
		insertStmt.setInt(3, seqnum);
		insertStmt.setString(4, fragment.getFragmentString());
		insertStmt.setString(5, fragment.getFragmentParse());
		insertStmt.executeUpdate();
		insertStmt.close();
	    }
	}
	stmt.close();
	conn.commit();

    }

}
