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
package io.gravitee.am.service.reporter.builder.management;

import io.gravitee.am.common.audit.EntityType;
import io.gravitee.am.common.audit.EventType;
import io.gravitee.am.model.Group;
import io.gravitee.am.model.ReferenceType;

/**
 * @author Titouan COMPIEGNE (titouan.compiegne at graviteesource.com)
 * @author GraviteeSource Team
 */
public class GroupAuditBuilder extends ManagementAuditBuilder<GroupAuditBuilder> {

    public GroupAuditBuilder() {
        super();
    }

    public GroupAuditBuilder group(Group group) {
        if (EventType.GROUP_CREATED.equals(getType())
                || EventType.GROUP_UPDATED.equals(getType())
                || EventType.GROUP_ROLES_ASSIGNED.equals(getType())) {
            setNewValue(group);
        }

        referenceType(group.getReferenceType());
        referenceId(group.getReferenceId());

        setTarget(group.getId(), EntityType.GROUP, null, group.getName(),
                group.getReferenceType() == ReferenceType.DOMAIN ? group.getReferenceId() : null);
        return this;
    }
}
