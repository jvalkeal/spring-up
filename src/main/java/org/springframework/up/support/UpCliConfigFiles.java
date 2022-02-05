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
package org.springframework.up.support;

import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.springframework.util.StringUtils;

public class UpCliConfigFiles {

	/**
	 * {@code hosts.yml} stores authentication spesific info for hosts.
	 */
	public final static String HOSTS = "hosts.yml";

	/**
	 * Optional env variable for {@code UpCli} configuration dir.
	 */
	public final static String UPCLI_CONFIG_DIR = "UPCLI_CONFIG_DIR";

	private final static String XDG_CONFIG_HOME = "XDG_CONFIG_HOME";
	private final static String APP_DATA = "APP_DATA";
	private final static String CONFIG_DIR = "springup";

	/**
	 * Gets hosts configuration.
	 *
	 * @return hosts configuration
	 */
	public Map<String, Host> getHosts() {
		YamlConfigFile yamlConfigFile = new YamlConfigFile();
		Path configDir = getConfigDir();
		Path configFile = configDir.resolve(Path.of(HOSTS));
		if (configFile.toFile().exists()) {
			Hosts hosts = yamlConfigFile.read(configFile, Hosts.class);
			return hosts.getHosts();
		}
		else {
			return Collections.emptyMap();
		}
	}

	private Path getConfigDir() {
		Path path;
		if (StringUtils.hasText(System.getenv(UPCLI_CONFIG_DIR))) {
			path = Path.of(System.getenv(UPCLI_CONFIG_DIR));
		}
		else if (StringUtils.hasText(System.getenv(XDG_CONFIG_HOME))) {
			path = Path.of(System.getProperty(XDG_CONFIG_HOME), CONFIG_DIR);
		}
		else if (StringUtils.hasText(System.getenv(APP_DATA)) && isWindows()) {
			path = Path.of(System.getProperty(APP_DATA), CONFIG_DIR);
		}
		else {
			path = Path.of(System.getProperty("user.home"), ".config", CONFIG_DIR);
		}
		return path;
	}

	private boolean isWindows() {
		String os = System.getProperty("os.name");
		if (!StringUtils.hasText(os)) {
			throw new RuntimeException("System property os.name didn't return anything");
		}
		return os.startsWith("Windows");
	}

	public static class Hosts {

		private Map<String, Host> hosts = new HashMap<>();

		public Map<String, Host> getHosts() {
			return hosts;
		}

		public void setHosts(Map<String, Host> hosts) {
			this.hosts = hosts;
		}
	}

	public static class Host {

		private String oauthToken;
		private String user;

		public String getOauthToken() {
			return oauthToken;
		}

		public void setOauthToken(String oauthToken) {
			this.oauthToken = oauthToken;
		}

		public String getUser() {
			return user;
		}

		public void setUser(String user) {
			this.user = user;
		}
	}
}
