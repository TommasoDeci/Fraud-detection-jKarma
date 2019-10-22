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
package org.jkarma.examples.purchases.model;


/**
 * Public class implementing a product to be purchased.
 * For simplicity, a product is defined by his name only.
 * @author Angelo Impedovo
 */
public class Product implements Comparable<Product> {
	
	public static final Product BREAD = new Product("bread");    
    public static final Product JUICE = new Product("juice");
    public static final Product WINE = new Product("wine");
    public static final Product SUGAR = new Product("sugar");
    public static final Product CAKE = new Product("cake");
	
    
    /**
     * Private status flag for itemsets.
     * If true, then the itemset having this product for suffix
     * already mixes drinks and foods. False, otherwise.
     */
    public boolean alreadyMixed;
    
    
    /**
     * The name of the product.
     */
	private String name;
	
	
	/**
	 * Constructs a new product given his name.
	 * @param pname
	 */
	public Product(String pname) {
		this.alreadyMixed = true;
		this.name = pname;
	}
	
	
	/**
	 * Checks whether this product is a drink.
	 * For simplicity we assume juice and wine to be the only drinks.
	 * @return True if wine or juice. False otherwise.
	 */
	public boolean isDrink() {
		return this.name.equalsIgnoreCase("juice") || this.name.equalsIgnoreCase("wine");
	}

	
	/**
	 * Checks whether this product is a food (or, equivalently, not a drink).
	 * @return True if neither wine nor juice, false otherwise
	 */
	public boolean isFood() {
		return !this.isDrink();
	}
	
	
	/**
	 * Returns the name of the product.
	 * @return the name of the product.
	 */
	public String getName() {
		return this.name;
	}
	
	
	@Override
	public int compareTo(Product o) {
		return this.name.compareTo(o.name);
	}
	
	
	@Override
	public boolean equals(Object o) {
		boolean result = false;
		if(o instanceof Product) {
			Product p = (Product)o;
			result = (this.compareTo(p)==0);
		}else {
			throw new ClassCastException();
		}
		
		return result;
	}
	
	@Override
	public String toString() {
		return this.name;
	}

}
