package io.insideout.wordlift.org.apache.stanbol.services;

import java.io.IOException;
import java.util.Collections;
import java.util.Map.Entry;

import org.apache.clerezza.rdf.core.UriRef;
import org.apache.stanbol.enhancer.servicesapi.Blob;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.helper.ContentItemHelper;

public class StanbolService {

    private static final String TEXT_PLAIN_MIMETYPE = "text/plain";

    public static String getTextFromContentItem(ContentItem ci) {
        Entry<UriRef,Blob> contentPart = ContentItemHelper.getBlob(ci,
            Collections.singleton(TEXT_PLAIN_MIMETYPE));

        if (contentPart == null) throw new IllegalStateException(
                "No ContentPart with Mimetype '" + TEXT_PLAIN_MIMETYPE + "' found for ContentItem "
                        + ci.getUri() + ": This is also checked in the canEnhance method! -> This "
                        + "indicated an Bug in the implementation of the " + "EnhancementJobManager!");

        try {
            return ContentItemHelper.getText(contentPart.getValue());
        } catch (IOException e) {
            return null;
        }
    }

    // public static Entityhub getEntityhub(ComponentContext context) {
    //
    // // according to http://incubator.apache.org/stanbol/docs/trunk/components/entityhub/
    // // "An Apache Stanbol instance can only have a single Entityhub".
    //
    // ServiceReference entityHubServiceReference = context.getBundleContext().getServiceReference(
    // Entityhub.class.getName());
    // return (Entityhub) context.getBundleContext().getService(entityHubServiceReference);
    // }
    //
    // public static Site getSite(ComponentContext context) {
    //
    // // according to http://incubator.apache.org/stanbol/docs/trunk/components/entityhub/
    // // "An Apache Stanbol instance can only have a single Entityhub".
    //
    // ServiceReference entityHubServiceReference = context.getBundleContext().getServiceReference(
    // Site.class.getName());
    // return (Site) context.getBundleContext().getService(entityHubServiceReference);
    // }

}
