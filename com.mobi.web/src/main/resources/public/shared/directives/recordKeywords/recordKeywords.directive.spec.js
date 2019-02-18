describe('Record Keywords directive', function() {
    var $compile, scope, prefixes;

    beforeEach(function() {
        module('templates');
        module('recordKeywords');
        mockPrefixes();

        inject(function(_$compile_, _$rootScope_, _prefixes_) {
            $compile = _$compile_;
            scope = _$rootScope_;
            prefixes = _prefixes_;
        });

        scope.record = {};
        scope.record[prefixes.catalog + 'keyword'] = [{'@value': 'b'}, {'@value': 'a'}];
        this.element = $compile(angular.element('<record-keywords record="record"></record-keywords>'))(scope);
        scope.$digest();
        this.isolatedScope = this.element.isolateScope();
        this.controller = this.element.controller('recordKeywords');
    });

    afterEach(function() {
        $compile = null;
        scope = null;
        prefixes = null;
        this.element.remove();
    });

    describe('in isolated scope', function() {
        it('record should be one way bound', function() {
            var previousRecord = angular.copy(scope.record);
            this.isolatedScope.record = {'@id': ''};
            scope.$digest();
            expect(scope.record).toEqual(previousRecord);
        });
    });
    describe('controller methods', function() {
        it('should return all the record keywords sorting alphabetically', function() {
            expect(this.controller.getKeywords(scope.record)).toEqual(['a', 'b']);
        });
    });
    describe('replaces the element with the correct html', function() {
        beforeEach(function() {
            spyOn(this.controller, 'getKeywords').and.returnValue(['a', 'b']);
        });
        it('for wrapping containers', function() {
            expect(this.element.hasClass('record-keywords')).toBe(true);
        });
        it('with a field-name span', function() {
            expect(this.element.querySelectorAll('span.field-name').length).toBe(1);
        });
        it('depending on how many keywords there are', function() {
            expect(this.element.querySelectorAll('.keywords li').length).toBe(2);

            this.controller.getKeywords.and.returnValue([]);
            scope.$digest();
            expect(this.element.querySelectorAll('.keywords li').length).toBe(1);
        });
        it('depending on whether a list item is last', function() {
            var listItems = this.element.querySelectorAll('.keywords li');
            expect(angular.element(listItems[0]).hasClass('last')).toBe(false);
            expect(angular.element(listItems[1]).hasClass('last')).toBe(true);
        });
    });
});