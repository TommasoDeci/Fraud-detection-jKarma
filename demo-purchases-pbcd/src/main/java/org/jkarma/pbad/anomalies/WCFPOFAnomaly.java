package org.jkarma.pbad.anomalies;

import java.util.HashSet;
import java.util.Set;
import java.util.function.BiFunction;

import org.jkarma.mining.interfaces.ItemSet;
import org.jkarma.mining.interfaces.Lattice;
import org.jkarma.mining.joiners.ClosureEvaluation;
import org.jkarma.mining.joiners.FrequencyEvaluation;
import org.jkarma.mining.structures.Pair;
import org.jkarma.model.Transaction;
import org.jkarma.pbcd.patterns.Pattern;
import org.jkarma.pbcd.patterns.Patterns;

public class WCFPOFAnomaly<A extends Transaction<B>, B extends Comparable<B>, C extends ClosureEvaluation & FrequencyEvaluation> 
implements BiFunction<A, Lattice<Pattern<B,C>>, Number>{
	
	@Override
	public Number apply(A t, Lattice<Pattern<B, C>> u) {
		double num = 0;
		double den = 0;
		
		//we iterate on the collection of frequent patterns
		for(Pattern<B,C> p : u) {
			if(p.getItemSet().getSuffix()!=null) {
				boolean wasClosed = Patterns.wasClosedByClosure(p);
				double freq = Patterns.getRelativeFrequency(p, true);
				
				if(wasClosed) {
					Set<B> itemset = this.getSet(p);
					if(t.getItems().containsAll(itemset)) {
						double weight = (double)itemset.size()/(double)t.getItems().size();
						num+=freq * weight;
					}
					den+=1.0;
				}
			}
		}
		
		//we return the ratio
		return num/den;
	}
	
	private Set<B> getSet(Pattern<B,C> p) {
		HashSet<B> set = new HashSet<B>();
		ItemSet<B,Pair<C>> currentP = p.getItemSet();
		while(currentP.getSuffix() !=null) {
			set.add(currentP.getSuffix());
			currentP = currentP.getPrefix();
		}
		return set;
	}

}
