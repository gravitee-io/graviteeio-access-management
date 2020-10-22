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
package io.gravitee.am.gateway.handler.root;

import io.gravitee.am.common.policy.ExtensionPoint;
import io.gravitee.am.gateway.handler.api.ProtocolProvider;
import io.gravitee.am.gateway.handler.common.auth.idp.IdentityProviderManager;
import io.gravitee.am.gateway.handler.common.auth.user.UserAuthenticationManager;
import io.gravitee.am.gateway.handler.common.certificate.CertificateManager;
import io.gravitee.am.gateway.handler.common.client.ClientSyncService;
import io.gravitee.am.gateway.handler.common.jwt.JWTService;
import io.gravitee.am.gateway.handler.common.vertx.web.auth.provider.UserAuthProvider;
import io.gravitee.am.gateway.handler.common.vertx.web.endpoint.ErrorEndpoint;
import io.gravitee.am.gateway.handler.common.vertx.web.handler.PolicyChainHandler;
import io.gravitee.am.gateway.handler.common.vertx.web.handler.impl.CookieHandler;
import io.gravitee.am.gateway.handler.common.vertx.web.handler.impl.CookieSessionHandler;
import io.gravitee.am.gateway.handler.factor.FactorManager;
import io.gravitee.am.gateway.handler.root.resources.auth.handler.FormLoginHandler;
import io.gravitee.am.gateway.handler.root.resources.auth.handler.SocialAuthHandler;
import io.gravitee.am.gateway.handler.root.resources.auth.provider.SocialAuthenticationProvider;
import io.gravitee.am.gateway.handler.root.resources.endpoint.login.LoginCallbackEndpoint;
import io.gravitee.am.gateway.handler.root.resources.endpoint.login.LoginEndpoint;
import io.gravitee.am.gateway.handler.root.resources.endpoint.login.LoginSSOPOSTEndpoint;
import io.gravitee.am.gateway.handler.root.resources.endpoint.logout.LogoutEndpoint;
import io.gravitee.am.gateway.handler.root.resources.endpoint.mfa.MFAChallengeEndpoint;
import io.gravitee.am.gateway.handler.root.resources.endpoint.mfa.MFAEnrollEndpoint;
import io.gravitee.am.gateway.handler.root.resources.endpoint.user.password.ForgotPasswordEndpoint;
import io.gravitee.am.gateway.handler.root.resources.endpoint.user.password.ForgotPasswordSubmissionEndpoint;
import io.gravitee.am.gateway.handler.root.resources.endpoint.user.password.ResetPasswordEndpoint;
import io.gravitee.am.gateway.handler.root.resources.endpoint.user.password.ResetPasswordSubmissionEndpoint;
import io.gravitee.am.gateway.handler.root.resources.endpoint.user.register.RegisterConfirmationEndpoint;
import io.gravitee.am.gateway.handler.root.resources.endpoint.user.register.RegisterConfirmationSubmissionEndpoint;
import io.gravitee.am.gateway.handler.root.resources.endpoint.user.register.RegisterEndpoint;
import io.gravitee.am.gateway.handler.root.resources.endpoint.user.register.RegisterSubmissionEndpoint;
import io.gravitee.am.gateway.handler.root.resources.endpoint.webauthn.WebAuthnLoginEndpoint;
import io.gravitee.am.gateway.handler.root.resources.endpoint.webauthn.WebAuthnRegisterEndpoint;
import io.gravitee.am.gateway.handler.root.resources.endpoint.webauthn.WebAuthnResponseEndpoint;
import io.gravitee.am.gateway.handler.root.resources.handler.client.ClientRequestParseHandler;
import io.gravitee.am.gateway.handler.root.resources.handler.error.ErrorHandler;
import io.gravitee.am.gateway.handler.root.resources.handler.login.*;
import io.gravitee.am.gateway.handler.root.resources.handler.user.PasswordPolicyRequestParseHandler;
import io.gravitee.am.gateway.handler.root.resources.handler.user.UserTokenRequestParseHandler;
import io.gravitee.am.gateway.handler.root.resources.handler.user.password.ForgotPasswordAccessHandler;
import io.gravitee.am.gateway.handler.root.resources.handler.user.password.ForgotPasswordSubmissionRequestParseHandler;
import io.gravitee.am.gateway.handler.root.resources.handler.user.password.ResetPasswordRequestParseHandler;
import io.gravitee.am.gateway.handler.root.resources.handler.user.password.ResetPasswordSubmissionRequestParseHandler;
import io.gravitee.am.gateway.handler.root.resources.handler.user.register.RegisterAccessHandler;
import io.gravitee.am.gateway.handler.root.resources.handler.user.register.RegisterConfirmationRequestParseHandler;
import io.gravitee.am.gateway.handler.root.resources.handler.user.register.RegisterConfirmationSubmissionRequestParseHandler;
import io.gravitee.am.gateway.handler.root.resources.handler.user.register.RegisterSubmissionRequestParseHandler;
import io.gravitee.am.gateway.handler.root.resources.handler.webauthn.WebAuthnAccessHandler;
import io.gravitee.am.gateway.handler.root.service.user.UserService;
import io.gravitee.am.gateway.handler.vertx.auth.webauthn.WebAuthn;
import io.gravitee.am.model.Domain;
import io.gravitee.am.service.AuditService;
import io.gravitee.am.service.TokenService;
import io.gravitee.am.service.authentication.crypto.password.PasswordValidator;
import io.gravitee.common.service.AbstractService;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpMethod;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.RoutingContext;
import io.vertx.reactivex.ext.web.handler.*;
import io.vertx.reactivex.ext.web.templ.thymeleaf.ThymeleafTemplateEngine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

/**
 * @author Titouan COMPIEGNE (titouan.compiegne at graviteesource.com)
 * @author GraviteeSource Team
 */
public class RootProvider extends AbstractService<ProtocolProvider> implements ProtocolProvider {

    @Autowired
    private Vertx vertx;

    @Autowired
    private Router router;

    @Autowired
    private Domain domain;

    @Autowired
    private IdentityProviderManager identityProviderManager;

    @Autowired
    private UserAuthenticationManager userAuthenticationManager;

    @Autowired
    private UserAuthProvider userAuthProvider;

    @Autowired
    private ThymeleafTemplateEngine thymeleafTemplateEngine;

    @Autowired
    private PasswordValidator passwordValidator;

    @Autowired
    private AuditService auditService;

    @Autowired
    private ClientSyncService clientSyncService;

    @Autowired
    private CookieSessionHandler sessionHandler;

    @Autowired
    private CookieHandler cookieHandler;

    @Autowired
    private CSRFHandler csrfHandler;

    @Autowired
    @Qualifier("managementUserService")
    private UserService userService;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private PolicyChainHandler policyChainHandler;

    @Autowired
    private FactorManager factorManager;

    @Autowired
    private WebAuthn webAuthn;

    @Autowired
    private JWTService jwtService;

    @Autowired
    private CertificateManager certificateManager;

    @Override
    protected void doStart() throws Exception {
        super.doStart();

        // create the root router
        final Router rootRouter = Router.router(vertx);

        // body handler
        bodyHandler(rootRouter);

        // static handler
        staticHandler(rootRouter);

        // session cookie handler
        sessionAndCookieHandler(rootRouter);

        // CSRF handler
        csrfHandler(rootRouter);

        // Root policy chain handler
        rootRouter.route().handler(policyChainHandler.create(ExtensionPoint.ROOT));

        // common handler
        Handler<RoutingContext> userTokenRequestParseHandler = new UserTokenRequestParseHandler(userService);
        ClientRequestParseHandler clientRequestParseHandler = new ClientRequestParseHandler(clientSyncService);
        clientRequestParseHandler.setRequired(true);
        Handler<RoutingContext> clientRequestParseHandlerOptional = new ClientRequestParseHandler(clientSyncService);
        Handler<RoutingContext> passwordPolicyRequestParseHandler = new PasswordPolicyRequestParseHandler(passwordValidator);

        // login route
        rootRouter.get("/login")
                .handler(clientRequestParseHandler)
                .handler(new LoginSocialAuthenticationHandler(identityProviderManager, jwtService, certificateManager))
                .handler(new LoginEndpoint(thymeleafTemplateEngine, domain));
        rootRouter.post("/login")
                .handler(FormLoginHandler.create(userAuthProvider));

        // logout route
        rootRouter.route("/logout").handler(new LogoutEndpoint(domain, tokenService, auditService));

        // SSO/Social login route
        Handler<RoutingContext> socialAuthHandler = SocialAuthHandler.create(new SocialAuthenticationProvider(userAuthenticationManager));
        Handler<RoutingContext> loginCallbackParseHandler = new LoginCallbackParseHandler(clientSyncService, identityProviderManager, jwtService, certificateManager);
        Handler<RoutingContext> loginCallbackOpenIDConnectFlowHandler = new LoginCallbackOpenIDConnectFlowHandler(thymeleafTemplateEngine);
        Handler<RoutingContext> loginCallbackFailureHandler = new LoginCallbackFailureHandler();
        Handler<RoutingContext> loginCallbackEndpoint = new LoginCallbackEndpoint();
        Handler<RoutingContext> loginSSOPOSTEndpoint = new LoginSSOPOSTEndpoint(thymeleafTemplateEngine);
        rootRouter.get("/login/callback")
                .handler(loginCallbackParseHandler)
                .handler(loginCallbackOpenIDConnectFlowHandler)
                .handler(socialAuthHandler)
                .handler(loginCallbackEndpoint)
                .failureHandler(loginCallbackFailureHandler);
        rootRouter.post("/login/callback")
                .handler(loginCallbackParseHandler)
                .handler(loginCallbackOpenIDConnectFlowHandler)
                .handler(socialAuthHandler)
                .handler(loginCallbackEndpoint)
                .failureHandler(loginCallbackFailureHandler);
        rootRouter.get("/login/SSO/POST")
                .handler(loginSSOPOSTEndpoint);

        // MFA route
        rootRouter.route("/mfa/enroll")
                .handler(clientRequestParseHandler)
                .handler(new MFAEnrollEndpoint(factorManager, thymeleafTemplateEngine));
        rootRouter.route("/mfa/challenge")
                .handler(clientRequestParseHandler)
                .handler(new MFAChallengeEndpoint(factorManager, userService, thymeleafTemplateEngine));

        // WebAuthn route
        Handler<RoutingContext> webAuthnAccessHandler = new WebAuthnAccessHandler(domain);
        rootRouter.route("/webauthn/register")
                .handler(clientRequestParseHandler)
                .handler(webAuthnAccessHandler)
                .handler(new WebAuthnRegisterEndpoint(domain, userAuthenticationManager, webAuthn, thymeleafTemplateEngine));
        rootRouter.route("/webauthn/login")
                .handler(clientRequestParseHandler)
                .handler(webAuthnAccessHandler)
                .handler(new WebAuthnLoginEndpoint(domain, userAuthenticationManager, webAuthn, thymeleafTemplateEngine));
        rootRouter.post("/webauthn/response")
                .handler(clientRequestParseHandler)
                .handler(webAuthnAccessHandler)
                .handler(new WebAuthnResponseEndpoint(userAuthenticationManager, webAuthn));

        // Registration route
        Handler<RoutingContext> registerAccessHandler = new RegisterAccessHandler(domain);
        rootRouter.route(HttpMethod.GET, "/register")
                .handler(clientRequestParseHandler)
                .handler(registerAccessHandler)
                .handler(new RegisterEndpoint(thymeleafTemplateEngine));
        rootRouter.route(HttpMethod.POST, "/register")
                .handler(new RegisterSubmissionRequestParseHandler())
                .handler(clientRequestParseHandlerOptional)
                .handler(registerAccessHandler)
                .handler(passwordPolicyRequestParseHandler)
                .handler(new RegisterSubmissionEndpoint(userService, domain));
        rootRouter.route(HttpMethod.GET,"/confirmRegistration")
                .handler(new RegisterConfirmationRequestParseHandler(userService))
                .handler(clientRequestParseHandlerOptional)
                .handler(new RegisterConfirmationEndpoint(thymeleafTemplateEngine));
        rootRouter.route(HttpMethod.POST, "/confirmRegistration")
                .handler(new RegisterConfirmationSubmissionRequestParseHandler())
                .handler(userTokenRequestParseHandler)
                .handler(passwordPolicyRequestParseHandler)
                .handler(new RegisterConfirmationSubmissionEndpoint(userService));

        // Forgot password route
        Handler<RoutingContext> forgotPasswordAccessHandler = new ForgotPasswordAccessHandler(domain);
        rootRouter.route(HttpMethod.GET, "/forgotPassword")
                .handler(clientRequestParseHandler)
                .handler(forgotPasswordAccessHandler)
                .handler(new ForgotPasswordEndpoint(thymeleafTemplateEngine));
        rootRouter.route(HttpMethod.POST, "/forgotPassword")
                .handler(new ForgotPasswordSubmissionRequestParseHandler())
                .handler(clientRequestParseHandler)
                .handler(forgotPasswordAccessHandler)
                .handler(new ForgotPasswordSubmissionEndpoint(userService, domain));
        rootRouter.route(HttpMethod.GET, "/resetPassword")
                .handler(new ResetPasswordRequestParseHandler(userService))
                .handler(clientRequestParseHandlerOptional)
                .handler(new ResetPasswordEndpoint(thymeleafTemplateEngine));
        rootRouter.route(HttpMethod.POST, "/resetPassword")
                .handler(new ResetPasswordSubmissionRequestParseHandler())
                .handler(userTokenRequestParseHandler)
                .handler(passwordPolicyRequestParseHandler)
                .handler(new ResetPasswordSubmissionEndpoint(userService));

        // error route
        rootRouter.route(HttpMethod.GET, "/error")
                .handler(new ErrorEndpoint(domain.getId(), thymeleafTemplateEngine, clientSyncService));

        // error handler
        errorHandler(rootRouter);

        // mount root router
        router.mountSubRouter(path(), rootRouter);
    }

    @Override
    public String path() {
        return "/";
    }

    private void sessionAndCookieHandler(Router router) {

        // Define cookieHandler once and globally.
        router.route().handler(cookieHandler);

        // Login endpoint
        router.route("/login")
                .handler(sessionHandler);
        router
                .route("/login/callback")
                .handler(sessionHandler);
        router
                .route("/login/SSO/POST")
                .handler(sessionHandler);

        // MFA endpoint
        router.route("/mfa/enroll")
                .handler(sessionHandler);
        router.route("/mfa/challenge")
                .handler(sessionHandler);

        // Logout endpoint
        router
                .route("/logout")
                .handler(sessionHandler);

        // Registration confirmation endpoint
        router
                .post("/register")
                .handler(sessionHandler);
        router
                .route("/confirmRegistration")
                .handler(sessionHandler);

        // Reset password endpoint
        router
                .route("/resetPassword")
                .handler(sessionHandler);

        // WebAuthn endpoint
        router
                .route("/webauthn/register")
                .handler(sessionHandler);
        router
                .route("/webauthn/response")
                .handler(sessionHandler);
        router
                .route("/webauthn/login")
                .handler(sessionHandler);
    }

    private void csrfHandler(Router router) {
        router.route("/forgotPassword").handler(csrfHandler);
        router.route("/login").handler(csrfHandler);
        // /login/callback does not need csrf as it is not submit to our server.
        router.route("/login/SSO/POST").handler(csrfHandler);
        router.route("/mfa/challenge").handler(csrfHandler);
        router.route("/mfa/enroll").handler(csrfHandler);
        // /consent csrf is managed by handler-oidc (see OAuth2Provider).
        router.route("/register").handler(csrfHandler);
        router.route("/confirmRegistration").handler(csrfHandler);
        router.route("/resetPassword").handler(csrfHandler);
    }

    private void staticHandler(Router router) {
        router.route().handler(StaticHandler.create());
    }

    private void bodyHandler(Router router) {
        router.route().handler(BodyHandler.create());
    }

    private void errorHandler(Router router) {
        Handler<RoutingContext> errorHandler = new ErrorHandler( "/error");
        router.route("/login").failureHandler(errorHandler);
        router.route("/forgotPassword").failureHandler(errorHandler);
    }
}
