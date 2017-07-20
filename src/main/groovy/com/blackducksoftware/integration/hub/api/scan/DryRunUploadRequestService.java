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
package com.blackducksoftware.integration.hub.api.scan;

import static com.blackducksoftware.integration.hub.api.UrlConstants.SEGMENT_API;
import static com.blackducksoftware.integration.hub.api.UrlConstants.SEGMENT_SCANS;
import static com.blackducksoftware.integration.hub.api.UrlConstants.SEGMENT_V1;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import com.blackducksoftware.integration.exception.IntegrationException;
import com.blackducksoftware.integration.hub.model.response.DryRunUploadResponse;
import com.blackducksoftware.integration.hub.rest.RestConnection;
import com.blackducksoftware.integration.hub.service.HubResponseService;

import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class DryRunUploadRequestService extends HubResponseService {
    private static final List<String> DRY_RUN_UPLOAD_SEGMENTS = Arrays.asList(SEGMENT_API, SEGMENT_V1, SEGMENT_SCANS);

    private final RestConnection restConnection;

    public DryRunUploadRequestService(final RestConnection restConnection) {
        super(restConnection);
        this.restConnection = restConnection;
    }

    public DryRunUploadResponse uploadDryRunFile(final File dryRunFile) throws IntegrationException {
        final HttpUrl httpUrl = restConnection.createHttpUrl(DRY_RUN_UPLOAD_SEGMENTS);
        final Request request = restConnection.createPostRequest(httpUrl, RequestBody.create(MediaType.parse("application/json"), dryRunFile));
        try (Response response = restConnection.handleExecuteClientCall(request)) {
            String responseString;
            try {
                responseString = response.body().string();
            } catch (final IOException e) {
                throw new IntegrationException(e);
            }
            final DryRunUploadResponse uploadResponse = getGson().fromJson(responseString, DryRunUploadResponse.class);
            uploadResponse.json = responseString;
            return uploadResponse;
        }
    }
}
