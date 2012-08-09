package io.insideout.wordlift.org.apache.stanbol.enhancer.engines.freeling.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.upc.freeling.LangIdent;
import edu.upc.freeling.SWIGTYPE_p_std__setT_std__wstring_t;
import edu.upc.freeling.Util;

public class LanguageIdentifier {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    public void analyzeText(String text) {

        logger.trace("Loading Freeling library.");

        System.load("/Users/david/workspaces/freeling/freeling/APIs/java/libfreeling_javaAPI.so");

        logger.trace("Loading Freeling Language Identifier configuration.");

        LangIdent languageIdentifier = new LangIdent(
                "/Users/david/workspaces/io.insideout/wordlift/freeling-engine/src/main/resources/languageIdentifierConfiguration.cfg");

        logger.trace("Identifying text.");

        SWIGTYPE_p_std__setT_std__wstring_t emptySet = Util.wstring2set("", ",");

        String language = languageIdentifier.identifyLanguage(text, emptySet);

        logger.trace("The language [{}] has been identified for the provided text.", language);

    }
}
