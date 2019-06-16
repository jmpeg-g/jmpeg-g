package es.gencom.mpegg.tools;

import java.io.*;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.stream.Stream;

public class SequenceNamesParser {
    public static String[] getSequenceNames(Path sequenceNamesPath) throws IOException {
        String[] sequenceNames = new String[23];
        int count = 0;

        BufferedReader reader = new BufferedReader(new FileReader(sequenceNamesPath.toFile()));
        while (true) {
            try {
                String newLine = reader.readLine();
                if(newLine == null){
                    break;
                }
                sequenceNames[count] = newLine;
                count++;
                if(sequenceNames.length == count){
                    sequenceNames = Arrays.copyOf(sequenceNames, sequenceNames.length*2);
                }
            }catch (EOFException ignored){
                break;
            }
        }
        return Arrays.copyOf(sequenceNames, count);
    }
}
