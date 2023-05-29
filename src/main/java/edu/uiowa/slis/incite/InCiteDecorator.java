package edu.uiowa.slis.incite;

import java.sql.Connection;
import java.sql.SQLException;

import org.apache.log4j.Logger;

import edu.uiowa.NLP_grammar.syntaxTree;
import edu.uiowa.NLP_grammar.syntaxMatch.syntaxMatch;
import edu.uiowa.NLP_grammar.syntaxMatch.syntaxMatcher;
import edu.uiowa.NLP_grammar.syntaxMatch.comparator.organizationComparator;
import edu.uiowa.PubMedCentral.comparator.OrganizationComparator;
import edu.uiowa.PubMedCentral.comparator.PersonComparator;
import edu.uiowa.PubMedCentral.comparator.PersonFirstNameComparator;
import edu.uiowa.PubMedCentral.comparator.PersonLastNameComparator;
import edu.uiowa.PubMedCentral.comparator.PersonTitleComparator;
import edu.uiowa.entity.named.Organization;
import edu.uiowa.entity.named.Person;
import edu.uiowa.extraction.Decorator;

public class InCiteDecorator extends Decorator {
	static Logger logger = Logger.getLogger(InCiteDecorator.class);

	public InCiteDecorator(Connection conn) throws Exception {
		super();
		init(conn);
	}

	private void init(Connection conn) throws SQLException {
		OrganizationComparator.init(conn);
		PersonComparator.init(conn);
		PersonFirstNameComparator.init(conn);
		PersonLastNameComparator.init(conn);
		PersonTitleComparator.init(conn);

		Organization.initialize();
		Person.initialize();
	}

	@Override
	public boolean decorateTree(syntaxTree theTree) throws Exception {
		decorateTree(theTree, "Organization");
		decorateTree(theTree, "Person");
		decorateTree(theTree, "TitleName");
		decorateTree(theTree, "Discipline");
		decorateTree(theTree, "FirstName");
		decorateTree(theTree, "LastName");
		return false;
	}

	private boolean decorateTree(syntaxTree theTree, String entity) throws Exception {
		return decorateTree2(theTree, entity, entity);
	}

	private boolean decorateTree2(syntaxTree theTree, String entity, String entityClass) throws Exception {
		decorateTree(theTree, "is" + entity + "(NN)", entityClass);
		decorateTree(theTree, "is" + entity + "(NNP)", entityClass);
		decorateTree(theTree, "is" + entity + "(NNPS)", entityClass);
		decorateTree(theTree, "is" + entity + "(NNS)", entityClass);

		return true;
	}

	private boolean decorateTree(syntaxTree theTree, String pattern, String entity) throws Exception {
		syntaxMatcher theMatcher = new syntaxMatcher(pattern);
		theMatcher.registerFunction("isOrganization", new organizationComparator());
		theMatcher.registerFunction("isOrganization", new OrganizationComparator());
		theMatcher.registerFunction("isPerson", new PersonComparator());
		theMatcher.registerFunction("isFirstName", new PersonFirstNameComparator());
		theMatcher.registerFunction("isLastName", new PersonLastNameComparator());
		theMatcher.registerFunction("isTitleName", new PersonTitleComparator());

		if (theMatcher.hasMatch(theTree)) {
			logger.debug("<<<<< match >>>>>");
			for (syntaxMatch theMatchNode : theMatcher.matches()) {
				if (theMatchNode.getPhrase().getEntity() != null)
					continue;
				if (entity.equals("*"))
					theMatchNode.getPhrase().setEntity(theMatchNode.getPhrase().getEntityClass());
				else
					theMatchNode.getPhrase().setEntity(entity);
				logger.debug("match node: " + theMatchNode.getPhrase().treeString());
				if (theMatchNode.getPhrase().getFragmentStringVector2().size() == 0) {
					logger.debug("** fragment is empty!");
				} else
					logger.debug("fragment: " + theMatchNode.getPhrase().getFragmentStringVector2().firstElement());
				for (int i = 1; i <= theMatchNode.matchCount(); i++)
					logger.debug("\tmatch slot [" + i + "]: " + theMatchNode.getMatch(i).treeString());
			}
		}
		return false;
	}
}
