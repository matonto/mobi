package org.matonto.ontology.core.impl.owlapi.axiom;

import java.util.Set;
import javax.annotation.Nonnull;
import org.matonto.ontology.core.api.Annotation;
import org.matonto.ontology.core.api.classexpression.ClassExpression;
import org.matonto.ontology.core.api.propertyexpression.ObjectPropertyExpression;
import org.matonto.ontology.core.api.axiom.ObjectPropertyRangeAxiom;
import org.matonto.ontology.core.api.types.AxiomType;


public class SimpleObjectPropertyRangeAxiom 
	extends SimpleAxiom 
	implements ObjectPropertyRangeAxiom {

	
	private ObjectPropertyExpression objectProperty;
	private ClassExpression range;
	
	
	public SimpleObjectPropertyRangeAxiom(@Nonnull ObjectPropertyExpression objectProperty, @Nonnull ClassExpression range, Set<Annotation> annotations) 
	{
		super(annotations);
		this.objectProperty = objectProperty;
		this.range = range;
	}

	
	@Override
	public ObjectPropertyRangeAxiom getAxiomWithoutAnnotations() 
	{
		if(!isAnnotated())
			return this;
		
		return new SimpleObjectPropertyRangeAxiom(objectProperty, range, NO_ANNOTATIONS);	
	}

	
	@Override
	public ObjectPropertyRangeAxiom getAnnotatedAxiom(@Nonnull Set<Annotation> annotations) 
	{
		return new SimpleObjectPropertyRangeAxiom(objectProperty, range, mergeAnnos(annotations));
	}


	@Override
	public AxiomType getAxiomType()
	{
		return AxiomType.OBJECT_PROPERTY_RANGE;
	}

	
	@Override
	public ObjectPropertyExpression getObjectProperty() 
	{
		return objectProperty;
	}

	
	@Override
	public ClassExpression getRange() 
	{
		return range;
	}
	
	
	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) 
		    return true;
		
		if (!super.equals(obj)) 
			return false;
		
		if (obj instanceof ObjectPropertyRangeAxiom) {
			ObjectPropertyRangeAxiom other = (ObjectPropertyRangeAxiom)obj;			 
			return ((objectProperty.equals(other.getObjectProperty())) && (range.equals(other.getRange())));
		}
		
		return false;
	}


}
