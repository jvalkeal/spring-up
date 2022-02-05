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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import org.springframework.util.Assert;

/**
 * Represents a single config file as a {@code yaml} structure.
 *
 * @author Janne Valkealahti
 */
public class YamlConfigFile {

	private ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

	/**
	 * De-serialise a given config file with a type.
	 *
	 * @param <T> type of pojo class
	 * @param path the config file path
	 * @param type the bean class type
	 * @return de-serialised pojo
	 */
	public <T> T read(Path path, Class<T> type) {
		Assert.notNull(path, "path cannot be null");
		try {
			return mapper.readValue(path.toFile(), type);
		} catch (Exception e) {
			throw new RuntimeException("Unable to read config file", e);
		}
	}

	/**
	 * Write a given pojo into a give config file
	 *
	 * @param path the config file path
	 * @param value the pojo to write
	 */
	public void write(Path path, Object value) {
		Assert.notNull(path, "path cannot be null");
		try {
			mapper.writeValue(path.toFile(), value);
		} catch (Exception e) {
			throw new RuntimeException("Unable to read config file", e);
		}
	}
}
