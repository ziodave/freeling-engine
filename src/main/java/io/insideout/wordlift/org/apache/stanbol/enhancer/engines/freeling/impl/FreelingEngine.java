package io.insideout.wordlift.org.apache.stanbol.enhancer.engines.freeling.impl;

import java.util.Collections;
import java.util.Map;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.EngineException;
import org.apache.stanbol.enhancer.servicesapi.EnhancementEngine;
import org.apache.stanbol.enhancer.servicesapi.ServiceProperties;
import org.apache.stanbol.enhancer.servicesapi.impl.AbstractEnhancementEngine;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(configurationFactory = true, policy = ConfigurationPolicy.REQUIRE, specVersion = "1.1", metatype = true, immediate = true, inherit = true)
@Service
@org.apache.felix.scr.annotations.Properties(value = {@Property(name = EnhancementEngine.PROPERTY_NAME)})
public class FreelingEngine extends AbstractEnhancementEngine<RuntimeException,RuntimeException> implements
        EnhancementEngine, ServiceProperties {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * The default value for the Execution of this Engine. Currently set to
     * {@link EnhancementJobManager#DEFAULT_ORDER}
     */
    public static final Integer defaultOrder = ORDERING_EXTRACTION_ENHANCEMENT;

    @Activate
    protected void activate(ComponentContext context) throws ConfigurationException {
        super.activate(context);

        logger.trace("The Freeling engine is being activated.");
    }

    @Deactivate
    protected void deactivate(ComponentContext context) {
        super.deactivate(context);

        logger.trace("The Freeling engine is being deactivated.");
    }

    @Override
    public Map<String,Object> getServiceProperties() {
        return Collections.unmodifiableMap(Collections.singletonMap(ENHANCEMENT_ENGINE_ORDERING,
            (Object) defaultOrder));
    }

    @Override
    public int canEnhance(ContentItem ci) throws EngineException {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void computeEnhancements(ContentItem ci) throws EngineException {
        // TODO Auto-generated method stub

    }

}
