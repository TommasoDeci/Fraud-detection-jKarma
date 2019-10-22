/*******************************************************************************
 * Copyright 2019 Angelo Impedovo
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
package org.jkarma.examples.purchases;

import org.jkarma.examples.purchases.model.Product;
import org.jkarma.mining.joiners.Joiner;
import org.jkarma.mining.providers.Context;

public class MixedProductJoiner implements Joiner<Product> {
	
	@Override
	public boolean testPrecondition(Product p1, Product p2, Context ctx, int height) {
		return true;
	}


	@Override
	public Product apply(Product p1, Product p2, int height) {
		Product result = new Product(p2.getName());
		
		if(p1.alreadyMixed && p2.alreadyMixed) {
			if(height>1) {
				result.alreadyMixed = this.areMixed(p1, p2); 
			}else {
				result.alreadyMixed = true;
			}
		}else {
			result.alreadyMixed = this.areMixed(p1, p2);
		}
		
		return result;
	}

	@Override
	public boolean testPostcondition(Product p, Context arg1, int length) {
		boolean result = true;
		if(length>1) {
			result = p.alreadyMixed;
		}
		return result;
	}
	
	private boolean areMixed(Product p1, Product p2) {
		boolean result = (
			p1.isDrink() && p2.isFood() ||
			p1.isFood() && p2.isDrink()
		);
		return result;
	}
	
}
