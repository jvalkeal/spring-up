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

import java.util.Map;

import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.up.support.UpCliConfigFiles;
import org.springframework.up.support.UpCliConfigFiles.Host;

/**
 * GitHub related commands.
 *
 * @author Janne Valkealahti
 */
@ShellComponent
public class GitHubCommands {

	@ShellMethod(key = "auth github status", value = "View GitHub authentication status")
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

	@ShellMethod(key = "auth github login", value = "Authenticate with a GitHub host")
	public void login() {
	}

	@ShellMethod(key = "auth github logout", value = "Log out of a GitHub host")
	public void logout() {
	}

	@ShellMethod(key = "auth github refresh", value = "Refresh stored GitHub authentication credentials")
	public void refresh() {
	}
}
