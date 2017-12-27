package com.mobi.rdf.orm.test;

/*-
 * #%L
 * rdf-orm-test
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.mobi.rdf.api.ModelFactory;
import com.mobi.rdf.api.ValueFactory;
import com.mobi.rdf.orm.AbstractOrmFactory;
import com.mobi.rdf.orm.Thing;
import com.mobi.rdf.orm.conversion.ValueConverterRegistry;
import com.mobi.rdf.orm.impl.ThingFactory;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.BlockJUnit4ClassRunner;

import java.lang.reflect.Field;

@RunWith(BlockJUnit4ClassRunner.class)
public class OrmEnabledTestCaseTest extends OrmEnabledTestCase {

    @Test
    public void test() throws Exception {
        assertFalse(OrmEnabledTestCaseTest.valueConverters.isEmpty());
        assertFalse(OrmEnabledTestCaseTest.ormFactories.isEmpty());
        assertTrue(OFR.getFactoryOfType(Thing.class)
                .orElseThrow(() -> new RuntimeException("Thing factory not registered")) instanceof ThingFactory);
        assertTrue(ormFactories.get(0).getType().equals(Thing.class));

        ThingFactory test = ThingFactory.class.cast(OFR.getFactoryOfType(Thing.class).orElse(null));
        ValueFactory valueFactory = getReference(AbstractOrmFactory.class.getDeclaredField("valueFactory"),
                test, ValueFactory.class);
        ModelFactory modelFactory = getReference(AbstractOrmFactory.class.getDeclaredField("modelFactory"),
                test, ModelFactory.class);
        ValueConverterRegistry valueConverterRegistry = getReference(AbstractOrmFactory.class
                .getDeclaredField("valueConverterRegistry"), test, ValueConverterRegistry.class);
        assertNotNull(valueFactory);
        assertEquals(VF, valueFactory);
        assertNotNull(modelFactory);
        assertEquals(MF, modelFactory);
        assertNotNull(valueConverterRegistry);
        assertEquals(OrmEnabledTestCase.valueConverterRegistry, valueConverterRegistry);
        assertEquals(14, OrmEnabledTestCaseTest.valueConverters.size());
        assertEquals(1, OrmEnabledTestCaseTest.ormFactories.size());
    }

    @SuppressWarnings("unchecked")
    private <T> T getReference(Field f, AbstractOrmFactory factory, Class<T> type) throws Exception {
        f.setAccessible(true);
        return (T) f.get(factory);
    }

}
