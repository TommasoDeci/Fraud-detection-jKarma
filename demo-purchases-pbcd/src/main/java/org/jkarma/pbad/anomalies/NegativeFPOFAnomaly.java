package org.jkarma.pbad.anomalies;

import java.util.HashSet;
import java.util.Set;
import java.util.function.BiFunction;

import org.jkarma.mining.interfaces.ItemSet;
import org.jkarma.mining.interfaces.Lattice;
import org.jkarma.mining.joiners.FrequencyEvaluation;
import org.jkarma.mining.structures.Pair;
import org.jkarma.model.Transaction;
import org.jkarma.pbcd.patterns.Pattern;
import org.jkarma.pbcd.patterns.Patterns;

public class NegativeFPOFAnomaly<A extends Transaction<B>, B extends Comparable<B>, C extends FrequencyEvaluation> 
implements BiFunction<A, Lattice<Pattern<B,C>>, Number>{
	
	private double minSup;
	
	public NegativeFPOFAnomaly(double minSup) {
		this.minSup = minSup;
	}
	
	@Override
	public Number apply(A t, Lattice<Pattern<B, C>> u) {
		double num = 0;
		double den = 0;
		
		//we iterate on the collection of frequent patterns
		for(Pattern<B,C> p : u) {
			if(p.getItemSet().getSuffix()!=null) {
				boolean wasFrequent = Patterns.wasFrequent(p, minSup);
				double freq = Patterns.getRelativeFrequency(p, true);
				
				if(wasFrequent) {
					Set<B> itemset = this.getSet(p);
					if(!t.getItems().containsAll(itemset)) {
						num+=freq;
					}
					den+=freq;
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
