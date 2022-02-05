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

import java.util.Map;

import reactor.core.publisher.Mono;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Providing oauth devide auth flow.
 *
 * @author Janne Valkealahti
 */
public class OauthDeviceFlow {

	private WebClient.Builder webClientBuilder;

	private OauthDeviceFlow(WebClient.Builder webClientBuilder) {
		Assert.notNull(webClientBuilder, "webClientBuilder must be set");
		this.webClientBuilder = webClientBuilder;
	}

	public static OauthDeviceFlow of(WebClient.Builder webClientBuilder) {
		return new OauthDeviceFlow(webClientBuilder);
	}

	public Mono<String> requestCode(String clientId, String scope) {
		ParameterizedTypeReference<Map<String, String>> responseTypeReference =
				new ParameterizedTypeReference<Map<String, String>>() {};
		WebClient client = webClientBuilder
			.baseUrl("https://github.com")
			.build();
		return client.post()
			.uri(uriBuilder -> uriBuilder
					.path("login/device/code")
					.queryParam("client_id", clientId)
					.queryParam("scope", scope)
					.build())
			.accept(MediaType.APPLICATION_JSON)
			.retrieve()
			.bodyToMono(responseTypeReference)
			.map(response -> response.get("device_code"));
	}

}
