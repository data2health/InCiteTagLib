package edu.uiowa.slis.incite;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Hashtable;

import org.apache.log4j.Logger;

import edu.uiowa.lex.BrillToken;
import edu.uiowa.lex.SentenceGenerator;
import edu.uiowa.lex.basicLexerToken;
import edu.uiowa.lex.parameterizedSentenceGenerator;

public class BiomedicalSentenceGenerator extends parameterizedSentenceGenerator {
    static Logger logger = Logger.getLogger(BiomedicalSentenceGenerator.class);
    Hashtable<String,String> abbreviationHash = new Hashtable<String,String>();
    
    public BiomedicalSentenceGenerator(Connection conn) {
	super();
	
	logger.info("Initializing BiomedicalSentenceGenerator...");
	try {
	    PreparedStatement stmt = conn.prepareStatement("select suffix from extraction.false_alarm");
	    ResultSet rs = stmt.executeQuery();
	    while (rs.next()) {
		String suffix = rs.getString(1);
		logger.debug("suffix: " + suffix);
		abbreviationHash.put(suffix, suffix);
	    }
	} catch (SQLException e) {
	    logger.error("Error initializing BiomedicalSentenceGenerator: " + e);
	}
	
    }

    @Override
    public boolean tokenIsMergable(basicLexerToken token) {
	return abbreviationHash.containsKey(token.text);
    }
    
    protected void reset() {
	if (theSentence.size() > 0) {
	    BrillToken current = (BrillToken) theSentence.lastElement();
	    String lastToken = current.text;
	    if (!SentenceGenerator.isTerminalPunctuation(lastToken) && lastToken.endsWith(".")) {
		logger.debug("correcting sentence: " + theSentence);
		current.setText(lastToken.substring(0, lastToken.length() - 1));
		BrillToken terminal = new BrillToken(BrillToken.WORD_Token, ".", current.line);
		terminal.tag = ".";
		theSentence.addElement(terminal);
		logger.debug("\tcorrected: " + theSentence);
	    }
	}
	super.reset();
    }

}
