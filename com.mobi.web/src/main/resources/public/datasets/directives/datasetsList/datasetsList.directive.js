(function() {
    'use strict';

    angular
        /**
         * @ngdoc overview
         * @name datasetsList
         *
         * @description
         * The `datasetsList` module only provides the `datasetsList` directive which creates a Bootstrap
         * row containing a block for displaying the paginated list of Dataset Records.
         */
        .module('datasetsList', [])
        /**
         * @ngdoc directive
         * @name datasetsList.directive:datasetsList
         * @scope
         * @restrict E
         * @requires datasetState.service:datasetStateService
         * @requires datasetManager.service:datasetManagerService
         * @requires catalogManager.service:catalogManagerService
         * @requires util.service:utilService
         * @requires prefixes.service:prefixes
         * @requires modal.service:modalService
         *
         * @description
         * `datasetsList` is a directive which creates a Bootstrap row containing a {@link block.directive:block block}
         * with a {@link pagination.directive:pagination paginated} list of
         * {@link datasetState.service:datasetStateService#results Dataset Records} and
         * {@link confirmationOverlay.directive:confirmationOverlay confirmation overlays} for deleting and clearing
         * datasets. Each dataset only displays its title, dataset IRI, and a portion of its description until it is
         * opened. Only one dataset can be open at a time. The directive is replaced by the contents of its template.
         */
        .directive('datasetsList', datasetsList);

        datasetsList.$inject = ['$q', 'datasetManagerService', 'datasetStateService', 'catalogManagerService', 'utilService', 'prefixes', 'modalService'];

        function datasetsList($q, datasetManagerService, datasetStateService, catalogManagerService, utilService, prefixes, modalService) {
            return {
                restrict: 'E',
                replace: true,
                templateUrl: 'datasets/directives/datasetsList/datasetsList.directive.html',
                scope: {},
                controllerAs: 'dvm',
                controller: function() {
                    var dvm = this;
                    var dm = datasetManagerService;
                    var cm = catalogManagerService;
                    var cachedOntologyRecords = [];
                    var catalogId = _.get(cm.localCatalog, '@id', '');
                    dvm.state = datasetStateService;
                    dvm.util = utilService;
                    dvm.prefixes = prefixes;
                    dvm.cachedOntologyIds = [];
                    dvm.currentPage = dvm.state.paginationConfig.pageIndex + 1;

                    dvm.getIdentifiedOntologyIds = function(dataset) {
                        return _.map(dataset.identifiers, identifier => identifier[prefixes.dataset + 'linksToRecord'][0]['@id']);
                    }
                    dvm.getOntologyTitle = function(id) {
                        return dvm.util.getDctermsValue(_.find(cachedOntologyRecords, {'@id': id}), 'title');
                    }
                    dvm.clickDataset = function(dataset) {
                        if (dvm.state.openedDatasetId === dataset.record['@id']) {
                            dvm.state.selectedDataset = undefined;
                            dvm.state.openedDatasetId = '';
                        } else {
                            dvm.state.selectedDataset = dataset;
                            dvm.state.openedDatasetId = dataset.record['@id'];
                            var toRetrieve = _.filter(dvm.getIdentifiedOntologyIds(dataset), id => !_.includes(dvm.cachedOntologyIds, id));
                            $q.all(_.map(toRetrieve, id => cm.getRecord(id, catalogId)))
                                .then(responses => {
                                    dvm.cachedOntologyIds = _.concat(dvm.cachedOntologyIds, _.map(responses, '@id'));
                                    cachedOntologyRecords = _.concat(cachedOntologyRecords, responses);
                                }, () => dvm.errorMessage = 'Unable to load all Dataset details.');
                        }
                    }
                    dvm.getPage = function(direction) {
                        dvm.state.paginationConfig.pageIndex = dvm.currentPage - 1;
                        dvm.state.setResults();
                    }
                    dvm.delete = function(dataset) {
                        dm.deleteDatasetRecord(dataset.record['@id'])
                            .then(() => {
                                dvm.util.createSuccessToast('Dataset successfully deleted');
                                if (dvm.state.results.length === 1 && dvm.state.paginationConfig.pageIndex > 0) {
                                    dvm.state.paginationConfig.pageIndex -= 1;
                                }
                                dvm.state.setResults();
                                dvm.state.submittedSearch = !!dvm.state.paginationConfig.searchText;
                            }, dvm.util.createErrorToast);
                    }
                    dvm.clear = function(dataset) {
                        dm.clearDatasetRecord(dataset.record['@id'])
                            .then(() => {
                                dvm.util.createSuccessToast('Dataset successfully cleared');
                            }, dvm.util.createErrorToast);
                    }
                    dvm.showUploadData = function(dataset) {
                        dvm.state.selectedDataset = dataset;
                        modalService.openModal('uploadDataOverlay');
                    }
                    dvm.showEdit = function(dataset) {
                        dvm.state.selectedDataset = dataset;
                        modalService.openModal('editDatasetOverlay');
                    }
                    dvm.showClear = function(dataset) {
                        modalService.openConfirmModal('<p>Are you sure that you want to clear <strong>' + dvm.util.getDctermsValue(dataset.record, 'title') + '</strong>?</p>', () => dvm.clear(dataset));
                    }
                    dvm.showDelete = function(dataset) {
                        modalService.openConfirmModal('<p>Are you sure that you want to delete <strong>' + dvm.util.getDctermsValue(dataset.record, 'title') + '</strong>?</p>', () => dvm.delete(dataset));
                    }
                }
            }
        }
})();
