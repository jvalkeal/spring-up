/*
 * Copyright 2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.up.command;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import reactor.core.publisher.Mono;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ResourceLoader;
import org.springframework.shell.component.SingleItemSelector;
import org.springframework.shell.component.StringInput;
import org.springframework.shell.component.SingleItemSelector.SingleItemSelectorContext;
import org.springframework.shell.component.StringInput.StringInputContext;
import org.springframework.shell.component.support.SelectorItem;
import org.springframework.shell.standard.AbstractShellComponent;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;
import org.springframework.shell.style.TemplateExecutor;
import org.springframework.up.config.UpCliProperties;
import org.springframework.up.support.OauthDeviceFlow;
import org.springframework.up.support.OauthDeviceFlow.RequestCodeResponse;
import org.springframework.up.support.OauthDeviceFlow.WaitTokenResponse;
import org.springframework.up.support.UpCliConfigFiles;
import org.springframework.up.support.UpCliConfigFiles.Host;
import org.springframework.up.support.UpCliConfigFiles.Hosts;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * GitHub related commands.
 *
 * @author Janne Valkealahti
 */
@ShellComponent
public class GitHubCommands extends AbstractShellComponent {

	private final static String HELP_STATUS = "View GitHub authentication status";
	private final static String HELP_LOGIN = "Authenticate with a GitHub host";
	private final static String HELP_LOGIN_SCOPES = "Additional authentication scopes for gh to have";
	private final static String HELP_LOGIN_WITHTOKEN = "Read token from standard input";
	private final static String HELP_LOGOUT = "Log out of a GitHub host";
	private final static String HELP_REFRESH = "Refresh stored GitHub authentication credentials";

	@Autowired
	private ResourceLoader resourceLoader;

	@Autowired
	private TemplateExecutor templateExecutor;

	@Autowired
	private UpCliProperties upCliProperties;

	@Autowired
	private WebClient.Builder webClientBuilder;

	@ShellMethod(key = "auth github status", value = HELP_STATUS)
	public String status() {
		UpCliConfigFiles upCliConfigFiles = new UpCliConfigFiles();
		Map<String, Host> hosts = upCliConfigFiles.getHosts();
		Host githubHost = hosts.get("github.com");
		if (githubHost != null) {
			return String.format("Logged in as user %s", githubHost.getUser());
		}
		else {
			return "not logged in";
		}
	}

	@ShellMethod(key = "auth github login", value = HELP_LOGIN)
	public void login(
		@ShellOption(value = "scopes", defaultValue = ShellOption.NULL, help = HELP_LOGIN_SCOPES) String scopes,
		@ShellOption(value = "with-token", defaultValue = ShellOption.NULL, help = HELP_LOGIN_WITHTOKEN) String token
	) {
		// ? How would you like to authenticate GitHub CLI? [Use arrows to move, type to filter]
		// > Login with a web browser
		//   Paste an authentication token

		String authType = askLoginType();

		if (ObjectUtils.nullSafeEquals(authType, "web")) {
			OauthDeviceFlow oauth = OauthDeviceFlow.of(webClientBuilder);
			Mono<RequestCodeResponse> requestCode = oauth.requestCode(upCliProperties.getGithub().getClientId(),
					"repo,read:org");
			RequestCodeResponse response1 = requestCode.block();
			String out1 = String.format("Open %s and paste code %s", response1.getVerificationUri(), response1.getUserCode());
			print(out1);
			Mono<WaitTokenResponse> waitToken = oauth.waitToken(upCliProperties.getGithub().getClientId(), response1);
			WaitTokenResponse response2 = waitToken.block();
			String out2 = String.format("Got token %s", response2.getAccessToken());
			print(out2);

			UpCliConfigFiles upCliConfigFiles = new UpCliConfigFiles();
			Map<String, Host> hosts = upCliConfigFiles.getHosts();
			Host host = new Host();
			host.setToken(response2.getAccessToken());
			hosts.put("github.com", host);
			upCliConfigFiles.setHosts(hosts);
		}


		// ! First copy your one-time code: 6072-E2D0
		// - Press Enter to open github.com in your browser...


		// ✓ Authentication complete. Press Enter to continue...
		//
		// - gh config set -h github.com git_protocol https
		// ✓ Configured git protocol
		// ✓ Logged in as jvalkeal


		// ? Paste your authentication token:

		else if (ObjectUtils.nullSafeEquals(authType, "token")) {
			String userToken = askToken();
		}
	}

	private void print(String text) {
		try {
			String out = text + "\n";
			getTerminal().output().write(out.getBytes());
		} catch (IOException e) {
		}
	}

	@ShellMethod(key = "auth github logout", value = HELP_LOGOUT)
	public String logout() {
		UpCliConfigFiles upCliConfigFiles = new UpCliConfigFiles();
		Map<String, Host> hosts = upCliConfigFiles.getHosts();
		Host githubHost = hosts.get("github.com");
		if (githubHost != null) {
			return "should logout";
		}
		else {
			return "not logged in to any hosts";
		}

		// ? Are you sure you want to log out of github.com account 'jvalkeal'? (Y/n)
	}

	@ShellMethod(key = "auth github refresh", value = HELP_REFRESH)
	public void refresh() {
		// ! First copy your one-time code: A84E-F1EC
		// - Press Enter to open github.com in your browser... ^C
	}

	private String askLoginType() {
		List<SelectorItem<String>> items = new ArrayList<>();
		items.add(SelectorItem.of("Login with a web browser", "web"));
		items.add(SelectorItem.of("Paste an authentication token", "token"));
		SingleItemSelector<String, SelectorItem<String>> component = new SingleItemSelector<>(getTerminal(),
				items, "How would you like to authenticate UP CLI?", null);
		component.setResourceLoader(resourceLoader);
		component.setTemplateExecutor(templateExecutor);
		SingleItemSelectorContext<String, SelectorItem<String>> context = component
				.run(SingleItemSelectorContext.empty());
		return context.getResultItem().flatMap(si -> Optional.ofNullable(si.getItem())).get();
	}

	private String askToken() {
		StringInput component = new StringInput(getTerminal(), "Paste your authentication token:", null);
		component.setResourceLoader(resourceLoader);
		component.setTemplateExecutor(templateExecutor);
		StringInputContext context = component.run(StringInputContext.empty());
		return context.getResultValue();
	}
}
