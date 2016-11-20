/*
 *
 *   Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *   WSO2 Inc. licenses this file to you under the Apache License,
 *   Version 2.0 (the "License"); you may not use this file except
 *   in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */

package org.wso2.carbon.apimgt.core.api;

import org.wso2.carbon.apimgt.core.exception.APIManagementException;
import org.wso2.carbon.apimgt.core.models.API;
import org.wso2.carbon.apimgt.core.models.Application;

import java.util.List;
import java.util.Map;

/**
 * This interface used to write Store specific methods.
 *
 */
public interface APIStore extends APIManager {

    /**
     * Returns a paginated list of all APIs in given Status list. If a given API has multiple APIs,
     * only the latest version will be included in this list.
     *
     * @param offset offset
     * @param limit  limit
     * @param status One or more Statuses
     * @return List<API>
     * @throws APIManagementException if failed to API set
     */
    List<API> getAllAPIsByStatus(int offset, int limit, String[] status) throws APIManagementException;

    /**
     * Returns a paginated list of all APIs which match the given search criteria.
     *
     * @param query searchType
     * @param limit limit
     * @return List<API>
     * @throws APIManagementException
     */
    List<API> searchAPIs(String query, int offset, int limit) throws APIManagementException;

    /**
     * Function to remove an Application from the API Store
     *
     * @param application - The Application Object that represents the Application
     * @throws APIManagementException
     */
    void removeApplication(Application application) throws APIManagementException;

    /**
     * Adds an application
     *
     * @param application Application
     * @return uuid of the newly created application
     * @throws APIManagementException if failed to add Application
     */
    String addApplication(Application application) throws APIManagementException;

    /**
     * This will return APIM application by giving name and subscriber
     *
     * @param userId          APIM subscriber ID.
     * @param applicationName APIM application name.
     * @param groupId         Group id.
     * @return it will return Application.
     * @throws APIManagementException
     */
    Application getApplicationsByName(String userId, String applicationName, String groupId)
            throws APIManagementException;

    /**
     * Returns a list of applications for a given subscriber
     *
     * @param subscriber Subscriber
     * @param groupId    the groupId to which the applications must belong.
     * @return Applications
     * @throws APIManagementException if failed to applications for given subscriber
     */

    Application[] getApplications(String subscriber, String groupId) throws APIManagementException;

    /**
     * Updates the details of the specified user application.
     *
     * @param application Application object containing updated data
     * @throws APIManagementException If an error occurs while updating the application
     */
    void updateApplication(Application application) throws APIManagementException;

    /**
     * Creates a request for getting Approval for Application Registration.
     *
     * @param userId          Subsriber name.
     * @param applicationName of the Application.
     * @param tokenType       Token type (PRODUCTION | SANDBOX)
     * @param callbackUrl     callback URL
     * @param allowedDomains  allowedDomains for token.
     * @param validityTime    validity time period.
     * @param groupingId      APIM application id.
     * @param jsonString      Callback URL for the Application.
     * @param tokenScope      Scopes for the requested tokens.
     * @throws APIManagementException if failed to applications for given subscriber
     */
    Map<String, Object> requestApprovalForApplicationRegistration(String userId, String applicationName,
            String tokenType, java.lang.String callbackUrl, String[] allowedDomains, String validityTime,
            String tokenScope, String groupingId, String jsonString) throws APIManagementException;

}