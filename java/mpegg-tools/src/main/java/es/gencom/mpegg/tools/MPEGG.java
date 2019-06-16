package es.gencom.mpegg.tools;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * @author Dmitry Repchevsky &amp; Daniel Naro
 */

public class MPEGG {
    
    private final static String HELP = 
            "mpegg-tools -i file \n" +
            "parameters:\n" +
            "-h (--help)                - this help message\n" +
            "-i (--input)               - input file \n" +
            "-r (--reference)           - reference file \n" +
            "example: >java -jar mpegg-tools.jar -i myfile.bam\n";
    
    public static void main(String[] args){

        Map<String, List<String>> params = parameters(args);

        List<String> input = params.get("-i");
        if (input == null) {
            input = params.get("--input");
        }
        
        if (input == null || input.isEmpty()) {
            System.out.println(HELP);
            System.exit(0);
        }
        
        final String file = input.get(0);
        
        List<String> reference = params.get("-r");
        if (reference == null) {
            reference = params.get("--reference");
        }
        
        if (reference == null || reference.isEmpty()) {
            System.out.println(HELP);
            System.exit(0);
        }
        
        final String rawReference = reference.get(0);
        
        final String sequenceNamesFileName = input.get(1);

        Path path = Paths.get(file);
        boolean isBam  = path.getFileName().toString().endsWith(".bam");
        boolean isMPEGG;
        if(!isBam){
            isMPEGG =  path.getFileName().toString().endsWith(".mpegg");
            if(!isMPEGG){
                throw new IllegalArgumentException("The input must be either of extension bam or mpegg");
            }
        }

        try {
            String[] sequenceNames = SequenceNamesParser.getSequenceNames(Paths.get(sequenceNamesFileName));

            if (isBam) {
                BAMToMPEGGBytestream.encode(
                        file,
                        rawReference,
                        file.replace("bam", "mpegg"),
                        sequenceNames,
                        false
                );
                System.out.println("num discarded records: "+BAMToMPEGGBytestream.getNumDiscardedRecords());
            } else {
                MPEGGBytestreamToBAM.decode(
                        file,
                        rawReference,
                        file.replace("mpegg", "mpegg_bam"),
                        sequenceNames
                );
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    
    private static Map<String, List<String>> parameters(String[] args) {
        TreeMap<String, List<String>> parameters = new TreeMap();        
        List<String> values = null;
        for (String arg : args) {
            switch(arg) {
                case "-i":
                case "--input":
                case "-r":
                case "--reference":
                case "-h":
                case "--help": values = parameters.get(arg);
                               if (values == null) {
                                   values = new ArrayList(); 
                                   parameters.put(arg, values);
                               }
                               break;
                default: if (values != null) {
                    values.add(arg);
                }
            }
        }
        return parameters;
    }

}
