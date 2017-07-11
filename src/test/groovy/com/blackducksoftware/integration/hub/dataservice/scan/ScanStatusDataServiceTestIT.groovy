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
package com.blackducksoftware.integration.hub.dataservice.scan

import org.junit.Assert
import org.junit.Test

import com.blackducksoftware.integration.hub.api.bom.BomImportRequestService
import com.blackducksoftware.integration.hub.rest.RestConnectionTestHelper
import com.blackducksoftware.integration.hub.service.HubServicesFactory
import com.blackducksoftware.integration.log.IntLogger

class ScanStatusDataServiceTestIT {
    private static final long FIVE_MINUTES = 5 * 60 * 1000;

    private final RestConnectionTestHelper restConnectionTestHelper = new RestConnectionTestHelper();

    @Test
    void testBdioImportForNewProject() {
        final HubServicesFactory hubServicesFactory = restConnectionTestHelper.createHubServicesFactory()
        final IntLogger logger = hubServicesFactory.getRestConnection().logger
        final BomImportRequestService bomImportRequestService = hubServicesFactory.createBomImportRequestService()
        final ScanStatusDataService scanStatusDataService = hubServicesFactory.createScanStatusDataService(logger, FIVE_MINUTES);

        // import the bdio
        final File file = restConnectionTestHelper.getFile('bdio/GRADLE_rest_backend_rest_backend_4_2_0_SNAPSHOT_bdio.jsonld')
        String contents = file.text
        String uniqueName = "rest-backend-${System.currentTimeMillis()}"
        String version = '4.2.0-SNAPSHOT'
        String alteredContents = contents.replace('"name": "rest-backend",', "\"name\": \"${uniqueName}\",")
        File uniquelyNamedBdio = File.createTempFile('uniquebdio', '.jsonld')
        uniquelyNamedBdio << alteredContents

        bomImportRequestService.importBomFile(uniquelyNamedBdio, 'application/ld+json');
        // wait for the scan to start/finish
        try {
            scanStatusDataService.assertBomImportScanStartedThenFinished(uniqueName, version);
        } catch (Throwable t) {
            Assert.fail("Nothing should have been thrown: " + t.getMessage())
        }
    }
}
