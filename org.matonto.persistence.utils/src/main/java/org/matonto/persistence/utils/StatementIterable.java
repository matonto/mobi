package org.matonto.persistence.utils;

/*-
 * #%L
 * org.matonto.persistence.utils
 * $Id:$
 * $HeadURL:$
 * %%
 * Copyright (C) 2016 - 2017 iNovex Information Systems, Inc.
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

import org.matonto.persistence.utils.api.SesameTransformer;
import org.openrdf.model.Statement;

import java.util.Iterator;

public class StatementIterable implements Iterable<Statement>, Iterator<Statement> {
    private Iterator<org.matonto.rdf.api.Statement> it;
    private SesameTransformer transformer;

    public StatementIterable(Iterable<org.matonto.rdf.api.Statement> it, SesameTransformer transformer) {
        this.it = it.iterator();
        this.transformer = transformer;
    }

    @Override
    public Iterator<Statement> iterator() {
        return this;
    }

    @Override
    public boolean hasNext() {
        return it.hasNext();
    }

    @Override
    public Statement next() {
        return transformer.sesameStatement(it.next());
    }
}
