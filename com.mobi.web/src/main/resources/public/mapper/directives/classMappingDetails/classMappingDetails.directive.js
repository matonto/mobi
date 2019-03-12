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
(function() {
    'use strict';

    angular
        /**
         * @ngdoc overview
         * @name classMappingDetails
         *
         * @description
         * The `classMappingDetails` module only provides the `classMappingDetails` directive which creates
         * a number of different tools to view and edit information about a class mapping.
         */
        .module('classMappingDetails', [])
        /**
         * @ngdoc directive
         * @name classMappingDetails.directive:classMappingDetails
         * @scope
         * @restrict E
         * @requires shared.service:mappingManagerService
         * @requires shared.service:mapperStateService
         * @requires shared.service:delimitedManagerService
         * @requires shared.service:prefixes
         * @requires shared.service:utilService
         * @requires shared.service:modalService
         *
         * @description
         * `classMappingDetails` is a directive that creates a div with sections to view and edit information
         * about the {@link shared.service:mapperStateService#selectedClassMappingId selected class mapping}.
         * One section is for viewing and editing the
         * {@link mapper.component:iriTemplateOverlay IRI template} of the class mapping. Another section is
         * for viewing the list of property mappings associated with the class mapping, adding to that list, editing
         * items in the list, and removing from that list. The directive houses methods for opening the modals for
         * {@link mapper.component:propMappingOverlay editing, adding}, and removing PropertyMappings. The
         * directive is replaced by the contents of its template.
         */
        .directive('classMappingDetails', classMappingDetails);

        classMappingDetails.$inject = ['utilService', 'prefixes', 'mappingManagerService', 'mapperStateService', 'delimitedManagerService', 'propertyManagerService', 'modalService'];

        function classMappingDetails(utilService, prefixes, mappingManagerService, mapperStateService, delimitedManagerService, propertyManagerService, modalService) {
            return {
                restrict: 'E',
                controllerAs: 'dvm',
                replace: true,
                scope: {},
                controller: function() {
                    var dvm = this;
                    dvm.state = mapperStateService;
                    dvm.mm = mappingManagerService;
                    dvm.dm = delimitedManagerService;
                    dvm.util = utilService;
                    var pm = propertyManagerService;

                    dvm.editIriTemplate = function() {
                        modalService.openModal('iriTemplateOverlay');
                    }
                    dvm.isInvalid = function(propMapping) {
                        return !!_.find(dvm.state.invalidProps, {'@id': propMapping['@id']});
                    }
                    dvm.getIriTemplate = function() {
                        var classMapping = _.find(dvm.state.mapping.jsonld, {'@id': dvm.state.selectedClassMappingId});
                        var prefix = dvm.util.getPropertyValue(classMapping, prefixes.delim + 'hasPrefix');
                        var localName = dvm.util.getPropertyValue(classMapping, prefixes.delim + 'localName');
                        return prefix + localName;
                    }
                    dvm.getPropValue = function(propMapping) {
                        if (dvm.mm.isDataMapping(propMapping)) {
                            return dvm.dm.getHeader(dvm.getLinkedColumnIndex(propMapping));
                        } else {
                            return dvm.util.getDctermsValue(_.find(dvm.state.mapping.jsonld, {'@id': dvm.getLinkedClassId(propMapping)}), 'title');
                        }
                    }
                    dvm.getDataValuePreview = function(propMapping) {
                        var firstRowIndex = dvm.dm.containsHeaders ? 1 : 0;
                        return _.get(dvm.dm.dataRows, '[' + firstRowIndex + '][' + dvm.getLinkedColumnIndex(propMapping) + ']', '(None)');
                    }
                    dvm.getDatatypePreview = function(propMapping) {
                        var props = dvm.state.getPropsByClassMappingId(dvm.state.selectedClassMappingId);
                        var mapProp = dvm.util.getPropertyId(propMapping, prefixes.delim + 'hasProperty');
                        var prop = _.find(props, {propObj: {'@id': mapProp}});
                        var propIRI = dvm.util.getPropertyId(propMapping, prefixes.delim + 'datatypeSpec') || dvm.util.getPropertyId(prop.propObj, prefixes.rdfs + 'range') || prefixes.xsd + 'string';
                        return dvm.util.getBeautifulIRI(propIRI);
                    }
                    dvm.getLanguagePreview = function(propMapping) {
                        var languageObj = _.find(pm.languageList, {value: dvm.getLanguageTag(propMapping)});
                        return languageObj ? languageObj.label : undefined;
                    }
                    dvm.getLanguageTag = function(propMapping) {
                        return dvm.util.getPropertyValue(propMapping, prefixes.delim + 'languageSpec');
                    }
                    dvm.getLinkedClassId = function(propMapping) {
                        return dvm.util.getPropertyId(propMapping, prefixes.delim + 'classMapping');
                    }
                    dvm.getLinkedColumnIndex = function(propMapping) {
                        return dvm.util.getPropertyValue(propMapping, prefixes.delim + 'columnIndex');
                    }
                    dvm.switchClass = function(propMapping) {
                        if (dvm.mm.isObjectMapping(propMapping)) {
                            dvm.state.selectedClassMappingId = dvm.getLinkedClassId(propMapping);
                            dvm.state.selectedPropMappingId = '';
                        }
                    }
                    dvm.addProp = function() {
                        dvm.state.newProp = true;
                        modalService.openModal('propMappingOverlay');
                    }
                    dvm.editProp = function(propMapping) {
                        dvm.state.selectedPropMappingId = propMapping['@id'];
                        modalService.openModal('propMappingOverlay');
                    }
                    dvm.confirmDeleteProp = function(propMapping) {
                        dvm.state.selectedPropMappingId = propMapping['@id'];
                        modalService.openConfirmModal('<p>Are you sure you want to delete <strong>' + dvm.getEntityName(dvm.state.selectedPropMappingId) + '</strong> from <strong>' + dvm.getEntityName(dvm.state.selectedClassMappingId) + '</strong>?</p>', dvm.deleteProp);
                    }
                    dvm.getEntityName = function(id) {
                        return dvm.util.getDctermsValue(_.find(dvm.state.mapping.jsonld, {'@id': id}), 'title');
                    }
                    dvm.deleteProp = function() {
                        var classMapping = _.find(dvm.state.mapping.jsonld, {'@id': dvm.state.selectedClassMappingId});
                        dvm.state.deleteProp(dvm.state.selectedPropMappingId, classMapping['@id']);
                        dvm.state.resetEdit();
                        dvm.state.selectedClassMappingId = classMapping['@id'];
                    }
                },
                templateUrl: 'mapper/directives/classMappingDetails/classMappingDetails.directive.html'
            }
        }
})();
