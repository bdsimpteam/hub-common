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
package com.blackducksoftware.integration.hub.dataservice.notification;

import java.util.List;
import java.util.SortedSet;

import com.blackducksoftware.integration.hub.dataservice.notification.item.NotificationContentItem;

public class NotificationResults {
    private final SortedSet<NotificationContentItem> notificationContentItems;

    private final List<Exception> exceptions;

    public NotificationResults(final SortedSet<NotificationContentItem> notificationContentItems, final List<Exception> exceptions) {
        super();
        this.notificationContentItems = notificationContentItems;
        this.exceptions = exceptions;
    }

    public SortedSet<NotificationContentItem> getNotificationContentItems() {
        return notificationContentItems;
    }

    public List<Exception> getExceptions() {
        return exceptions;
    }

    public boolean isError() {
        if ((exceptions != null) && (exceptions.size() > 0)) {
            return true;
        }
        return false;
    }

    @Override
    public String toString() {
        return "NotificationResults [notificationContentItems=" + notificationContentItems + ", errorMessages=" + exceptions + "]";
    }

}