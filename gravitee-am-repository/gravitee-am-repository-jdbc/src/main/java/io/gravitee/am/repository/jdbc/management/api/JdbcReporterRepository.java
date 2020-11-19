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
package io.gravitee.am.repository.jdbc.management.api;

import io.gravitee.am.common.utils.RandomString;
import io.gravitee.am.model.Reporter;
import io.gravitee.am.repository.jdbc.management.AbstractJdbcRepository;
import io.gravitee.am.repository.jdbc.management.api.model.JdbcReporter;
import io.gravitee.am.repository.jdbc.management.api.spring.SpringReporterRepository;
import io.gravitee.am.repository.management.api.ReporterRepository;
import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.reactivex.Single;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.util.List;

import static reactor.adapter.rxjava.RxJava2Adapter.monoToSingle;

/**
 * @author Eric LELEU (eric.leleu at graviteesource.com)
 * @author GraviteeSource Team
 */
@Repository
public class JdbcReporterRepository extends AbstractJdbcRepository implements ReporterRepository {

    @Autowired
    protected SpringReporterRepository reporterRepository;

    protected Reporter toEntity(JdbcReporter entity) {
        return mapper.map(entity, Reporter.class);
    }

    protected JdbcReporter toJdbcEntity(Reporter entity) {
        return mapper.map(entity, JdbcReporter.class);
    }

    @Override
    public Single<List<Reporter>> findAll() {
        LOGGER.debug("findAll()");
        return reporterRepository.findAll()
                .map(this::toEntity)
                .toList()
                .doOnError(error -> LOGGER.error("Unable to retrieve all reporters", error));
    }

    @Override
    public Single<List<Reporter>> findByDomain(String domain) {
        LOGGER.debug("findByDomain({})", domain);
        return reporterRepository.findByDomain(domain)
                .map(this::toEntity)
                .toList()
                .doOnError(error -> LOGGER.error("Unable to retrieve all reporters for domain {}", domain, error));
    }

    @Override
    public Maybe<Reporter> findById(String id) {
        LOGGER.debug("findById({})", id);
        return reporterRepository.findById(id)
                .map(this::toEntity)
                .doOnError(error -> LOGGER.error("Unable to retrieve the reporter with id {}", id, error));
    }

    @Override
    public Single<Reporter> create(Reporter item) {
        item.setId(item.getId() == null ? RandomString.generate() : item.getId());
        LOGGER.debug("Create Reporter with id {}", item.getId());

        Mono<Integer> insertResult = dbClient.insert()
                .into(JdbcReporter.class)
                .using(toJdbcEntity(item))
                .fetch().rowsUpdated();

        return monoToSingle(insertResult).flatMap((i) -> this.findById(item.getId()).toSingle())
                .doOnError((error) -> LOGGER.error("Unable to create reporter with id {}", item.getId(), error));
    }

    @Override
    public Single<Reporter> update(Reporter item) {
        LOGGER.debug("Update reporter with id '{}'", item.getId());
        return reporterRepository.save(toJdbcEntity(item))
                .map(this::toEntity)
                .doOnError(error -> LOGGER.error("unable to update reporter with id {}", item.getId()));
    }

    @Override
    public Completable delete(String id) {
        LOGGER.debug("delete({})", id);
        return reporterRepository.deleteById(id)
                .doOnError(error -> LOGGER.error("Unable to delete the reporter with id {}", id, error));
    }

}
