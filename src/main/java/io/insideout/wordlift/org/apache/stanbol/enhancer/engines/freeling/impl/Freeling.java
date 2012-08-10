package io.insideout.wordlift.org.apache.stanbol.enhancer.engines.freeling.impl;

public class Freeling {

    private final static String libraryPath = "/Users/david/workspaces/freeling/freeling/APIs/java/libfreeling_javaAPI.so";

    static {
        System.load(libraryPath);
    }

}
