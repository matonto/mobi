(function() {
    'use strict';

    angular
        /**
         * @ngdoc overview
         * @name mergeBlock
         *
         * @description
         * The `mergeBlock` module only provides the `mergeBlock` directive which creates a form and display for
         * merging two branches of an ontology together.
         */
        .module('mergeBlock', [])
        /**
         * @ngdoc directive
         * @name mergeBlock.directive:mergeBlock
         * @scope
         * @restrict E
         * @requires util.service:utilService
         * @requires ontologyState.service:ontologyStateService
         * @requires catalogManager.service:catalogManagerService
         *
         * @description
         * `mergeBlock` is a directive that creates a form for merging the current branch of the opened
         * {@link ontologyState.service:ontologyStateService ontology} into another branch. The form contains a
         * {@link branchSelect.directive:branchSelect} for the target branch, a {@link checkbox.directive:checkbox}
         * for indicating whether the source branch should be removed after the merge, a button to submit the merge,
         * and a button to cancel the merge. Once a target is selected, a
         * {@link commitDifferenceTabset.directive:commitDifferenceTabset} is displayed. The form calls the appropriate
         * methods to check for conflicts before performing the merge. The directive is replaced by the contents of its
         * template.
         */
        .directive('mergeBlock', mergeBlock);

        mergeBlock.$inject = ['utilService', 'ontologyStateService', 'catalogManagerService', '$q'];

        function mergeBlock(utilService, ontologyStateService, catalogManagerService, $q) {
            return {
                restrict: 'E',
                replace: true,
                templateUrl: 'ontology-editor/directives/mergeBlock/mergeBlock.directive.html',
                scope: {},
                controllerAs: 'dvm',
                controller: function() {
                    var dvm = this;
                    var cm = catalogManagerService;
                    dvm.os = ontologyStateService;
                    dvm.util = utilService;

                    var catalogId = _.get(cm.localCatalog, '@id', '');
                    dvm.error = '';
                    dvm.branches = _.reject(dvm.os.listItem.branches, {'@id': dvm.os.listItem.ontologyRecord.branchId});
                    var branch = _.find(dvm.os.listItem.branches, {'@id': dvm.os.listItem.ontologyRecord.branchId});
                    dvm.branchTitle = dvm.util.getDctermsValue(branch, 'title');
                    dvm.targetHeadCommitId = undefined;

                    dvm.changeTarget = function() {
                        if (dvm.os.listItem.merge.target) {
                            cm.getBranchHeadCommit(dvm.os.listItem.merge.target['@id'], dvm.os.listItem.ontologyRecord.recordId, catalogId)
                                .then(target => {
                                    dvm.targetHeadCommitId = target.commit['@id'];
                                    return cm.getDifference(dvm.os.listItem.ontologyRecord.commitId, dvm.targetHeadCommitId);
                                    }, $q.reject)
                                .then( diff => {
                                    dvm.os.listItem.merge.difference = diff;
                                }, errorMessage => {
                                    dvm.util.createErrorToast(errorMessage);
                                    dvm.os.listItem.merge.difference = undefined;
                                });
                        } else {
                            dvm.os.listItem.merge.difference = undefined;
                        }
                    }
                    dvm.submit = function() {
                        dvm.os.attemptMerge()
                            .then(() => {
                                dvm.os.resetStateTabs();
                                dvm.util.createSuccessToast('Your merge was successful.');
                                dvm.os.cancelMerge();
                            }, error => dvm.error = error);
                    }

                    dvm.changeTarget();
                }
            }
        }
})();
