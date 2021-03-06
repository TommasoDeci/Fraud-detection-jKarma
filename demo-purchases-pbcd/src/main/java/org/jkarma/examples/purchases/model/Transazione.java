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

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.jkarma.model.Transaction;


/**
 * Class defining a purchase as a transaction of products.
 * The purchase follows the transactional notation. Therefore
 * it is uniquely identified by a transaction id (tid), it is
 * registered at a given timestamp and contains a set of products
 * purchased together.
 * @author Angelo Impedovo
 */
public class Transazione implements Transaction<String>
{
	private List<String> valori;
	private Instant timestamp;
	private Integer ID;
	private String label;

	public Transazione(String[] trans)
	{
		timestamp = Instant.now();
		ID = Integer.parseInt(trans[0]);
		valori = new ArrayList<>();
		int i = 2;		//salto il transactionID e il cardID
		while(i < trans.length-1)		//salto anche l'ultimo attributo, ovvero quello di classe (TRUE - FALSE)
		{
			valori.add(trans[i]);
			i++;
		}
		label = trans[i];	//qui metto TRUE o FALSE, in modo che jKarma non lo consideri nella creazione dei patten
	}

	public int getId()
	{
		return ID;
	}

	public Collection<String> getItems()
	{
		return valori;
	}

	public Instant getTimestamp()
	{
		return timestamp;
	}

	public Iterator<String> iterator()
	{
		return valori.iterator();
	}

	public String getLabel()
	{
		return label;
	}

	public String toString()
	{
		return ID.toString();
	}

	public boolean isAnomaly() {
		return this.label.equalsIgnoreCase("true");
	}
	
	public boolean isNormal() {
		return !this.isAnomaly();
	}
}
