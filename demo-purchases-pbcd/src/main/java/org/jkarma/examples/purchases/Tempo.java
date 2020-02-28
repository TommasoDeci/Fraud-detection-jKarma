package org.jkarma.examples.purchases;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Tempo
{
    public static void main(String[] args) throws IOException {
        List<String> esiste = new ArrayList<>();
        File file = new File("9500-321-150-226.csv");

        BufferedReader br = new BufferedReader(new FileReader(file));

        String st;
        st = br.readLine();
        String parole[];
        int  i;
        while ((st = br.readLine()) != null)
        {
            parole = st.split(",");
            i = 0;
            while(i < parole.length)
            {
                if(!esiste.contains(parole[i]))
                {
                    esiste.add(parole[i]);
                }
                i++;
            }
        }
    System.out.println(esiste.size());
    }
}
