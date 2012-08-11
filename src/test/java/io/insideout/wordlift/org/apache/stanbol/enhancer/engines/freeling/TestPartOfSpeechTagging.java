package io.insideout.wordlift.org.apache.stanbol.enhancer.engines.freeling;

import io.insideout.wordlift.org.apache.stanbol.enhancer.engines.freeling.impl.FreelingProperties;
import io.insideout.wordlift.org.apache.stanbol.enhancer.engines.freeling.impl.PartOfSpeechTagging;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestPartOfSpeechTagging {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final String freelingSharePath = "/usr/local/Cellar/freeling/HEAD/share/freeling";
    private final String configurationPath = "/usr/local/Cellar/freeling/HEAD/share/freeling/config";
    private final String configurationFilenameSuffix = ".cfg";

    @Test
    public void test() {
        // testLanguage("bg");
        testLanguage("ca");
        // testLanguage("cs");
        // testLanguage("de");
        testLanguage("en");
        testLanguage("es");
        // testLanguage("fr");
        testLanguage("gl");
        // testLanguage("hi");
        // testLanguage("hr");
        testLanguage("it");
        // testLanguage("ja");
        testLanguage("pt");
        // testLanguage("sk");
        // testLanguage("sl");
        // testLanguage("sr");
        // testLanguage("zh");
    }

    private void testLanguage(String language) {

        String propertiesFilePath = String.format("%s/%s%s", configurationPath, language,
            configurationFilenameSuffix);

        logger.info("Reading properties from configuration file [{}]", propertiesFilePath);

        FreelingProperties freelingProperties = new FreelingProperties(propertiesFilePath, freelingSharePath);
        logger.info("[locale :: {}]", freelingProperties.getLocale());

        String text = TestUtils.getText(String.format("/%s.txt", language));

        PartOfSpeechTagging partOfSpeechTagging = new PartOfSpeechTagging();
        partOfSpeechTagging.getNouns(freelingProperties, text);

    }

}
