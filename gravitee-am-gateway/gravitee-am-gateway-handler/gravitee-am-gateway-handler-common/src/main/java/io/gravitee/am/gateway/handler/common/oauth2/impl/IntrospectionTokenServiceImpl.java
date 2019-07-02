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
package io.gravitee.am.gateway.handler.common.oauth2.impl;

import io.gravitee.am.common.jwt.JWT;
import io.gravitee.am.common.jwt.exception.JWTException;
import io.gravitee.am.common.oauth2.exception.InvalidTokenException;
import io.gravitee.am.gateway.handler.common.client.ClientSyncService;
import io.gravitee.am.gateway.handler.common.jwt.JWTService;
import io.gravitee.am.gateway.handler.common.oauth2.IntrospectionTokenService;
import io.gravitee.am.repository.oauth2.api.AccessTokenRepository;
import io.reactivex.Maybe;
import io.reactivex.Single;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.Date;

/**
 * @author Titouan COMPIEGNE (titouan.compiegne at graviteesource.com)
 * @author GraviteeSource Team
 */
public class IntrospectionTokenServiceImpl implements IntrospectionTokenService {

    private static final long OFFLINE_VERIFICATION_TIMER_SECONDS = 10;

    @Autowired
    private JWTService jwtService;

    @Autowired
    private ClientSyncService clientService;

    @Autowired
    private AccessTokenRepository accessTokenRepository;

    @Override
    public Single<JWT> introspect(String token, boolean offlineVerification) {
        return jwtService.decode(token)
                .flatMapMaybe(jwt -> clientService.findByDomainAndClientId(jwt.getDomain(), jwt.getAud()))
                .switchIfEmpty(Maybe.error(new InvalidTokenException("Invalid or unknown client for this token")))
                .flatMapSingle(client -> jwtService.decodeAndVerify(token, client))
                .flatMap(jwt -> {
                    // Just check the JWT signature and JWT validity if offline verification option is enabled
                    // or if the token has just been created (could not be in database so far because of async database storing process delay)
                    if (offlineVerification || Instant.now().isBefore(Instant.ofEpochSecond(jwt.getIat() + OFFLINE_VERIFICATION_TIMER_SECONDS))) {
                        return Single.just(jwt);
                    }

                    // check if token is not revoked
                    return accessTokenRepository.findByToken(jwt.getJti())
                            .switchIfEmpty(Single.error(new InvalidTokenException("The token is invalid")))
                            .map(accessToken -> {
                                if (accessToken.getExpireAt().before(new Date())) {
                                    throw new InvalidTokenException("The token expired");
                                }
                                return jwt;
                            });
                })
                .onErrorResumeNext(ex -> {
                    if (ex instanceof JWTException) {
                        return Single.error(new InvalidTokenException(ex.getMessage(), ex));
                    }
                    return Single.error(ex);
                });
    }
}
