describe('Discover State Service', function() {
    var discoverStateSvc;

    beforeEach(function() {
        module('discoverState');

        inject(function(discoverStateService) {
            discoverStateSvc = discoverStateService;
        });
    });

    afterEach(function() {
        discoverStateSvc = null;
    });

    it('default variables should be set properly', function() {
        expectInitialState();
    });
    it('reset should reset all state variables', function() {
        discoverStateSvc.reset();
        expectInitialState();
    });
    it('resetPagedInstanceDetails should reset the proper variables', function() {
        discoverStateSvc.explore.instanceDetails = {
            currentPage: 1,
            data: [{prop: 'stuff'}],
            limit: 100,
            links: {
                next: 'next',
                prev: 'prev'
            },
            total: 1
        };
        discoverStateSvc.resetPagedInstanceDetails();
        expect(discoverStateSvc.explore.instanceDetails).toEqual({
            currentPage: 1,
            data: [],
            limit: 99,
            links: {
                next: '',
                prev: ''
            },
            total: 0
        });
    });
    describe('cleanUpOnDatasetDelete should reset the proper variables if the datasetIRI', function() {
        beforeEach(function() {
            spyOn(discoverStateSvc, 'resetPagedInstanceDetails');
            discoverStateSvc.explore = {
                breadcrumbs: ['Classes', 'instance'],
                classDetails: [{}],
                classId: 'classId',
                instance: {
                    changed: ['prop'],
                    entity: {'@id': 'instanceId'},
                    metadata: {prop: 'prop'}
                },
                recordId: 'recordId'
            };
        });
        it('matches the recordId', function() {
            discoverStateSvc.cleanUpOnDatasetDelete('recordId');
            expect(discoverStateSvc.explore.breadcrumbs).toEqual(['Classes']);
            expect(discoverStateSvc.explore.classDetails).toEqual([]);
            expect(discoverStateSvc.explore.classId).toEqual('');
            expect(discoverStateSvc.explore.instance).toEqual({changed: [], entity: {}, metadata: {}});
            expect(discoverStateSvc.explore.recordId).toEqual('');
            expect(discoverStateSvc.resetPagedInstanceDetails).toHaveBeenCalled();
        });
        it('does not match the recordId', function() {
            discoverStateSvc.cleanUpOnDatasetDelete('other');
            expect(discoverStateSvc.explore.breadcrumbs).toEqual(['Classes', 'instance']);
            expect(discoverStateSvc.explore.classDetails).toEqual([{}]);
            expect(discoverStateSvc.explore.classId).toEqual('classId');
            expect(discoverStateSvc.explore.instance).toEqual({changed: ['prop'], entity: {'@id': 'instanceId'}, metadata: {prop: 'prop'}});
            expect(discoverStateSvc.explore.recordId).toEqual('recordId');
            expect(discoverStateSvc.resetPagedInstanceDetails).not.toHaveBeenCalled();
        });
    });
    describe('cleanUpOnDatasetClear should reset the proper variables if the datasetIRI', function() {
        beforeEach(function() {
            spyOn(discoverStateSvc, 'resetPagedInstanceDetails');
            discoverStateSvc.explore = {
                breadcrumbs: ['Classes', 'instance'],
                classDetails: [{}],
                classId: 'classId',
                instance: {
                    changed: ['prop'],
                    entity: {'@id': 'instanceId'},
                    metadata: {prop: 'prop'}
                },
                recordId: 'recordId'
            };
        });
        it('matches the recordId', function() {
            discoverStateSvc.cleanUpOnDatasetClear('recordId');
            expect(discoverStateSvc.explore.breadcrumbs).toEqual(['Classes']);
            expect(discoverStateSvc.explore.classDetails).toEqual([]);
            expect(discoverStateSvc.explore.classId).toEqual('');
            expect(discoverStateSvc.explore.instance).toEqual({changed: [], entity: {}, metadata: {}});
            expect(discoverStateSvc.resetPagedInstanceDetails).toHaveBeenCalled();
        });
        it('does not match the recordId', function() {
            discoverStateSvc.cleanUpOnDatasetClear('other');
            expect(discoverStateSvc.explore.breadcrumbs).toEqual(['Classes', 'instance']);
            expect(discoverStateSvc.explore.classDetails).toEqual([{}]);
            expect(discoverStateSvc.explore.classId).toEqual('classId');
            expect(discoverStateSvc.explore.instance).toEqual({changed: ['prop'], entity: {'@id': 'instanceId'}, metadata: {prop: 'prop'}});
            expect(discoverStateSvc.resetPagedInstanceDetails).not.toHaveBeenCalled();
        });
    });
    it('clickCrumb should navigate to the selected crumb', function() {
        discoverStateSvc.explore.breadcrumbs = ['', ''];
        discoverStateSvc.explore.editing = true;
        discoverStateSvc.explore.creating = true;
        discoverStateSvc.clickCrumb(0);
        expect(discoverStateSvc.explore.breadcrumbs.length).toBe(1);
        expect(discoverStateSvc.explore.editing).toBe(false);
        expect(discoverStateSvc.explore.creating).toBe(false);
    });
    it('getInstance should return the correct object in the entity', function() {
        discoverStateSvc.explore.classId = 'https://mobi.com#classId';
        discoverStateSvc.explore.instance.entity = [{
            '@type': ['https://mobi.com#classId']
        }, {
            '@id': '_:x1'
        }];
        expect(discoverStateSvc.getInstance()).toEqual({'@type': ['https://mobi.com#classId']});
    });
    it('resetSearchQueryConfig should reset the query config variables', function() {
        discoverStateSvc.search.queryConfig = {
            isOrKeywords: true,
            isOrTypes: true,
            keywords: ['keyword'],
            types: ['type'],
            filters: [{
                prop: 'filter'
            }],
            variables: {
                var0: 'var0'
            }
        };
        discoverStateSvc.resetSearchQueryConfig();
        expect(discoverStateSvc.search.queryConfig).toEqual({
            isOrKeywords: false,
            isOrTypes: false,
            keywords: [],
            types: [],
            filters: [],
            variables: {
                var0: 'var0'
            }
        });
    });

    function expectInitialState() {
        expect(discoverStateSvc.explore).toEqual({
            active: true,
            breadcrumbs: ['Classes'],
            classDeprecated: false,
            classDetails: [],
            classId: '',
            creating: false,
            editing: false,
            instance: {
                changed: [],
                entity: [{}],
                metadata: {},
                objectMap: {},
                original: []
            },
            instanceDetails: {
                currentPage: 1,
                data: [],
                limit: 99,
                links: {
                    next: '',
                    prev: ''
                },
                total: 0
            },
            recordId: ''
        });
        expect(discoverStateSvc.search).toEqual({
            active: false,
            datasetRecordId: '',
            noDomains: undefined,
            properties: undefined,
            queryConfig: {
                isOrKeywords: false,
                isOrTypes: false,
                keywords: [],
                types: [],
                filters: [],
                variables: {}
            },
            results: undefined,
            targetedId: 'discover-search-results',
            typeObject: undefined
        });
        expect(discoverStateSvc.query).toEqual({active: false});
    }
});
