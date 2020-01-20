package org.jkarma.examples.purchases;

import org.jkarma.mining.joiners.Joiner;
import org.jkarma.mining.providers.Context;

public class MixedProductJoiner implements Joiner<String> {

    @Override
    public boolean testPrecondition(String p1, String p2, Context ctx, int height) {
        return false;
    }

    @Override
    public String apply(String p1, String p2, int height) {
        return p2;
    }

    @Override
    public boolean testPostcondition(String p, Context arg1, int length) {
        return false;
    }
}
