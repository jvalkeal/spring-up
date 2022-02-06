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
import java.util.Map;

import org.kohsuke.github.GHContent;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;

import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;
import org.springframework.up.support.UpCliConfigFiles;
import org.springframework.up.support.UpCliConfigFiles.Host;
import org.springframework.util.FileCopyUtils;

/**
 * These are a set of demo commands to show integration with git repos
 * what comes for using tokens stored in a config files after user
 * has done the auth flows. This until we have some real commands
 * using git repos.
 *
 * @author Janne Valkealahti
 */
@ShellComponent
public class DemoCommands {

	@ShellMethod(key = "demo readme", value = "Get github repo readme")
	public String repoReadme(
		@ShellOption(value = "--repo", help = "Repo as <username>/<repository>") String repo
	) throws IOException {
		UpCliConfigFiles upCliConfigFiles = new UpCliConfigFiles();
		Map<String, Host> hosts = upCliConfigFiles.getHosts();
		Host host = hosts.get("github.com");
		if (host != null) {
			GitHub github = new GitHubBuilder().withOAuthToken(host.getToken()).build();
			GHRepository atest7Repo = github.getRepository("jvalkeal/atest7");
			GHContent readme = atest7Repo.getReadme();
			byte[] readmeBytes = FileCopyUtils.copyToByteArray(readme.read());
			return new String(readmeBytes);
		}
		else {
			return "crap";
		}

		// GitHub github = new GitHubBuilder().withOAuthToken(token).build();
		// GHRepository atest7Repo = github.getRepository("jvalkeal/atest7");
		// GHContent readme = atest7Repo.getReadme();
		// byte[] readmeBytes = FileCopyUtils.copyToByteArray(readme.read());
		// log.info("README {}", new String(readmeBytes));
	}
}
