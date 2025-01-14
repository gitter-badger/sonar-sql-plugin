package org.sonar.plugins.sql.sensors;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.sonar.api.batch.fs.FilePredicates;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.rule.Severity;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.issue.NewExternalIssue;
import org.sonar.api.batch.sensor.issue.NewIssueLocation;
import org.sonar.api.rules.RuleType;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.plugins.sql.Constants;
import org.sonar.plugins.sql.issues.SqlIssue;
import org.sonar.plugins.sql.issues.SqlIssuesList;

public class BaseSensor {

	private static final Logger LOGGER = Loggers.get(BaseSensor.class);

	public static List<InputFile> find(SensorContext context, String path) throws IOException {
		FilePredicates p = context.fileSystem().predicates();
		Set<InputFile> files = new HashSet<>();
		URI uri = new File(path).getCanonicalFile().toURI();

		context.fileSystem().inputFiles(p.hasLanguage(Constants.languageKey)).forEach(i -> {
			LOGGER.debug(() -> {
				return "Trying to match: " + i.uri() + " against " + uri;
			});

			if (uri.equals(i.uri())) {
				files.add(i);
			}
		});
		return new ArrayList<>(files);
	}

	public static List<InputFile> findByName(SensorContext context, String path) {
		final FilePredicates p = context.fileSystem().predicates();
		final Set<InputFile> files = new HashSet<>();
		final String search = path.replace("[", "").replace("]", "");
		final String temp[] = search.split("\\.");
		final String name = temp[temp.length - 1];
		context.fileSystem().inputFiles(p.hasLanguage(Constants.languageKey)).forEach(i -> {

			final File file = new File(i.uri());

			LOGGER.debug(() -> {
				return "Trying to match: " + i.uri() + " against " + search;
			});

			// schema.name.sql
			if (search.equals(FilenameUtils.getBaseName(file.getAbsolutePath()).replace("[", "").replace("]", ""))) {
				files.add(i);
				return;
			}
			// schema/name.sql
			if (search.equals(file.getParentFile().getName().replace("[", "").replace("]", "") + "."
					+ FilenameUtils.getBaseName(file.getAbsolutePath().replace("[", "").replace("]", "")))) {
				files.add(i);

			}
			// name.sql
			if (name.equals(FilenameUtils.getBaseName(file.getAbsolutePath().replace("[", "").replace("]", "")))) {
				files.add(i);
			}

		});
		return new ArrayList<>(files);
	}

	protected static void addIssues(SensorContext context, final SqlIssuesList issues, final InputFile file)
			throws IOException {
		synchronized (context) {
			for (final Entry<String, Set<SqlIssue>> fileIssues : issues.getIssues().entrySet()) {

				String fileName = fileIssues.getKey();

				InputFile main = file;
				if (main == null) {
					final List<InputFile> files = find(context, fileName);
					if (files.isEmpty()) {
						LOGGER.debug("Was not able to find file {} to add issues", fileName);
						continue;
					}
					main = files.get(0);
				}

				for (final SqlIssue issue : fileIssues.getValue()) {
					try {

						if (issue.isAdhoc()) {
							context.newAdHocRule().description(issue.getDescription()).engineId(issue.getRepo())
									.name(issue.getName()).ruleId(issue.getKey())
									.severity(extractSeverity(issue.getSeverity())).type(RuleType.valueOf(issue.getRuleType())).save();
						}

						final NewExternalIssue newExternalIssue = context.newExternalIssue().ruleId(issue.getKey())
								.engineId(issue.getRepo()).type(RuleType.valueOf(issue.getRuleType()));
						final NewIssueLocation location = newExternalIssue.newLocation().on(main)
								.message(issue.getMessage());
						if (issue.getLine() > 0) {
							location.at(main.selectLine(issue.getLine()));
						}
						newExternalIssue.at(location).severity(extractSeverity(issue.getSeverity())).save();
					} catch (Throwable e) {
						LOGGER.warn("Unexpected error adding issue on file " + fileName, e);

					}
				}
			}
		}
	}

	protected static final Severity extractSeverity(final String severityValue) {
		String severity = "MAJOR";
		if (severityValue != null) {
			severity = severityValue.toUpperCase();
		}
		if ("ERROR".equalsIgnoreCase(severity)) {
			return Severity.CRITICAL;
		}
		if ("WARNING".equalsIgnoreCase(severity)) {
			return Severity.MAJOR;
		}
		if (StringUtils.containsIgnoreCase(severityValue, "HIGH")) {
			return Severity.MAJOR;
		}
		if (StringUtils.containsIgnoreCase(severityValue, "LOW")) {
			return Severity.INFO;
		}
		try {
			return Severity.valueOf(severity);
		} catch (Exception e) {

		}

		return Severity.MAJOR;

	}
}
