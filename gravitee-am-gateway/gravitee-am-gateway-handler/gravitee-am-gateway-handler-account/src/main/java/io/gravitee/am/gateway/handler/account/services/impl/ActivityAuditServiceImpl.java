/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.am.gateway.handler.account.services.impl;

import io.gravitee.am.gateway.handler.account.services.ActivityAuditService;
import io.gravitee.am.gateway.handler.common.audit.AuditReporterManager;
import io.gravitee.am.model.ReferenceType;
import io.gravitee.am.model.common.Page;
import io.gravitee.am.reporter.api.audit.AuditReportableCriteria;
import io.gravitee.am.reporter.api.audit.model.Audit;
import io.gravitee.am.reporter.api.provider.Reporter;
import io.reactivex.Single;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Objects;

import static io.gravitee.am.service.reporter.vertx.EventBusReporterWrapper.logger;

public class ActivityAuditServiceImpl implements ActivityAuditService {

    public static final Logger logger = LoggerFactory.getLogger(ActivityAuditServiceImpl.class);

    @Autowired
    private AuditReporterManager auditReporterManager;

    @Override
    public Single<Page<Audit>> search(ReferenceType referenceType, String referenceId, AuditReportableCriteria criteria, int page, int size) {
        try {
            Single<Page<Audit>> reporter = getReporter().search(referenceType, referenceId, criteria, page, size);
            return reporter.map(result -> {
                if(Objects.isNull(result) || Objects.isNull(result.getData())){
                    return new Page<>(new ArrayList<>(), 0, 0);
                }
                return result;
            });
        } catch (Exception ex) {
            logger.error("An error occurs during audits search for {}}: {}", referenceType, referenceId, ex);
            return Single.error(ex);
        }
    }

    private Reporter getReporter() {
        return auditReporterManager.getReporter();
    }
}
