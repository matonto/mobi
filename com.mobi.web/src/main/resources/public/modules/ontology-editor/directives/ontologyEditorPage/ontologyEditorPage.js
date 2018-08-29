/*-
 * #%L
 * com.mobi.web
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
(function() {
    'use strict';

    angular
        /**
         * @ngdoc overview
         * @name ontologyEditorPage
         *
         * @description
         * The `ontologyEditorPage` module provides the `ontologyEditorPage` directive which creates a `div`
         * with the main components of the Ontology Editor.
         */
        .module('ontologyEditorPage', [])
        /**
         * @ngdoc directive
         * @name ontologyEditorPage.directive:ontologyEditorPage
         * @scope
         * @restrict E
         * @requires ontologyManager.service:ontologyManagerService
         * @requires ontologyState.service:ontologyStateService
         *
         * @description
         * `ontologyEditorPage` is a directive that creates a `div` containing the main components of the Ontology
         * Editor. These components are {@link ontologySidebar.directive:ontologySidebar},
         * {@link ontologyTab.directive:ontologyTab} with the
         * {@link ontologyState.service:ontologyStateService currently selected open ontology}, and
         * {@link ontologyDefaultTab.directive:ontologyDefaultTab}. The directive is replaced by the contents of
         * its template.
         */
        .directive('ontologyEditorPage', ontologyEditorPage);

        ontologyEditorPage.$inject = ['ontologyStateService'];

        function ontologyEditorPage(ontologyStateService) {
            return {
                restrict: 'E',
                replace: true,
                templateUrl: 'modules/ontology-editor/directives/ontologyEditorPage/ontologyEditorPage.html',
                scope: {},
                controllerAs: 'dvm',
                controller: ['$scope', function($scope) {
                    var dvm = this;
                    dvm.os = ontologyStateService;

                    dvm.isDefaultTab = function() {
                        return _.isEmpty(dvm.os.listItem);
                    }
                }]
            }
        }
})();