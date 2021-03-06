/*-
 * #%L
 * com.mobi.web
 * $Id:$
 * $HeadURL:$
 * %%
 * Copyright (C) 2016 - 2019 iNovex Information Systems, Inc.
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
import { isArray, intersection } from 'lodash';

/**
 * @ngdoc filter
 * @name shared.filter:inArray
 * @kind function
 *
 * @description
 * Takes an array and removes any elements are not within the passed in array. If the passed
 * in array is not actually an array, returns an empty array.
 *
 * @param {*[]} list The array to remove elements from
 * @param {*[]} arrayFilter The array to intersect with the original list
 * @returns {*[]} Either an empty array if the passed in array is not actually an
 * array or the intersection of the two arrays.
 */
function inArray() {
    return function(list, arrayFilter) {
        return isArray(list) && isArray(arrayFilter) ? intersection(list, arrayFilter) : [];
    }
}

export default inArray;