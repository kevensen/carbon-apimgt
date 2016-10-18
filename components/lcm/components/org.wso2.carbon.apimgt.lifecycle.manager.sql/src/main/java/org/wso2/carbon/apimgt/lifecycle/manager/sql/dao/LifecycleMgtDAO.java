/*
 * Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.apimgt.lifecycle.manager.sql.dao;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.apimgt.lifecycle.manager.sql.beans.LifecycleConfigBean;
import org.wso2.carbon.apimgt.lifecycle.manager.sql.beans.LifecycleStateBean;
import org.wso2.carbon.apimgt.lifecycle.manager.sql.constants.Constants;
import org.wso2.carbon.apimgt.lifecycle.manager.sql.constants.SQLConstants;
import org.wso2.carbon.apimgt.lifecycle.manager.sql.exception.LifecycleManagerDatabaseException;
import org.wso2.carbon.apimgt.lifecycle.manager.sql.utils.LifecycleMgtDBUtil;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * This class represent the DAO layer for lifecycle related operations.
 */
public class LifecycleMgtDAO {

    private static final Log log = LogFactory.getLog(LifecycleMgtDAO.class);

    private LifecycleMgtDAO() {

    }

    /**
     * Method to get the instance of the LCMgtDAO.
     *
     * @return {@link LifecycleMgtDAO} instance
     */
    public static LifecycleMgtDAO getInstance() {
        return LCMgtDAOHolder.INSTANCE;
    }

    /**
     * Add lifecycle config for a specific tenant.
     *
     * @param lifecycleConfigBean                  Contains information about name and content.
     * @param tenantId
     * @throws LifecycleManagerDatabaseException   If failed to add lifecycle.
     */
    public void addLifecycle(LifecycleConfigBean lifecycleConfigBean, int tenantId)
            throws LifecycleManagerDatabaseException {
        Connection connection = null;
        PreparedStatement prepStmt = null;

        String query = SQLConstants.ADD_LIFECYCLE_SQL;
        try {
            connection = LifecycleMgtDBUtil.getConnection();
            connection.setAutoCommit(false);
            String dbProductName = connection.getMetaData().getDatabaseProductName();
            boolean returnsGeneratedKeys = LifecycleMgtDBUtil.canReturnGeneratedKeys(dbProductName);
            if (returnsGeneratedKeys) {
                prepStmt = connection.prepareStatement(query, new String[] {
                        LifecycleMgtDBUtil.getConvertedAutoGeneratedColumnName(dbProductName, Constants.LC_ID) });
            } else {
                prepStmt = connection.prepareStatement(query);
            }
            //prepStmt = connection.prepareStatement(query);
            prepStmt.setString(1, lifecycleConfigBean.getLcName());
            prepStmt.setBinaryStream(2, new ByteArrayInputStream(lifecycleConfigBean.getLcContent().getBytes()));
            prepStmt.setInt(3, tenantId);
            prepStmt.execute();
            connection.commit();
        } catch (SQLException e) {
            try {
                connection.rollback();
            } catch (SQLException e1) {
                log.error("Error while roll back operation for adding new lifecycle with name :" + lifecycleConfigBean
                        .getLcName(), e);
            }
            handleException("Error while adding the lifecycle " + lifecycleConfigBean.getLcName(), e);
        } finally {
            LifecycleMgtDBUtil.closeAllConnections(prepStmt, connection, null);
        }
    }

    /**
     * Update existing lifecycle config..
     *
     * @param lifecycleConfigBean                  Contains information about name and content.
     * @param tenantId
     * @throws LifecycleManagerDatabaseException   If failed to add lifecycle.
     */
    public void updateLifecycle(LifecycleConfigBean lifecycleConfigBean, int tenantId)
            throws LifecycleManagerDatabaseException {
        Connection connection = null;
        PreparedStatement prepStmt = null;

        String query = SQLConstants.UPDATE_LIFECYCLE_SQL;
        try {
            connection = LifecycleMgtDBUtil.getConnection();
            connection.setAutoCommit(false);
            prepStmt = connection.prepareStatement(query);
            prepStmt.setBinaryStream(1, new ByteArrayInputStream(lifecycleConfigBean.getLcContent().getBytes()));
            prepStmt.setString(2, lifecycleConfigBean.getLcName());
            prepStmt.setInt(3, tenantId);
            prepStmt.executeUpdate();
            connection.commit();
        } catch (SQLException e) {
            try {
                connection.rollback();
            } catch (SQLException e1) {
                log.error("Error while roll back operation for updating existing lifecycle with name :"
                        + lifecycleConfigBean.getLcName(), e);
            }
            handleException("Error while updating the lifecycle" + lifecycleConfigBean.getLcName(), e);
        } finally {
            LifecycleMgtDBUtil.closeAllConnections(prepStmt, connection, null);
        }
    }

    /**
     * Delete existing lifecycle config. Will be called only lifecycle is not associated with any asset.
     *
     * @param lcName                                Contains information about name and content.
     * @param tenantId
     * @throws LifecycleManagerDatabaseException    If failed to add lifecycle.
     */
    public void deleteLifecycle(String lcName, int tenantId) throws LifecycleManagerDatabaseException {
        Connection connection = null;
        PreparedStatement prepStmt = null;

        String query = SQLConstants.DELETE_LIFECYCLE_SQL;
        try {
            connection = LifecycleMgtDBUtil.getConnection();
            connection.setAutoCommit(false);
            prepStmt = connection.prepareStatement(query);
            prepStmt.setString(1, lcName);
            prepStmt.setInt(2, tenantId);
            prepStmt.execute();
            connection.commit();
        } catch (SQLException e) {
            try {
                connection.rollback();
            } catch (SQLException e1) {
                log.error("Error while roll back operation for deleting lifecycle with name :" + lcName, e);
            }
            handleException("Error while deleting the lifecycle " + lcName, e);
        } finally {
            LifecycleMgtDBUtil.closeAllConnections(prepStmt, connection, null);
        }
    }

    /**
     * Get lifecycle list of specific tenant.
     *
     * @param tenantId
     * @return                                      Lifecycle list containing names;
     * @throws LifecycleManagerDatabaseException    If failed to get lifecycle list.
     */
    public String[] getLifecycleList(int tenantId) throws LifecycleManagerDatabaseException {
        List<String> lifecycleList = new ArrayList<>();
        Connection connection = null;
        PreparedStatement prepStmt = null;
        ResultSet rs = null;
        String query = SQLConstants.GET_LIFECYCLE_LIST_SQL;
        try {
            connection = LifecycleMgtDBUtil.getConnection();
            prepStmt = connection.prepareStatement(query);
            prepStmt.setInt(1, tenantId);
            rs = prepStmt.executeQuery();
            while (rs.next()) {
                lifecycleList.add(rs.getString(Constants.LIFECYCLE_LIST));
            }

        } catch (SQLException e) {
            handleException("Error while getting the lifecycle list for tenant " + tenantId, e);
        } finally {
            LifecycleMgtDBUtil.closeAllConnections(prepStmt, connection, rs);
        }
        return lifecycleList.toArray(new String[0]);
    }

    /**
     * Get lifecycle configuration.
     *
     * @param lcName                                Name of the lifecycle
     * @param tenantId
     * @return                                      Bean with content and name
     * @throws LifecycleManagerDatabaseException    If failed to get lifecycle config.
     */
    public LifecycleConfigBean getLifecycleConfig(String lcName, int tenantId)
            throws LifecycleManagerDatabaseException {
        LifecycleConfigBean lifecycleConfigBean = new LifecycleConfigBean();
        Connection connection = null;
        PreparedStatement prepStmt = null;
        ResultSet rs = null;
        String query = SQLConstants.GET_LIFECYCLE_CONFIG_SQL;
        try {
            connection = LifecycleMgtDBUtil.getConnection();
            prepStmt = connection.prepareStatement(query);
            prepStmt.setString(1, lcName);
            prepStmt.setInt(2, tenantId);
            rs = prepStmt.executeQuery();
            while (rs.next()) {
                lifecycleConfigBean.setLcName(rs.getString(Constants.LIFECYCLE_NAME));
                InputStream rawInputStream = rs.getBinaryStream(Constants.LIFECYCLE_CONTENT);
                lifecycleConfigBean.setLcContent(IOUtils.toString(rawInputStream));
            }

        } catch (SQLException e) {
            handleException("Error while getting the lifecycle content for lifecycle " + lcName, e);
        } catch (IOException e) {
            handleException("Error while converting lifecycle content stream to string", e);
        } finally {
            LifecycleMgtDBUtil.closeAllConnections(prepStmt, connection, rs);
        }
        return lifecycleConfigBean;
    }

    /**
     * Set initial lifecycle state.
     *
     * @param initialState                          Initial state provided in lifecycle config.
     * @param lcName                                Name of the lifecycle
     * @param user                                  The user who invoked the action. This will be used for
     *                                              auditing purposes.
     * @param tenantId
     * @return                                      UUID generated by framework which is stored as reference by
     *                                              external systems.
     * @throws LifecycleManagerDatabaseException    If failed to add initial lifecycle state.
     */
    public String addLifecycleState(String initialState, String lcName, String user, int tenantId)
            throws LifecycleManagerDatabaseException {
        Connection connection = null;
        PreparedStatement prepStmt1 = null;
        PreparedStatement prepStmt2 = null;
        ResultSet rs1 = null;
        String getLCIdQuery = SQLConstants.GET_LIFECYCLE_DEFINITION_ID_FROM_NAME_SQL;
        String addLCStateQuery = SQLConstants.ADD_LIFECYCLE_STATE_SQL;
        String uuid = null;
        int lcDefinitionId = -1;
        try {
            connection = LifecycleMgtDBUtil.getConnection();
            connection.setAutoCommit(false);
            prepStmt1 = connection.prepareStatement(getLCIdQuery);
            prepStmt1.setString(1, lcName);
            prepStmt1.setInt(2, tenantId);
            rs1 = prepStmt1.executeQuery();

            while (rs1.next()) {
                lcDefinitionId = rs1.getInt(Constants.LIFECYCLE_DEFINITION_ID);
            }
            if (lcDefinitionId == -1) {
                throw new LifecycleManagerDatabaseException("There is no lifecycle configuration with name " + lcName);
            }
            uuid = generateUUID();
            prepStmt2 = connection.prepareStatement(addLCStateQuery);
            prepStmt2.setString(1, uuid);
            prepStmt2.setInt(2, lcDefinitionId);
            prepStmt2.setString(3, initialState);
            prepStmt2.setInt(4, tenantId);
            prepStmt2.execute();
            connection.commit();
            addLifecycleHistory(uuid, null, initialState, user, tenantId);

        } catch (SQLException e) {
            uuid = null;
            try {
                connection.rollback();
            } catch (SQLException e1) {
                log.error("Error while roll back operation for setting initial lifecycle state :" + initialState, e);
            }
            handleException("Error while adding the lifecycle", e);
        } finally {
            LifecycleMgtDBUtil.closeAllConnections(prepStmt1, null, rs1);
            LifecycleMgtDBUtil.closeAllConnections(prepStmt2, connection, null);
        }
        return uuid;
    }

    /**
     * Change lifecycle state.
     *
     * @param lifecycleStateBean                    Bean containing lifecycle id and required state.
     * @param user                                  The user who invoked the action. This will be used for
     *                                              auditing purposes.
     * @throws LifecycleManagerDatabaseException    If failed to change lifecycle state.
     */
    public void changeLifecycleState(LifecycleStateBean lifecycleStateBean, String user)
            throws LifecycleManagerDatabaseException {
        Connection connection = null;
        PreparedStatement prepStmt = null;
        String updateLifecycleStateQuery = SQLConstants.UPDATE_LIFECYCLE_STATE_SQL;
        try {
            connection = LifecycleMgtDBUtil.getConnection();
            connection.setAutoCommit(false);
            prepStmt = connection.prepareStatement(updateLifecycleStateQuery);
            prepStmt.setString(1, lifecycleStateBean.getPostStatus());
            prepStmt.setString(2, lifecycleStateBean.getStateId());
            prepStmt.setInt(3, lifecycleStateBean.getTenantId());
            prepStmt.executeUpdate();
            connection.commit();
            addLifecycleHistory(lifecycleStateBean.getStateId(), lifecycleStateBean.getPreviousStatus(),
                    lifecycleStateBean.getPostStatus(), user, lifecycleStateBean.getTenantId());

        } catch (SQLException e) {
            try {
                connection.rollback();
            } catch (SQLException e1) {
                log.error("Error while roll back operation for lifecycle state change :" + lifecycleStateBean
                        .getPostStatus(), e);
            }
            handleException("Error while changing the lifecycle state to " + lifecycleStateBean.getPostStatus(), e);
        } finally {
            LifecycleMgtDBUtil.closeAllConnections(prepStmt, connection, null);
        }
    }

    /**
     * Get lifecycle state data for a particular uuid.
     *
     * @param uuid                                  Reference variable that maps lc data with external system.
     * @param tenantId
     * @return                                      Life cycle state bean with all the required information
     * @throws LifecycleManagerDatabaseException    If failed to get lifecycle state data.
     */
    public LifecycleStateBean getLifecycleStateDataFromId(String uuid, int tenantId)
            throws LifecycleManagerDatabaseException {
        LifecycleStateBean lifecycleStateBean = new LifecycleStateBean();
        Connection connection = null;
        PreparedStatement prepStmt = null;
        ResultSet rs = null;
        String getLifecycleNameFromIdQuery = SQLConstants.GET_LIFECYCLE_NAME_FROM_ID_SQL;
        try {
            connection = LifecycleMgtDBUtil.getConnection();
            prepStmt = connection.prepareStatement(getLifecycleNameFromIdQuery);
            prepStmt.setString(1, uuid);
            prepStmt.setInt(2, tenantId);
            rs = prepStmt.executeQuery();
            while (rs.next()) {
                lifecycleStateBean.setLcName(rs.getString(Constants.LIFECYCLE_NAME));
                lifecycleStateBean.setPostStatus(rs.getString(Constants.LIFECYCLE_STATUS));
            }
            lifecycleStateBean.setTenantId(tenantId);
            lifecycleStateBean.setStateId(uuid);

        } catch (SQLException e) {
            handleException("Error while getting the lifecycle state data for id" + uuid, e);
        } finally {
            LifecycleMgtDBUtil.closeAllConnections(prepStmt, connection, rs);
        }
        return lifecycleStateBean;
    }

    /**
     * Get all lifecycle configurations from lc database.
     *
     * @return                                      List of beans with content and name
     * @throws LifecycleManagerDatabaseException    If failed to get lifecycle config.
     */
    public LifecycleConfigBean[] getAllLifecycleConfigs() throws LifecycleManagerDatabaseException {
        List<LifecycleConfigBean> lifecycleConfigBeanList = new ArrayList<>();
        Connection connection = null;
        PreparedStatement prepStmt = null;
        ResultSet rs = null;
        String query = SQLConstants.GET_ALL_LIFECYCLE_CONFIGS_SQL;
        try {
            connection = LifecycleMgtDBUtil.getConnection();
            prepStmt = connection.prepareStatement(query);
            rs = prepStmt.executeQuery();
            while (rs.next()) {
                LifecycleConfigBean lifecycleConfigBean = new LifecycleConfigBean();
                lifecycleConfigBean.setLcName(rs.getString(Constants.LIFECYCLE_NAME));
                InputStream rawInputStream = rs.getBinaryStream(Constants.LIFECYCLE_CONTENT);
                lifecycleConfigBean.setLcContent(IOUtils.toString(rawInputStream));
                lifecycleConfigBean.setTenantId(rs.getInt(Constants.TENANT_ID));
                lifecycleConfigBeanList.add(lifecycleConfigBean);
            }

        } catch (SQLException e) {
            handleException("Error while getting the lifecycle list", e);
        } catch (IOException e) {
            handleException("Error while converting lifecycle content stream to string", e);
        } finally {
            LifecycleMgtDBUtil.closeAllConnections(prepStmt, connection, rs);
        }
        return lifecycleConfigBeanList.toArray(new LifecycleConfigBean[0]);
    }

    /**
     * Check lifecycle already exist with same name.
     *
     * @param lcName                                Name of the lifecycle
     * @param tenantId
     * @return                                      Bean with content and name
     * @throws LifecycleManagerDatabaseException
     */
    public boolean checkLifecycleExist(String lcName, int tenantId) throws LifecycleManagerDatabaseException {
        Connection connection = null;
        PreparedStatement prepStmt = null;
        ResultSet rs = null;
        String query = SQLConstants.CHECK_LIFECYCLE_EXIST_SQL;
        boolean result = false;
        try {
            connection = LifecycleMgtDBUtil.getConnection();
            prepStmt = connection.prepareStatement(query);
            prepStmt.setString(1, lcName);
            prepStmt.setInt(2, tenantId);
            rs = prepStmt.executeQuery();
            result = rs.next();
        } catch (SQLException e) {
            handleException("Error while checking for lifecycle exist with name" + lcName, e);
        } finally {
            LifecycleMgtDBUtil.closeAllConnections(prepStmt, connection, rs);
        }
        return result;
    }

    /**
     * Method used to update lifecycle history tables. Invoked when association and updating lifecycle.
     *
     * @param id                            UUID of the lifecycle state. (Associates with asset)
     * @param previousState                 Current state.
     * @param postState                     Target state.
     * @param user                          The user associated with lifecycle operation.
     * @param tenantId
     * @throws LifecycleManagerDatabaseException
     */
    private void addLifecycleHistory(String id, String previousState, String postState, String user, int tenantId) {
        Connection connection = null;
        PreparedStatement prepStmt = null;
        String query = SQLConstants.INSERT_LIFECYCLE_HISTORY_SQL;
        try {
            connection = LifecycleMgtDBUtil.getConnection();
            connection.setAutoCommit(false);
            String dbProductName = connection.getMetaData().getDatabaseProductName();
            boolean returnsGeneratedKeys = LifecycleMgtDBUtil.canReturnGeneratedKeys(dbProductName);
            if (returnsGeneratedKeys) {
                prepStmt = connection.prepareStatement(query, new String[] {
                        LifecycleMgtDBUtil.getConvertedAutoGeneratedColumnName(dbProductName, Constants.LC_ID) });
            } else {
                prepStmt = connection.prepareStatement(query);
            }
            //prepStmt = connection.prepareStatement(query);
            prepStmt.setString(1, id);
            prepStmt.setString(2, previousState);
            prepStmt.setString(3, postState);
            prepStmt.setString(4, user);
            prepStmt.setInt(5, tenantId);
            prepStmt.setTimestamp(6, new Timestamp(System.currentTimeMillis()));
            prepStmt.execute();
            connection.commit();
        } catch (SQLException e) {
            try {
                connection.rollback();
            } catch (SQLException e1) {
                log.error("Error while roll back operation for lifecycle history data insertion ", e);
            }
            log.error("Error while adding the lifecycle history ", e);
        } finally {
            LifecycleMgtDBUtil.closeAllConnections(prepStmt, connection, null);
        }
    }

    public boolean isLifecycleIsInUse(String lcName, int tenantId) throws LifecycleManagerDatabaseException {
        Connection connection = null;
        PreparedStatement prepStmt = null;
        ResultSet rs = null;
        boolean result = false;
        String query = SQLConstants.CHECK_LIFECYCLE_IN_USE;
        try {
            connection = LifecycleMgtDBUtil.getConnection();
            prepStmt = connection.prepareStatement(query);
            prepStmt.setString(1, lcName);
            prepStmt.setInt(2, tenantId);
            rs = prepStmt.executeQuery();
            result = rs.next();
        } catch (SQLException e) {
            handleException("Error while checking for lifecycle associated with assets" + lcName, e);
        } finally {
            LifecycleMgtDBUtil.closeAllConnections(prepStmt, connection, rs);
        }
        return result;
    }

    private void handleException(String msg, Throwable t) throws LifecycleManagerDatabaseException {
        log.error(msg, t);
        throw new LifecycleManagerDatabaseException(msg, t);
    }

    private String generateUUID() {
        return UUID.randomUUID().toString().replaceAll("-", "");
    }

    /**
     * This is an inner class to hold the instance of the LCMgtDAO.
     * The reason for writing it like this is to guarantee that only one instance would be created.
     */
    private static class LCMgtDAOHolder {

        private static final LifecycleMgtDAO INSTANCE = new LifecycleMgtDAO();
    }
}