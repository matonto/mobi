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

import './actionMenu.component.scss';

const template = require('./actionMenu.component.html');

/**
 * @ngdoc component
 * @name shared.component:actionMenu
 *
 * @description
 * `actionMenu` is a component that creates a `uib-dropdown` div element that is meant to contain the contents of a 
 * Bootstrap {@link http://daemonite.github.io/material/docs/4.1/components/dropdowns/|dropdown menu} for performing
 * various actions. Typically, this component should be used in a `.list-group-item`.
 */
const actionMenuComponent = {
    template,
    transclude: true,
    bindings: {},
    controllerAs: 'dvm',
    controller: actionMenuComponentCtrl
};

function actionMenuComponentCtrl() {}

export default actionMenuComponent;
