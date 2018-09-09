/*
 * Copyright 2016 DiffPlug
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.diffplug.gradle.spotless;

import static java.util.Objects.requireNonNull;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.gradle.api.Project;

import com.diffplug.common.base.Strings;
import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.extra.npm.TsFmtFormatterStep;

public class TypescriptExtension extends FormatExtension {

	static final String NAME = "typescript";

	public TypescriptExtension(SpotlessExtension root) {
		super(root);
	}

	public TypescriptFormatExtension tsfmt() {
		TypescriptFormatExtension tsfmt = new TypescriptFormatExtension();
		addStep(tsfmt.createStep());
		return tsfmt;
	}

	public class TypescriptFormatExtension extends NpmStepConfig<TypescriptFormatExtension> {

		protected Map<String, Object> config = Collections.emptyMap();

		protected String configFileType = null;

		protected String configFilePath = null;

		public TypescriptFormatExtension config(Map<String, Object> config) {
			this.config = new TreeMap<>(requireNonNull(config));
			replaceStep(createStep());
			return this;
		}

		public TypescriptFormatExtension configFile(String filetype, String path) {
			this.configFileType = requireNonNull(filetype);
			this.configFilePath = path; // might be null for 'editorconfig'
			replaceStep(createStep());
			return this;
		}

		public FormatterStep createStep() {
			final Project project = getProject();

			Map<String, Object> tsFmtCliOptions = createTsFmtCliOptions();

			return TsFmtFormatterStep.create(
					GradleProvisioner.fromProject(project),
					project.getBuildDir(),
					npmFileOrNull(),
					tsFmtCliOptions,
					config);
		}

		private Map<String, Object> createTsFmtCliOptions() {
			Map<String, Object> tsFmtConfig = new TreeMap<>();
			if (!Strings.isNullOrEmpty(this.configFileType)) {
				tsFmtConfig.put(this.configFileType, Boolean.TRUE);
				if (!Strings.isNullOrEmpty(this.configFilePath) && !this.configFileType.equals("editorconfig")) {
					tsFmtConfig.put(this.configFileType + "File", this.configFilePath);
				}
			}
			tsFmtConfig.put("basedir", getProject().getRootDir().getAbsolutePath()); //by default we use our project dir
			return tsFmtConfig;
		}
	}

	private String toJsonString(Map<String, Object> map) {
		final String valueString = map.entrySet()
				.stream()
				.map(entry -> "    " + jsonEscape(entry.getKey()) + ": " + jsonEscape(entry.getValue()))
				.collect(Collectors.joining(",\n"));

		return "{\n" + valueString + "\n}";
	}

	private String jsonEscape(Object val) {
		requireNonNull(val);
		if (val instanceof String) {
			return "\"" + val + "\"";
		}
		return val.toString(); // numbers, booleans - currently nothing else
	}

	@Override
	public PrettierConfig prettier() {
		PrettierConfig prettierConfig = new TypescriptPrettierConfig();
		addStep(prettierConfig.createStep());
		return prettierConfig;
	}

	/**
	 * Overrides the parser to be set to typescript, no matter what the user's config says.
	 */
	public class TypescriptPrettierConfig extends PrettierConfig {
		@Override
		FormatterStep createStep() {
			fixParserToTypescript();
			return super.createStep();
		}

		private void fixParserToTypescript() {
			if (this.prettierConfig == null) {
				this.prettierConfig = Collections.singletonMap("parser", "typescript");
			} else {
				final Object replaced = this.prettierConfig.put("parser", "typescript");
				if (replaced != null) {
					getProject().getLogger().warn("overriding parser option to 'typescript'. Was set to '{}'", replaced);
				}
			}
		}
	}

	@Override
	protected void setupTask(SpotlessTask task) {
		// defaults to all typescript files
		if (target == null) {
			target = parseTarget("**/*.ts");
		}
		super.setupTask(task);
	}
}
