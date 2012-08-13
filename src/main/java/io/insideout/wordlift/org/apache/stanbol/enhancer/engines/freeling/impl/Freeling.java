package io.insideout.wordlift.org.apache.stanbol.enhancer.engines.freeling.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Freeling {

    private final static String libraryPath = "/Users/david/workspaces/freeling/freeling/APIs/java/libfreeling_javaAPI.so";

    private final static Logger logger = LoggerFactory.getLogger(Freeling.class);

    public Freeling() {}

    static {
        logger.trace("The Freeling support library is being loaded.");

        System.load(libraryPath);
    }

    public void finalize() {
        logger.trace("The Freeling support library is being unloaded.");
    }

}
