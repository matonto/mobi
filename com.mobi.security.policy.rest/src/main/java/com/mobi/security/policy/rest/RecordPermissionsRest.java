package com.mobi.security.policy.rest;

/*-
 * #%L
 * com.mobi.security.policy.rest
 * $Id:$
 * $HeadURL:$
 * %%
 * Copyright (C) 2016 - 2018 iNovex Information Systems, Inc.
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

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/record-permissions")
@Api(value = "/record-permissions")
public interface RecordPermissionsRest {
    /**
     * Retrieves a specific record policy JSON identified by its ID of which users can perform each rule. If the policy
     * could not be found, returns a 400. Return JSON is structured like:
     * {
     *   "urn:read": {
     *     "everyone": false,
     *     "users": [
     *       "http://mobi.com/users/userIRI1",
     *       "http://mobi.com/users/userIRI2"
     *     ],
     *     "groups": []
     *   }, ...
     * }
     *
     * @param policyId The String representing a policy ID. NOTE: Assumes ID represents an IRI unless String
     *                 begins with "_:"
     * @return A JSON representation of which user can perform each rule.
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{policyId}")
    @ApiOperation("Retrieves a specific record security policy by its ID.")
    Response retrieveRecordPolicy(@PathParam("policyId") String policyId);

    /**
     * Updates the identified record policy with the provided JSON representation in the body. Provided policy must have
     * the same ID. JSON object defines who has permission to perform each rule. Each rule is structured like:
     * {
     *   "urn:read": {
     *     "everyone": false,
     *     "users": [
     *       "http://mobi.com/users/userIRI1",
     *       "http://mobi.com/users/userIRI2"
     *     ],
     *     "groups": []
     *   }, ...
     * }
     *
     * @param policyId The String representing a policy ID. NOTE: Assumes ID represents an IRI unless String
     *                 begins with "_:"
     * @param policyJson A JSON representation of the new version of the policy
     * @return A Response indicating the success of the request
     */
    @PUT
    @Path("{policyId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation("Updates an existing record security policy using the provided JSON body.")
    Response updateRecordPolicy(@PathParam("policyId") String policyId, String policyJson);
}

