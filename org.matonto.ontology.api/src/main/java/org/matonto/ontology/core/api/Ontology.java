package org.matonto.ontology.core.api;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.Set;

import org.matonto.ontology.core.api.axiom.Axiom;
import org.matonto.ontology.core.utils.MatontoOntologyException;
import org.openrdf.model.Model;

public interface Ontology {
	
	public Model asModel() throws MatontoOntologyException;
	
	public OutputStream asTurtle() throws MatontoOntologyException;
	
	public OutputStream asRdfXml() throws MatontoOntologyException;
	
	public OutputStream asOwlXml() throws MatontoOntologyException;
	
	public OutputStream asJsonLD() throws MatontoOntologyException;
	
	public OntologyId getOntologyId();
	
	public Set<Annotation> getAnnotations();
	
	public void setOntologyId(OntologyId ontologyId);
	
	public void setAnnotations(Set<Annotation> annotations); 
	
//	public Set<Axiom> getAxioms();
	
	public void addAxiom(Axiom axiom);
	
	public void addAxioms(Set<Axiom> axioms);
	
	public void addAnnotation(Annotation annotation);
	
	public void addAnnotations(Set<Annotation> annotations);
	
	public void addDirectImport(OntologyIRI diIri);
	
	public void addDirectImports(Set<OntologyIRI> diIris);
	
	public Set<OntologyIRI> getDirectImportsDocuments();
	
	public boolean importOntology(InputStream inputStream, OntologyId ontologyId) throws MatontoOntologyException;
	
	public boolean importOntology(File file, OntologyId ontologyId) throws MatontoOntologyException;
	
	public boolean importOntology(URL url, OntologyId ontologyId) throws MatontoOntologyException;
	
	/**
	 * Compares two SimpleOntology objects by their resource ids (ontologyId) and RDF model of the ontology objects, 
	 * and returns true if the resource ids are equal and their RDF models are isomorphic. 
	 * 
	 * Two models are considered isomorphic if for each of the graphs in one model, an isomorphic graph exists in the 
	 * other model, and the context identifiers of these graphs are either identical or (in the case of blank nodes) 
	 * map 1:1 on each other.  RDF graphs are isomorphic graphs if statements from one graphs can be mapped 1:1 on to 
	 * statements in the other graphs. In this mapping, blank nodes are not considered mapped when having an identical 
	 * internal id, but are mapped from one graph to the other by looking at the statements in which the blank nodes 
	 * occur.
     *
     * Note: Depending on the size of the models, this can be an expensive operation.
     *
     * @return true if the resource ids are equal and their RDF models are isomorphic.
	 */
	public boolean equals(Object o);
	
	public int hashCode();
	
}
