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
import { map } from 'lodash';

const template = require('./objectPropertyAxioms.component.html');

/**
 * @ngdoc component
 * @name ontology-editor.component:objectPropertyAxioms
 * @requires shared.service:ontologyStateService
 * @requires shared.service:propertyManagerService
 * @requires shared.service:prefixes
 * @requires ontology-editor.service:ontologyUtilsManagerService
 * @requires shared.service:ontologyManagerService
 * @requires shared.service:modalService
 *
 * @description
 * `objectPropertyAxioms` is a component that creates a list of {@link ontology-editor.component:propertyValues} of
 * the axioms on the {@link shared.service:ontologyStateService selected object property}. The component houses the
 * methods for opening the modal for removing property axioms.
 */
const objectPropertyAxiomsComponent = {
    template,
    bindings: {},
    controllerAs: 'dvm',
    controller: objectPropertyAxiomsComponentCtrl
};

objectPropertyAxiomsComponentCtrl.$inject = ['ontologyStateService', 'propertyManagerService', 'prefixes', 'ontologyUtilsManagerService', 'ontologyManagerService', 'modalService'];

function objectPropertyAxiomsComponentCtrl(ontologyStateService, propertyManagerService, prefixes, ontologyUtilsManagerService, ontologyManagerService, modalService) {
    var dvm = this;
    var om = ontologyManagerService;
    dvm.os = ontologyStateService;
    dvm.pm = propertyManagerService;
    dvm.ontoUtils = ontologyUtilsManagerService;

    dvm.getAxioms = function() {
        return map(dvm.pm.objectAxiomList, 'iri');
    }
    dvm.openRemoveOverlay = function(key, index) {
        dvm.key = key;
        modalService.openConfirmModal(dvm.ontoUtils.getRemovePropOverlayMessage(key, index), () => {
            dvm.ontoUtils.removeProperty(key, index).then(dvm.removeFromHierarchy);
        });
    }
    dvm.removeFromHierarchy = function(axiomObject) {
        if (prefixes.rdfs + 'subPropertyOf' === dvm.key && !om.isBlankNodeId(axiomObject['@id'])) {
            dvm.os.deleteEntityFromParentInHierarchy(dvm.os.listItem.objectProperties, dvm.os.listItem.selected['@id'], axiomObject['@id']);
            dvm.os.listItem.objectProperties.flat = dvm.os.flattenHierarchy(dvm.os.listItem.objectProperties);
            dvm.os.setVocabularyStuff();
        }
    }
}

export default objectPropertyAxiomsComponent;