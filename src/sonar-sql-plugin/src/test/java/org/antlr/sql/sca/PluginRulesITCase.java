package org.antlr.sql.sca;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.antlr.sql.dialects.Dialects;
import org.antlr.sql.dialects.SQLDialectRules;
import org.antlr.sql.models.AntlrContext;
import org.antlr.sql.tools.PrettyPrinter;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.sonar.plugins.sql.models.rules.Rule;
import org.sonar.plugins.sql.models.rules.SqlRules;

@RunWith(Parameterized.class)
public class PluginRulesITCase {

	private final IssuesProvider sut = new IssuesProvider();

	@Parameters(name = "{3}: {1} - {0} ({index})")
	public static Iterable<Object[]> data() throws Throwable {

		final List<Object[]> data = new ArrayList<>();

		List<SqlRules> rules = SQLDialectRules.INSTANCE.getRules();

		for (final SqlRules rule : rules) {
			for (final Rule r : rule.getRule()) {
				r.getRuleImplementation().getViolatingRulesCodeExamples().getRuleCodeExample().forEach(t -> {
					data.add(new Object[] { t, r.getKey(), r, Dialects.valueOf(rule.getDialect().toUpperCase()), true

					});

				});

				r.getRuleImplementation().getCompliantRulesCodeExamples().getRuleCodeExample().forEach(t -> {
					data.add(new Object[] { t, r.getKey(), r, Dialects.valueOf(rule.getDialect().toUpperCase()), false

					});

				});
			}
		}

		return data;
	}

	@Test
	public void test() throws Throwable {
		AntlrContext ctx = dialect.parse(text);
		SqlRules a = new SqlRules();
		a.getRule().add(rule);
		ctx.rules = Arrays.asList(a);
		List<org.sonar.plugins.sql.issues.SqlIssue> issues = sut.analyze(ctx);

		if (issueExists && issues.isEmpty() || !issueExists && !issues.isEmpty()) {
			PrettyPrinter.print(ctx.root, 0, ctx.stream);
		}

		Assert.assertTrue("Found issues on : " + text + " for rule: " + rule.getKey(),
				!issues.isEmpty() == issueExists);

	}

	@Test
	public void testRuleContainsViolationMessgae() throws Throwable {

		Assert.assertNotNull("Rule " + rule.getKey() + " does not contain violation message",
				rule.getRuleImplementation().getRuleViolationMessage());

	}

	private String text;
	private Rule rule;
	private Dialects dialect;
	private boolean issueExists;

	public PluginRulesITCase(String text, String ruleId, Rule rule, Dialects dialect, boolean issueExists) {
		this.text = text;
		this.rule = rule;
		this.dialect = dialect;
		this.issueExists = issueExists;

	}
}
