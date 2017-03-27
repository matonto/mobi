/*-
 * #%L
 * org.matonto.web
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

describe('Tree Item directive', function() {
    var $compile,
        scope,
        element,
        controller,
        isolatedScope,
        ontologyStateSvc,
        ontologyManagerSvc,
        settingsManagerSvc;

    beforeEach(function() {
        module('templates');
        module('treeItem');
        injectRegexConstant();
        mockSettingsManager();
        mockOntologyManager();
        mockOntologyState();
        mockPrefixes();

        inject(function(_$compile_, _$rootScope_, _ontologyStateService_, _ontologyManagerService_, _settingsManagerService_) {
            $compile = _$compile_;
            scope = _$rootScope_;
            ontologyStateSvc = _ontologyStateService_;
            ontologyManagerSvc = _ontologyManagerService_;
            settingsManagerSvc = _settingsManagerService_;
        });

        scope.hasChildren = true;
        scope.isActive = false;
        scope.onClick = jasmine.createSpy('onClick');
        scope.currentEntity = {};
        scope.isOpened = true;
        scope.isBold = false;
        scope.path = '';
        element = $compile(angular.element('<tree-item path="path" is-opened="isOpened" current-entity="currentEntity" is-active="isActive" on-click="onClick()" has-children="hasChildren" is-bold="isBold"></tree-item>'))(scope);
        scope.$digest();
    });

    describe('in isolated scope', function() {
        beforeEach(function() {
            isolatedScope = element.isolateScope();
        });
        it('hasChildren should be one way bound', function() {
            isolatedScope.hasChildren = false;
            scope.$digest();
            expect(scope.hasChildren).toBe(true);
        });
        it('isActive should be one way bound', function() {
            isolatedScope.isActive = true;
            scope.$digest();
            expect(scope.isActive).toBe(false);
        });
        it('isBold should be one way bound', function() {
            isolatedScope.isBold = true;
            scope.$digest();
            expect(scope.isBold).toBe(false);
        });
        it('onClick should be called in parent scope when invoked', function() {
            isolatedScope.onClick();
            expect(scope.onClick).toHaveBeenCalled();
        });
    });
    it('controller bound variable', function() {
        beforeEach(function() {
            controller = element.controller('treeItem');
        });
        it('currentEntity should be two way bound', function() {
            controller.currentEntity = {id: 'new'};
            scope.$digest();
            expect(controller.currentEntity).toEqual({id: 'new'});
        });
        it('isOpened should be two way bound', function() {
            controller.isOpened = false;
            scope.$digest();
            expect(controller.isOpened).toEqual(false);
        });
        it('path should be two way bound', function() {
            controller.path = 'new';
            scope.$digest();
            expect(controller.path).toEqual('new');
        });
    });
    describe('replaces the element with the correct html', function() {
        it('for wrapping containers', function() {
            expect(element.prop('tagName')).toBe('LI');
            expect(element.hasClass('tree-item')).toBe(true);
        });
        it('depending on whether or not the currentEntity is unsaved', function() {
            expect(element.find('a').hasClass('unsaved')).toBe(false);

            scope.currentEntity.matonto = {unsaved: true};
            scope.$digest();
            expect(element.find('a').hasClass('unsaved')).toBe(true);
        });
        it('depending on whether it has children', function() {
            var anchor = element.find('a');
            expect(anchor.length).toBe(1);
            expect(anchor.attr('ng-dblclick')).toBeTruthy();
            expect(element.find('i').length).toBe(1);

            scope.hasChildren = false;
            scope.$digest();
            var anchor = element.find('a');
            expect(anchor.length).toBe(1);
            expect(anchor.attr('ng-dblclick')).toBeFalsy();
            expect(element.find('i').length).toBe(1);
        });
        it('depending on whether it is active', function() {
            var anchor = element.find('a');
            expect(anchor.hasClass('active')).toBe(false);

            scope.isActive = true;
            scope.$digest();
            expect(anchor.hasClass('active')).toBe(true);
        });
        it('depending on whether it is bold', function() {
            var span = element.find('span');
            expect(span.hasClass('bold')).toBe(false);

            scope.isBold = true;
            scope.$digest();
            expect(span.hasClass('bold')).toBe(true);
        });
    });
    describe('controller methods', function() {
        beforeEach(function() {
            controller = element.controller('treeItem');
        });
        describe('getTreeDisplay', function() {
            it('should return originalIRI when not pretty', function() {
                scope.currentEntity = {matonto: {originalIRI: 'originalIRI', anonymous: 'anon'}};
                scope.$digest();
                var result = controller.getTreeDisplay();
                expect(result).toBe('originalIRI');
                expect(ontologyManagerSvc.getEntityName).not.toHaveBeenCalled();
            });
            it('should return anonymous when not pretty and no originalIRI', function() {
                scope.currentEntity = {matonto: {anonymous: 'anon'}};
                scope.$digest();
                var result = controller.getTreeDisplay();
                expect(result).toBe('anon');
                expect(ontologyManagerSvc.getEntityName).not.toHaveBeenCalled();
            });
            it('should call getEntityName if pretty', function() {
                settingsManagerSvc.getTreeDisplay.and.returnValue('pretty');
                element = $compile(angular.element('<tree-item path="path" is-opened="isOpened" current-entity="currentEntity" is-active="isActive" on-click="onClick()" has-children="hasChildren"></tree-item>'))(scope);
                scope.$digest();
                expect(ontologyManagerSvc.getEntityName).toHaveBeenCalledWith(controller.currentEntity, ontologyStateSvc.state.type);
            });
        });
        describe('toggleOpen', function() {
            it('should call correct manager function', function() {
                scope.currentEntity = {matonto: {originalIRI: 'originalIRI', anonymous: 'anon'}};
                scope.$digest();
                controller.toggleOpen();
                expect(ontologyStateSvc.setOpened).toHaveBeenCalledWith(controller.path, controller.isOpened);
            });
            it('should return true when not set', function() {
                controller.isOpened = undefined;
                controller.toggleOpen();
                expect(controller.isOpened).toBe(true);
            });
            it('should return true if it is false', function() {
                controller.isOpened = false;
                controller.toggleOpen();
                expect(controller.isOpened).toBe(true);
            });
            it('should return false if it is true', function() {
                controller.isOpened = true;
                controller.toggleOpen();
                expect(controller.isOpened).toBe(false);
            });
            it('should be called when double clicked', function() {
                spyOn(controller, 'toggleOpen');
                var anchor = element.querySelectorAll('a')[0];
                angular.element(anchor).triggerHandler('dblclick');
                expect(controller.toggleOpen).toHaveBeenCalled();
            });
        });
        it('isSaved returns whether or not the inProgressCommit additions or deletions contains the current entity @id', function() {
            expect(controller.isSaved()).toBe(false);
            controller.currentEntity = {'@id': 'id'};
            ontologyStateSvc.listItem.inProgressCommit = {
                additions: [{'@id': 'id'}]
            }
            expect(controller.isSaved()).toBe(true);
        });
    });
});
