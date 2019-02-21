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
(function() {
    'use strict';

    checkbox.$inject = ['$timeout'];

    function checkbox($timeout) {
        return {
            restrict: 'E',
            replace: true,
            scope: {},
            bindToController: {
                bindModel: '=ngModel',
                changeEvent: '&',
                displayText: '<',
                inline: '<?',
                isDisabled: '<'
            },
            controllerAs: 'dvm',
            controller: function() {
                var dvm = this;

                dvm.onChange = function() {
                    $timeout(function() {
                        dvm.changeEvent();
                    });
                }
            },
            templateUrl: 'shared/directives/checkbox/checkbox.directive.html'
        }
    }

    angular
        .module('shared')
        /**
         * @ngdoc directive
         * @name shared.directive:checkbox
         * @scope
         * @restrict E
         * @requires $timeout
         *
         * @description 
         * `checkbox` is a directive that creates a checkbox styled using the Bootstrap "checkbox"
         * class, a custom on change function, a custom disabled condition, and custom label text.
         * The true and false values of the checkbox will always be the boolean true and false values.
         * The directive is replaced by the content of the template.
         *
         * @param {*} bindModel the variable to bind the value of the checkbox to
         * @param {function=undefined} changeEvent a function to be called when the value of the checkbox 
         * changes
         * @param {string=''} displayText label text to display for the checkbox
         * @param {boolean=false} isDisabledWhen when the checkbox should be disabled
         */
        .directive('checkbox', checkbox);
})();