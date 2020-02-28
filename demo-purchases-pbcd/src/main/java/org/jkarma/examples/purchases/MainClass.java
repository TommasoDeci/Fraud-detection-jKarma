package org.jkarma.examples.purchases;

import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import com.github.habernal.confusionmatrix.ConfusionMatrix;
import org.jkarma.examples.purchases.model.Transazione;
import org.jkarma.mining.heuristics.AreaHeuristic;
import org.jkarma.mining.interfaces.ItemSet;
import org.jkarma.mining.joiners.TidSet;
import org.jkarma.mining.providers.TidSetProvider;
import org.jkarma.mining.structures.MiningStrategy;
import org.jkarma.mining.structures.Pair;
import org.jkarma.mining.structures.Strategies;
import org.jkarma.mining.windows.Windows;
import org.jkarma.pbcd.detectors.Detectors;
import org.jkarma.pbcd.detectors.PBCD;
import org.jkarma.pbcd.events.ChangeDescriptionCompletedEvent;
import org.jkarma.pbcd.events.ChangeDescriptionStartedEvent;
import org.jkarma.pbcd.events.ChangeDetectedEvent;
import org.jkarma.pbcd.events.ChangeNotDetectedEvent;
import org.jkarma.pbcd.events.PBCDEventListener;
import org.jkarma.pbcd.events.PatternUpdateCompletedEvent;
import org.jkarma.pbcd.events.PatternUpdateStartedEvent;
import org.jkarma.pbcd.patterns.Pattern;
import org.jkarma.pbcd.patterns.Patterns;
import org.jkarma.pbcd.similarities.WeightedJaccard;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.OptionHandlerFilter;


public class MainClass
{
    @Option(name="-ms", aliases="--minSupport", usage="Threshold above which a pattern is considered")
    public float minSup = 0.5f;

    @Option(name="-mc", aliases="--minChange", usage="Threshold above which two blocks are different")
    public static float minChange = 0.8f;

    @Option(name="-bs", aliases="--blockSize", usage="Number of examples into a single block")
    public static int blockSize = 4;

    @Option(name="-ma", aliases="--maxAnomaly", usage="Threshold below which an example is anomalous")
    public static float maxAnomaly = 0.1f;

    @Option(name="-d", aliases="--depth", usage="Depth of research space of patterns")
    public int depth = 3;

    @Option(name="-f", aliases="--file", usage="Fully qualified path and name of file.", required=true)
    private static String fileName;


    public static Stream<Transazione> getDataset(String fileCSV) throws IOException
    {
        List<Transazione> transazioni = new LinkedList<>();
        File file = new File(fileCSV);

        BufferedReader br = new BufferedReader(new FileReader(file));
        String st;
        String values[];
        st = br.readLine();
        while ((st = br.readLine()) != null)
        {
            values = st.split(",");
            transazioni.add(new Transazione(values));
        }

        return transazioni.stream();
    }

//	public Stream<Transazione> getData() throws FileNotFoundException{
//		return Utils.parseStream(new File(this.fileName), Transazione.class)
//				.sorted(Comparator.comparing(Transazione::getTimestamp));
//	}

    public PBCD<Transazione, String, TidSet, Double> getPBCD()
    {
        //we prepare the time window model and the data accessor
        TidSetProvider<String> accessor = new TidSetProvider<>(Windows.cumulativeSliding());

        AreaHeuristic<String, TidSet> areaHeuristic = new AreaHeuristic<>();
        int k = 25;

        //we instantiate the mining strategy
        MiningStrategy<String, TidSet> strategy = Strategies
                .uponItemsets(new HashSet<String>()).limitDepth(depth).eclat(minSup).beam(accessor,areaHeuristic,k);


        WeightedJaccard measure = new WeightedJaccard();

        return Detectors.upon(strategy)
                .weighted(Patterns::getRelativeFrequency, measure)
                .build(minChange, blockSize);

    }

    public static void main(String[] args) throws IOException
    {
        long t0 = System.currentTimeMillis();
        FileWriter fileWriter = new FileWriter("scores.txt");
        PrintWriter printWriter = new PrintWriter(fileWriter);
        final MainClass mainClass = new MainClass();
        final CmdLineParser argsParser = new CmdLineParser(mainClass);
        try {
            //we parse the command line arguments
            argsParser.parseArgument(args);

            Stream<Transazione> dataset = mainClass.getDataset(fileName);
            PBCD<Transazione,String,TidSet,Double> detector = mainClass.getPBCD();

            AtomicBoolean patternChanged = new AtomicBoolean();
            //we listen for events
            detector.registerListener(
                    new PBCDEventListener<String,TidSet>()
                    {
                        @Override
                        public void patternUpdateCompleted(PatternUpdateCompletedEvent<String, TidSet> arg0) {
                            //do nofing;
                        }

                        @Override
                        public void patternUpdateStarted(PatternUpdateStartedEvent<String, TidSet> arg0) {
                            //do nothing
                        }

                        @Override
                        public void changeDetected(ChangeDetectedEvent<String, TidSet> event)
                        {
                            patternChanged.set(true);
                            System.out.println("change detected: " + event.getAmount());
                            //						System.out.println("\tdescribed by:");
                            //
                            //						//and the associated explanation
                            //						event.getDescription().forEach(p -> {
                            //							double freqReference = p.getFirstEval().getRelativeFrequency()*100;
                            //							double freqTarget = p.getSecondEval().getRelativeFrequency()*100;
                            //
                            //							String message;
                            //							if(freqTarget > freqReference) {
                            //								message="increased frequency from ";
                            //							}else {
                            //								message="decreased frequency from ";
                            //							}
                            //							message+=Double.toString(freqReference)+"% to "+Double.toString(freqTarget)+"%";
                            //							System.out.println("\t\t"+p.getItemSet()+" "+message);
                            //						});
                        }

                        @Override
                        public void changeNotDetected(ChangeNotDetectedEvent<String, TidSet> arg0)
                        {
                            patternChanged.set(false);
                            System.out.println("change not detected: " + arg0.getAmount());
                        }

                        @Override
                        public void changeDescriptionCompleted(ChangeDescriptionCompletedEvent<String, TidSet> arg0) {
                            //do nothing
                        }

                        @Override
                        public void changeDescriptionStarted(ChangeDescriptionStartedEvent<String, TidSet> arg0) {
                            //do nothing
                        }

                    }
            );

            AtomicInteger transactionCount = new AtomicInteger();
            Vector<Byte> predetti = new Vector<>();
            Vector<Byte> reali = new Vector<>();
            AtomicInteger count = new AtomicInteger(1);
            dataset.forEach(transaction -> {
                detector.accept(transaction);		//calcolo prima i pattern
                try
                {
                    float num = 0;
                    float den = 0;
                    for (Pattern<String, TidSet> p : detector.getLattice()) //IllegalArgumentException finche' non raggiunge il numero di transazioni pari a blockSize
                    {
                        try
                        {
                            double freq;
                            if(patternChanged.get() || transactionCount.get() < blockSize){		//se mi trovo nel primo blocco o e' stato rilevato un change
                                freq = p.getSecondEval().getRelativeFrequency();	//prendi la frequenza da w2 (secondEval)
                            }else{
                                freq = p.getFirstEval().getRelativeFrequency();
                            }

                            Set<String> valori = new HashSet<>();
                            addValue(valori, p.getItemSet());        //funzione ricorsiva che aggiunge al set tutti gli elementi del pattern
                            if (transaction.getItems().containsAll(valori))
                            {
                                num += freq;        //incremento il numeratore col supporto del relativo pattern che la copre
                            }
                            den += freq;

                        }
                        catch (NullPointerException e)
                        {
                            //dato che il primo pattern risulta essere sempre vuoto, nullo cioe'
                        }
                    }
                    transactionCount.getAndIncrement();

                    float fpof = num/den;
                    if(fpof < maxAnomaly){		//se il FPOF e' sotto la soglia di anomalia
                        predetti.add((byte) 1);
                    }else{
                        predetti.add((byte) 0);
                    }

                    printWriter.print(count.get());
                    printWriter.print(",");
                    printWriter.print(fpof);
                    printWriter.print(",");
                    printWriter.print(transaction.getLabel());
                    printWriter.print("\n");

                    count.getAndIncrement();

                    if(transaction.getLabel().equals("TRUE")){	//se la transazione e' effettivamente fraudolenta
                        reali.add((byte) 1);
                    }else{
                        reali.add((byte) 0);
                    }
                }
                catch(IllegalArgumentException  e)
                {
                    System.err.println("Calculating pattern...");
                }
            });

            ConfusionMatrix cm = new ConfusionMatrix();		//creo la matrice di confusione
            int i = 0;
            while(i < predetti.size())
            {
                if(predetti.get(i) == 0 && reali.get(i) == 0){
                    cm.increaseValue("NON-FRAUD", "NON-FRAUD", 1);
                }else if(predetti.get(i) == 1 && reali.get(i) == 0){
                    cm.increaseValue("NON-FRAUD", "FRAUD", 1);
                }else if(predetti.get(i) == 0 && reali.get(i) == 1){
                    cm.increaseValue("FRAUD", "NON-FRAUD", 1);
                }else{
                    cm.increaseValue("FRAUD", "FRAUD", 1);
                }
                i++;
            }

//            System.out.println(cm);
//            System.out.println(cm.getPrecisionForLabels());
//            System.out.println(cm.getRecallForLabels());
        } catch (CmdLineException e) {
            System.err.println(e.getMessage());
            System.err.println("java -jar [jar-file] [options...] arguments...");
            argsParser.printUsage(System.err);
            System.err.println();
            System.err.println(" Example: java -jar jKarma-fraud-detection"+
                    argsParser.printExample(OptionHandlerFilter.ALL)
            );
        } catch (FileNotFoundException e) {
            System.err.println("File not found!");
        }
        long t1 = System.currentTimeMillis();
		double t = (double)(t1 - t0) / 1000.0;
        System.out.println("The JKarma algorithm took " + t + " seconds");
        printWriter.close();
    }

    private static void addValue(Set<String> set, ItemSet<String,Pair<TidSet>> itemSet)
    {
        if(itemSet.getPrefix() != null) {
            set.add(itemSet.getSuffix());
            addValue(set,itemSet.getPrefix());		//passo ricorsivo
        }
    }

}