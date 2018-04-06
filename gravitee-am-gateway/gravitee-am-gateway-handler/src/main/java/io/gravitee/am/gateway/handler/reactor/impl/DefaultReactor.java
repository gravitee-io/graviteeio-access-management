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
package io.gravitee.am.gateway.handler.reactor.impl;

import io.gravitee.am.gateway.core.event.DomainEvent;
import io.gravitee.am.gateway.handler.reactor.Reactor;
import io.gravitee.am.gateway.handler.reactor.ReactorHandlerResolver;
import io.gravitee.am.gateway.handler.reactor.SecurityDomainHandlerRegistry;
import io.gravitee.am.gateway.handler.vertx.VertxSecurityDomainHandler;
import io.gravitee.am.model.Domain;
import io.gravitee.common.event.Event;
import io.gravitee.common.event.EventListener;
import io.gravitee.common.event.EventManager;
import io.gravitee.common.http.HttpHeaders;
import io.gravitee.common.http.HttpHeadersValues;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.common.service.AbstractService;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.core.buffer.Buffer;
import io.vertx.reactivex.core.http.HttpServerRequest;
import io.vertx.reactivex.core.http.HttpServerResponse;
import io.vertx.reactivex.ext.web.Router;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class DefaultReactor extends AbstractService implements Reactor, EventListener<DomainEvent, Domain> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultReactor.class);

    @Autowired
    private Environment environment;

    @Autowired
    private ReactorHandlerResolver reactorHandlerResolver;

    @Autowired
    private SecurityDomainHandlerRegistry securityDomainHandlerRegistry;

    @Autowired
    private EventManager eventManager;

    @Autowired
    private Vertx vertx;

    @Override
    public void route(HttpServerRequest request) {
        VertxSecurityDomainHandler securityDomainHandler = reactorHandlerResolver.resolve(request);
        if (securityDomainHandler != null) {
            Router.router(vertx).mountSubRouter(securityDomainHandler.contextPath(), securityDomainHandler.oauth2()).accept(request);
        } else {
            LOGGER.debug("No handler can be found for request {}, returning NOT_FOUND (404)", request.path());
            sendNotFound(request.response());
        }
    }

    @Override
    public void doStart() throws Exception {
        super.doStart();

        eventManager.subscribeForEvents(this, DomainEvent.class);
    }

    @Override
    public void doStop() throws Exception {
        super.doStop();

        securityDomainHandlerRegistry.clear();
    }

    @Override
    public void onEvent(Event<DomainEvent, Domain> event) {
        switch (event.type()) {
            case DEPLOY:
                securityDomainHandlerRegistry.create(event.content());
                break;
            case UPDATE:
                securityDomainHandlerRegistry.update(event.content());
                break;
            case UNDEPLOY:
                securityDomainHandlerRegistry.remove(event.content());
                break;
        }
    }

    private void sendNotFound(HttpServerResponse serverResponse) {
        // Send a NOT_FOUND HTTP status code (404)
        serverResponse.setStatusCode(HttpStatusCode.NOT_FOUND_404);

        String message = environment.getProperty("http.errors[404].message", "No context-path matches the request URI.");
        serverResponse.headers().set(HttpHeaders.CONTENT_LENGTH, Integer.toString(message.length()));
        serverResponse.headers().set(HttpHeaders.CONTENT_TYPE, "text/plain");
        serverResponse.headers().set(HttpHeaders.CONNECTION, HttpHeadersValues.CONNECTION_CLOSE);
        serverResponse.write(Buffer.buffer(message));

        serverResponse.end();
    }
}
