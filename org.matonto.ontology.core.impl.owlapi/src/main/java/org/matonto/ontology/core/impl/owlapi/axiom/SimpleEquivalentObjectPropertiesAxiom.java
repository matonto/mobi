package org.matonto.ontology.core.impl.owlapi.axiom;

import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import javax.annotation.Nonnull;
import org.matonto.ontology.core.api.Annotation;
import org.matonto.ontology.core.api.axiom.EquivalentObjectPropertiesAxiom;
import org.matonto.ontology.core.api.propertyexpression.ObjectPropertyExpression;
import org.matonto.ontology.core.api.types.AxiomType;


public class SimpleEquivalentObjectPropertiesAxiom 
	extends SimpleAxiom 
	implements EquivalentObjectPropertiesAxiom {

	
	private Set<ObjectPropertyExpression> properties;
	
	
	public SimpleEquivalentObjectPropertiesAxiom(@Nonnull Set<ObjectPropertyExpression> properties, Set<Annotation> annotations) 
	{
		super(annotations);
		this.properties = new TreeSet<ObjectPropertyExpression>(properties);;
	}

	
	@Override
	public EquivalentObjectPropertiesAxiom getAxiomWithoutAnnotations() 
	{
		if(!isAnnotated())
			return this;
		
		return new SimpleEquivalentObjectPropertiesAxiom(properties, NO_ANNOTATIONS);	
	}

	
	@Override
	public EquivalentObjectPropertiesAxiom getAnnotatedAxiom(@Nonnull Set<Annotation> annotations) 
	{
		return new SimpleEquivalentObjectPropertiesAxiom(properties, mergeAnnos(annotations));
	}

	
	@Override
	public AxiomType getAxiomType()
	{
		return AxiomType.EQUIVALENT_OBJECT_PROPERTIES;
	}

	
	@Override
	public Set<ObjectPropertyExpression> getObjectPropertys() 
	{
		return new HashSet<ObjectPropertyExpression> (properties);
	}
	
	
	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) 
		    return true;
		
		if (!super.equals(obj)) 
			return false;
		
		if (obj instanceof EquivalentObjectPropertiesAxiom) {
			EquivalentObjectPropertiesAxiom other = (EquivalentObjectPropertiesAxiom)obj;			 
			return properties.equals(other.getObjectPropertys());
		}
		
		return false;
	}

}
