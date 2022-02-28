/*
 * Copyright 2021 the original author or authors.
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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.maven.model.Model;
import org.apache.tika.Tika;
import org.apache.tools.ant.util.FileUtils;
import org.codehaus.plexus.util.DirectoryScanner;
import org.jetbrains.annotations.Nullable;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;
import org.openrewrite.Recipe;
import org.openrewrite.SourceFile;
import org.openrewrite.java.ChangePackage;
import org.openrewrite.java.Java11Parser;
import org.openrewrite.java.JavaParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;
import org.springframework.up.UpException;
import org.springframework.up.config.TemplateRepository;
import org.springframework.up.config.UpCliProperties;
import org.springframework.up.git.SourceRepositoryService;
import org.springframework.up.util.FileTypeCollectingFileVisitor;
import org.springframework.up.util.IoUtils;
import org.springframework.up.util.PackageNameUtils;
import org.springframework.up.util.ProjectInfo;
import org.springframework.up.util.PomReader;
import org.springframework.up.util.ResultsExecutor;
import org.springframework.up.util.RootPackageFinder;
import org.springframework.util.StringUtils;

@ShellComponent
public class BootCommands {

	private static final Logger logger = LoggerFactory.getLogger(BootCommands.class);

	private UpCliProperties upCliProperties;

	private final SourceRepositoryService sourceRepositoryService;

	private static final String DEFAULT_PROJECT_NAME = "demo";

	@Autowired
	public BootCommands(UpCliProperties upCliProperties,
			SourceRepositoryService sourceRepositoryService) {
		this.upCliProperties = upCliProperties;
		this.sourceRepositoryService = sourceRepositoryService;
	}

	@ShellMethod(key = "boot new", value = "Create a new Spring Boot project from a template")
	public void bootNew(
			@ShellOption(help = "Name of the new project", defaultValue = ShellOption.NULL) String projectName,
			@ShellOption(help = "Name or URL of runnable project template", defaultValue = ShellOption.NULL) String template,
			@ShellOption(help = "Package name for the new project", defaultValue = ShellOption.NULL) String packageName) {
		String projectNameToUse = getProjectName(projectName); // Will return string or throw exception
		String urlToUse = getTemplateRepositoryUrl(template);  // Will return string or throw exception
		String packageNameToUse = PackageNameUtils.getPackageName(packageName, this.upCliProperties.getDefaults().getPackageName());
		generateFromUrl(projectNameToUse, urlToUse, packageNameToUse);
	}

	private String getProjectName(String projectName) {
		if (StringUtils.hasText(projectName)) {
			return projectName;
		}
		String defaultProjectName = this.upCliProperties.getDefaults().getProjectName();
		if (StringUtils.hasText(defaultProjectName)) {
			return defaultProjectName;
		}
		// The last resort default project name
		return DEFAULT_PROJECT_NAME;
	}

	@Nullable
	private String getTemplateRepositoryUrl(String templateName) {
		// If provided template name on the command line
		if (StringUtils.hasText(templateName)) {
			// Check it if it a URL
			if (templateName.startsWith("https")) {
				return templateName;
			}
			// Find URL from name
			else {
				// look up url based on name
				return findTemplateUrl(templateName);
			}
		} else {
			// no cli argument specified, fall back to application default value
			String defaultTemplateName = this.upCliProperties.getDefaults().getTemplateRepositoryName();
			if (StringUtils.hasText(defaultTemplateName)) {
				return findTemplateUrl(defaultTemplateName);
			} else {
				// no default template name found
				throw new UpException("Template name not specified and no default value configured.");
			}
		}
	}

	@Nullable
	private String findTemplateUrl(String templateName) {
		List<TemplateRepository> templateRepositories = this.upCliProperties.getTemplateRepositories();
		for (TemplateRepository templateRepository : templateRepositories) {
			if (templateName.trim().equalsIgnoreCase(templateRepository.getName().trim())) {
				// match - get url
				String url = templateRepository.getUrl();
				if (StringUtils.hasText(url)) {
					return url;
				}
				break;
			}
		}
		throw new UpException("Could not resolve template name " + templateName + " to URL.  Check configuration file settings.");
	}

	private void generateFromUrl(String projectName, String url, String packageName) {

		logger.debug("Generating project {} from url {} with Java package name {} ", projectName, url, packageName);
		Path repositoryContentsPath = sourceRepositoryService.retrieveRepositoryContents(url);

		// Get existing package name
		Optional<String> existingPackageName = this.getRootPackageName(repositoryContentsPath);

		// Refactor packages
		if (StringUtils.hasText(packageName) && existingPackageName.isPresent()) {
			refactorPackage(packageName, existingPackageName.get(), repositoryContentsPath);
		}

		// Derive existing project name
		Optional<ProjectInfo> projectInfo = getProjectInfo(repositoryContentsPath);
		logger.debug("Existing project = " + projectInfo);

		// Copy files
		File fromDir = repositoryContentsPath.toFile();
		File toDir = createProjectDirectory(projectName);

		DirectoryScanner ds = new DirectoryScanner();
		ds.setBasedir(fromDir);
		ds.scan();
		String[] fileNames = ds.getIncludedFiles();

		toDir.mkdirs();
		for (String fileName : fileNames) {
			File srcFile = new File(fromDir, fileName);
			File destFile = new File(toDir, fileName);
			logger.debug("Copy from " + srcFile + " to " + destFile);
			Tika tika = new Tika();
			try {
				FileUtils.getFileUtils().copyFile(srcFile, destFile);
				if (projectInfo.isPresent()) {
					if (tika.detect(destFile).startsWith("text") || tika.detect(destFile).contains("xml")) {
						List<String> replacedLines = new ArrayList<>();
						List<String> originalLines = Files.readAllLines(destFile.toPath());
						for (String originalLine : originalLines) {
							replaceString(projectName, projectInfo, destFile, replacedLines, originalLine);
						}
						Files.write(destFile.toPath(), replacedLines);
					}
					// set executable file system permissions if needed.
					if (srcFile.canExecute()) {
						destFile.setExecutable(true);
					}
				}
			} catch (IOException e) {
				throw new UpException(
						"Could not copy files from " + fromDir.getAbsolutePath() + " to " + toDir.getAbsolutePath());
			}
		}

		AttributedStringBuilder sb = new AttributedStringBuilder();
		sb.append("Project " + projectName + " created.")
				.style(sb.style().foreground(AttributedStyle.GREEN));
		System.out.println(sb.toAnsi());
	}

	private void replaceString(String projectName, Optional<ProjectInfo> projectInfo, File destFile, List<String> replacedLines, String originalLine) {
		boolean replaced = false;
		if (originalLine.contains(projectInfo.get().getName())) {
			replaced = true;
			// can only replace one token per line with this algorithm
			String processedLine = originalLine.replace(projectInfo.get().getName(), projectName);
			replacedLines.add(processedLine);
			logger.debug("In file " + destFile.getAbsolutePath() + " replaced " + projectInfo.get().getName() + " with "
					+ projectName);
			logger.debug("Processed line = " + processedLine);
		}
		if (!replaced) {
			replacedLines.add(originalLine);
		}
	}

	private Optional<ProjectInfo> getProjectInfo(Path repositoryContentsPath) {
		File contentDirectory = repositoryContentsPath.toFile();
		File pomFile = new File(contentDirectory, "pom.xml");
		if (pomFile.exists()) {
			PomReader pomReader = new PomReader();
			Model model = pomReader.readPom(pomFile);
			ProjectInfo projectInfo = new ProjectInfo(model.getName(), model.getGroupId(), model.getArtifactId(), model.getVersion());
			return Optional.of(projectInfo);
		}
		// TODO search settings.gradle
		return Optional.empty();
	}

	private File createProjectDirectory(String projectName) {
		File workingDirectory = IoUtils.getWorkingDirectory();
		String projectNameToUse = projectName.replaceAll(" ", "_");
		File projectDirectory = new File(workingDirectory, projectNameToUse);
		IoUtils.createDirectory(projectDirectory);
		logger.debug("Created directory " + projectDirectory);
		return projectDirectory;
	}

	private Optional<String> getRootPackageName(Path workingPath) {
		// Derive fromPackage using location of existing @SpringBootApplication class.
		// TODO warning if find multiple @SpringBootApplication classes.
		RootPackageFinder rootPackageFinder = new RootPackageFinder();
		logger.debug("Looking for @SpringBootApplication in directory " + workingPath.toFile());
		Optional<String> rootPackage = rootPackageFinder.findRootPackage(workingPath.toFile());
		if (rootPackage.isEmpty()) {
			AttributedStringBuilder sb = new AttributedStringBuilder();
			sb.append("Could find root package containing class with @SpringBootApplication.  No Java Package refactoring from the template will occur.")
					.style(sb.style().foreground(AttributedStyle.YELLOW));
			System.out.println(sb.toAnsi());
			return Optional.empty();
		}

		return rootPackage;
	}

	private void refactorPackage(String targetPackageName, String fromPackage, Path workingPath) {
		logger.debug("Refactoring to package name " + targetPackageName);
		JavaParser javaParser = new Java11Parser.Builder().build();
		FileTypeCollectingFileVisitor collector = new FileTypeCollectingFileVisitor(".java");
		try {
			Files.walkFileTree(workingPath, collector);
		}
		catch (IOException e) {
			throw new UpException("Failed reading files in " + workingPath, e);
		}
		List<? extends SourceFile> compilationUnits = javaParser.parse(collector.getMatches(), null, null);
		ResultsExecutor container = new ResultsExecutor();

		Recipe recipe = new ChangePackage(fromPackage, targetPackageName, true);
		container.addAll(recipe.run(compilationUnits));
		try {
			container.execute();
		}
		catch (IOException e) {
			throw new UpException("Error performing refactoring", e);
		}

		//TODO change groupId and artifactId
	}


}
