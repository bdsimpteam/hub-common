/**
 * Hub Common
 *
 * Copyright (C) 2017 Black Duck Software, Inc.
 * http://www.blackducksoftware.com/
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
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
package com.blackducksoftware.integration.hub.dataservice.cli

import com.blackducksoftware.integration.exception.IntegrationException
import com.blackducksoftware.integration.hub.HubSupportHelper
import com.blackducksoftware.integration.hub.api.codelocation.CodeLocationRequestService
import com.blackducksoftware.integration.hub.api.nonpublic.HubVersionRequestService
import com.blackducksoftware.integration.hub.api.project.ProjectRequestService
import com.blackducksoftware.integration.hub.api.project.version.ProjectVersionRequestService
import com.blackducksoftware.integration.hub.api.scan.DryRunUploadRequestService
import com.blackducksoftware.integration.hub.api.scan.ScanSummaryRequestService
import com.blackducksoftware.integration.hub.builder.HubScanConfigBuilder
import com.blackducksoftware.integration.hub.cli.CLIDownloadService
import com.blackducksoftware.integration.hub.cli.SimpleScanService
import com.blackducksoftware.integration.hub.dataservice.phonehome.PhoneHomeDataService
import com.blackducksoftware.integration.hub.dataservice.scan.ScanStatusDataService
import com.blackducksoftware.integration.hub.exception.DoesNotExistException
import com.blackducksoftware.integration.hub.global.HubServerConfig
import com.blackducksoftware.integration.hub.model.request.ProjectRequest
import com.blackducksoftware.integration.hub.model.response.DryRunUploadResponse
import com.blackducksoftware.integration.hub.model.view.CodeLocationView
import com.blackducksoftware.integration.hub.model.view.ProjectVersionView
import com.blackducksoftware.integration.hub.model.view.ProjectView
import com.blackducksoftware.integration.hub.model.view.ScanSummaryView
import com.blackducksoftware.integration.hub.phonehome.IntegrationInfo
import com.blackducksoftware.integration.hub.scan.HubScanConfig
import com.blackducksoftware.integration.hub.util.HostnameHelper
import com.blackducksoftware.integration.log.IntLogger
import com.blackducksoftware.integration.util.CIEnvironmentVariables
import com.google.gson.Gson

public class CLIDataService {
    private final Gson gson

    private final IntLogger logger

    private final CIEnvironmentVariables ciEnvironmentVariables

    private final HubVersionRequestService hubVersionRequestService

    private final CLIDownloadService cliDownloadService

    private final PhoneHomeDataService phoneHomeDataService

    private final ProjectRequestService projectRequestService

    private final ProjectVersionRequestService projectVersionRequestService

    private final DryRunUploadRequestService dryRunUploadRequestService

    private final CodeLocationRequestService codeLocationRequestService

    private final ScanSummaryRequestService scanSummaryRequestService

    private final ScanStatusDataService scanStatusDataService

    private HubSupportHelper hubSupportHelper

    private ProjectVersionView version

    public CLIDataService(final IntLogger logger, final Gson gson, final CIEnvironmentVariables ciEnvironmentVariables,
    final HubVersionRequestService hubVersionRequestService,
    final CLIDownloadService cliDownloadService, final PhoneHomeDataService phoneHomeDataService,
    final ProjectRequestService projectRequestService, final ProjectVersionRequestService projectVersionRequestService,
    final DryRunUploadRequestService dryRunUploadRequestService, final CodeLocationRequestService codeLocationRequestService,
    final ScanSummaryRequestService scanSummaryRequestService, final ScanStatusDataService scanStatusDataService) {
        this.gson = gson
        this.logger = logger
        this.ciEnvironmentVariables = ciEnvironmentVariables
        this.hubVersionRequestService = hubVersionRequestService
        this.cliDownloadService = cliDownloadService
        this.phoneHomeDataService = phoneHomeDataService
        this.projectRequestService = projectRequestService
        this.projectVersionRequestService = projectVersionRequestService
        this.dryRunUploadRequestService = dryRunUploadRequestService
        this.codeLocationRequestService = codeLocationRequestService
        this.scanSummaryRequestService = scanSummaryRequestService
        this.scanStatusDataService = scanStatusDataService
    }

    public ProjectVersionView installAndRunControlledScan(final HubServerConfig hubServerConfig,
            final HubScanConfig hubScanConfig, final ProjectRequest projectRequest, boolean shouldWaitForScansFinished, final IntegrationInfo integrationInfo)
    throws IntegrationException {
        preScan(hubServerConfig, hubScanConfig, projectRequest, integrationInfo)
        final File[] dryRunFiles = runControlledScan(hubServerConfig, hubScanConfig)
        postScan(hubScanConfig, dryRunFiles, projectRequest, shouldWaitForScansFinished)
        return version
    }

    private void printConfiguration(final HubScanConfig hubScanConfig, final ProjectRequest projectRequest) {
        logger.alwaysLog("--> Log Level : ${logger.getLogLevel().name()}")
        logger.alwaysLog("--> Using Hub Project Name : ${projectRequest?.getName()}, Version : ${projectRequest?.getVersionRequest()?.getVersionName()}, Phase : ${projectRequest?.getVersionRequest()?.getPhase()}, Distribution : ${projectRequest?.getVersionRequest()?.getDistribution()}")
        hubScanConfig.print(logger)
    }

    private void preScan(final HubServerConfig hubServerConfig,
            final HubScanConfig hubScanConfig, final ProjectRequest projectRequest, final IntegrationInfo integrationInfo) throws IntegrationException {
        final String localHostName = HostnameHelper.getMyHostname()
        logger.info("Running on machine : " + localHostName)
        printConfiguration(hubScanConfig, projectRequest)
        final String hubVersion = hubVersionRequestService.getHubVersion()
        cliDownloadService.performInstallation(hubScanConfig.getToolsDir(), ciEnvironmentVariables,
                hubServerConfig.getHubUrl().toString(),
                hubVersion, localHostName)
        phoneHomeDataService.phoneHome(hubServerConfig, integrationInfo, hubVersion)

        hubSupportHelper = new HubSupportHelper()
        hubSupportHelper.checkHubSupport(hubVersionRequestService, logger)

        if (!hubScanConfig.isDryRun()) {
            getProjectVersion(projectRequest)
        }
    }

    private void postScan(final HubScanConfig hubScanConfig, final File[] dryRunFiles, final ProjectRequest projectRequest, boolean shouldWaitForScansFinished)
    throws IntegrationException {
        if (!hubScanConfig.isDryRun()) {
            final List<CodeLocationView> codeLocationViews = new ArrayList<>()
            final List<String> scanSummaryIds = new ArrayList<>();
            dryRunFiles.each{ dryRunFile ->
                final DryRunUploadResponse uploadResponse = dryRunUploadRequestService.uploadDryRunFile(dryRunFile)
                if (uploadResponse != null && uploadResponse?.id != null ) {
                    scanSummaryIds.add(uploadResponse.id.toString())
                }
                if (uploadResponse != null && uploadResponse?.scanGroup != null && uploadResponse?.scanGroup?.codeLocationKey != null) {
                    final CodeLocationView codeLocationView = codeLocationRequestService.getCodeLocationById(uploadResponse.scanGroup.codeLocationKey.entityId)
                    codeLocationViews.add(codeLocationView)
                    codeLocationRequestService.mapCodeLocation(codeLocationView, version)
                }
            }
            cleanupCodeLocations(codeLocationViews, hubScanConfig)
            if (shouldWaitForScansFinished) {
                final List<ScanSummaryView> pendingScans = new ArrayList<>()
                scanSummaryIds.each{
                    pendingScans.add(scanSummaryRequestService.getScanSummaryViewById(it))
                }
                logger.debug("Waiting for the Bom to be updated.");
                scanStatusDataService.assertBomImportScansFinished(pendingScans)
            }
        }
    }

    private File[] runControlledScan(final HubServerConfig hubServerConfig,
            final HubScanConfig hubScanConfig) throws IntegrationException {
        final SimpleScanService simpleScanService = new SimpleScanService(logger, gson, hubServerConfig, hubSupportHelper,
                ciEnvironmentVariables, getControlledScanConfig(hubScanConfig), null, null)
        simpleScanService.setupAndExecuteScan()
        if (hubScanConfig.isCleanupLogsOnSuccess()) {
            cleanUpLogFiles(simpleScanService)
        }
        return simpleScanService.getDryRunFiles()
    }

    private HubScanConfig getControlledScanConfig(final HubScanConfig originalHubScanConfig) {
        final HubScanConfigBuilder builder = new HubScanConfigBuilder()
        builder.setCodeLocationAlias(originalHubScanConfig.getCodeLocationAlias())
        builder.setVerbose(originalHubScanConfig.isVerbose())
        builder.setDryRun(true)
        builder.setExcludePatterns(originalHubScanConfig.getExcludePatterns())
        builder.setScanMemory(originalHubScanConfig.getScanMemory())
        builder.setToolsDir(originalHubScanConfig.getToolsDir())
        builder.setWorkingDirectory(originalHubScanConfig.getWorkingDirectory())
        builder.addAllScanTargetPaths(new ArrayList<>(originalHubScanConfig.getScanTargetPaths()))
        return builder.build()
    }

    private void cleanUpLogFiles(final SimpleScanService simpleScanService) {
        final File standardOutputFile = simpleScanService.getStandardOutputFile()
        if (standardOutputFile != null && standardOutputFile.exists()) {
            standardOutputFile.delete()
        }
        final File cliLogDirectory = simpleScanService.getCLILogDirectory()
        if (cliLogDirectory != null && cliLogDirectory.exists()) {
            for (final File log : cliLogDirectory.listFiles()) {
                log.delete()
            }
            cliLogDirectory.delete()
        }
    }

    private void cleanupCodeLocations(final List<CodeLocationView> codeLocationsFromCurentScan, final HubScanConfig hubScanConfig) throws IntegrationException {
        if (hubScanConfig.isDeletePreviousCodeLocations() || hubScanConfig.isUnmapPreviousCodeLocations()) {
            final List<CodeLocationView> codeLocationsNotJustScanned = getCodeLocationsNotJustScanned(version, codeLocationsFromCurentScan)
            if (hubScanConfig.isDeletePreviousCodeLocations()) {
                codeLocationRequestService.deleteCodeLocations(codeLocationsNotJustScanned)
            } else if (hubScanConfig.isUnmapPreviousCodeLocations()) {
                codeLocationRequestService.unmapCodeLocations(codeLocationsNotJustScanned)
            }
        }
    }

    private List<CodeLocationView> getCodeLocationsNotJustScanned(final ProjectVersionView version,
            final List<CodeLocationView> codeLocationsFromCurentScan) throws IntegrationException {
        final List<CodeLocationView> codeLocationsMappedToVersion = codeLocationRequestService.getAllCodeLocationsForProjectVersion(version)
        return getCodeLocationsNotJustScanned(codeLocationsMappedToVersion, codeLocationsFromCurentScan)
    }

    private List<CodeLocationView> getCodeLocationsNotJustScanned(final List<CodeLocationView> codeLocationsMappedToVersion,
            final List<CodeLocationView> codeLocationsFromCurentScan) {
        final List<CodeLocationView> codeLocationsNotJustScanned = new ArrayList<>()
        for (final CodeLocationView codeLocationItemMappedToVersion : codeLocationsMappedToVersion) {
            boolean partOfCurrentScan = false
            for (final CodeLocationView codeLocationFromCurentScan : codeLocationsFromCurentScan) {
                if (codeLocationItemMappedToVersion.url.equals(codeLocationFromCurentScan.url)) {
                    partOfCurrentScan = true
                    break
                }
            }
            if (!partOfCurrentScan) {
                codeLocationsNotJustScanned.add(codeLocationItemMappedToVersion)
            }
        }
        return codeLocationsNotJustScanned
    }

    private void getProjectVersion( final ProjectRequest projectRequest) throws IntegrationException {
        ProjectView project = null
        try {
            project = projectRequestService.getProjectByName(projectRequest.getName())
        } catch (final DoesNotExistException e) {
            final String projectURL = projectRequestService.createHubProject(projectRequest)
            project = projectRequestService.getItem(projectURL, ProjectView.class)
        }
        try {
            version = projectVersionRequestService.getProjectVersion(project, projectRequest.getVersionRequest().getVersionName())
        } catch (final DoesNotExistException e) {
            final String versionURL = projectVersionRequestService.createHubVersion(project, projectRequest.getVersionRequest())
            version = projectVersionRequestService.getItem(versionURL, ProjectVersionView.class)
        }
    }
}
