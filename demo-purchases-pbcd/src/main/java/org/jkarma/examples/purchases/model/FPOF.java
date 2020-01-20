package org.jkarma.examples.purchases.model;

import org.jkarma.pbcd.similarities.Similarity;

import java.util.*;

public class FPOF implements Similarity<Boolean>
{
    static private int count = 0;
    static private float max;
    static private Map<String,Float> transactionSup;

    public FPOF(List<Transazione> transazioni)  //passo in input le transazioni di un singolo utente
    {
        if (count == 0) {
            Map<String,Float> itemCount = new HashMap<>();
            Iterator<Transazione> itTrans = transazioni.iterator(); //iteratore su tutte le transazioni
            Iterator<String> itString;
            while(itTrans.hasNext())
            {
                itString = itTrans.next().iterator();  //iteratore sui valori di una singola transazione
                String currentItem;
                while(itString.hasNext())
                {
                    currentItem = itString.next();
                    if(itemCount.containsKey(currentItem)){  //se l'elemento e' gia' stato letto allora incrementa il suo conteggio di 1
                        itemCount.replace(currentItem,itemCount.get(currentItem)+1);
                    } else {                                //altrimenti inseriscilo in Map
                        itemCount.put(currentItem,1f);
                    }
                }
            }

            Set<String> keys = itemCount.keySet();
            itString = keys.iterator();
            String currentKey;
            while(itString.hasNext())
            {
                currentKey = itString.next();
                itemCount.replace(currentKey,itemCount.get(currentKey)/transazioni.size());     //sostituisco tutti i conteggi con il supporto
            }

            transactionSup = new HashMap<>();
            itTrans = transazioni.iterator();           //iteratore su tutte le transazioni
            Transazione currentTrans;
            Integer currentID;
            while(itTrans.hasNext())
            {
                currentTrans = itTrans.next();
                itString = currentTrans.iterator();     //iteratore sui valori di una singola transazione
                currentID = currentTrans.getId();
                float support = 1;     //inizializzo il supporto della transazione a 1 dato che sara' per forza coperta dall'insieme vuoto
                while(itString.hasNext())
                {
                    support += itemCount.get(itString.next());      //il supporto sara' uguale alla somma di tutti i supporti dei valori che la transazione possiede
                }
                transactionSup.put(currentID.toString(),support);   //inserisco il supporto totale della transazione in una Map
            }

            max = Collections.max(transactionSup.values());     //memorizzo il supporto massimo , che fungera' da divisore per il calcolo del FPOF
            for (Map.Entry<String,Float> entry : transactionSup.entrySet())
            {
                System.out.println("TransactionID = " + entry.getKey() + ": " + (entry.getValue() / max));
            }
        }
    }

    public Double apply(Vector<Boolean> u, Vector<Boolean> v) {

        count++;
        return 0.4d;
    }
}
