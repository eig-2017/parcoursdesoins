package terminology;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import exceptions.MyExceptions;
import integration.DBconnection;
import parameters.Util;
import query.Query;

public class TerminologyServer {
	
	final static Logger logger = LoggerFactory.getLogger(TerminologyServer.class);
	
	private DBconnection con ;
	
	public DBconnection getCon(){
		return(con);
	}
	
	private Terminology terminology ;
	
	private Set<IRI> instancesIRI ;
	
	public TerminologyServer(Terminology terminology){
		this.terminology = terminology;
		String sparqlEndpoint = terminology.getEndpoint().getEndpointIPadress();
		con = new DBconnection(sparqlEndpoint);
		
		// test connection 
		String queryString = "ASK {?s ?p ?o}" ;
		try {
			logger.info("Trying to connect to " + sparqlEndpoint);
			con.getDBcon().prepareBooleanQuery(queryString).evaluate();
		} catch (IllegalArgumentException | MalformedQueryException | RepositoryException e) {
			MyExceptions.logException(logger, e);
			throw e;
		}
		logger.info("Connection successful");
	}
	
	public void countInstances(){
		IRI classNameIRI = terminology.getMainClassIRI();
		logger.info("Counting number of instances of " + classNameIRI.stringValue());
		String query = countInstancesQuery(classNameIRI);
		TupleQueryResult queryResult = con.getDBcon().prepareTupleQuery(query).evaluate();
		String count="0";
		if (queryResult.hasNext()){
			BindingSet set = queryResult.next();
			count = set.getValue("count").stringValue();
		}
		logger.info("\t" + classNameIRI.stringValue() + ": " + count);
		try{
			queryResult.close();
		} catch (Exception e) {
			MyExceptions.logException(logger, e);
		}

	}
	
	public void loadTerminology() throws Exception{
		File instancesFile = terminology.getInstancesFile();
		if (instancesFile == null){
			return;
		}
		String terminoNameSpace = terminology.getNAMESPACE();
		logger.info("Trying to load "+ instancesFile.getAbsolutePath() + "...");
		try {
			con.getDBcon().add(instancesFile, terminoNameSpace, Util.DefaultRDFformat);
			logger.info("\t Successful");
		} catch (RDFParseException | RepositoryException | IOException e) {
			// TODO Auto-generated catch block
			MyExceptions.logException(logger, e);
			throw e;
		}
	}
	
	private String countInstancesQuery (IRI className){
		String queryString = "SELECT (count(distinct ?s) as ?count) WHERE {?s a " + 
				Query.formatIRI4query(className) + ". } \n";
		return(queryString);
	}
	
	private String getInstancesQuery(IRI className){
		String queryString = "SELECT ?s WHERE {?s a " + Query.formatIRI4query(className) + ". } \n";
		return(queryString);
	}
	
	public void setInstancesOfTerminology(){
		Set<IRI> instancesIRI = new HashSet<IRI>();
		IRI className = terminology.getMainClassIRI();
		String query = getInstancesQuery(className);
		
		logger.info("Trying to get instances of " + className.stringValue() + "...");
		
		TupleQueryResult queryResult = con.getDBcon().prepareTupleQuery(query).evaluate();
		while (queryResult.hasNext()){
			BindingSet set = queryResult.next();
			IRI instanceIRI = (IRI) set.getValue("s");
			instancesIRI.add(instanceIRI);
		}
		try{
			queryResult.close();
		} catch (Exception e) {
			MyExceptions.logException(logger, e);
		}
		
		logger.info("\t" + instancesIRI.size() + " instances retrieved");
		this.instancesIRI = instancesIRI;
	}
	
	public boolean isInstanceOfTerminology(IRI instanceIRI){
		if (instancesIRI == null){
			setInstancesOfTerminology();
		}
		return(instancesIRI.contains(instanceIRI));
	}
	
	public static void main (String[] args) throws Exception{
		
//		Set<TerminoEnum> terminoToLoad = new HashSet<TerminoEnum>();
//		//terminoToLoad.add(TerminoEnum.FINESS);
//		//terminoToLoad.add(TerminoEnum.RPPS);
//		// terminoToLoad.add(TerminoEnum.CONTEXT);
//		 terminoToLoad.add(TerminoEnum.CIM10);
		 
		for (Terminology terminology : TerminologyInstances.terminologies){
			TerminologyServer terminoServer = terminology.getTerminologyServer();
			//terminoServer.loadTerminology();
			terminoServer.countInstances();
			terminoServer.getCon().close();
		}
	}
}
