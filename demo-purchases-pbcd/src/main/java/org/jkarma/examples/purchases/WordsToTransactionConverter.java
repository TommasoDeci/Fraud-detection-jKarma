package org.jkarma.examples.purchases;

import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

import com.univocity.parsers.conversions.Conversion;

public class WordsToTransactionConverter implements Conversion<String, Set<String>>
{
    private final String separator;

    public WordsToTransactionConverter(String... args) {
        String separator = ",";

        if (args.length == 1) {
            separator = args[0];
        }

        this.separator = separator;
    }

    public WordsToTransactionConverter(String separator) {
        this.separator = separator;
    }


    /**
     * Converts a string into a set of products.
     */
    public Set<String> execute(String input) {
        if (input == null) {
            return Collections.emptySet();
        }

        Set<String> out = new TreeSet<String>();
        for (String token : input.split(separator)) {
            //extracting words separated by white space as well
            for (String word : token.trim().split("\\s")) {
                String p = word.trim();
                out.add(p);
            }
        }

        return out;
    }


    /**
     * Converts a set of products into a string.
     */
    public String revert(Set<String> input) {
        if (input == null || input.isEmpty()) {
            return null;
        }
        StringBuilder out = new StringBuilder();

        for (String product : input) {
            if (product == null || product.trim().isEmpty()) {
                continue;
            }
            if (out.length() > 0) {
                out.append(separator);
            }
            out.append(product.trim());
        }

        if (out.length() == 0) {
            return null;
        }

        return out.toString();
    }
}
