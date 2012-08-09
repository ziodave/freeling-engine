package io.insideout.wordlift.org.apache.stanbol.enhancer.engines.freeling.impl;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.clerezza.rdf.core.UriRef;
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

    @Activate
    protected void activate(ComponentContext context) throws ConfigurationException {
        super.activate(context);

        logger.trace("The Freeling Language Identifier engine is being activated.");
    }

    @Deactivate
    protected void deactivate(ComponentContext context) {
        super.deactivate(context);

        logger.trace("The Freeling Language Identifier engine is being deactivated.");
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

        LanguageIdentifier languageIdentifier = new LanguageIdentifier();
        languageIdentifier.analyzeText(text);

    }
}