package io.insideout.wordlift.org.apache.stanbol.enhancer.engines.freeling.impl;

import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.DC_LANGUAGE;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.DC_TYPE;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_CONFIDENCE;
import static org.apache.stanbol.enhancer.servicesapi.rdf.TechnicalClasses.DCTERMS_LINGUISTIC_SYSTEM;

import io.insideout.wordlift.org.apache.stanbol.domain.Language;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.clerezza.rdf.core.LiteralFactory;
import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.impl.PlainLiteralImpl;
import org.apache.clerezza.rdf.core.impl.TripleImpl;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.enhancer.servicesapi.Blob;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.EngineException;
import org.apache.stanbol.enhancer.servicesapi.EnhancementEngine;
import org.apache.stanbol.enhancer.servicesapi.InvalidContentException;
import org.apache.stanbol.enhancer.servicesapi.ServiceProperties;
import org.apache.stanbol.enhancer.servicesapi.helper.ContentItemHelper;
import org.apache.stanbol.enhancer.servicesapi.helper.EnhancementEngineHelper;
import org.apache.stanbol.enhancer.servicesapi.impl.AbstractEnhancementEngine;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(configurationFactory = true, policy = ConfigurationPolicy.REQUIRE, specVersion = "1.1", metatype = true, immediate = true, inherit = true)
@Service
@Properties(value = {@Property(name = EnhancementEngine.PROPERTY_NAME, value = "freelingLanguageIdentifier")})
public class FreelingLanguageIdentifierEngine extends
        AbstractEnhancementEngine<RuntimeException,RuntimeException> implements EnhancementEngine,
        ServiceProperties {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    public static final Integer defaultOrder = ORDERING_PRE_PROCESSING - 2;
    private static final String TEXT_PLAIN_MIMETYPE = "text/plain";
    private static final Set<String> SUPPORTED_MIMETYPES = Collections.singleton(TEXT_PLAIN_MIMETYPE);

    private final String locale = "default";
//    private final String libraryPath = "/Users/david/workspaces/freeling/freeling/APIs/java/libfreeling_javaAPI.so";
    private final String configurationPath = "/Users/david/workspaces/io.insideout/wordlift/freeling-engine/src/main/resources/languageIdentifierConfiguration.cfg";
    private final String languages = "";
    private LanguageIdentifier languageIdentifier;

    @Activate
    protected void activate(ComponentContext context) throws ConfigurationException {
        super.activate(context);

        logger.trace("The Freeling Language Identifier engine is being activated.");

        languageIdentifier = new LanguageIdentifier(locale, configurationPath);

    }

    @Deactivate
    protected void deactivate(ComponentContext context) {
        super.deactivate(context);

        logger.trace("The Freeling Language Identifier engine is being deactivated.");

        languageIdentifier = null;

        // ensure the resources used by the languageIdentifier get released.
        System.gc();
    }

    @Override
    public Map<String,Object> getServiceProperties() {
        return Collections.unmodifiableMap(Collections.singletonMap(ENHANCEMENT_ENGINE_ORDERING,
            (Object) defaultOrder));
    }

    @Override
    public int canEnhance(ContentItem ci) throws EngineException {
        if (ContentItemHelper.getBlob(ci, SUPPORTED_MIMETYPES) != null) {
            logger.trace("The Freeling Language Identifier engine can process a Content Item.");
            return ENHANCE_ASYNC; // Langid now supports async processing
        }

        logger.trace("The Freeling Language Identifier engine cannot process this Content Item.");
        return CANNOT_ENHANCE;
    }

    @Override
    public void computeEnhancements(ContentItem ci) throws EngineException {

        Entry<UriRef,Blob> contentPart = ContentItemHelper.getBlob(ci, SUPPORTED_MIMETYPES);

        if (contentPart == null) throw new IllegalStateException(
                "No ContentPart with Mimetype '" + TEXT_PLAIN_MIMETYPE + "' found for ContentItem "
                        + ci.getUri() + ": This is also checked in the canEnhance method! -> This "
                        + "indicated an Bug in the implementation of the " + "EnhancementJobManager!");

        String text = "";

        try {
            text = ContentItemHelper.getText(contentPart.getValue());
        } catch (IOException e) {
            throw new InvalidContentException(this, ci, e);
        }

        if (text.trim().length() == 0) {
            logger.warn("No text contained in ContentPart {} of ContentItem {}", contentPart.getKey(),
                ci.getUri());
            return;
        }

        logger.trace("The Freeling Language Identifier engine received the following text for analysis:\n{}",
            text);

        Set<Language> identifiedLanguages = languageIdentifier.identifyLanguage(text, languages);

        // return if no languages have been found.
        if (null == identifiedLanguages || 0 == identifiedLanguages.size()) return;

        MGraph g = ci.getMetadata();
        ci.getLock().writeLock().lock();

        try {
            for (Language language : identifiedLanguages) {
                UriRef textEnhancement = EnhancementEngineHelper.createTextEnhancement(ci, this);
                g.add(new TripleImpl(textEnhancement, DC_LANGUAGE, new PlainLiteralImpl(language
                        .getTwoLetterCode())));
                g.add(new TripleImpl(textEnhancement, ENHANCER_CONFIDENCE, LiteralFactory.getInstance()
                        .createTypedLiteral(language.getRank())));
                g.add(new TripleImpl(textEnhancement, DC_TYPE, DCTERMS_LINGUISTIC_SYSTEM));

            }
        } finally {
            ci.getLock().writeLock().unlock();
        }

    }
}
