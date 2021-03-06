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
const template = require('./discoverTabset.component.html');

/**
 * @ngdoc component
 * @name discover.component:discoverTabset
 * @requires shared.service:discoverStateService
 *
 * @description
 * `discoverTabset` is a component that creates a {@link shared.component:materialTabset tabset} of the
 * {@link explore.component:exploreTab}, {@link query.component:queryTab}, and {@link search.component:searchTab}.
 */
const discoverTabsetComponent = {
    template,
    bindings: {},
    controllerAs: 'dvm',
    controller: discoverTabsetComponentCtrl
};

discoverTabsetComponentCtrl.$inject = ['discoverStateService'];
    
function discoverTabsetComponentCtrl(discoverStateService) {
    var dvm = this;
    dvm.ds = discoverStateService;
}

export default discoverTabsetComponent;