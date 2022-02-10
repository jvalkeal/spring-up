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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import org.springframework.up.support.ComponentFlow.ComponentFlowResult;
import org.springframework.up.support.ComponentFlow.ResultMode;
import org.springframework.up.support.ComponentFlow.SelectItem;

import static org.assertj.core.api.Assertions.assertThat;

public class ComponentFlowTests extends AbstractShellTests {

	@Test
	public void testSimpleFlow() throws InterruptedException {
		Map<String, String> single1SelectItems = new HashMap<>();
		single1SelectItems.put("key1", "value1");
		single1SelectItems.put("key2", "value2");
		List<SelectItem> multi1SelectItems = Arrays.asList(SelectItem.of("key1", "value1"),
				SelectItem.of("key2", "value2"), SelectItem.of("key3", "value3"));
		ComponentFlow wizard = ComponentFlow.builder(getTerminal())
				.resourceLoader(getResourceLoader())
				.templateExecutor(getTemplateExecutor())
				.withStringInput("field1")
					.name("Field1")
					.defaultValue("defaultField1Value")
					.and()
				.withStringInput("field2")
					.name("Field2")
					.and()
				.withPathInput("path1")
					.name("Path1")
					.and()
				.withSingleItemSelector("single1")
					.name("Single1")
					.selectItems(single1SelectItems)
					.and()
				.withMultiItemSelector("multi1")
					.name("Multi1")
					.selectItems(multi1SelectItems)
					.and()
				.build();

		ExecutorService service = Executors.newFixedThreadPool(1);
		CountDownLatch latch = new CountDownLatch(1);
		AtomicReference<ComponentFlowResult> result = new AtomicReference<>();

		service.execute(() -> {
			result.set(wizard.run());
			latch.countDown();
		});

		// field1
		TestBuffer testBuffer = new TestBuffer().cr();
		write(testBuffer.getBytes());
		// field2
		testBuffer = new TestBuffer().append("Field2Value").cr();
		write(testBuffer.getBytes());
		// path1
		testBuffer = new TestBuffer().append("fakedir").cr();
		write(testBuffer.getBytes());
		// single1
		testBuffer = new TestBuffer().cr();
		write(testBuffer.getBytes());
		// multi1
		testBuffer = new TestBuffer().ctrlE().space().cr();
		write(testBuffer.getBytes());

		latch.await(4, TimeUnit.SECONDS);
		ComponentFlowResult inputWizardResult = result.get();
		assertThat(inputWizardResult).isNotNull();
		String field1 = inputWizardResult.getContext().get("field1");
		String field2 = inputWizardResult.getContext().get("field2");
		Path path1 = inputWizardResult.getContext().get("path1");
		String single1 = inputWizardResult.getContext().get("single1");
		List<String> multi1 = inputWizardResult.getContext().get("multi1");
		assertThat(field1).isEqualTo("defaultField1Value");
		assertThat(field2).isEqualTo("Field2Value");
		assertThat(path1.toString()).contains("fakedir");
		assertThat(single1).isEqualTo("value1");
		assertThat(multi1).containsExactlyInAnyOrder("value2");
		assertThat(consoleOut()).contains("Field1 defaultField1Value");
	}

	@Test
	public void testSkipsGivenComponents() throws InterruptedException {
		ComponentFlow wizard = ComponentFlow.builder(getTerminal())
			.withStringInput("id1")
				.name("name")
				.resultValue("value1")
				.resultMode(ResultMode.ACCEPT)
				.and()
			.withPathInput("id2")
				.name("name")
				.resultValue("value2")
				.resultMode(ResultMode.ACCEPT)
				.and()
			.withSingleItemSelector("id3")
				.resultValue("value3")
				.resultMode(ResultMode.ACCEPT)
				.and()
			.withMultiItemSelector("id4")
				.resultValues(Arrays.asList("value4"))
				.resultMode(ResultMode.ACCEPT)
				.and()
			.build();

			ExecutorService service = Executors.newFixedThreadPool(1);
			CountDownLatch latch = new CountDownLatch(1);
			AtomicReference<ComponentFlowResult> result = new AtomicReference<>();

			service.execute(() -> {
				result.set(wizard.run());
				latch.countDown();
			});

			latch.await(4, TimeUnit.SECONDS);
			ComponentFlowResult inputWizardResult = result.get();
			assertThat(inputWizardResult).isNotNull();

			String id1 = inputWizardResult.getContext().get("id1");
			Path id2 = inputWizardResult.getContext().get("id2");
			String id3 = inputWizardResult.getContext().get("id3");
			List<String> id4 = inputWizardResult.getContext().get("id4");

			assertThat(id1).isEqualTo("value1");
			assertThat(id2.toString()).contains("value2");
			assertThat(id3).isEqualTo("value3");
			assertThat(id4).containsExactlyInAnyOrder("value4");
		}
}
