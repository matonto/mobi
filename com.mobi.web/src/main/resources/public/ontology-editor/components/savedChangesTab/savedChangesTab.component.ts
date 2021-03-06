/*-
 * #%L
 * com.mobi.web
 * $Id:$
 * $HeadURL:$
 * %%
 * Copyright (C) 2016 - 2019 iNovex Information Systems, Inc.
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
import { get, chunk, intersection, find, concat, forEach, map, filter, sortBy } from 'lodash';

import './savedChangesTab.component.scss';

const template = require('./savedChangesTab.component.html');

/**
 * @ngdoc component
 * @name ontology-editor.component:savedChangesTab
 * @requires shared.service:ontologyStateService
 * @requires shared.service:ontologyManagerService
 * @requires shared.service:utilService
 * @requires shared.service:catalogManagerService
 * @requires shared.service:prefixes
 *
 * @description
 * `savedChangesTab` is a component that creates a page that displays all the current users's saved changes
 * (aka inProgressCommit) of the current
 * {@link shared.service:ontologyStateService selected ontology and branch}. The changes are grouped by
 * subject. The display will include a button to remove all the saved changes if there are any. If there are
 * no changes, an {@link shared.component:infoMessage} is shown stating as such. If the current branch is
 * not up to date and there are changes, an {@link shared.component:errorDisplay} is shown. If there are
 * no changes and the current branch is not up to date, an `errorDisplay` is shown with a link to pull in the
 * latest changes. If there are no changes and the user is on a UserBranch then an `errorDisplay` is shown with
 * a lin to "pull changes" which will perform a merge of the UserBranch into the parent branch. If there are
 * no changes, the user is on a UserBranch, and the parent branch no longer exists, an `errorDisplay` is shown
 * with a link to restore the parent branch with the UserBranch. 
 */
const savedChangesTabComponent = {
    template,
    bindings: {
        additions: '<',
        deletions: '<'
    },
    controllerAs: 'dvm',
    controller: savedChangesTabComponentCtrl
};

savedChangesTabComponentCtrl.$inject = ['$q', 'ontologyStateService', 'ontologyManagerService', 'utilService', 'catalogManagerService', 'prefixes'];

function savedChangesTabComponentCtrl($q, ontologyStateService, ontologyManagerService, utilService, catalogManagerService, prefixes) {
    var dvm = this;
    var cm = catalogManagerService;
    var om = ontologyManagerService;
    var catalogId = get(cm.localCatalog, '@id', '');
    var typeIRI = prefixes.rdf + 'type';
    var types = [prefixes.owl + 'Class', prefixes.owl + 'ObjectProperty', prefixes.owl + 'DatatypeProperty', prefixes.owl + 'AnnotationProperty', prefixes.owl + 'NamedIndividual', prefixes.skos + 'Concept', prefixes.skos + 'ConceptScheme'];

    dvm.os = ontologyStateService;
    dvm.util = utilService;
    dvm.list = [];
    dvm.showList = [];
    dvm.checkedStatements = {
        additions: [],
        deletions: []
    };

    dvm.index = 0;
    dvm.size = 100;

    dvm.$onChanges = function() {
        var inProgressAdditions = map(dvm.os.listItem.inProgressCommit.additions, addition => ({
            additions: dvm.util.getPredicatesAndObjects(addition),
            id: addition['@id']
        }));
        var inProgressDeletions = map(dvm.os.listItem.inProgressCommit.deletions, deletion => ({
            deletions: dvm.util.getPredicatesAndObjects(deletion),
            id: deletion['@id']
        }));
        var mergedInProgressCommitsMap = [].concat(inProgressAdditions, inProgressDeletions).reduce((dict, currentItem) => {
            var existingValue = dict[currentItem['id']] || {};
            var mergedValue = Object.assign({ 'id' : '', 'additions' : [], 'deletions' : []}, existingValue, currentItem);
            dict[currentItem.id] = mergedValue;
            return dict;  
        }, {});
        var mergedInProgressCommits = Object.keys(mergedInProgressCommitsMap).map(key => mergedInProgressCommitsMap[key]);
        dvm.list = map(mergedInProgressCommits, inProgressItem => ({
                id: inProgressItem.id,
                additions: inProgressItem.additions,
                deletions: inProgressItem.deletions,
                disableAll: hasSpecificType(inProgressItem.additions) || hasSpecificType(inProgressItem.deletions)
        }));
        dvm.list = sortBy(dvm.list, dvm.orderByEntityName)
        dvm.showList = getList();
    }
    dvm.go = function($event, id) {
        $event.stopPropagation();
        dvm.os.goTo(id);
    }
    dvm.update = function() {
        cm.getBranchHeadCommit(dvm.os.listItem.ontologyRecord.branchId, dvm.os.listItem.ontologyRecord.recordId, catalogId)
            .then(headCommit => {
                var commitId = get(headCommit, "commit['@id']", '');
                return dvm.os.updateOntology(dvm.os.listItem.ontologyRecord.recordId, dvm.os.listItem.ontologyRecord.branchId, commitId);
            }, $q.reject)
            .then(() => dvm.util.createSuccessToast('Your ontology has been updated.'), dvm.util.createErrorToast);
    }
    dvm.restoreBranchWithUserBranch = function() {
        var userBranchId = dvm.os.listItem.ontologyRecord.branchId;
        var userBranch = find(dvm.os.listItem.branches, {'@id': userBranchId})
        var createdFromId = dvm.util.getPropertyId(userBranch, prefixes.catalog + 'createdFrom');
        var branchConfig = {
            title: dvm.util.getDctermsValue(userBranch, 'title'),
            description: dvm.util.getDctermsValue(userBranch, 'description')
        };

        var createdBranchId;
        cm.createRecordBranch(dvm.os.listItem.ontologyRecord.recordId, catalogId, branchConfig, dvm.os.listItem.ontologyRecord.commitId)
            .then(branchId => {
                createdBranchId = branchId;
                return cm.getRecordBranch(branchId, dvm.os.listItem.ontologyRecord.recordId, catalogId);
            }, $q.reject)
            .then(branch => {
                dvm.os.listItem.branches.push(branch);
                dvm.os.listItem.ontologyRecord.branchId = branch['@id'];
                var commitId = dvm.util.getPropertyId(branch, prefixes.catalog + 'head');
                return dvm.os.updateOntologyState({recordId: dvm.os.listItem.ontologyRecord.recordId, commitId, branchId: createdBranchId});
            }, $q.reject)
            .then(() => om.deleteOntologyBranch(dvm.os.listItem.ontologyRecord.recordId, userBranchId), $q.reject)
            .then(() => dvm.os.deleteOntologyBranchState(dvm.os.listItem.ontologyRecord.recordId, userBranchId), $q.reject)
            .then(() => {
                dvm.os.removeBranch(dvm.os.listItem.ontologyRecord.recordId, userBranchId);
                changeUserBranchesCreatedFrom(createdFromId, createdBranchId);
                dvm.util.createSuccessToast('Branch has been restored with changes.');
            }, dvm.util.createErrorToast);
    }
    dvm.mergeUserBranch = function() {
        var branch = find(dvm.os.listItem.branches, {'@id': dvm.os.listItem.ontologyRecord.branchId});
        dvm.os.listItem.merge.target = find(dvm.os.listItem.branches, {'@id': dvm.util.getPropertyId(branch, prefixes.catalog + 'createdFrom')});
        dvm.os.listItem.merge.checkbox = true;
        dvm.os.checkConflicts()
            .then(() => {
                dvm.os.merge()
                    .then(() => {
                        dvm.os.resetStateTabs();
                        dvm.util.createSuccessToast('Changes have been pulled successfully');
                        dvm.os.cancelMerge();
                    }, () => {
                        dvm.util.createErrorToast('Pulling changes failed');
                        dvm.os.cancelMerge();
                    });
            }, () => dvm.os.listItem.merge.active = true);
    }
    dvm.removeChanges = function() {
        cm.deleteInProgressCommit(dvm.os.listItem.ontologyRecord.recordId, catalogId)
            .then(() => {
                dvm.os.resetStateTabs();
                return dvm.os.updateOntology(dvm.os.listItem.ontologyRecord.recordId, dvm.os.listItem.ontologyRecord.branchId, dvm.os.listItem.ontologyRecord.commitId, dvm.os.listItem.upToDate);
            }, $q.reject)
            .then(() => dvm.os.clearInProgressCommit(), errorMessage => dvm.error = errorMessage);
    }
    dvm.orderByEntityName = function(item) {
        return dvm.os.getEntityNameByListItem(item.id);
    }
    dvm.getMoreResults = function() {
        dvm.index++;
        var currChunk = get(dvm.chunks, dvm.index, []);
        dvm.showList = concat(dvm.showList, currChunk);
    }

    function changeUserBranchesCreatedFrom(oldCreatedFromId, newCreatedFromId) {
        forEach(dvm.os.listItem.branches, branch => {
            if (cm.isUserBranch(branch)) {
                var currentCreatedFromId = dvm.util.getPropertyId(branch, prefixes.catalog + 'createdFrom');
                if (currentCreatedFromId === oldCreatedFromId) {
                    dvm.util.replacePropertyId(branch, prefixes.catalog + 'createdFrom', dvm.util.getPropertyId(branch, prefixes.catalog + 'createdFrom'), newCreatedFromId);
                    cm.updateRecordBranch(branch['@id'], dvm.os.listItem.ontologyRecord.recordId, catalogId, branch)
                        .then(() => dvm.util.createSuccessToast('Updated referenced branch.'), dvm.util.createErrorToast);
                }
            }
        });
        dvm.os.listItem.upToDate = true;
        dvm.os.listItem.userBranch = false;
        dvm.os.listItem.createdFromExists = true;
    }
    function hasSpecificType(array) {
        return !!intersection(map(filter(array, {p: typeIRI}), 'o'), types).length;
    }
    function getList() {
        dvm.chunks = chunk(dvm.list, dvm.size);
        return get(dvm.chunks, dvm.index, []);
    }
}

export default savedChangesTabComponent;