/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License") +  you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.openmeetings.web.pages.auth;

import static org.apache.openmeetings.util.OpenmeetingsVariables.CONFIG_DEFAULT_LDAP_ID;
import static org.apache.openmeetings.web.app.Application.getAuthenticationStrategy;
import static org.apache.openmeetings.web.pages.HashPage.APP;
import static org.apache.openmeetings.web.pages.HashPage.APP_TYPE_NETWORK;
import static org.apache.openmeetings.web.pages.auth.SignInPage.showAuth;

import java.util.List;

import org.apache.openmeetings.db.dao.basic.ConfigurationDao;
import org.apache.openmeetings.db.dao.server.LdapConfigDao;
import org.apache.openmeetings.db.dao.server.OAuth2Dao;
import org.apache.openmeetings.db.entity.server.LdapConfig;
import org.apache.openmeetings.db.entity.server.OAuthServer;
import org.apache.openmeetings.db.entity.user.User.Type;
import org.apache.openmeetings.util.OmException;
import org.apache.openmeetings.web.app.Application;
import org.apache.openmeetings.web.app.OmAuthenticationStrategy;
import org.apache.openmeetings.web.app.WebSession;
import org.apache.openmeetings.web.common.OmAjaxClientInfoBehavior;
import org.apache.openmeetings.web.pages.HashPage;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.PasswordTextField;
import org.apache.wicket.markup.html.form.RequiredTextField;
import org.apache.wicket.markup.html.form.StatelessForm;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.util.string.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.agilecoders.wicket.core.markup.html.bootstrap.button.BootstrapAjaxLink;
import de.agilecoders.wicket.core.markup.html.bootstrap.button.Buttons;
import de.agilecoders.wicket.core.markup.html.bootstrap.common.NotificationPanel;
import de.agilecoders.wicket.core.markup.html.bootstrap.dialog.Modal;
import de.agilecoders.wicket.core.markup.html.bootstrap.image.IconType;
import de.agilecoders.wicket.extensions.markup.html.bootstrap.spinner.SpinnerAjaxButton;

public class SignInDialog extends Modal<String> {
	private static final long serialVersionUID = 1L;
	private static final Logger log = LoggerFactory.getLogger(SignInDialog.class);
	private Form<String> form;
	private final PasswordTextField passField = new PasswordTextField("pass", Model.of(""));
	private final RequiredTextField<String> loginField = new RequiredTextField<>("login", Model.of(""));
	private boolean rememberMe = false;
	private RegisterDialog register;
	private ForgetPasswordDialog f;
	private LdapConfig domain;
	private NotificationPanel feedback = new NotificationPanel("feedback");
	@SpringBean
	private ConfigurationDao cfgDao;
	@SpringBean
	private LdapConfigDao ldapDao;
	@SpringBean
	private OAuth2Dao oauthDao;

	public SignInDialog(String id) {
		super(id);
		add(new OmAjaxClientInfoBehavior());
	}

	@Override
	protected void onInitialize() {
		add(form = new SignInForm("signin"));
		header(new ResourceModel("108"));
		show(true);
		setCloseOnEscapeKey(false);
		setBackdrop(Backdrop.STATIC);
		addButton(new BootstrapAjaxLink<>("button", Model.of(""), Buttons.Type.Outline_Secondary, new ResourceModel("123")) {
			private static final long serialVersionUID = 1L;

			public void onClick(AjaxRequestTarget target) {
				SignInDialog.this.close(target);
				register.setClientTimeZone();
				register.show(target);
			}
		});
		addButton(new SpinnerAjaxButton("button", new ResourceModel("112"), form, Buttons.Type.Outline_Primary)); // Login

		super.onInitialize();
	}

	@Override
	protected Component createHeaderCloseButton(String id) {
		return super.createHeaderCloseButton(id).setVisible(false);
	}

	public void setRegisterDialog(RegisterDialog r) {
		this.register = r;
	}

	public void setForgetPasswordDialog(ForgetPasswordDialog f) {
		this.f = f;
	}

	class SignInForm extends StatelessForm<String> {
		private static final long serialVersionUID = 1L;
		private final WebMarkupContainer credentials = new WebMarkupContainer("credentials");

		public SignInForm(String id) {
			super(id);
		}

		@Override
		protected void onInitialize() {
			loginField.setLabel(new ResourceModel("114"));
			passField.setLabel(new ResourceModel("110"));
			super.onInitialize();
			if (WebSession.get().isSignedIn()) {
				alreadyLoggedIn();
			}
			add(credentials, feedback.setOutputMarkupId(true));
			credentials.add(loginField, passField.setResetPassword(true));
			List<LdapConfig> ldaps = ldapDao.get();
			final boolean showLdap = ldaps.size() > 1;
			int selectedLdap = cfgDao.getInt(CONFIG_DEFAULT_LDAP_ID, 0);
			domain = ldaps.get(selectedLdap < ldaps.size() && selectedLdap > 0 ? selectedLdap : 0);
			credentials.add(new WebMarkupContainer("ldap")
				.add(new DropDownChoice<>("domain", new PropertyModel<LdapConfig>(SignInDialog.this, "domain")
						, ldaps, new ChoiceRenderer<LdapConfig>("name", "id"))).setVisible(showLdap));
			credentials.add(new CheckBox("rememberMe", new PropertyModel<Boolean>(SignInDialog.this, "rememberMe")).setOutputMarkupId(true));
			AjaxButton ab = new AjaxButton("submit") { //FAKE button so "submit-on-enter" works as expected
				private static final long serialVersionUID = 1L;
			};
			add(ab);
			setDefaultButton(ab);
			credentials.add(new AjaxLink<Void>("forget") {
				private static final long serialVersionUID = 1L;

				@Override
				public void onClick(AjaxRequestTarget target) {
					SignInDialog.this.close(target);
					f.show(target);
				}
			});
			add(new WebMarkupContainer("netTest").add(AttributeModifier.append("href"
					, RequestCycle.get().urlFor(HashPage.class, new PageParameters().add(APP, APP_TYPE_NETWORK)).toString())));
			final boolean showOauth = ((SignInPage)getPage()).allowOAuthLogin();
			add(new WebMarkupContainer("oauth").add(
				new ListView<>("oauthList", oauthDao.getActive()) {
					private static final long serialVersionUID = 1L;

					@Override
					protected void populateItem(final ListItem<OAuthServer> item) {
						final OAuthServer s = item.getModelObject();

						BootstrapAjaxLink<String> btn = new BootstrapAjaxLink<>("oauthBtn", null, Buttons.Type.Outline_Info, Model.of(s.getName())) {
							private static final long serialVersionUID = 1L;
							{
								setMarkupId("om-oauth-btn-" + s.getId());
								setOutputMarkupId(true);
							}

							@Override
							public void onClick(AjaxRequestTarget target) {
								showAuth(s);
							}

							@Override
							public void renderHead(IHeaderResponse response) {
								if (!Strings.isEmpty(s.getIconUrl())) {
									response.render(CssHeaderItem.forCSS("#" + this.getMarkupId() + " .provider {background-image: url(" + s.getIconUrl() + ")}", "oauth-btn-css-" + this.getMarkupId()));
								}
							}
						};
						item.add(btn.setIconType(new IconType("provider") {
							private static final long serialVersionUID = 1L;

							@Override
							public String cssClassName() {
								return "provider";
							}
						}));
						item.setRenderBodyOnly(true);
					}
				}).setVisible(showOauth));
			if (showOauth) {
				add(AttributeModifier.append("class", "wide"));
			}
		}

		private void alreadyLoggedIn() {
			// logon successful. Continue to the original destination
			continueToOriginalDestination();
			// Ups, no original destination. Go to the home page
			throw new RestartResponseException(Application.get().getHomePage());
		}

		@Override
		protected void onError() {
			RequestCycle.get().find(AjaxRequestTarget.class).ifPresent(this::onError);
		}

		protected void onError(AjaxRequestTarget target) {
			target.add(feedback);
		}

		@Override
		protected void onSubmit() {
			RequestCycle.get().find(AjaxRequestTarget.class).ifPresent(this::onSubmit);
		}

		protected void onSubmit(AjaxRequestTarget target) {
			final String login = String.format(domain.getAddDomainToUserName() ? "%s@%s" : "%s"
					, loginField.getModelObject(), domain.getDomain());
			final String password = passField.getModelObject();
			OmAuthenticationStrategy strategy = getAuthenticationStrategy();
			WebSession ws = WebSession.get();
			Type type = domain.getId() > 0 ? Type.LDAP : Type.USER;
			boolean signIn = false;
			try {
				signIn = ws.signIn(login, password, type, domain.getId());
			} catch (OmException e) {
				error(getString(e.getKey()));
				target.add(feedback);
			}
			if (signIn) {
	 			setResponsePage(Application.get().getHomePage());
				if (rememberMe) {
					strategy.save(login, password, type, domain.getId());
				} else {
					strategy.remove();
				}
			} else {
				if (!hasErrorMessage()) {
					error(getString("error.bad.credentials"));
					target.add(feedback);
				}
				// add random timeout
				try {
					Thread.sleep(6 + (long)(10 * Math.random() * 1000));
				} catch (InterruptedException e) {
					log.error("Unexpected exception while sleeping", e);
					Thread.currentThread().interrupt();
				}
				strategy.remove();
			}
		}
	}
}
