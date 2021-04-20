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
package io.gravitee.am.repository.mongodb.management;

import static com.mongodb.client.model.Filters.eq;

import com.mongodb.reactivestreams.client.MongoCollection;
import io.gravitee.am.common.utils.RandomString;
import io.gravitee.am.model.Factor;
import io.gravitee.am.repository.management.api.FactorRepository;
import io.gravitee.am.repository.mongodb.management.internal.model.FactorMongo;
import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Single;
import java.util.HashSet;
import java.util.Set;
import javax.annotation.PostConstruct;
import org.bson.Document;
import org.springframework.stereotype.Component;

/**
 * @author Titouan COMPIEGNE (titouan.compiegne at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class MongoFactorRepository extends AbstractManagementMongoRepository implements FactorRepository {

    private static final String FIELD_ID = "_id";
    private static final String FIELD_DOMAIN = "domain";
    private static final String FIELD_FACTOR_TYPE = "factorType";
    private MongoCollection<FactorMongo> factorsCollection;

    @PostConstruct
    public void init() {
        factorsCollection = mongoOperations.getCollection("factors", FactorMongo.class);
        super.createIndex(factorsCollection, new Document(FIELD_DOMAIN, 1));
        super.createIndex(factorsCollection, new Document(FIELD_DOMAIN, 1).append(FIELD_FACTOR_TYPE, 1));
    }

    @Override
    public Single<Set<Factor>> findAll() {
        return Observable.fromPublisher(factorsCollection.find()).map(this::convert).collect(HashSet::new, Set::add);
    }

    @Override
    public Single<Set<Factor>> findByDomain(String domain) {
        return Observable
            .fromPublisher(factorsCollection.find(eq(FIELD_DOMAIN, domain)))
            .map(this::convert)
            .collect(HashSet::new, Set::add);
    }

    @Override
    public Maybe<Factor> findByDomainAndFactorType(String domain, String factorType) {
        return Observable
            .fromPublisher(factorsCollection.find(new Document(FIELD_DOMAIN, domain).append(FIELD_FACTOR_TYPE, factorType)).first())
            .firstElement()
            .map(this::convert);
    }

    @Override
    public Maybe<Factor> findById(String factorId) {
        return Observable.fromPublisher(factorsCollection.find(eq(FIELD_ID, factorId)).first()).firstElement().map(this::convert);
    }

    @Override
    public Single<Factor> create(Factor item) {
        FactorMongo authenticator = convert(item);
        authenticator.setId(authenticator.getId() == null ? RandomString.generate() : authenticator.getId());
        return Single
            .fromPublisher(factorsCollection.insertOne(authenticator))
            .flatMap(success -> findById(authenticator.getId()).toSingle());
    }

    @Override
    public Single<Factor> update(Factor item) {
        FactorMongo authenticator = convert(item);
        return Single
            .fromPublisher(factorsCollection.replaceOne(eq(FIELD_ID, authenticator.getId()), authenticator))
            .flatMap(updateResult -> findById(authenticator.getId()).toSingle());
    }

    @Override
    public Completable delete(String id) {
        return Completable.fromPublisher(factorsCollection.deleteOne(eq(FIELD_ID, id)));
    }

    private Factor convert(FactorMongo factorMongo) {
        if (factorMongo == null) {
            return null;
        }

        Factor factor = new Factor();
        factor.setId(factorMongo.getId());
        factor.setName(factorMongo.getName());
        factor.setType(factorMongo.getType());
        factor.setFactorType(factorMongo.getFactorType());
        factor.setConfiguration(factorMongo.getConfiguration());
        factor.setDomain(factorMongo.getDomain());
        factor.setCreatedAt(factorMongo.getCreatedAt());
        factor.setUpdatedAt(factorMongo.getUpdatedAt());
        return factor;
    }

    private FactorMongo convert(Factor factor) {
        if (factor == null) {
            return null;
        }

        FactorMongo factorMongo = new FactorMongo();
        factorMongo.setId(factor.getId());
        factorMongo.setName(factor.getName());
        factorMongo.setType(factor.getType());
        factorMongo.setFactorType(factor.getFactorType());
        factorMongo.setConfiguration(factor.getConfiguration());
        factorMongo.setDomain(factor.getDomain());
        factorMongo.setCreatedAt(factor.getCreatedAt());
        factorMongo.setUpdatedAt(factor.getUpdatedAt());
        return factorMongo;
    }
}
