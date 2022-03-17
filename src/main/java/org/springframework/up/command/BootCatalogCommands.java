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

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;
import org.springframework.shell.table.ArrayTableModel;
import org.springframework.shell.table.BorderStyle;
import org.springframework.shell.table.Table;
import org.springframework.shell.table.TableBuilder;
import org.springframework.shell.table.TableModel;
import org.springframework.up.support.AbstractUpCliCommands;
import org.springframework.up.support.UpCliUserConfig;
import org.springframework.up.support.UpCliUserConfig.TemplateCatalog;
import org.springframework.up.support.UpCliUserConfig.TemplateCatalogs;
import org.springframework.util.ObjectUtils;

@ShellComponent
public class BootCatalogCommands extends AbstractUpCliCommands {

	private final UpCliUserConfig upCliUserConfig;

	@Autowired
	public BootCatalogCommands(UpCliUserConfig upCliUserConfig) {
		this.upCliUserConfig = upCliUserConfig;
	}

	@ShellMethod(key = "catalog add", value = "Add a catalog")
	public void catalogAdd(
		@ShellOption(help = "Catalog name") String name,
		@ShellOption(help = "Catalog url") String url,
		@ShellOption(help = "Catalog description", defaultValue = ShellOption.NULL) String description
	) {
		List<TemplateCatalog> templateCatalogs = upCliUserConfig.getTemplateCatalogsConfig().getTemplateCatalogs();
		templateCatalogs.add(TemplateCatalog.of(name, description, url));
		TemplateCatalogs templateCatalogsConfig = new TemplateCatalogs();
		templateCatalogsConfig.setTemplateCatalogs(templateCatalogs);
		upCliUserConfig.setTemplateCatalogsConfig(templateCatalogsConfig);
	}

	@ShellMethod(key = "catalog list", value = "List catalogs")
	public Table catalogList() {
		Stream<String[]> header = Stream.<String[]>of(new String[] { "Name", "Description" });
		Collection<TemplateCatalog> templateRepositories = upCliUserConfig.getTemplateCatalogsConfig().getTemplateCatalogs();
		Stream<String[]> rows = null;
		if (templateRepositories != null) {
			rows = templateRepositories.stream()
				.map(tr -> new String[] { tr.getName(), tr.getDescription()});
		}
		else {
			rows = Stream.empty();
		}
		String[][] data = Stream.concat(header, rows).toArray(String[][]::new);
		TableModel model = new ArrayTableModel(data);
		TableBuilder tableBuilder = new TableBuilder(model);
		return tableBuilder.addFullBorder(BorderStyle.fancy_light).build();
	}

	@ShellMethod(key = "catalog remove", value = "Remove catalogs")
	public void catalogRemove(
		@ShellOption(help = "Catalog name") String name
	) {
		List<TemplateCatalog> templateCatalogs = upCliUserConfig.getTemplateCatalogsConfig().getTemplateCatalogs();
		templateCatalogs = templateCatalogs.stream()
			.filter(tc -> !ObjectUtils.nullSafeEquals(tc.getName(), name))
			.collect(Collectors.toList());
		TemplateCatalogs templateCatalogsConfig = new TemplateCatalogs();
		templateCatalogsConfig.setTemplateCatalogs(templateCatalogs);
		upCliUserConfig.setTemplateCatalogsConfig(templateCatalogsConfig);
	}
}
