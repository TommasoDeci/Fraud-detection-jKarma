package org.jkarma.examples.purchases.model;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.univocity.parsers.common.IterableResult;
import com.univocity.parsers.common.ParsingContext;
import com.univocity.parsers.csv.CsvParserSettings;
import com.univocity.parsers.csv.CsvRoutines;

public class Utils {

    /**
     * Boilerplate methods which uses the univocity parser library
     * for deserializing beans from rows in a csv file.
     * @param <T> The bean type to be deserialized.
     * @param source The file to be read.
     * @param type The bean class
     * @return a stream of bean instances.
     * @throws FileNotFoundException
     */
    public static <T> Stream<T> parseStream(File source, Class<T> type) throws FileNotFoundException{
        if(source==null || type==null) {
            throw new IllegalArgumentException();
        }

        //we open a valid InputStreamReader
        InputStreamReader reader = new InputStreamReader(
                new FileInputStream(source), StandardCharsets.UTF_8
        );

        //we set the csv parsing settings
        CsvParserSettings settings = new CsvParserSettings();
        settings.getFormat().setLineSeparator("\n");
        settings.setHeaderExtractionEnabled(true);
        settings.setReadInputOnSeparateThread(false);

        //we open a CSV strategy
        CsvRoutines parsingRoutine = new CsvRoutines(settings);

        //we get the first value
        IterableResult<T, ParsingContext> first = parsingRoutine.iterate(
                type, reader
        );

        //we build a stream using a spliterator built from an IterableResult (which
        //implements Iterator<T> interface).
        return StreamSupport.stream(first.spliterator(), false);
    }

}

