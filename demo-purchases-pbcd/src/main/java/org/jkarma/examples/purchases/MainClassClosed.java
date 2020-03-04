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
import org.jkarma.mining.joiners.ProjectedDB;
import org.jkarma.mining.joiners.ProjectedDB;
import org.jkarma.mining.providers.ProjectedDBProvider;
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
import com.google.common.collect.Sets;

public class MainClassClosed {
    @Option(name = "-ms", aliases = "--minSupport", usage = "Threshold above which a pattern is considered")
    public float minSup = 0.5f;

    @Option(name = "-mc", aliases = "--minChange", usage = "Threshold above which two blocks are different")
    public static float minChange = 0.8f;

    @Option(name = "-bs", aliases = "--blockSize", usage = "Number of examples into a single block")
    public static int blockSize = 4;

    @Option(name = "-ma", aliases = "--maxAnomaly", usage = "Threshold below which an example is anomalous")
    public static float maxAnomaly = 0.1f;

    @Option(name = "-d", aliases = "--depth", usage = "Depth of research space of patterns")
    public int depth = 3;

    @Option(name = "-f", aliases = "--file", usage = "Fully qualified path and name of file.", required = true)
    private static String fileName;


    public static Stream<Transazione> getDataset(String fileCSV) throws IOException {
        List<Transazione> transazioni = new LinkedList<>();
        File file = new File(fileCSV);

        BufferedReader br = new BufferedReader(new FileReader(file));
        String st;
        String values[];
        st = br.readLine();
        while ((st = br.readLine()) != null) {
            values = st.split(",");
            transazioni.add(new Transazione(values));
        }

        return transazioni.stream();
    }

//	public Stream<Transazione> getData() throws FileNotFoundException{
//		return Utils.parseStream(new File(this.fileName), Transazione.class)
//				.sorted(Comparator.comparing(Transazione::getTimestamp));
//	}

    public PBCD<Transazione, String, ProjectedDB<String>, Double> getPBCD() {
        //we prepare the time window model and the data accessor
        ProjectedDBProvider<String> accessor = new ProjectedDBProvider<>(Windows.cumulativeSliding());

        AreaHeuristic<String, ProjectedDB<String>> areaHeuristic = new AreaHeuristic<>();
        int k = 25;

        //we instantiate the mining strategy
        MiningStrategy<String, ProjectedDB<String>> strategy = Strategies
                .uponItemsets(new HashSet<String>()).limitDepth(depth).lcm(minSup).beam(accessor, areaHeuristic, k);


        WeightedJaccard measure = new WeightedJaccard();

        return Detectors.upon(strategy)
                .weighted(Patterns::getRelativeFrequency, measure)
                .build(minChange, blockSize);

    }

    public static void main(String[] args) throws IOException {
        long t0 = System.currentTimeMillis();
        final MainClassClosed mainClass = new MainClassClosed();
        final CmdLineParser argsParser = new CmdLineParser(mainClass);
        AtomicInteger transactionCount = new AtomicInteger();
        try {
            //we parse the command line arguments
            argsParser.parseArgument(args);

            Stream<Transazione> dataset = mainClass.getDataset(fileName);
            PBCD<Transazione, String, ProjectedDB<String>, Double> detector = mainClass.getPBCD();

            AtomicBoolean patternChanged = new AtomicBoolean();
            //we listen for events
            detector.registerListener(
                    new PBCDEventListener<String, ProjectedDB<String>>() {
                        @Override
                        public void patternUpdateCompleted(PatternUpdateCompletedEvent<String, ProjectedDB<String>> arg0) {
                            //do nofing;
                        }

                        @Override
                        public void patternUpdateStarted(PatternUpdateStartedEvent<String, ProjectedDB<String>> arg0) {
                            //do nothing
                        }

                        @Override
                        public void changeDetected(ChangeDetectedEvent<String, ProjectedDB<String>> event) {
                            patternChanged.set(true);
                            System.out.println("change detected: " + event.getAmount());
                            for (Pattern<String, ProjectedDB<String>> p : detector.getLattice()) //IllegalArgumentException finche' non raggiunge il numero di transazioni pari a blockSize
                            {
                                if(p.getItemSet().getSuffix() != null){
                                    boolean shouldUseFirstEval = true;
                                    if (patternChanged.get() || transactionCount.get() < blockSize) {        //se mi trovo nel primo blocco o e' stato rilevato un change
                                        shouldUseFirstEval = false;
                                    }

                                    boolean isClosed = Patterns.isClosedByClosure(p,shouldUseFirstEval);   //se il pattern p oltre ad essere frequente e' anche cloased
                                    if(isClosed){
                                        System.out.println(p);
                                    }
                                }

                            }
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
                        public void changeNotDetected(ChangeNotDetectedEvent<String, ProjectedDB<String>> arg0) {
                            patternChanged.set(false);
                            System.out.println("change not detected: " + arg0.getAmount());
                            for (Pattern<String, ProjectedDB<String>> p : detector.getLattice()) //IllegalArgumentException finche' non raggiunge il numero di transazioni pari a blockSize
                            {
                                if(p.getItemSet().getSuffix() != null){
                                    boolean shouldUseFirstEval = true;
                                    if (patternChanged.get() || transactionCount.get() < blockSize) {        //se mi trovo nel primo blocco o e' stato rilevato un change
                                        shouldUseFirstEval = false;
                                    }

                                    boolean isClosed = Patterns.isClosedByClosure(p,shouldUseFirstEval);   //se il pattern p oltre ad essere frequente e' anche cloased
                                    if(isClosed){
                                        System.out.println(p);
                                    }
                                }

                            }
                        }

                        @Override
                        public void changeDescriptionCompleted(ChangeDescriptionCompletedEvent<String, ProjectedDB<String>> arg0) {
                            //do nothing
                        }

                        @Override
                        public void changeDescriptionStarted(ChangeDescriptionStartedEvent<String, ProjectedDB<String>> arg0) {
                            //do nothing
                        }

                    }
            );

            Vector<Byte> predetti = new Vector<>();
            Vector<Byte> reali = new Vector<>();
            dataset.forEach(transaction -> {
                detector.accept(transaction);        //calcolo prima i pattern
                try {
                    float num = 0;
                    float den = 0;
                    for (Pattern<String, ProjectedDB<String>> p : detector.getLattice()) //IllegalArgumentException finche' non raggiunge il numero di transazioni pari a blockSize
                    {
                        try {
                            double freq;
                            boolean shouldUseFirstEval = true;
                            if (patternChanged.get() || transactionCount.get() < blockSize) {        //se mi trovo nel primo blocco o e' stato rilevato un change
                                shouldUseFirstEval = false;
                            }
                            freq = Patterns.getRelativeFrequency(p,shouldUseFirstEval);

                            boolean isClosed = Patterns.isClosedByClosure(p,shouldUseFirstEval);   //se il pattern p oltre ad essere frequente e' anche cloased
                            if(isClosed){
                                Set<String> valori = new HashSet<>();
                                addValue(valori, p.getItemSet());        //funzione ricorsiva che aggiunge al set tutti gli elementi del pattern
                                if (transaction.getItems().containsAll(valori)) {
                                    num += freq;        //incremento il numeratore col supporto del relativo pattern che la copre
                                }
                                den += freq;
                            }


                        } catch (NullPointerException e) {
                            //dato che il primo pattern risulta essere sempre vuoto, nullo cioe'
                        }
                    }
                    transactionCount.getAndIncrement();

                    float fpof = num / den;
                    if (fpof < maxAnomaly) {        //se il FPOF e' sotto la soglia di anomalia
                        predetti.add((byte) 1);
                    } else {
                        predetti.add((byte) 0);
                    }

                    if (transaction.getLabel().equals("TRUE")) {    //se la transazione e' effettivamente fraudolenta
                        reali.add((byte) 1);
                    } else {
                        reali.add((byte) 0);
                    }
                } catch (IllegalArgumentException e) {
                    System.err.println("Calculating pattern...");
                }
            });

            ConfusionMatrix cm = new ConfusionMatrix();        //creo la matrice di confusione
            int i = 0;
            while (i < predetti.size()) {
                if (predetti.get(i) == 0 && reali.get(i) == 0) {
                    cm.increaseValue("NON-FRAUD", "NON-FRAUD", 1);
                } else if (predetti.get(i) == 1 && reali.get(i) == 0) {
                    cm.increaseValue("NON-FRAUD", "FRAUD", 1);
                } else if (predetti.get(i) == 0 && reali.get(i) == 1) {
                    cm.increaseValue("FRAUD", "NON-FRAUD", 1);
                } else {
                    cm.increaseValue("FRAUD", "FRAUD", 1);
                }
                i++;
            }

            System.out.println(cm);
            System.out.println(cm.getPrecisionForLabels());
            System.out.println(cm.getRecallForLabels());
            System.out.println(cm.getAccuracy());
            System.out.println(cm.getCohensKappa());
            System.out.println(cm.getMacroFMeasure());

        } catch (CmdLineException e) {
            System.err.println(e.getMessage());
            System.err.println("java -jar [jar-file] [options...] arguments...");
            argsParser.printUsage(System.err);
            System.err.println();
            System.err.println(" Example: java -jar jKarma-fraud-detection" +
                    argsParser.printExample(OptionHandlerFilter.ALL)
            );
        } catch (FileNotFoundException e) {
            System.err.println("File not found!");
        }
        long t1 = System.currentTimeMillis();
        double t = (double) (t1 - t0) / 1000.0;
        System.out.println("The JKarma algorithm took " + t + " seconds");
    }

    private static void addValue(Set<String> set, ItemSet<String, Pair<ProjectedDB<String>>> itemSet) {
        if (itemSet.getPrefix() != null) {
            set.add(itemSet.getSuffix());
            addValue(set, itemSet.getPrefix());        //passo ricorsivo
        }
    }
}

