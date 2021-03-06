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
import * as angular from 'angular';
import { union, has, get } from 'lodash';

import './annotationOverlay.component.scss';

const template = require('./annotationOverlay.component.html');

/**
 * @ngdoc component
 * @name ontology-editor.component:annotationOverlay
 * @requires shared.service:propertyManagerService
 * @requires shared.service:ontologyStateService
 * @requires shared.service:utilService
 * @requires ontology-editor.service:ontologyUtilsManagerService
 * @requires shared.service:prefixes
 *
 * @description
 * `annotationOverlay` is a component that creates content for a modal that adds or edits an annotation on the
 * {@link shared.service:ontologyStateService selected entity}. The form in the modal contains a
 * `ui-select` for the annotation property, a {@link shared.component:textArea} for the annotation value, and
 * a {@link shared.component:languageSelect}. If the annotation is owl:deprecated, the `textArea` and
 * `languageSelect` are replaced by {@link shared.component:radioButton radio buttons} for the boolean
 * value. Meant to be used in conjunction with the {@link shared.service:modalService}.
 *
 * @param {Function} close A function that closes the modal
 * @param {Function} dismiss A function that dismisses the modal
 */
const annotationOverlayComponent = {
    template,
    bindings: {
        close: '&',
        dismiss: '&'
    },
    controllerAs: 'dvm',
    controller: annotationOverlayComponentCtrl
};

annotationOverlayComponentCtrl.$inject = ['ontologyManagerService', 'propertyManagerService', 'ontologyStateService', 'utilService', 'ontologyUtilsManagerService', 'prefixes'];

function annotationOverlayComponentCtrl(ontologyManagerService, propertyManagerService, ontologyStateService, utilService, ontologyUtilsManagerService, prefixes) {
    var dvm = this;
    var om = ontologyManagerService;
    dvm.ontoUtils = ontologyUtilsManagerService;
    dvm.pm = propertyManagerService;
    dvm.os = ontologyStateService;
    dvm.util = utilService;
    dvm.prefixes = prefixes;
    dvm.annotations = [];

    dvm.$onInit = function() {
        dvm.annotations = union(Object.keys(dvm.os.listItem.annotations.iris), dvm.pm.defaultAnnotations, dvm.pm.owlAnnotations);
    }
    dvm.disableProp = function(annotation) {
        return annotation === prefixes.owl + 'deprecated' && has(dvm.os.listItem.selected, "['" + prefixes.owl + 'deprecated' + "']");
    }
    dvm.selectProp = function() {
        dvm.os.annotationValue = '';
        if (dvm.os.annotationSelect === prefixes.owl + 'deprecated') {
            dvm.os.annotationType = prefixes.xsd + 'boolean';
            dvm.os.annotationLanguage = '';
        } else {
            dvm.os.annotationType = undefined;
            dvm.os.annotationLanguage = 'en';
        }
    }
    dvm.submit = function() {
        if (dvm.os.editingAnnotation) {
            dvm.editAnnotation();
        } else {
            dvm.addAnnotation();
        }
    }
    dvm.isDisabled = function() {
        var isDisabled = dvm.annotationForm.$invalid || dvm.os.annotationValue === ''
        if (!dvm.os.editingAnnotation) {
            isDisabled = isDisabled || dvm.os.annotationSelect === undefined;
        }
        return isDisabled;
    }
    dvm.changeLanguage = function(value) {
        dvm.os.annotationLanguage = value;
        dvm.os.annotationType = undefined; // Unset type to create valid JSON-LD when language is set
    }
    dvm.addAnnotation = function() {
        var added = dvm.pm.addValue(dvm.os.listItem.selected, dvm.os.annotationSelect, dvm.os.annotationValue, dvm.os.annotationType, dvm.os.annotationLanguage);
        
        if (added) {
            dvm.os.addToAdditions(dvm.os.listItem.ontologyRecord.recordId, createJson(dvm.os.annotationValue, dvm.os.annotationType, dvm.os.annotationLanguage));
            dvm.ontoUtils.saveCurrentChanges();
            if (om.entityNameProps.includes(dvm.os.annotationSelect)) {
                dvm.ontoUtils.updateLabel();
            }
            dvm.os.annotationModified(dvm.os.listItem.selected['@id'], dvm.os.annotationSelect, dvm.os.annotationValue);
        } else {
            dvm.util.createWarningToast('Duplicate property values not allowed');
        }
        dvm.close();
    }
    dvm.editAnnotation = function() {
        var oldObj = angular.copy(get(dvm.os.listItem.selected, "['" + dvm.os.annotationSelect + "']['" + dvm.os.annotationIndex + "']"));
        var edited = dvm.pm.editValue(dvm.os.listItem.selected, dvm.os.annotationSelect, dvm.os.annotationIndex, dvm.os.annotationValue, dvm.os.annotationType, dvm.os.annotationLanguage);
        
        if (edited) {
            if (dvm.os.annotationLanguage) {
                dvm.changeLanguage(dvm.os.annotationLanguage);
            }
            dvm.os.addToDeletions(dvm.os.listItem.ontologyRecord.recordId, createJson(get(oldObj, '@value'), get(oldObj, '@type'), get(oldObj, '@language')));
            dvm.os.addToAdditions(dvm.os.listItem.ontologyRecord.recordId, createJson(dvm.os.annotationValue, dvm.os.annotationType, dvm.os.annotationLanguage));
            dvm.ontoUtils.saveCurrentChanges();
            if (om.entityNameProps.includes(dvm.os.annotationSelect)) {
                dvm.ontoUtils.updateLabel();
            }
            dvm.os.annotationModified(dvm.os.listItem.selected['@id'], dvm.os.annotationSelect, dvm.os.annotationValue);
        } else {
            dvm.util.createWarningToast('Duplicate property values not allowed');
        }
        dvm.close();
    }
    dvm.cancel = function() {
        dvm.dismiss();
    }

    function createJson(value, type, language) {
        var valueObj = dvm.pm.createValueObj(value, type, language);
        return dvm.util.createJson(dvm.os.listItem.selected['@id'], dvm.os.annotationSelect, valueObj);
    }
}

export default annotationOverlayComponent;