(function () {
    'use strict';

    /**
     * @ngdoc component
     * @name ontology-editor.component:hierarchyTree
     * @requires ontologyManager.service:ontologyManagerService
     * @requires ontologyState.service:ontologyStateService
     * @requires ontologyUtilsManager.service:ontologyUtilsManagerService
     * @requires prefixes.service:prefixes
     *
     * @description
     * `hierarchyTree` is a component which creates a `div` containing a {@link searchBar.directive:searchBar}
     * and hierarchy of {@link treeItem.directive:treeItem}. When search text is provided, the hierarchy filters what
     * is shown based on value matches with predicates in the {@link ontologyManager.service:ontologyManagerService entityNameProps}.
     *
     * @param {Object[]} hierarchy An array which represents a flattened hierarchy
     * @param {Function} updateSearch A function to update the state variable used to track the search filter text
     */
    const hierarchyTreeComponent = {
        templateUrl: 'ontology-editor/directives/hierarchyTree/hierarchyTree.component.html',
        bindings: {
            hierarchy: '<',
            updateSearch: '<'
        },
        controllerAs: 'dvm',
        controller: hierarchyTreeComponentCtrl
    };

    hierarchyTreeComponentCtrl.$inject = ['ontologyManagerService', 'ontologyStateService', 'ontologyUtilsManagerService', 'prefixes', 'INDENT'];

    function hierarchyTreeComponentCtrl(ontologyManagerService, ontologyStateService, ontologyUtilsManagerService, prefixes, INDENT) {
        var dvm = this;
        var om = ontologyManagerService;
        dvm.indent = INDENT;
        dvm.os = ontologyStateService;
        dvm.ou = ontologyUtilsManagerService;
        dvm.searchText = '';
        dvm.filterText = '';

        dvm.$onInit = function() {
            update();
        }
        dvm.$onChanges = function() {
            update();
        }
        dvm.onKeyup = function () {
            dvm.filterText = dvm.searchText;
            update();
        }
        dvm.searchFilter = function (node) {
            delete node.underline;
            delete node.parentNoMatch;
            delete node.displayNode;
            if (dvm.filterText) {
                var entity = dvm.os.getEntityByRecordId(dvm.os.listItem.ontologyRecord.recordId, node.entityIRI);
                var searchValues = _.pick(entity, om.entityNameProps);
                var match = false;
                _.forEach(_.keys(searchValues), key => _.forEach(searchValues[key], value => {
                    if (value['@value'].toLowerCase().includes(dvm.filterText.toLowerCase()))
                        match = true;
                }));
                if (match) {
                    var path = node.path[0];
                    for (var i = 1; i < node.path.length - 1; i++) {
                        var iri = node.path[i];
                        path = path + '.' + iri;
                        dvm.os.setOpened(path, true);

                        var parentNode = _.find(dvm.hierarchy, {'entityIRI': iri});
                        parentNode.displayNode = true;
                    }
                    node.underline = true;
                }
                if (!match && node.hasChildren) {
                    node.parentNoMatch = true;
                    return true;
                }
                return match;
            } else {
                return true;
            }
        }
        dvm.isShown = function (node) {
            var displayNode = (node.indent > 0 && dvm.os.areParentsOpen(node)) || (node.indent === 0 && _.get(node, 'path', []).length === 2);
            if (dvm.filterText && node.parentNoMatch) {
                if (node.displayNode === undefined) {
                    return false;
                } else {
                    return displayNode && node.displayNode;
                }
            }
            return displayNode;
        }

        function update() {
            dvm.updateSearch(dvm.filterText);
            dvm.filteredHierarchy = _.filter(dvm.hierarchy, dvm.searchFilter);
        }
    }

    angular.module('ontology-editor')
        .component('hierarchyTree', hierarchyTreeComponent);
})();