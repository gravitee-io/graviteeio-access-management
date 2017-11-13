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
package io.gravitee.am.gateway.handler.oauth2.provider.token;

import io.gravitee.am.identityprovider.api.User;
import io.gravitee.am.extensiongrant.api.ExtensionGrantProvider;
import io.gravitee.am.extensiongrant.api.exceptions.InvalidGrantException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.oauth2.provider.*;
import org.springframework.security.oauth2.provider.token.AbstractTokenGranter;
import org.springframework.security.oauth2.provider.token.AuthorizationServerTokenServices;
import org.springframework.util.Assert;

/**
 * @author Titouan COMPIEGNE (titouan.compiegne at graviteesource.com)
 * @author GraviteeSource Team
 */
public class CustomTokenGranter extends AbstractTokenGranter {

    private ExtensionGrantProvider extensionGrantProvider;

    public CustomTokenGranter(AuthorizationServerTokenServices tokenServices, ClientDetailsService clientDetailsService, OAuth2RequestFactory requestFactory, String grantType) {
        super(tokenServices, clientDetailsService, requestFactory, grantType);
    }

    @Override
    protected OAuth2Authentication getOAuth2Authentication(ClientDetails client, TokenRequest tokenRequest) {
        try {
            Authentication userAuth = null;
            User user = extensionGrantProvider.grant(convert(tokenRequest));
            if (user != null) {
                userAuth = new UsernamePasswordAuthenticationToken(user, "", AuthorityUtils.NO_AUTHORITIES);
            }

            OAuth2Request storedOAuth2Request = getRequestFactory().createOAuth2Request(client, tokenRequest);
            return new OAuth2Authentication(storedOAuth2Request, userAuth);
        } catch (InvalidGrantException e) {
            throw new org.springframework.security.oauth2.common.exceptions.InvalidGrantException(e.getMessage(), e);
        }
    }

    public void setExtensionGrantProvider(ExtensionGrantProvider extensionGrantProvider) {
        Assert.notNull(extensionGrantProvider, "Extension Grant provider must not be null");
        this.extensionGrantProvider = extensionGrantProvider;
    }

    private io.gravitee.am.repository.oauth2.model.request.TokenRequest convert(TokenRequest request) {
        io.gravitee.am.repository.oauth2.model.request.TokenRequest tokenRequest = new io.gravitee.am.repository.oauth2.model.request.TokenRequest();
        tokenRequest.setGrantType(request.getGrantType());
        tokenRequest.setClientId(request.getClientId());
        tokenRequest.setScope(request.getScope());
        tokenRequest.setRequestParameters(request.getRequestParameters());
        return tokenRequest;
    }
}
