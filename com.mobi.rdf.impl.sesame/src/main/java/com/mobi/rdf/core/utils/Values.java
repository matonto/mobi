package com.mobi.rdf.core.utils;

/*-
 * #%L
 * com.mobi.rdf.impl.sesame
 * $Id:$
 * $HeadURL:$
 * %%
 * Copyright (C) 2016 iNovex Information Systems, Inc.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * #L%
 */

import com.mobi.rdf.api.BNode;
import com.mobi.rdf.api.IRI;
import com.mobi.rdf.api.Literal;
import com.mobi.rdf.api.Model;
import com.mobi.rdf.api.ModelFactory;
import com.mobi.rdf.api.Resource;
import com.mobi.rdf.api.Statement;
import com.mobi.rdf.api.Value;
import com.mobi.rdf.api.ValueFactory;
import com.mobi.rdf.core.impl.sesame.LinkedHashModelFactory;
import com.mobi.rdf.core.impl.sesame.SimpleValueFactory;

import java.util.Set;
import java.util.stream.Collectors;

public class Values {

    private static final org.openrdf.model.ValueFactory SESAME_VF = org.openrdf.model.impl.SimpleValueFactory.getInstance();
    private static final ValueFactory MOBI_VF = SimpleValueFactory.getInstance();
    private static final ModelFactory MOBI_MF = LinkedHashModelFactory.getInstance();

    private Values() {}

    public static org.openrdf.model.Statement sesameStatement(Statement statement) {
        if (statement.getContext().isPresent()) {
            return SESAME_VF.createStatement(sesameResource(statement.getSubject()), sesameIRI(statement.getPredicate()),
                    sesameValue(statement.getObject()), sesameResource(statement.getContext().get()));
        } else {
            return SESAME_VF.createStatement(sesameResource(statement.getSubject()), sesameIRI(statement.getPredicate()),
                    sesameValue(statement.getObject()));
        }
    }

    public static Statement mobiStatement(org.openrdf.model.Statement statement) {
        if (statement.getContext() != null) {
            return MOBI_VF.createStatement(mobiResource(statement.getSubject()), mobiIRI(statement.getPredicate()),
                    mobiValue(statement.getObject()), mobiResource(statement.getContext()));
        } else {
            return MOBI_VF.createStatement(mobiResource(statement.getSubject()), mobiIRI(statement.getPredicate()),
                    mobiValue(statement.getObject()));
        }
    }

    public static org.openrdf.model.Resource sesameResource(Resource resource) {
        if (resource == null) {
            return null;
        } else if (resource instanceof IRI) {
            return sesameIRI((IRI) resource);
        } else {
            return SESAME_VF.createBNode(((BNode) resource).getID());
        }
    }

    public static Resource mobiResource(org.openrdf.model.Resource resource) {
        if (resource == null) {
          return null;
        } else if (resource instanceof org.openrdf.model.IRI) {
            return mobiIRI((org.openrdf.model.IRI) resource);
        } else {
            return MOBI_VF.createBNode(((org.openrdf.model.BNode) resource).getID());
        }
    }

    public static org.openrdf.model.IRI sesameIRI(IRI iri) {
        if (iri == null) {
            return null;
        } else {
            return SESAME_VF.createIRI(iri.stringValue());
        }
    }

    public static IRI mobiIRI(org.openrdf.model.IRI iri) {
        if (iri == null) {
            return null;
        } else {
            return MOBI_VF.createIRI(iri.stringValue());
        }
    }

    public static org.openrdf.model.Value sesameValue(Value value) {
        if (value == null) {
            return null;
        } else if (value instanceof IRI) {
            return sesameIRI((IRI) value);
        } else if (value instanceof BNode) {
            return sesameResource((BNode) value);
        } else {
            // Else it's a Mobi Literal
            Literal literal = (Literal) value;
            if (literal.getLanguage().isPresent()) {
                return SESAME_VF.createLiteral(literal.stringValue(), literal.getLanguage().get());
            } else {
                org.openrdf.model.IRI datatype = SESAME_VF.createIRI(literal.getDatatype().stringValue());
                return SESAME_VF.createLiteral(literal.stringValue(), datatype);
            }
        }
    }

    public static Value mobiValue(org.openrdf.model.Value value) {
        if (value == null) {
            return null;
        } else if (value instanceof org.openrdf.model.IRI) {
            return mobiIRI((org.openrdf.model.IRI) value);
        } else if (value instanceof org.openrdf.model.BNode) {
            return mobiResource((org.openrdf.model.BNode) value);
        } else {
            // Else it's a Sesame Literal
            org.openrdf.model.Literal literal = (org.openrdf.model.Literal) value;
            if (literal.getLanguage().isPresent()) {
                return MOBI_VF.createLiteral(literal.stringValue(), literal.getLanguage().get());
            } else {
                IRI datatype = MOBI_VF.createIRI(literal.getDatatype().stringValue());
                return MOBI_VF.createLiteral(literal.stringValue(), datatype);
            }
        }
    }

    public static org.openrdf.model.Resource[] sesameResources(Resource... resources) {
        if (resources == null) {
            return null;
        } else {
            org.openrdf.model.Resource[] sesameContexts = new org.openrdf.model.Resource[resources.length];

            for (int i = 0; i < resources.length; i++) {
                sesameContexts[i] = Values.sesameResource(resources[i]);
            }

            return sesameContexts;
        }
    }

    public static Model mobiModel(org.openrdf.model.Model model) {
        Set<Statement> statements = model.stream()
                .map(Values::mobiStatement)
                .collect(Collectors.toSet());
        return MOBI_MF.createModel(statements);
    }

    public static org.openrdf.model.Model sesameModel(Model model) {
        Set<org.openrdf.model.Statement> statements = model.stream()
                .map(Values::sesameStatement)
                .collect(Collectors.toSet());
        org.openrdf.model.Model sesameModel = new org.openrdf.model.impl.LinkedHashModel();
        sesameModel.addAll(statements);
        return sesameModel;
    }
}