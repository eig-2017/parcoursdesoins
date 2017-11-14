package query;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.query.impl.SimpleDataset;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import exceptions.InvalidContextException;
import exceptions.UnfoundEventException;
import exceptions.UnfoundFilterException;
import exceptions.UnfoundPredicatException;
import exceptions.UnfoundTerminologyException;
import parameters.Util;
import query.XMLFile.XMLelement;
import servlet.Endpoint;
import terminology.OneClass;
import terminology.Terminology;

/**
 * The describe event query return predicate and value of a particular event
 * @author cossin
 *
 */
public abstract class XMLDescribeQuery implements Query {
	final static Logger logger = LoggerFactory.getLogger(XMLDescribeQuery.class);
	/**
	 * The initial query is a XML file
	 */
	private XMLFile xml ;
	
	/**
	 * A string containing VALUES { value1 value2 ... valueN} where value1 ... valueN are eventInstances
	 */
	private Set<IRI> eventValuesSPARQL = new HashSet<IRI>();
	
	/**
	 * A a set of IRI containing VALUES { value1 value2 ... valueN} where value1 ... valueN are predicatesInstances
	 */
	private Set<IRI> predicateValuesBasic = new HashSet<IRI>();
	
	protected final String eventReplacementString = "EVENTSINSTANCESgoHERE";
	protected final String basicReplacementString = "PREDICATESgoHERE";
	
	private Terminology terminology; 
	
	protected String basicQuery ;
	
	protected void setBasicQuery(String basicQuery){
		this.basicQuery = basicQuery;
	}
	
	/**
	 * 
	 * @param xml
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 * @throws IOException
	 * @throws UnfoundEventException
	 * @throws UnfoundPredicatException
	 * @throws InvalidContextException
	 * @throws UnfoundTerminologyException 
	 * @throws UnfoundFilterException 
	 * @throws RepositoryException 
	 * @throws RDFParseException 
	 */
	public XMLDescribeQuery (XMLFile xml) throws ParserConfigurationException, SAXException, IOException, UnfoundPredicatException, InvalidContextException, UnfoundTerminologyException, UnfoundEventException, RDFParseException, RepositoryException, UnfoundFilterException{
		this.xml = xml;
		Node eventNode = xml.getEventNodes().item(0);
		this.terminology = XMLFile.getTerminology(eventNode);
		setEventValuesSPARQL(eventNode);
		setPredicatesValues(eventNode);
		//replacePredicatesValues();
	}
	
	/**
	 * main function of the Query type : return a SPARQL query
	 */
	public String getSPARQLQueryString() {
		return(basicQuery);
	}
	
	/**
	 * List of predicates asked in the query and their expected value (datatype / object type) 
	 * @param eventNode
	 * @throws UnfoundPredicatException
	 * @throws IOException 
	 * @throws RepositoryException 
	 * @throws RDFParseException 
	 * @throws UnfoundFilterException 
	 */
	private void setPredicatesValues (Node eventNode) throws UnfoundPredicatException, RDFParseException, RepositoryException, IOException, UnfoundFilterException{
		Element element = (Element) eventNode;
		NodeList predicates = element.getElementsByTagName(XMLelement.predicateType.toString());
		Node predicate = predicates.item(0);
		String predicateNames[] = predicate.getTextContent().split("\t");
		for (String predicateName : predicateNames){
			IRI predicateIRI = terminology.getPredicateDescription().getPredicate(predicateName).getPredicateIRI();
			predicateValuesBasic.add(predicateIRI);
		}
	}
	
	protected void replacePredicatesValues(){
		// basic : 
		StringBuilder sb = new StringBuilder();
		sb.append(" ");
		for (IRI predicateIRI : predicateValuesBasic){
			sb.append(Query.formatIRI4query(predicateIRI));
			sb.append(" ");
		}
		this.basicQuery = basicQuery.replace(basicReplacementString, sb.toString());
		
		// events : 
		sb.setLength(0);
		sb.append(" ");
		for (IRI predicateIRI : eventValuesSPARQL){
			sb.append(Query.formatIRI4query(predicateIRI));
			sb.append(" ");
		}
		this.basicQuery = basicQuery.replace(eventReplacementString, sb.toString());
	}
	
	/**
	 * Create a "VALUES" condition of events instances for the SPARQL query
	 * @param eventNode
	 * @throws UnfoundPredicatException
	 * @throws UnfoundEventException 
	 * @throws IOException 
	 * @throws RepositoryException 
	 * @throws RDFParseException 
	 * @throws UnfoundFilterException 
	 */
	private void setEventValuesSPARQL (Node eventNode) throws UnfoundPredicatException, UnfoundEventException, RDFParseException, RepositoryException, IOException, UnfoundFilterException{
		Element element = (Element) eventNode;
		NodeList eventInstance = element.getElementsByTagName(XMLelement.value.toString());
		Node eventInstances = eventInstance.item(0);
		String eventInstancesNames[] = eventInstances.getTextContent().split("\t");
		NodeList eventTypeNode = element.getElementsByTagName(XMLelement.eventType.toString());
		String className = eventTypeNode.item(0).getTextContent();
		OneClass oneClass = terminology.getClassDescription().getClass(className);
		for (String eventInstanceName : eventInstancesNames){
			eventValuesSPARQL.add(Util.vf.createIRI(oneClass.getClassIRI().getNamespace(), eventInstanceName));
		}
	}


	/**
	 * The list of variable for the XML describe query : 
	 * <ul>
	 * <li> ?context : the namedGraph specific to a patient
	 * <li> ?event : the event instance
	 * <li> ?predicate : the predicateIRI
	 * <li> ?value : datatype or object
	 * <ul>
	 */
	public String[] getVariableNames() {
		// TODO Auto-generated method stub
		return(null);
	}

	/**
	 * Get the context (namedGraphIRI) where to perform the query
	 */
	public SimpleDataset getContextDataset() {
		return xml.getContextDataSet();
	}
	
	@Override
	public Endpoint getEndpoint() {
		return terminology.getEndpoint();
	}

	@Override
	public Terminology getTerminology() {
		return terminology;
	}
}
