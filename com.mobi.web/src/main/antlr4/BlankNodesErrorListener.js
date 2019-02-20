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
var ErrorListener = require('antlr4/error/ErrorListener').ErrorListener;

function BlankNodesErrorListener(resultObj) {
    ErrorListener.call(this);
    this.resultObj = resultObj;
    return this;
}

BlankNodesErrorListener.prototype = Object.create(ErrorListener.prototype);
BlankNodesErrorListener.prototype.constructor = BlankNodesErrorListener;

BlankNodesErrorListener.prototype.syntaxError = function(recognizer, offendingSymbol, line, column, msg, e) {
    this.resultObj.errorMessage = msg.charAt(0).toUpperCase() + msg.slice(1);
    this.resultObj.jsonld = undefined;
};

exports.BlankNodesErrorListener = BlankNodesErrorListener;