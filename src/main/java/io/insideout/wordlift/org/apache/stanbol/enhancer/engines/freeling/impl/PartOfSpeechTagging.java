package io.insideout.wordlift.org.apache.stanbol.enhancer.engines.freeling.impl;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.upc.freeling.ChartParser;
import edu.upc.freeling.DepTxala;
import edu.upc.freeling.Depnode;
import edu.upc.freeling.HmmTagger;
import edu.upc.freeling.ListSentence;
import edu.upc.freeling.ListWord;
import edu.upc.freeling.Maco;
import edu.upc.freeling.MacoOptions;
import edu.upc.freeling.Nec;
import edu.upc.freeling.Sentence;
import edu.upc.freeling.Splitter;
import edu.upc.freeling.Tokenizer;
import edu.upc.freeling.TreeDepnode;
import edu.upc.freeling.TreeNode;
import edu.upc.freeling.UkbWrap;
import edu.upc.freeling.Util;
import edu.upc.freeling.Word;

public class PartOfSpeechTagging {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final static Freeling freeling = new Freeling();

    public void tag(FreelingProperties properties, String text) {

        logger.trace("Setting locale [{}].", properties.getLocale());

        Util.initLocale(properties.getLocale());

        logger.info("Setting language [{}].", properties.getLanguage());

        MacoOptions macoOptions = new MacoOptions(properties.getLanguage());

        logger.info(
            "Setting MACO options [{}][{}][{}][{}][{}][{}][{}][{}][{}][{}][{}].",
            new Object[] {false, properties.isAffixAnalysis(), properties.isMultiwordsDetection(),
                          properties.isNumbersDetection(), properties.isPunctuationDetection(),
                          properties.isDatesDetection(), properties.isQuantitiesDetection(),
                          properties.isDictionarySearch(), properties.isProbabilityAssignment(),
                          properties.isNeRecognition(), properties.isOrtographicCorrection()});
        macoOptions.setActiveModules(false, properties.isAffixAnalysis(), properties.isMultiwordsDetection(),
            properties.isNumbersDetection(), properties.isPunctuationDetection(),
            properties.isDatesDetection(), properties.isQuantitiesDetection(),
            properties.isDictionarySearch(), properties.isProbabilityAssignment(),
            properties.isNeRecognition(), properties.isOrtographicCorrection());

        logger.info(
            "Setting MACO data files [{}][{}][{}][{}][{}][{}][{}][{}][{}].",
            new Object[] {"", properties.getLocutionsFile(), properties.getQuantitiesFile(),
                          properties.getAffixFile(), properties.getProbabilityFile(),
                          properties.getDictionaryFile(), properties.getNpDataFile(),
                          properties.getPunctuationFile(), properties.getCorrectorFile()});
        macoOptions.setDataFiles("", properties.getLocutionsFile(), properties.getQuantitiesFile(),
            properties.getAffixFile(), properties.getProbabilityFile(), properties.getDictionaryFile(),
            properties.getNpDataFile(), properties.getPunctuationFile(), properties.getCorrectorFile());

        logger.info("Creating the tokenizer [{}].", properties.getTokenizerFile());
        // Create analyzers.
        Tokenizer tokenizer = new Tokenizer(properties.getTokenizerFile());

        logger.info("Creating the splitter [{}].", properties.getSplitterFile());
        Splitter splitter = new Splitter(properties.getSplitterFile());

        logger.info("Creating the MACO analyzer.");
        Maco maco = new Maco(macoOptions);

        logger.info("Creating the tagger.");
        HmmTagger hmmTagger = new HmmTagger(properties.getLanguage(), properties.getTaggerHMMFile(),
                properties.isTaggerRetokenize(), properties.getTaggerForceSelect());

        ChartParser chartParser = null;
        File grammarFile = new File(properties.getGrammarFile());
        if (grammarFile.exists() && !grammarFile.isDirectory()) {
            logger.info("Creating the chart parser.");
            chartParser = new ChartParser(properties.getGrammarFile());
        }

        DepTxala depTxala = null;
        File depTxalaFile = new File(properties.getDepTxalaFile());
        if (null != chartParser && depTxalaFile.exists() && !depTxalaFile.isDirectory()) {
            logger.info("Creating the dependencies analyzer.");
            depTxala = new DepTxala(properties.getDepTxalaFile(), chartParser.getStartSymbol());
        }

        Nec nec = null;
        File necFile = new File(properties.getNecFile());
        if (necFile.exists() && !necFile.isDirectory()) {
            logger.info("Creating the named entity classification.");
            nec = new Nec(properties.getNecFile());
        }

        logger.info("Creating the disambiguation tool.");
        UkbWrap ukbWrap = new UkbWrap(properties.getUkbConfigFile());

        // Instead of "UkbWrap", you can use a "Senses" object, that simply
        // gives all possible WN senses, sorted by frequency.
        // Senses dis = new Senses(DATA+LANG+"/senses.dat");
        //
        // Make sure the encoding matches your input text (utf-8, iso-8859-15, ...)
        // BufferedReader input = new BufferedReader(new InputStreamReader(System.in, "utf-8"));

        // Extract the tokens from the line of text.
        ListWord listWord = tokenizer.tokenize(text);

        // Split the tokens into distinct sentences.
        ListSentence listSentence = splitter.split(listWord, properties.isAlwaysFlush());

        // Perform morphological analysis
        maco.analyze(listSentence);

        // Perform part-of-speech tagging.
        hmmTagger.analyze(listSentence);

        if (null != nec) {
            // Perform named entity (NE) classificiation.
            nec.analyze(listSentence);
        }

        // sen.analyze(ls);
        ukbWrap.analyze(listSentence);

        // collect nouns.
        Set<String> nouns = getNouns(listSentence);

        // Chunk parser
        if (null != chartParser) chartParser.analyze(listSentence);

        // Dependency parser
        if (null != depTxala) depTxala.analyze(listSentence);

        for (String noun : nouns)
            System.out.println(noun);
    }

    private Set<String> getNouns(ListSentence listSentence) {
        Set<String> nouns = new HashSet<String>();

        // get the analyzed words out of ls.
        for (int i = 0; i < listSentence.size(); i++) {
            Sentence sentence = listSentence.get(i);

            for (int j = 0; j < sentence.size(); j++) {
                Word word = sentence.get(j);

                if (!"NP".equals(word.getShortTag())) continue;

                nouns.add(word.getForm().replace("_", " "));

                logger.info(
                    "[form :: {}][lc_form :: {}][lemma :: {}][tag :: {}][short_tag :: {}].",
                    new Object[] {word.getForm(), word.getLcForm(), word.getLemma(), word.getTag(),
                                  word.getShortTag()});
            }
        }

        return nouns;
    }

    private static void printSenses(Word w) {
        String ss = w.getSensesString();

        // The senses for a FreeLing word are a list of
        // pair<string,double> (sense and page rank). From java, we
        // have to get them as a string with format
        // sense:rank/sense:rank/sense:rank
        // which will have to be splitted to obtain the info.
        //
        // Here, we just output it:
        System.out.print(" " + ss);
    }

    private static void printResults(ListSentence ls, String format) {
        if (format == "parsed") {
            System.out.println("-------- CHUNKER results -----------");

            for (int i = 0; i < ls.size(); i++) {
                TreeNode tree = ls.get(i).getParseTree();
                printParseTree(0, tree);
            }
        } else if (format == "dep") {
            System.out.println("-------- DEPENDENCY PARSER results -----------");

            for (int i = 0; i < ls.size(); i++) {
                TreeDepnode deptree = ls.get(i).getDepTree();
                printDepTree(0, deptree);
            }
        } else {
            System.out.println("-------- TAGGER results -----------");

            // get the analyzed words out of ls.
            for (int i = 0; i < ls.size(); i++) {
                Sentence s = ls.get(i);

                for (int j = 0; j < s.size(); j++) {
                    Word w = s.get(j);

                    System.out.print(w.getForm() + " " + w.getLemma() + " " + w.getTag());
                    printSenses(w);
                    System.out.println();
                }

                System.out.println();
            }
        }
    }

    private static void printParseTree(int depth, TreeNode tr) {
        Word w;
        TreeNode child;
        long nch;

        // Indentation
        for (int i = 0; i < depth; i++) {
            System.out.print("  ");
        }

        nch = tr.numChildren();

        if (nch == 0) {
            // The node represents a leaf
            if (tr.getInfo().isHead()) {
                System.out.print("+");
            }
            w = tr.getInfo().getWord();
            System.out.print("(" + w.getForm() + " " + w.getLemma() + " " + w.getTag());
            printSenses(w);
            System.out.println(")");
        } else {
            // The node probably represents a tree
            if (tr.getInfo().isHead()) {
                System.out.print("+");
            }

            System.out.println(tr.getInfo().getLabel() + "_[");

            for (int i = 0; i < nch; i++) {
                child = tr.nthChildRef(i);

                if (child != null) {
                    printParseTree(depth + 1, child);
                } else {
                    System.err.println("ERROR: Unexpected NULL child.");
                }
            }
            for (int i = 0; i < depth; i++) {
                System.out.print("  ");
            }

            System.out.println("]");
        }
    }

    private static void printDepTree(int depth, TreeDepnode tr) {
        TreeDepnode child = null;
        TreeDepnode fchild = null;
        Depnode childnode;
        long nch;
        int last, min;
        Boolean trob;

        for (int i = 0; i < depth; i++) {
            System.out.print("  ");
        }

        System.out
                .print(tr.getInfo().getLinkRef().getInfo().getLabel() + "/" + tr.getInfo().getLabel() + "/");

        Word w = tr.getInfo().getWord();

        System.out.print("(" + w.getForm() + " " + w.getLemma() + " " + w.getTag());
        printSenses(w);
        System.out.print(")");

        nch = tr.numChildren();

        if (nch > 0) {
            System.out.println(" [");

            for (int i = 0; i < nch; i++) {
                child = tr.nthChildRef(i);

                if (child != null) {
                    if (!child.getInfo().isChunk()) {
                        printDepTree(depth + 1, child);
                    }
                } else {
                    System.err.println("ERROR: Unexpected NULL child.");
                }
            }

            // Print chunks (in order)
            last = 0;
            trob = true;

            // While an unprinted chunk is found, look for the one with lower
            // chunk_ord value.
            while (trob) {
                trob = false;
                min = 9999;

                for (int i = 0; i < nch; i++) {
                    child = tr.nthChildRef(i);
                    childnode = child.getInfo();

                    if (childnode.isChunk()) {
                        if ((childnode.getChunkOrd() > last) && (childnode.getChunkOrd() < min)) {
                            min = childnode.getChunkOrd();
                            fchild = child;
                            trob = true;
                        }
                    }
                }
                if (trob && (child != null)) {
                    printDepTree(depth + 1, fchild);
                }

                last = min;
            }

            for (int i = 0; i < depth; i++) {
                System.out.print("  ");
            }

            System.out.print("]");
        }

        System.out.println("");
    }

}
