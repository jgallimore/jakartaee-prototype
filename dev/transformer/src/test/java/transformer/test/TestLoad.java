package transformer.test;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.ibm.ws.jakarta.transformer.JakartaTransformer;

public class TestLoad {

	public static final String RULES_RENAMES_PATH =
		JakartaTransformer.DEFAULT_RENAMES_REFERENCE;

	public static final String RULES_VERSIONS_PATH =
		JakartaTransformer.DEFAULT_VERSIONS_REFERENCE;

	public static final String TRANSFORMER_PREFIX;

	static {
		String transformerPackageName = JakartaTransformer.class.getPackage().getName();
		TRANSFORMER_PREFIX = transformerPackageName.replace('.',  '/') + '/';
	}

	public static String putIntoTransformer(String path) {
		return TRANSFORMER_PREFIX + path;
	}

	@Test
	public void testRulesLoad() throws IOException {
		testLoad(RULES_RENAMES_PATH);
		testLoad(RULES_VERSIONS_PATH);
	}

	public void testLoad(String path) throws IOException {
		System.out.println("Load [ " + path + " ]");
		String qualifiedRulesPath = putIntoTransformer(path);

		System.out.println("Load (Qualified) [ " + qualifiedRulesPath + " ]");

		InputStream simpleInput = TestUtils.getResourceStream(qualifiedRulesPath);

		List<String> actualLines;
		try {
			actualLines = TestUtils.loadLines(simpleInput);
		} finally {
			simpleInput.close();
		}

		System.out.println("Loaded [ " + path + " ] [ " + actualLines.size() + " ]");
		for ( String line : actualLines ) {
			System.out.println(" [ " + line + " ]");
		}
	}

	public static final String SIMPLE_RESOURCE_PATH = "transformer/test/data/simple.resource";
	public static final String[] SIMPLE_RESOURCE_LINES = {
		"Simple Resource 1",
		"Simple Resource 2"
	};

	@Test
	public void testSimpleLoad() throws IOException {
		System.out.println("Load [ " + SIMPLE_RESOURCE_PATH + " ]");

		InputStream simpleInput = TestUtils.getResourceStream(SIMPLE_RESOURCE_PATH);

		List<String> actualLines;
		try {
			actualLines = TestUtils.loadLines(simpleInput);
		} finally {
			simpleInput.close();
		}

		System.out.println("Loaded [ " + SIMPLE_RESOURCE_PATH + " ] [ " + actualLines.size() + " ]");
		System.out.println("Expected [ " + SIMPLE_RESOURCE_PATH + " ] [ " + SIMPLE_RESOURCE_LINES.length + " ]");

		TestUtils.verify(SIMPLE_RESOURCE_PATH, SIMPLE_RESOURCE_LINES, actualLines);
	}
}
