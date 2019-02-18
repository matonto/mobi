
describe('Record Card component', function() {
    var $compile, scope, utilSvc;

    beforeEach(function() {
        module('templates');
        module('catalog');
        mockComponent('catalog', 'entityPublisher');
        mockComponent('catalog', 'recordIcon');
        mockComponent('catalog', 'recordType');
        mockComponent('catalog', 'catalogRecordKeywords');
        mockUtil();

        inject(function(_$compile_, _$rootScope_, _utilService_) {
            $compile = _$compile_;
            scope = _$rootScope_;
            utilSvc = _utilService_;
        });

        utilSvc.getDctermsValue.and.callFake((obj, prop) => prop);
        utilSvc.getDate.and.returnValue('date');

        scope.record = {};
        scope.clickCard = jasmine.createSpy('clickCard');
        this.element = $compile(angular.element('<record-card record="record" click-card="clickCard()"></record-card>'))(scope);
        scope.$digest();
        this.controller = this.element.controller('recordCard');
    });

    afterEach(function() {
        $compile = null;
        scope = null;
        utilSvc = null;
        this.element.remove();
    });

    describe('controller bound variable', function() {
        it('record should be one way bound', function() {
            this.controller.record = {a: 'b'};
            scope.$digest();
            expect(scope.record).toEqual({});
        });
        it('clickCard is called in the parent scope', function() {
            this.controller.clickCard();
            expect(scope.clickCard).toHaveBeenCalled();
        });
    });
    describe('should initialize', function() {
        it('with a title, description, and modified date', function() {
            expect(this.controller.title).toEqual('title');
            expect(this.controller.description).toEqual('description');
            expect(this.controller.modified).toEqual('date');
        });
    });
    describe('contains the correct html', function() {
        it('for wrapping containers', function() {
            expect(this.element.prop('tagName')).toEqual('RECORD-CARD');
            expect(this.element.querySelectorAll('.record-card.card').length).toEqual(1);
            expect(this.element.querySelectorAll('.card-body').length).toEqual(1);
            expect(this.element.querySelectorAll('.card-footer').length).toEqual(1);
        });
        ['record-icon', 'record-type', 'entity-publisher', 'catalog-record-keywords'].forEach(test => {
            it('with a ' + test, function() {
                expect(this.element.find(test).length).toBe(1);
            });
        });
    });
    it('should call clickCard when the card is clicked', function() {
        var card = angular.element(this.element.querySelectorAll('.card')[0]);
        card.triggerHandler('click');
        expect(scope.clickCard).toHaveBeenCalled();
    });
});