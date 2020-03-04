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

public class EPPOFAnomaly<A extends Transaction<B>, B extends Comparable<B>, C extends FrequencyEvaluation> 
implements BiFunction<A, Lattice<Pattern<B,C>>, Number>{
	
	private double minSup;
	private double minGr;
	
	public EPPOFAnomaly(double minSup, double minGr) {
		this.minSup = minSup;
		this.minGr = minGr;
	}
	
	@Override
	public Number apply(A t, Lattice<Pattern<B, C>> u) {
		double num = 0;
		double den = 0;
		
		//we iterate on the collection of frequent patterns
		for(Pattern<B,C> p : u) {
			if(p.getItemSet().getSuffix()!=null) {
				boolean isEmerging = Patterns.isEmerging(p, minGr);
				double freq1 = Patterns.getRelativeFrequency(p, true);
				double freq2 = Patterns.getRelativeFrequency(p, false);
				
				if(freq1 > freq2 && isEmerging) {
					Set<B> itemset = this.getSet(p);
					if(t.getItems().containsAll(itemset)) {
						num+=1.0;
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
