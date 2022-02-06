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

import java.net.URI;
import java.time.Duration;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Providing oauth devide auth flow.
 *
 * @author Janne Valkealahti
 */
public class OauthDeviceFlow {

	private final static Logger log = LoggerFactory.getLogger(OauthDeviceFlow.class);
	private WebClient.Builder webClientBuilder;
	private OauthDeviceFlow(WebClient.Builder webClientBuilder) {
		Assert.notNull(webClientBuilder, "webClientBuilder must be set");
		this.webClientBuilder = webClientBuilder;
	}

	public static OauthDeviceFlow of(WebClient.Builder webClientBuilder) {
		return new OauthDeviceFlow(webClientBuilder);
	}

	public Mono<RequestCodeResponse> requestCode(String clientId, String scope) {
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
			.bodyToMono(RequestCodeResponse.class)
			;
	}

	public Mono<WaitTokenResponse> waitToken(String clientId, RequestCodeResponse requestCodeResponse) {
		WebClient client = webClientBuilder
			.baseUrl("https://github.com")
			.build();
		return client.post()
			.uri(uriBuilder -> uriBuilder
					.path("login/oauth/access_token")
					.queryParam("client_id", clientId)
					.queryParam("device_code", requestCodeResponse.getDeviceCode())
					.queryParam("grant_type", "urn:ietf:params:oauth:grant-type:device_code")
					.build())
			.accept(MediaType.APPLICATION_JSON)
			.exchangeToMono(x -> x.bodyToMono(WaitTokenResponse.class).flatMap(body -> {
				log.info("XXX {}", body);
				if (StringUtils.hasText(body.getError())) {
					return Mono.error(new RuntimeException());
				}
				return Mono.just(body);
			}))
			.retryWhen(Retry.fixedDelay(10, Duration.ofSeconds(requestCodeResponse.getInterval())))
			;
	}

	/**
	 *
	 */
	public static class RequestCodeResponse {

		@JsonProperty("device_code")
		private String deviceCode;

		@JsonProperty("expires_in")
		private Integer expiresIn;

		@JsonProperty("interval")
		private Integer interval;

		@JsonProperty("user_code")
		private String userCode;

		@JsonProperty("verification_uri")
		private URI verificationUri;

		public String getDeviceCode() {
			return deviceCode;
		}

		public void setDeviceCode(String deviceCode) {
			this.deviceCode = deviceCode;
		}

		public Integer getExpiresIn() {
			return expiresIn;
		}

		public void setExpiresIn(Integer expiresIn) {
			this.expiresIn = expiresIn;
		}

		public Integer getInterval() {
			return interval;
		}

		public void setInterval(Integer interval) {
			this.interval = interval;
		}

		public String getUserCode() {
			return userCode;
		}

		public void setUserCode(String userCode) {
			this.userCode = userCode;
		}

		public URI getVerificationUri() {
			return verificationUri;
		}

		public void setVerificationUri(URI verificationUri) {
			this.verificationUri = verificationUri;
		}

		@Override
		public String toString() {
			return "RequestCodeResponse [deviceCode=" + deviceCode + ", expiresIn=" + expiresIn + ", interval="
					+ interval + ", userCode=" + userCode + ", verificationUri=" + verificationUri + "]";
		}
	}

	/**
	 *
	 */
	public static class WaitTokenResponse {

		@JsonProperty("error")
		private String error;

		@JsonProperty("access_token")
		private String accessToken;

		@JsonProperty("token_type")
		private String tokenType;

		@JsonProperty("scope")
		private String scope;

		public String getError() {
			return error;
		}

		public void setError(String error) {
			this.error = error;
		}

		public String getAccessToken() {
			return accessToken;
		}

		public void setAccessToken(String accessToken) {
			this.accessToken = accessToken;
		}

		public String getTokenType() {
			return tokenType;
		}

		public void setTokenType(String tokenType) {
			this.tokenType = tokenType;
		}

		public String getScope() {
			return scope;
		}

		public void setScope(String scope) {
			this.scope = scope;
		}

		@Override
		public String toString() {
			return "WaitTokenResponse [accessToken=" + accessToken + ", error=" + error + ", scope=" + scope
					+ ", tokenType=" + tokenType + "]";
		}
	}
}
