package savorhub.runners;

import io.cucumber.junit.platform.engine.Constants;
import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;

/**
 * Entry point for running Cucumber through the JUnit 5 platform.
 * @SelectClasspathResource points at src/test/resources/features;
 * GLUE_PROPERTY_NAME lists the packages holding step definitions and hooks.
 */
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features")
@ConfigurationParameter(key = Constants.GLUE_PROPERTY_NAME, value = "savorhub.steps,savorhub.hooks")
public class TestRunner {
}
