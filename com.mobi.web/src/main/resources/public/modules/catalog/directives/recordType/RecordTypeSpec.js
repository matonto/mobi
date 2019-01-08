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
describe('Record Type component', function() {
    var $compile, scope, catalogManagerSvc;

    beforeEach(function() {
        module('templates');
        module('catalog');
        mockCatalogManager();
        injectSplitIRIFilter();
        injectChromaConstant();

        inject(function(_$compile_, _$rootScope_, _catalogManagerService_) {
            $compile = _$compile_;
            scope = _$rootScope_;
            catalogManagerSvc = _catalogManagerService_;
        });

        catalogManagerSvc.recordTypes = ['type'];
        scope.type = '';
        this.element = $compile(angular.element('<record-type type="type"></record-type>'))(scope);
        scope.$digest();
        this.controller = this.element.controller('recordType');
    });

    afterEach(function() {
        $compile = null;
        scope = null;
        catalogManagerSvc = null;
        this.element.remove();
    });

    describe('controller bound variable', function() {
        it('type should be one way bound', function() {
            this.controller.type = 'test';
            scope.$digest();
            expect(scope.type).toEqual('');
        });
    });
    describe('controller methods', function() {
        it('should get the color for a type', function() {
            var result = this.controller.getColor('type');
            expect(typeof result).toBe('string');
        });
    });
    describe('contains the correct html', function() {
        it('for wrapping containers', function() {
            expect(this.element.prop('tagName')).toBe('RECORD-TYPE');
            expect(this.element.querySelectorAll('.badge.badge-pill').length).toEqual(1);
        });
        it('with the correct background color depending on the record type', function() {
            var badge = angular.element(this.element.querySelectorAll('.badge')[0]);
            spyOn(this.controller, 'getColor').and.returnValue('white');
            scope.$digest();
            expect(badge.css('background-color')).toBe('white');
        });
    });
});