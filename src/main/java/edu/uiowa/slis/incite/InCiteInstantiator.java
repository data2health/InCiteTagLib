package edu.uiowa.slis.incite;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

import edu.uiowa.GRID.Address;
import edu.uiowa.GRID.Institute;
import edu.uiowa.GeoNames.GeoName;
import edu.uiowa.NLP_grammar.syntaxTree;
import edu.uiowa.NLP_grammar.syntaxMatch.syntaxMatcher;
import edu.uiowa.NLP_grammar.syntaxMatch.comparator.emailComparator;
import edu.uiowa.NLP_grammar.syntaxMatch.comparator.entityComparator;
import edu.uiowa.NLP_grammar.syntaxMatch.comparator.urlComparator;
import edu.uiowa.PubMedCentral.entity.Organization;
import edu.uiowa.PubMedCentral.entity.Person;
import edu.uiowa.concept.Concept;
import edu.uiowa.concept.ExhaustiveVectorConceptRecognizer;
import edu.uiowa.concept.detector.GRIDDetector;
import edu.uiowa.concept.detector.GeoNamesDetector;
import edu.uiowa.entity.Entity;
import edu.uiowa.extraction.LocalProperties;
import edu.uiowa.extraction.Template;
import edu.uiowa.extraction.TemplateInstantiator;
import edu.uiowa.lex.Sentence;
import edu.uiowa.lex.basicLexerToken;

public class InCiteInstantiator extends TemplateInstantiator {
//    Hashtable<String, Person> personHash = new Hashtable<String, Person>();
//    Hashtable<String, Organization> organizationHash = new Hashtable<String, Organization>();

    public InCiteInstantiator(LocalProperties prop_file, Connection conn) throws ClassNotFoundException, SQLException {
	super(prop_file, conn);
    }

    @Override
    protected void instantiateEntity(int id, syntaxTree constituent, Template template) throws Exception {
	logger.debug("matched template: " + template);
	switch (template.relation) {
	case "organization":
	    Organization organization = organizationMatch(constituent, template.tgrep);
	    if (organization == null) {
		logger.debug("organization instantiation failed! : " + organization);
		logger.debug("\t\t" + template.tgrep);
		logger.debug("\t\t" + constituent.getFragmentString());
		logger.debug("\t\t" + constituent.treeString());
		break;
	    }
	    storeOrganization(id, organization);
	    constituent.setEntityClass("Organization");
	    bindNamedEntity(constituent, template, organization);
	    break;
	case "person":
	    Person person = coreferenceMatch(constituent, template, personMatch(constituent, template));
	    if (person != null) {
		storePerson(id, person);
		constituent.setEntityClass("Person");
		bindNamedEntity(constituent, template, person);
	    } else {
		syntaxMatcher theMatcher = new syntaxMatcher(template.tgrep);
		if (theMatcher.isMatch(constituent)) {
		    logger.info("person vector: " + theMatcher.matchesAsTokens());
		}
		
		if (person == null) {
		    logger.debug("person instantiation failed! : " + person);
		    logger.debug("\t\t" + template.tgrep);
		    logger.debug("\t\t" + constituent.getFragmentString());
		    logger.debug("\t\t" + constituent.treeString());
		    break;
		}
	    }
	    break;
	}
    }

    Person coreferenceMatch(syntaxTree node, Template template, Person candidate) {
	if (candidate == null)
	    return candidate;
	if (candidate.getID() > 0) {
	    logger.debug("coreference skip: " + candidate.getID() + " : " + candidate);
	    return candidate;
	}
	Person thePerson = candidate;
//	try {
//	    syntaxMatcher theMatcher = new syntaxMatcher(template.tgrep);
//	    if (theMatcher.isMatch(node)) {
//		if (otherSet.hasAuthor(theMatcher.matchesAsTokens().firstElement().text)) {
//		    thePerson = authorSet.getAuthor(theMatcher.matchesAsTokens().firstElement().text);
//		    logger.debug("coreference match: " + theMatcher.matchesAsTokens() + " : " + thePerson);
//		} else {
//		    otherSet.generateHashEntries(candidate.getFirstName(), candidate.getSurname(), candidate.getID());
//		    logger.debug("coreference miss: " + theMatcher.matchesAsTokens() + " : " + thePerson);
//		}
//	    }
//	} catch (Exception e) {
//	    logger.error("coreferenceMatch failed on Person: " + candidate);
//	}
	return thePerson;
    }
    
    int getScanFence(syntaxTree theNode) {
	int count = theNode.getLeadingLeafCount();
	if (count > 0)
	    return count;
	
	syntaxTree constituent = theNode.getConstituent(0);
	if (constituent.getConstituent(0) == null)
	    return 0;
	else
	    return constituent.getLeadingLeafCount();
    }

    protected void registerMatchFunctions(syntaxMatcher theMatcher) {
	theMatcher.registerFunction("isEntity", new entityComparator());
	theMatcher.registerFunction("isEmail", new emailComparator());
	theMatcher.registerFunction("isURL", new urlComparator());
    }

    GeoName heuristicGeoNameMatch(Vector<GeoName> geonames) {
	GeoName aGeoName = null;
	GeoName lGeoName = null;
	GeoName pGeoName = null;
	GeoName sGeoName = null;
	GeoName otherGeoName = null;
	
	for (GeoName current : geonames) {
	    switch (current.getFeatureClass()) {
	    case "A" :
		if (aGeoName == null || aGeoName.getPopulation() < current.getPopulation())
		    aGeoName = current;
		break;
	    case "L" :
		if (lGeoName == null || lGeoName.getPopulation() < current.getPopulation())
		    lGeoName = current;
		break;
	    case "P" :
		if (pGeoName == null || pGeoName.getPopulation() < current.getPopulation())
		    pGeoName = current;
		break;
	    case "S" :
		if (sGeoName == null || sGeoName.getPopulation() < current.getPopulation())
		    sGeoName = current;
		break;
            default:
		if (otherGeoName == null || otherGeoName.getPopulation() < current.getPopulation())
		    otherGeoName = current;
		break;
	    }
	}
	
	if (pGeoName != null)
	    return pGeoName;
	
	if (aGeoName != null)
	    return aGeoName;
	
	if (lGeoName != null)
	    return lGeoName;
	
	if (sGeoName != null)
	    return sGeoName;
	
	return otherGeoName;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    Organization organizationMatch(syntaxTree theNode, String pattern) throws Exception {
	logger.debug("organizationMatch: " + getScanFence(theNode) + " : " + theNode.treeString() + "\t" + pattern);
	Organization establishment = null;
	syntaxMatcher theMatcher = new syntaxMatcher(pattern);
	registerMatchFunctions(theMatcher);
	if (theMatcher.hasMatch(theNode)) {
	    Vector<basicLexerToken> matchVector = theMatcher.matchesAsTokens();
	    logger.debug("organization vector: " + matchVector);
	    establishment = new Organization(pruneMatchVector(matchVector));

	    ExhaustiveVectorConceptRecognizer gridRecognizer = new ExhaustiveVectorConceptRecognizer(new GRIDDetector(), false);
	    List theResults = gridRecognizer.recognize(new Sentence(matchVector), getScanFence(theNode));
	    logger.debug("GRID results: " + theResults);
	    boolean firstConcept = true;
	    for (Concept concept : (List<Concept>)theResults) {
		Vector<Institute> institutes = (Vector<Institute>)concept.getKey();
		if (firstConcept && institutes.size() == 1) {
		    establishment.setGridID(institutes.get(0).getId());
		    establishment.setGridMatchString((String)concept.getPhrase());
		} else if (firstConcept) {
		    boolean firstAddress = true;
		    for (Institute institute : institutes) {
			logger.debug("GRID option: " + institute);
			if (institute.getAddresses().size() > 0) {
			    Address address = institute.getAddresses().firstElement();
			    logger.debug("location match pattern: " + syntaxTree.generateNodePattern(theNode)+" >>[S <<(/" + address.getCity() + "|" + address.getState() + "|" + address.getCountryish() + "/) ]");
			    syntaxMatcher locationMatcher = null;
			    String matchPattern = null;
			    try {
				matchPattern = syntaxTree.generateNodePattern(theNode) + " >>[S <<(/" + address.getCity() + "|" + address.getState() + "|" + address.getCountryish() + "/) ]";
				locationMatcher = new syntaxMatcher(matchPattern);
				if (locationMatcher != null && locationMatcher.hasMatch(theNode)) {
				    Vector<basicLexerToken> addressMatchVector = locationMatcher.matchesAsTokens();
				    logger.debug("\tGRID address match vector: " + addressMatchVector);
				    if (firstAddress) {
					establishment.setGridID(institute.getId());
					establishment.setGridMatchString((String) concept.getPhrase());
					firstAddress = false;
				    }
				}
			    } catch (Exception e) {
				logger.error("organizationMatch: trapping erronous pattern: " + matchPattern);
			    }
			}
		    }
		}
		firstConcept = false;
	    }

	    ExhaustiveVectorConceptRecognizer geonameRecognizer = new ExhaustiveVectorConceptRecognizer(new GeoNamesDetector(), false);
	    List geoResults = geonameRecognizer.recognize(new Sentence(matchVector), getScanFence(theNode));
	    logger.debug("GeoNames results: " + geoResults);
	    firstConcept = true;
	    for (Concept concept : (List<Concept>)geoResults) {
		Vector<GeoName> geonames = (Vector<GeoName>)concept.getKey();
		if (firstConcept && geonames.size() == 1) {
		    establishment.setGeonamesID(geonames.get(0).getId());
		    establishment.setGeonamesMatchString((String)concept.getPhrase() + " := " + geonames.get(0).getAncestorContext());
		} else if (firstConcept) {
		    boolean firstAddress = true;
		    for (GeoName geoname : geonames) {
			logger.debug("GeoName option: " + geoname);
			logger.debug("location match pattern: " + syntaxTree.generateNodePattern(theNode) + " >>[S <<(/" + geoname.getAncestorContext(true) + "/) ]");
			syntaxMatcher locationMatcher = null;
			String matchPattern = null;
			try {
			    matchPattern = syntaxTree.generateNodePattern(theNode) + " >>[S <<(/" + geoname.getAncestorContext(true) + "/) ]";
			    locationMatcher = new syntaxMatcher(matchPattern);
			    if (locationMatcher != null & locationMatcher.hasMatch(theNode)) {
				Vector<basicLexerToken> addressMatchVector = locationMatcher.matchesAsTokens();
				logger.debug("\tGeoName match vector: " + addressMatchVector);
				if (firstAddress) {
				    establishment.setGeonamesID(geoname.getId());
				    establishment.setGeonamesMatchString((String) concept.getPhrase() + " :< " + geoname.getAncestorContext(true));
				    firstAddress = false;
				}
			    }
			} catch (Exception e) {
			    logger.error("organizationMatch: trapping erronous pattern: " + matchPattern);
			}
		    }
		    if (firstAddress) {
			GeoName heuristicMatch = heuristicGeoNameMatch(geonames);
			if (heuristicMatch != null) {
			    establishment.setGeonamesID(heuristicMatch.getId());
			    establishment.setGeonamesMatchString(concept.getPhrase() + " :? " + heuristicMatch.getAncestorContext());
			}
		    }
		}
		firstConcept = false;
	    }

	    logger.debug("organization entity: " + establishment);
	}
	return establishment;
    }

    Person personMatch(syntaxTree theNode, Template template) throws Exception {
	Person thePerson = null;
	if (template.tgrep.equals("[NP NNP:Person NN:FirstName NN:LastName ]") || template.tgrep.equals("[NP NNP:Person NN:FirstName NN:FirstName ]")) {
	    logger.info("problematic matching: " + template.tgrep);
	}
	syntaxMatcher theMatcher = new syntaxMatcher(template.tgrep);
	if (theMatcher.isMatch(theNode)) {
	    logger.debug("person vector: " + theMatcher.matchesAsTokens());
	    thePerson = new Person(pruneMatchVector(theMatcher.matchesAsTokens()));
	}
	logger.debug("person entity: " + thePerson);
	return thePerson;
    }

    void storeOrganization(int docID, Organization organization) throws SQLException {
	int id = 0;

	Organization match = null; // organizationHash.get(organization.toString());
	if (match != null) {
	    organization.setID(match.getID());
	} else {
	    try {
		PreparedStatement insert = conn.prepareStatement("insert into entity.organization(organization,grid_id,grid_match_string,geonames_id,geonames_match_string) values(?,?,?,?,?)", Statement.RETURN_GENERATED_KEYS);
		insert.setString(1, organization.toString());
		insert.setString(2, organization.getGridID());
		insert.setString(3, organization.getGridMatchString());
		if (organization.getGeonamesID() == 0)
		    insert.setNull(4, Types.INTEGER);
		else
		    insert.setInt(4, organization.getGeonamesID());
		insert.setString(5, organization.getGeonamesMatchString());
		insert.execute();
		ResultSet rs = insert.getGeneratedKeys();
		while (rs.next()) {
		    id = rs.getInt(1);
		    logger.info("new organization id: " + id + " : " + organization);
		    organization.setID(id);
//		    organizationHash.put(organization.toString(), organization);
		}
		insert.close();
	    } catch (SQLException e) {
		if (e.getSQLState().equals("23505")) {
		    conn.rollback();
		    PreparedStatement select = conn.prepareStatement("select id,grid_id,geonames_id from entity.organization where organization = ?");
		    select.setString(1, organization.toString());
		    ResultSet rs = select.executeQuery();
		    while (rs.next()) {
			id = rs.getInt(1);
			logger.debug("organization id: " + id);
			organization.setID(id);
			organization.setGridID(rs.getString(2));
			organization.setGeonamesID(rs.getInt(3));
//			organizationHash.put(organization.toString(), organization);
		    }
		    select.close();

		} else {
		    e.printStackTrace();
		}
	    } finally {
		conn.commit();
	    }
	}
	try {
	    PreparedStatement insert = conn.prepareStatement("insert into entity.organization_mention(organization_id,id) values(?,?)");
	    insert.setInt(1, organization.getID());
	    insert.setInt(2, docID);
	    insert.execute();
	} catch (SQLException e) {
	    if (e.getSQLState().equals("23505")) {
		conn.rollback();
	    } else {
		e.printStackTrace();
	    }
	} finally {
	    conn.commit();
	}
    }
    
    void storePerson(int docID, Person thePerson) throws SQLException {
	int id = 0;

	Person match = null; //personHash.get(thePerson.toString());
	if (match != null) {
	    thePerson.setID(match.getID());
	} else {
	    try {
		PreparedStatement insert = conn.prepareStatement("insert into entity.person(first_name,last_name,middle_name,title,appendix) values(?,?,?,?,?)", Statement.RETURN_GENERATED_KEYS);
		insert.setString(1, thePerson.getFirstName());
		insert.setString(2, thePerson.getSurname());
		insert.setString(3, thePerson.getMiddleName());
		insert.setString(4, thePerson.getTitle());
		insert.setString(5, thePerson.getAppendix());
		insert.execute();
		ResultSet rs = insert.getGeneratedKeys();
		while (rs.next()) {
		    id = rs.getInt(1);
		    logger.info("new person id: " + id + "\t" + thePerson);
		    thePerson.setID(id);
//		    personHash.put(thePerson.toString(), thePerson);
		}
		insert.close();
	    } catch (SQLException e) {
		if (e.getSQLState().equals("23505")) {
		    conn.rollback();
		    PreparedStatement select = conn.prepareStatement("select id from entity.person where first_name = ? and last_name = ? and middle_name = ? and title = ? and appendix = ?");
		    select.setString(1, thePerson.getFirstName());
		    select.setString(2, thePerson.getSurname());
		    select.setString(3, thePerson.getMiddleName());
		    select.setString(4, thePerson.getTitle());
		    select.setString(5, thePerson.getAppendix());
		    boolean found = false;
		    ResultSet rs = select.executeQuery();
		    while (rs.next()) {
			id = rs.getInt(1);
			logger.debug("person id: " + id);
			thePerson.setID(id);
//			personHash.put(thePerson.toString(), thePerson);
			found = true;
		    }
		    select.close();
		    if (!found)
			logger.error("failed to retrieve person: " + thePerson);
		} else {
		    e.printStackTrace();
		}
	    } finally {
		conn.commit();
	    }
	}
	try {
	    PreparedStatement insert = conn.prepareStatement("insert into entity.person_mention(person_id,id) values(?,?)");
	    insert.setInt(1, thePerson.getID());
	    insert.setInt(2, docID);
	    insert.execute();
	} catch (SQLException e) {
	    if (e.getSQLState().equals("23505")) {
		conn.rollback();
	    } else {
		e.printStackTrace();
	    }
	} finally {
	    conn.commit();
	}
    }

    @Override
    public void resolveID(int id, Entity elementAt) throws SQLException {
	// TODO Auto-generated method stub
	
    }

}
