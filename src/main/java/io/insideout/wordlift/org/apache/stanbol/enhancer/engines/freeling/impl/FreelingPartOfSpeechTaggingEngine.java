package io.insideout.wordlift.org.apache.stanbol.enhancer.engines.freeling.impl;

import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.DC_TYPE;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_CONFIDENCE;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_END;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_SELECTED_TEXT;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_START;
import io.insideout.wordlift.org.apache.stanbol.domain.Noun;
import io.insideout.wordlift.org.apache.stanbol.services.StanbolService;

import java.io.File;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.apache.clerezza.rdf.core.Language;
import org.apache.clerezza.rdf.core.LiteralFactory;
import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.impl.PlainLiteralImpl;
import org.apache.clerezza.rdf.core.impl.TripleImpl;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.EngineException;
import org.apache.stanbol.enhancer.servicesapi.EnhancementEngine;
import org.apache.stanbol.enhancer.servicesapi.EnhancementJobManager;
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
@org.apache.felix.scr.annotations.Properties(value = {@Property(name = EnhancementEngine.PROPERTY_NAME)})
public class FreelingPartOfSpeechTaggingEngine extends
        AbstractEnhancementEngine<RuntimeException,RuntimeException> implements EnhancementEngine,
        ServiceProperties {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private static final String TEXT_PLAIN_MIMETYPE = "text/plain";
    private static final Set<String> SUPPORTED_MIMETYPES = Collections.singleton(TEXT_PLAIN_MIMETYPE);

    @SuppressWarnings("unused")
    private final Freeling freeling = new Freeling();

    private final String freelingSharePath = "/usr/local/Cellar/freeling/HEAD/share/freeling";
    private final String configurationPath = "/usr/local/Cellar/freeling/HEAD/share/freeling/config";
    private final String configurationFilenameSuffix = ".cfg";

    private PartOfSpeechTagging partOfSpeechTagging;

    /**
     * The default value for the Execution of this Engine. Currently set to
     * {@link EnhancementJobManager#DEFAULT_ORDER}
     */
    public static final Integer defaultOrder = ServiceProperties.ORDERING_CONTENT_EXTRACTION - 2;

    @Activate
    protected void activate(ComponentContext context) throws ConfigurationException {
        super.activate(context);

        logger.trace("The Freeling PoS Tagging engine is being activated.");

        try {
            partOfSpeechTagging = new PartOfSpeechTagging();
        } catch (Exception e) {
            logger.error("An exception [{}] occured while loading the PoS Tagging:\n{}",
                new Object[] {e.getClass(), e.getMessage()}, e);
        }
    }

    @Deactivate
    protected void deactivate(ComponentContext context) {
        super.deactivate(context);

        logger.trace("The Freeling engine is being deactivated.");

        // ensure the Freeling support library gets released.
        partOfSpeechTagging = null;
        System.gc();
    }

    @Override
    public Map<String,Object> getServiceProperties() {
        return Collections.unmodifiableMap(Collections.singletonMap(ENHANCEMENT_ENGINE_ORDERING,
            (Object) defaultOrder));
    }

    @Override
    public int canEnhance(ContentItem ci) throws EngineException {

        if (null != ContentItemHelper.getBlob(ci, SUPPORTED_MIMETYPES)) {

            String language = EnhancementEngineHelper.getLanguage(ci);
            String propertiesFilePath = String.format("%s/%s%s", configurationPath, language,
                configurationFilenameSuffix);

            File propertiesFile = new File(propertiesFilePath);
            if (propertiesFile.exists() && propertiesFile.isFile()) return ENHANCE_ASYNC;
        }

        return CANNOT_ENHANCE;
    }

    @Override
    public void computeEnhancements(ContentItem ci) throws EngineException {
        String languageTwoLetterCode = EnhancementEngineHelper.getLanguage(ci);

        String propertiesFilePath = String.format("%s/%s%s", configurationPath, languageTwoLetterCode,
            configurationFilenameSuffix);

        logger.info("Reading properties from configuration file [{}]", propertiesFilePath);

        FreelingProperties freelingProperties = new FreelingProperties(propertiesFilePath, freelingSharePath);

        logger.info("[locale :: {}]", freelingProperties.getLocale());

        String text = StanbolService.getTextFromContentItem(ci);

        if (null == text || 0 == text.trim().length()) throw new InvalidContentException(this, ci, null);

        Set<Noun> nouns = partOfSpeechTagging.getNouns(freelingProperties, text);

        Language language = new Language(languageTwoLetterCode);
        MGraph g = ci.getMetadata();
        ci.getLock().writeLock().lock();
        try {
            for (Noun noun : nouns) {

                UriRef textAnnotation = EnhancementEngineHelper.createTextEnhancement(ci, this);
                g.add(new TripleImpl(textAnnotation, ENHANCER_SELECTED_TEXT, new PlainLiteralImpl(noun
                        .getWord(), language)));
                // g.add(new TripleImpl(textAnnotation, ENHANCER_SELECTION_CONTEXT, new PlainLiteralImpl(
                // occurrence.context, language)));
                g.add(new TripleImpl(textAnnotation, DC_TYPE, new UriRef(
                        "http://www.w3.org/2002/07/owl#Thing")));
                g.add(new TripleImpl(textAnnotation, ENHANCER_CONFIDENCE, LiteralFactory.getInstance()
                        .createTypedLiteral(noun.getConfidence())));
                // if (occurrence.start != null && occurrence.end != null) {
                g.add(new TripleImpl(textAnnotation, ENHANCER_START, LiteralFactory.getInstance()
                        .createTypedLiteral(noun.getStart())));
                g.add(new TripleImpl(textAnnotation, ENHANCER_END, LiteralFactory.getInstance()
                        .createTypedLiteral(noun.getEnd())));
            }
        } finally {
            ci.getLock().writeLock().unlock();
        }
    }
}
