/*-
 * #%L
 * com.mobi.web
 * $Id:$
 * $HeadURL:$
 * %%
 * Copyright (C) 2016 iNovex Information Systems, Inc.
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
(function() {
    'use strict';

    angular
        /**
         * @ngdoc overview
         * @name createPropertyOverlay
         *
         * @description
         * The `createPropertyOverlay` module only provides the `createPropertyOverlay` directive which creates
         * content for a modal to add a data, object, or annotation property to an ontology.
         */
        .module('createPropertyOverlay', [])
        /**
         * @ngdoc directive
         * @name createPropertyOverlay.directive:createPropertyOverlay
         * @scope
         * @restrict E
         * @requires ontologyManager.service:ontologyManagerService
         * @requires ontologyState.service:ontologyStateService
         * @requires prefixes.service:prefixes
         * @requires ontologyUtilsManager.service:ontologyUtilsManagerService
         *
         * @description
         * `createPropertyOverlay` is a directive that creates content for a modal that creates a data, object, or
         * annotation property in the current {@link ontologyState.service:ontologyStateService selected ontology}.
         * The form in the modal contains a text input for the property name (which populates the
         * {@link staticIri.directive:staticIri IRI}), a {@link textArea.directive:textArea} for the property
         * description, {@link advancedLanguageSelect.directive:advancedLanguageSelect}, and
         * {@link radioButton.directive:radioButton radioButtons} to select the type of the property. The form will
         * contain other fields depending on the property type selected. If the property type is data or object
         * property, the fields shown are {@link checkbox.directive:checkbox checkboxes} for the property
         * characteristics, an {@link iriSelect.directive:iriSelect} for the domain, an
         * {@link iriSelect.directive:iriSelect} for the range, and a
         * {@link superPropertySelect.directive:superPropertySelect}. Meant to be used in conjunction with the
         * {@link modalService.directive:modalService}.
         *
         * @param {Function} close A function that closes the modal
         * @param {Function} dismiss A function that dismisses the modal
         */
        .directive('createPropertyOverlay', createPropertyOverlay);

        createPropertyOverlay.$inject = ['$filter', 'ontologyManagerService', 'ontologyStateService', 'prefixes', 'ontologyUtilsManagerService'];

        function createPropertyOverlay($filter, ontologyManagerService, ontologyStateService, prefixes, ontologyUtilsManagerService) {
            return {
                restrict: 'E',
                templateUrl: 'modules/ontology-editor/directives/createPropertyOverlay/createPropertyOverlay.html',
                scope: {
                    close: '&',
                    dismiss: '&'
                },
                controllerAs: 'dvm',
                controller: ['$scope', function($scope) {
                    var dvm = this;

                    dvm.characteristics = [
                        {
                            checked: false,
                            typeIRI: prefixes.owl + 'FunctionalProperty',
                            displayText: 'Functional Property',
                            objectOnly: false
                        },
                        {
                            checked: false,
                            typeIRI: prefixes.owl + 'AsymmetricProperty',
                            displayText: 'Asymmetric Property',
                            objectOnly: true
                        }
                    ];
                    dvm.prefixes = prefixes;
                    dvm.om = ontologyManagerService;
                    dvm.os = ontologyStateService;
                    dvm.ontoUtils = ontologyUtilsManagerService;
                    dvm.prefix = dvm.os.getDefaultPrefix();
                    dvm.values = [];
                    dvm.property = {
                        '@id': dvm.prefix,
                        [prefixes.dcterms + 'title']: [{
                            '@value': ''
                        }],
                        [prefixes.dcterms + 'description']: [{
                            '@value': ''
                        }]
                    };
                    dvm.domains = [];
                    dvm.ranges = [];

                    dvm.nameChanged = function() {
                        if (!dvm.iriHasChanged) {
                            dvm.property['@id'] = dvm.prefix + $filter('camelCase')(dvm.property[prefixes.dcterms + 'title'][0]['@value'], 'property');
                        }
                    }
                    dvm.onEdit = function(iriBegin, iriThen, iriEnd) {
                        dvm.iriHasChanged = true;
                        dvm.property['@id'] = iriBegin + iriThen + iriEnd;
                        dvm.os.setCommonIriParts(iriBegin, iriThen);
                    }
                    dvm.create = function() {
                        if (dvm.property[prefixes.dcterms + 'description'][0]['@value'] === '') {
                            _.unset(dvm.property, prefixes.dcterms + 'description');
                        }
                        _.forEach(dvm.characteristics, (obj, key) => {
                            if (obj.checked) {
                                dvm.property['@type'].push(obj.typeIRI);
                            }
                        });
                        if (dvm.domains.length) {
                            dvm.property[prefixes.rdfs + 'domain'] = _.map(dvm.domains, iri => ({'@id': iri}));
                        }
                        if (dvm.ranges.length) {
                            dvm.property[prefixes.rdfs + 'range'] = _.map(dvm.ranges, iri => ({'@id': iri}));
                        }
                        dvm.ontoUtils.addLanguageToNewEntity(dvm.property, dvm.language);
                        dvm.os.updatePropertyIcon(dvm.property);
                        // add the entity to the ontology
                        dvm.os.addEntity(dvm.os.listItem, dvm.property);
                        // update relevant lists
                        if (dvm.om.isObjectProperty(dvm.property)) {
                            commonUpdate('objectProperties', dvm.os.setObjectPropertiesOpened);
                            dvm.os.listItem.flatEverythingTree = dvm.os.createFlatEverythingTree(dvm.os.getOntologiesArray(), dvm.os.listItem);
                        } else if (dvm.om.isDataTypeProperty(dvm.property)) {
                            commonUpdate('dataProperties', dvm.os.setDataPropertiesOpened);
                            dvm.os.listItem.flatEverythingTree = dvm.os.createFlatEverythingTree(dvm.os.getOntologiesArray(), dvm.os.listItem);
                        } else if (dvm.om.isAnnotation(dvm.property)) {
                            dvm.values = [];
                            commonUpdate('annotations', dvm.os.setAnnotationPropertiesOpened);
                        }
                        dvm.os.addToAdditions(dvm.os.listItem.ontologyRecord.recordId, dvm.property);
                        // select the new property
                        dvm.os.selectItem(_.get(dvm.property, '@id'));
                        // Save the changes to the ontology
                        dvm.ontoUtils.saveCurrentChanges();
                        // hide the overlay
                        $scope.close();
                    }
                    dvm.getKey = function() {
                        if (dvm.om.isDataTypeProperty(dvm.property)) {
                            return 'dataProperties'
                        }
                        return 'objectProperties';
                    }
                    dvm.typeChange = function() {
                        dvm.values = [];
                        if (dvm.om.isAnnotation(dvm.property)) {
                            dvm.domains = [];
                            _.forEach(dvm.characteristics, obj => {
                                obj.checked = false;
                            });
                        } else if (dvm.om.isDataTypeProperty(dvm.property)) {
                            _.forEach(_.filter(dvm.characteristics, 'objectOnly'), obj => {
                                obj.checked = false;
                            });
                        }
                        dvm.ranges = [];
                    }
                    dvm.characteristicsFilter = function(obj) {
                        return !obj.objectOnly || dvm.om.isObjectProperty(dvm.property);
                    }
                    dvm.cancel = function() {
                        $scope.dismiss();
                    }

                    function commonUpdate(key, setThisOpened) {
                        dvm.os.listItem[key].iris[dvm.property['@id']] = dvm.os.listItem.ontologyId;
                        if (dvm.values.length) {
                            dvm.property[prefixes.rdfs + 'subPropertyOf'] = dvm.values;
                            dvm.ontoUtils.setSuperProperties(dvm.property['@id'], _.map(dvm.values, '@id'), key);
                            if (dvm.ontoUtils.containsDerivedSemanticRelation(_.map(dvm.values, '@id'))) {
                                dvm.os.listItem.derivedSemanticRelations.push(dvm.property['@id']);
                            }
                        } else {
                            dvm.os.listItem[key].hierarchy.push({'entityIRI': dvm.property['@id']});
                            dvm.os.listItem[key].flat = dvm.os.flattenHierarchy(dvm.os.listItem[key].hierarchy, dvm.os.listItem.ontologyRecord.recordId);
                        }
                        setThisOpened(dvm.os.listItem.ontologyRecord.recordId, true);
                    }
                }]
            }
        }
})();
