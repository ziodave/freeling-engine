package io.insideout.wordlift.org.apache.stanbol.enhancer.engines.freeling.impl;

import io.insideout.wordlift.org.apache.stanbol.domain.Language;

import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.upc.freeling.LangIdent;
import edu.upc.freeling.PairDoubleString;
import edu.upc.freeling.SWIGTYPE_p_std__setT_std__wstring_t;
import edu.upc.freeling.Util;
import edu.upc.freeling.VectorPairDoubleString;

public class LanguageIdentifier {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private LangIdent languageIdentifier = null;

    @SuppressWarnings("unused")
    private final Freeling freeling = new Freeling();

    public LanguageIdentifier(String locale, String configurationPath) {

        logger.trace("Loading Freeling Language Identifier configuration.");

        Util.initLocale(locale);
        languageIdentifier = new LangIdent(configurationPath);

        logger.trace("Freeling Language Identifier configuration loaded.");

    }

    @Override
    protected void finalize() throws Throwable {
        languageIdentifier = null;

        super.finalize();
    }

    public Set<Language> identifyLanguage(String text) {
        return identifyLanguage(text, "");
    }

    public Set<Language> identifyLanguage(String text, String languages) {

        // TODO: check for support of languages like Japanese, Chinese, Russian, Bulgarian, Hindi, ...

        logger.trace("Identifying language.");

        SWIGTYPE_p_std__setT_std__wstring_t allowedLanguages = Util.wstring2set(languages, ",");

        Set<Language> languageSet = identifyMultipleLanguages(text, allowedLanguages);

        if (0 == languageSet.size()) {
            logger.warn("No language has been identified for this Content Item.");
            return null;
        }

        // return the found languages
        return languageSet;
    }

    @SuppressWarnings("unused")
    private String identifyOneLanguage(String text, SWIGTYPE_p_std__setT_std__wstring_t languages) {
        return languageIdentifier.identifyLanguage(text, languages);
    }

    private Set<Language> identifyMultipleLanguages(String text, SWIGTYPE_p_std__setT_std__wstring_t languages) {

        VectorPairDoubleString languageRanks = new VectorPairDoubleString();
        languageIdentifier.rankLanguages(languageRanks, text, languages);

        int size = (int) languageRanks.size();

        Set<Language> languageSet = new HashSet<Language>(size);

        for (long i = 0; i < size; i++) {
            PairDoubleString pair = languageRanks.get((int) i);
            Double rank = pair.getFirst();
            String language = pair.getSecond();

            logger.trace("The language [{}][rank :: {}] has been identified for the provided text.",
                new Object[] {language, rank});

            languageSet.add(new Language(language, rank));
        }

        return languageSet;
    }
}
