/*-
 * #%L
 * org.matonto.web
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
         * @name util
         *
         * @description
         * The `util` module only provides the `utilService` service which provides various common utility
         * methods for use across MatOnto.
         */
        .module('util', [])
        /**
         * @ngdoc service
         * @name util.service:utilService
         * @requires $filter
         *
         * @description
         * `utilService` is a service that provides various utility methods for use across MatOnto.
         */
        .service('utilService', utilService);

        utilService.$inject = ['$filter', 'prefixes', 'toastr'];

        function utilService($filter, prefixes, toastr) {
            var self = this;

            /**
             * @ngdoc method
             * @name getBeautifulIRI
             * @methodOf util.service:utilService
             *
             * @description
             * Gets the "beautified" IRI representation for the iri passed. Returns the modified IRI.
             *
             * @param {string} iri The IRI string that you want to beautify.
             * @returns {string} The beautified IRI string.
             */
            self.getBeautifulIRI = function(iri) {
                var splitEnd = $filter('splitIRI')(iri).end;
                return splitEnd ? $filter('beautify')(splitEnd) : iri;
            }
            /**
             * @ngdoc method
             * @name getPropertyValue
             * @methodOf util.service:utilService
             *
             * @description
             * Gets the first value of the specified property from the passed entity. Returns an empty
             * string if not found.
             *
             * @param {Object} entity The entity to retrieve the property value from
             * @param {string} propertyIRI The IRI of a property
             * @return {string} The first value of the property if found; empty string otherwise
             */
            self.getPropertyValue = function(entity, propertyIRI) {
                return _.get(entity, "['" + propertyIRI + "'][0]['@value']", '');
            }
            /**
             * @ngdoc method
             * @name setPropertyValue
             * @methodOf util.service:utilService
             *
             * @description
             * Sets the first value of the specified property of the passed entity to the passed value.
             *
             * @param {Object} entity The entity to set the property value of
             * @param {string} propertyIRI The IRI of a property
             * @param {string} value The new value for the property
             */
            self.setPropertyValue = function(entity, propertyIRI, value) {
                _.set(entity, "['" + propertyIRI + "'][0]['@value']", value);
            }
            /**
             * @ngdoc method
             * @name getPropertyId
             * @methodOf util.service:utilService
             *
             * @description
             * Gets the first id value of the specified property from the passed entity. Returns an empty
             * string if not found.
             *
             * @param {Object} entity The entity to retrieve the property id value from
             * @param {string} propertyIRI The IRI of a property
             * @return {string} The first id value of the property if found; empty string otherwise
             */
            self.getPropertyId = function(entity, propertyIRI) {
                return _.get(entity, "['" + propertyIRI + "'][0]['@id']", '');
            }
            /**
             * @ngdoc method
             * @name setPropertyId
             * @methodOf util.service:utilService
             *
             * @description
             * Sets the first or appends to the existing id of the specified property of the passed entity to the passed
             * id.
             *
             * @param {Object} entity The entity to set the property value of
             * @param {string} propertyIRI The IRI of a property
             * @param {string} id The new id for the property
             */
            self.setPropertyId = function(entity, propertyIRI, id) {
                var idObj = {'@id': id};
                if (_.has(entity, "['" + propertyIRI + "']")) {
                    entity[propertyIRI].push(idObj);
                } else {
                    _.set(entity, "['" + propertyIRI + "'][0]", idObj);
                }
            }
            /**
             * @ngdoc method
             * @name getPropertyValue
             * @methodOf util.service:utilService
             *
             * @description
             * Gets the first value of the specified dcterms property from the passed entity. Returns an empty
             * string if not found.
             *
             * @param {Object} entity The entity to retrieve the property value from
             * @param {string} property The local name of a dcterms property IRI
             * @return {string} The first value of the dcterms property if found; empty string otherwise
             */
            self.getDctermsValue = function(entity, property) {
                return self.getPropertyValue(entity, prefixes.dcterms + property);
            }
            /**
             * @ngdoc method
             * @name setDctermsValue
             * @methodOf util.service:utilService
             *
             * @description
             * Sets the first value of the specified dcterms property of the passed entity to the passed value.
             *
             * @param {Object} entity The entity to set the property value of
             * @param {string} property The local name of a dcterms property IRI
             * @param {string} value The new value for the property
             */
            self.setDctermsValue = function(entity, property, value) {
                self.setPropertyValue(entity, prefixes.dcterms + property, value);
            }
            /**
             * @ngdoc method
             * @name getItemNamespace
             * @methodOf util.service:utilService
             *
             * @description
             * Gets the namespace value from the passed IRI item object.
             *
             * @param {Object} item A IRI object
             * @return {string} The namespace of the object if found; otherwise a default message
             */
            self.getItemNamespace = function(item) {
                return _.get(item, 'namespace', 'No namespace');
            }
            /**
             * @ngdoc method
             * @name mergingArrays
             * @methodOf util.service:utilService
             *
             * @description
             * Merges two arrays together using the Lodash isEqual function and returns the merged
             * array.
             *
             * @param {*[]} objValue An array to be merged into
             * @param {*[]} srcValue An array
             * @return {*[]} The result of merging the two arrays using Lodash's isEqual
             */
            self.mergingArrays = function(objValue, srcValue) {
                if (_.isArray(objValue)) {
                    return _.unionWith(objValue, srcValue, _.isEqual);
                }
            }
            /**
             * @ngdoc method
             * @name getDctermsId
             * @methodOf util.service:utilService
             *
             * @description
             * Gets the first id value of the specified dcterms property from the passed entity. Returns an
             * empty string if not found.
             *
             * @param {Object} entity The entity to retrieve the property id value from
             * @param {string} property The local name of a dcterms property IRI
             * @return {string} The first id value of the dcterms property if found; empty string otherwise
             */
            self.getDctermsId = function(entity, property) {
                return _.get(entity, "['" + prefixes.dcterms + property + "'][0]['@id']", '');
            }
            /**
             * @ngdoc method
             * @name parseLinks
             * @methodOf util.service:utilService
             *
             * @description
             * Parses through the passed "link" header string to retrieve each individual link and its rel label.
             * Return an object with keys of the link rel labels and values of the link URLs.
             *
             * @param {string} header A "link" header string from an HTTP response
             * @return {Object} An object with keys of the rel labels and values of URLs
             */
            self.parseLinks = function(header) {
                // Split parts by comma
                var parts = header.split(',');
                var links = {};
                // Parse each part into a named link
                _.forEach(parts, p => {
                    var section = p.split(';');
                    if (section.length === 2) {
                        var url = section[0].replace(/<(.*)>/, '$1').trim();
                        var name = section[1].replace(/rel="(.*)"/, '$1').trim();
                        links[name] = url;
                    }
                });
                return links;
            }
            /**
             * @ngdoc method
             * @name createErrorToast
             * @methodOf util.service:utilService
             *
             * @description
             * Creates an error toast with the passed error text that will not disappear until it is dismissed.
             *
             * @param {string} text The text for the body of the error toast
             */
            self.createErrorToast = function(text) {
                toastr.error(text, 'Error', {timeOut: 0});
            }
            /**
             * @ngdoc method
             * @name createSuccessToast
             * @methodOf util.service:utilService
             *
             * @description
             * Creates a success toast with the passed success text that will not disappear until it is dismissed.
             *
             * @param {string} text The text for the body of the success toast
             */
            self.createSuccessToast = function(text) {
                toastr.success(text, 'Success', {timeOut: 0});
            }
            /**
             * @ngdoc method
             * @name getIRINamespace
             * @methodOf util.service:utilService
             *
             * @description
             * Gets the namespace of a JSON-LD entity.
             *
             * @param {Object} entity A JSON-LD entity
             * @return {string} The namespace of the entity's `@id` value
             */
            self.getIRINamespace = function(entity) {
                var split = $filter('splitIRI')(entity);
                return split.begin + split.then;
            }
            /**
             * @ngdoc method
             * @name createJson
             * @methodOf util.service:utilService
             *
             * @description
             * Creates an initial JSON-LD object with the passed id and starting property IRI with initial value
             * object.
             *
             * @param {string} id An IRI for the new object
             * @param {string} property A property IRI
             * @param {Object} valueObj A value object in JSON-LD. Must contain either a `@value` or a `@id` key
             * @return {Object} A JSON-LD object with an id and property with a starting value
             */
            self.createJson = function(id, property, valueObj) {
                return {
                    '@id': id,
                    [property]: [valueObj]
                }
            }
            /**
             * @ngdoc method
             * @name getDate
             * @methodOf util.service:utilService
             *
             * @description
             * Creates a new Date string in the specified format from the passed date string. Used when converting
             * date strings from the backend into other date strings.
             *
             * @param {string} dateStr A string containing date information
             * @param {string} format A string representing a format for the new date string (See MDN spec for Date)
             * @return {string} A newly formatted date string from the original date string
             */
            self.getDate = function(dateStr, format) {
                 return dateStr ? $filter('date')(new Date(dateStr), format) : '(No Date Specified)';
            }
            /**
             * @ngdoc method
             * @name condenseCommitId
             * @methodOf util.service:utilService
             *
             * @description
             * Retrieves a shortened id for from an IRI for a commit.
             *
             * @param {string} id The IRI of a commit
             * @return {string} A shortened id from the commit IRI
             */
            self.condenseCommitId = function(id) {
                return $filter('splitIRI')(id).end.substr(0, 10);
            }
            /**
             * @ngdoc method
             * @name paginatedConfigToParams
             * @methodOf util.service:utilService
             *
             * @description
             * Converts a common paginated configuration object into a $http query parameter object with
             * common query parameters for pagination. These query parameters are: sort, ascending, limit,
             * and offset.
             *
             * @param {Object} paginatedConfig A configuration object for paginated requests
             * @param {number} paginatedConfig.pageIndex The index of the page of results to retrieve
             * @param {number} paginatedConfig.limit The number of results per page
             * @param {Object} paginatedConfig.sortOption A sort option object
             * @param {string} paginatedConfig.sortOption.field A property IRI
             * @param {boolean} paginatedConfig.sortOption.asc Whether the sort should be ascending or descending
             * @return {Object} An object with converted query parameters if present in original configuration.
             */
            self.paginatedConfigToParams = function(paginatedConfig) {
                var params = {};
                if (_.has(paginatedConfig, 'sortOption.field')) {
                    params.sort = paginatedConfig.sortOption.field;
                }
                if (_.has(paginatedConfig, 'sortOption.asc')) {
                    params.ascending = paginatedConfig.sortOption.asc;
                }
                if (_.has(paginatedConfig, 'limit')) {
                    params.limit = paginatedConfig.limit;
                    if (_.has(paginatedConfig, 'pageIndex')) {
                        params.offset = paginatedConfig.pageIndex * paginatedConfig.limit;
                    }
                }
                return params;
            }
            /**
             * @ngdoc method
             * @name onError
             * @methodOf util.service:utilService
             *
             * @description
             * Rejects the provided deferred promise with the status text of the passed HTTP response object
             * if present, otherwise uses the passed default message.
             *
             * @param {Object} error A HTTP response object
             * @param {?} deferred A deferred promise created by $q.defer()
             * @param {string='Something went wrong. Please try again later.'} defaultMessage The optional
             * default error text for the rejection
             */
            self.onError = function(error, deferred, defaultMessage = 'Something went wrong. Please try again later.') {
                deferred.reject(_.get(error, 'statusText', defaultMessage));
            }
        }
})();
