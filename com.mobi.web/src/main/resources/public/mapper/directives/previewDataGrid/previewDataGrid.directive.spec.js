describe('Preview Data Grid directive', function() {
    var $compile, scope, delimitedManagerSvc, mapperStateSvc;

    beforeEach(function() {
        module('templates');
        module('previewDataGrid');
        mockDelimitedManager();
        mockMapperState();

        module(function($provide) {
            $provide.service('hotRegisterer', function() {
                this.getInstance = jasmine.createSpy('getInstance');
            });
        });

        inject(function(_$compile_, _$rootScope_, _delimitedManagerService_, _mapperStateService_) {
            $compile = _$compile_;
            scope = _$rootScope_;
            mapperStateSvc = _mapperStateService_;
            delimitedManagerSvc = _delimitedManagerService_;
        });

        this.hotTable = {
            selectCell: jasmine.createSpy('selectCell'),
            countRows: jasmine.createSpy('countRows').and.returnValue(0),
            render: jasmine.createSpy('render'),
            deselectCell: jasmine.createSpy('deselectCell')
        };
        this.element = $compile(angular.element('<preview-data-grid></preview-data-grid>'))(scope);
        scope.$digest();
        this.controller = this.element.controller('previewDataGrid');
    });

    afterEach(function() {
        $compile = null;
        scope = null;
        delimitedManagerSvc = null;
        mapperStateSvc = null;
        this.element.remove();
    });

    describe('should update when', function() {
        beforeEach(function() {
            this.controller.hotTable = this.hotTable;
        });
        it('the highlight indexes change', function() {
            mapperStateSvc.highlightIndexes = ['0'];
            scope.$digest();
            expect(this.controller.hotTable.render).toHaveBeenCalled();
        });
        it('whether the data has headers or not changes', function() {
            delimitedManagerSvc.dataRows = [];
            delimitedManagerSvc.containsHeaders = false;
            scope.$digest();
            expect(this.controller.hotTable.render).toHaveBeenCalled();
        });
    });
    describe('replaces the element with the correct html', function() {
        it('for wrapping containers', function() {
            expect(this.element.hasClass('preview-data-grid')).toBe(true);
        });
        it('with a hot table', function() {
            expect(this.element.find('hot-table').length).toBe(1);
        });
    });
});