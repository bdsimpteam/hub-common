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
package com.blackducksoftware.integration.hub.scan

import org.junit.Assert
import org.junit.BeforeClass
import org.junit.Test

import com.blackducksoftware.integration.hub.api.codelocation.CodeLocationRequestService
import com.blackducksoftware.integration.hub.api.scan.DryRunUploadRequestService
import com.blackducksoftware.integration.hub.model.response.DryRunUploadResponse
import com.blackducksoftware.integration.hub.model.view.CodeLocationView
import com.blackducksoftware.integration.hub.rest.RestConnectionTestHelper
import com.blackducksoftware.integration.hub.rest.exception.IntegrationRestException
import com.blackducksoftware.integration.hub.service.HubServicesFactory
import com.blackducksoftware.integration.log.IntLogger
import com.blackducksoftware.integration.log.LogLevel
import com.blackducksoftware.integration.log.PrintStreamIntLogger

class DryRunUploadRequestServiceTestIT {

    private static final RestConnectionTestHelper restConnectionTestHelper = new RestConnectionTestHelper();

    private IntLogger logger = new PrintStreamIntLogger(System.out, LogLevel.INFO)

    private static File dryRunFile;

    @BeforeClass
    public static void init(){
        final ClassLoader classLoader = Thread.currentThread().getContextClassLoader()
        dryRunFile = new File(classLoader.getResource('dryRun.json').getFile())
    }

    @Test
    public void testDryRunUpload(){
        HubServicesFactory services = restConnectionTestHelper.createHubServicesFactory(logger)
        DryRunUploadRequestService dryRunUploadRequestService = services.createDryRunUploadRequestService()
        DryRunUploadResponse response = dryRunUploadRequestService.uploadDryRunFile(dryRunFile)
        Assert.assertNotNull(response)

        CodeLocationRequestService codeLocationRequestService = services.createCodeLocationRequestService()
        CodeLocationView codeLocationView = codeLocationRequestService.getCodeLocationById(response.scanGroup.codeLocationKey.entityId)
        Assert.assertNotNull(codeLocationView)

        //cleanup
        codeLocationRequestService.deleteCodeLocation(codeLocationView)
        try{
            codeLocationRequestService.getCodeLocationById(response.scanGroup.codeLocationKey.entityId)
            Assert.fail('This should have thrown an exception')
        } catch (IntegrationRestException e){
            Assert.assertEquals(404, e.getHttpStatusCode())
        }
    }
}
