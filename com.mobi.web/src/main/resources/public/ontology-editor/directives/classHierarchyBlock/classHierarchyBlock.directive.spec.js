describe('Class Hierarchy Block directive', function() {
    var $compile, scope, ontologyStateSvc;

    beforeEach(function() {
        module('templates');
        module('classHierarchyBlock');
        mockOntologyState();

        inject(function(_$compile_, _$rootScope_, _ontologyStateService_) {
            $compile = _$compile_;
            scope = _$rootScope_;
            ontologyStateSvc = _ontologyStateService_;
        });

        this.element = $compile(angular.element('<class-hierarchy-block></class-hierarchy-block>'))(scope);
        scope.$digest();
        this.controller = this.element.controller('classHierarchyBlock');
    });

    afterEach(function() {
        $compile = null;
        scope = null;
        ontologyStateSvc = null;
        this.element.remove();
    });

    describe('replaces the element with the correct html', function() {
        it('for wrapping containers', function() {
            expect(this.element.prop('tagName')).toBe('DIV');
            expect(this.element.hasClass('class-hierarchy-block')).toBe(true);
        });
        it('depending on whether the tree is empty', function() {
            expect(this.element.find('info-message').length).toEqual(1);
            expect(this.element.find('hierarchy-tree').length).toBe(0);

            ontologyStateSvc.listItem.classes.flat = [{}];
            scope.$digest();
            expect(this.element.find('info-message').length).toEqual(0);
            expect(this.element.find('hierarchy-tree').length).toBe(1);
        });
    });
});