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
describe('See History component', function() {
    var $compile, scope, $filter, catalogManagerSvc, manchesterConverterSvc, ontologyManagerSvc, ontologyStateSvc, ontologyUtilsManagerSvc, utilSvc;

    beforeEach(function() {
        module('templates');
        module('ontology-editor');
        mockComponent('staticIri', 'staticIri');
        injectTrustedFilter();
        injectHighlightFilter();
        injectPrefixationFilter();
        mockCatalogManager();
        mockManchesterConverter();
        mockOntologyManager();
        mockOntologyState();
        mockOntologyUtilsManager();
        mockUtil();

        inject(function(_$compile_, _$rootScope_, _$filter_, _catalogManagerService_, _manchesterConverterService_, _ontologyManagerService_, _ontologyStateService_, _ontologyUtilsManagerService_, _utilService_) {
            $compile = _$compile_;
            scope = _$rootScope_;
            $filter = _$filter_;
            catalogManagerSvc = _catalogManagerService_;
            manchesterConverterSvc = _manchesterConverterService_;
            ontologyManagerSvc = _ontologyManagerService_;
            ontologyStateSvc = _ontologyStateService_;
            ontologyUtilsManagerSvc = _ontologyUtilsManagerService_;
            utilSvc = _utilService_;
        });

        this.commits = ['commits1', 'commits2'];
        scope.dismiss = false;
        this.element = $compile(angular.element('<see-history></see-history>'))(scope);
        scope.$digest();
        this.controller = this.element.controller('seeHistory');
        this.isolatedScope = this.element.isolateScope();
    });

    afterEach(function() {
        $compile = null;
        scope = null;
        $filter = null;
        catalogManagerSvc = null;
        manchesterConverterSvc = null;
        ontologyManagerSvc = null;
        ontologyStateSvc = null;
        ontologyUtilsManagerSvc = null;
        utilSvc = null;
        this.element.remove();
    });

    describe('should initialize', function() {
        it('if the ontology state seeHistory is set to true', function() {
            ontologyStateSvc.listItem.seeHistory = true;
            scope.$digest();
            expect(scope.dismiss).toEqual(false);
        });
    });
    describe('controller methods', function() {
        describe('getTypes functions properly', function() {
            it('when @type is empty', function() {
                ontologyStateSvc.listItem.selected = {};
                expect(this.controller.getTypes()).toEqual('');
            });
            it('when @type has items', function() {
                var expected = 'test, test2';
                ontologyStateSvc.listItem.selected = {'@type': ['test', 'test2']};
                expect(this.controller.getTypes()).toEqual(expected);
            });
            it('when @type has blank node items', function() {
                ontologyManagerSvc.isBlankNodeId.and.returnValue(true);
                ontologyStateSvc.listItem.selected = {'@type': ['test', 'test2']};
                this.controller.getTypes();
                expect(manchesterConverterSvc.jsonldToManchester).toHaveBeenCalledWith(jasmine.any(String), ontologyStateSvc.listItem.ontology);
            });
        });
        it('should go to prev', function() {
            this.controller.commits = [this.commits];
            ontologyStateSvc.listItem.selectedEntity = this.controller.commits[1];
            scope.$digest();
            this.controller.prev();
            expect(ontologyStateSvc.listItem.selectedEntity).toEqual(this.controller.commits[0]);
        });
        it('should go to next', function() {
            this.controller.commits = [this.commits];
            ontologyStateSvc.listItem.selectedEntity = this.controller.commits[0];
            scope.$digest();
            this.controller.next();
            expect(ontologyStateSvc.listItem.selectedEntity).toEqual(this.controller.commits[1]);
        });
        it('should go back', function() {
            this.controller.goBack();
            expect(ontologyStateSvc.listItem.seeHistory).toBeUndefined();
            expect(ontologyStateSvc.listItem.selectedEntity).toBeUndefined();
        });
    });
    describe('contains the correct html', function() {
        it('for wrapping containers', function() {
            expect(this.element.prop('tagName')).toEqual('SEE-HISTORY');
        });
        it('for classes', function() {
            expect(this.element.querySelectorAll('.see-history-header').length).toBe(1);
            expect(this.element.querySelectorAll('.see-history-title').length).toBe(1);
            expect(this.element.querySelectorAll('.form-group').length).toBe(2);
        });
        it('components used', function() {
            expect(this.element.find('commit-compiled-resource').length).toBe(1);
            expect(this.element.find('commit-history-table').length).toBe(1);
        });
    });
});
