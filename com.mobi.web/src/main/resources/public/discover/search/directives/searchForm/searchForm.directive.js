(function() {
    'use strict';

    angular
        /**
         * @ngdoc overview
         * @name searchForm
         *
         * @description
         * The `searchForm` module only provides the `searchForm` directive which creates
         * the search form within the Search page.
         */
        .module('searchForm', [])
        /**
         * @ngdoc directive
         * @name searchForm.directive:searchForm
         * @scope
         * @restrict E
         * @requires search.service:searchService
         * @requires discoverState.service:discoverStateService
         * @requires explore.service:exploreService
         * @requires util.service:utilService
         * @requires modal.service:modalService
         *
         * @description
         * HTML contents in the search form within the Search page for entering a keyword search combined
         * using the AND operator or the OR operator.
         */
        .directive('searchForm', searchForm);

        searchForm.$inject = ['searchService', 'discoverStateService', 'exploreService', 'utilService', 'modalService'];

        function searchForm(searchService, discoverStateService, exploreService, utilService, modalService) {
            return {
                restrict: 'E',
                templateUrl: 'discover/search/directives/searchForm/searchForm.directive.html',
                replace: true,
                scope: {},
                controllerAs: 'dvm',
                controller: function() {
                    var dvm = this;
                    var s = searchService;
                    var es = exploreService;
                    dvm.ds = discoverStateService;
                    dvm.util = utilService;
                    dvm.typeSearch = '';
                    dvm.errorMessage = '';

                    dvm.createPropertyFilter = function() {
                        modalService.openModal('propertyFilterOverlay');
                    }
                    dvm.submit = function() {
                        s.submitSearch(dvm.ds.search.datasetRecordId, dvm.ds.search.queryConfig)
                            .then(data => {
                                dvm.ds.search.results = data;
                                dvm.errorMessage = '';
                            }, errorMessage => {
                                dvm.ds.search.results = undefined;
                                dvm.errorMessage = errorMessage;
                            });
                    }

                    dvm.getTypes = function() {
                        dvm.ds.resetSearchQueryConfig();
                        dvm.ds.search.properties = undefined;
                        es.getClassDetails(dvm.ds.search.datasetRecordId)
                            .then(details => {
                                dvm.ds.search.typeObject = _.groupBy(details, 'ontologyRecordTitle');
                                dvm.errorMessage = '';
                            }, errorMessage => {
                                dvm.ds.search.typeObject = {};
                                dvm.errorMessage = errorMessage;
                            });
                    }

                    dvm.getSelectedText = function() {
                        return _.join(_.map(dvm.ds.search.queryConfig.types, 'classTitle'), ', ');
                    }

                    dvm.removeFilter = function(index) {
                        _.pullAt(dvm.ds.search.queryConfig.filters, index);
                    }

                    dvm.isSubmittable = function() {
                         return dvm.ds.search.datasetRecordId && (dvm.ds.search.queryConfig.keywords.length || dvm.ds.search.queryConfig.types.length || dvm.ds.search.queryConfig.filters.length);
                    }

                    dvm.getLast = function(path) {
                        return _.last(path);
                    }

                    dvm.refresh = function() {
                        if (dvm.ds.search.datasetRecordId) {
                            dvm.getTypes();
                        }
                    }
                }
            }
        }
})();
