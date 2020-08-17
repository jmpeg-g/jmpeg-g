package es.gencom.mpegg.tools;

import es.gencom.mpegg.dataunits.DataUnits;
import es.gencom.mpegg.format.*;
import es.gencom.mpegg.format.ref.Reference;
import es.gencom.mpegg.io.MPEGWriter;
import es.gencom.mpegg.io.ReadableMSBitFileChannel;
import es.gencom.mpegg.io.WritableMSBitChannel;
import es.gencom.mpegg.tools.DataUnitsToFile.AbstractDataUnitsToDataset;
import es.gencom.mpegg.tools.DataUnitsToFile.DataUnitsToAUCNoMITDataset;
import es.gencom.mpegg.tools.DataUnitsToFile.FASTAToFASTAReference;

import java.io.FileInputStream;
import java.io.FileOutputStream;
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
            if (isBam) {
                List<String> reference = params.get("-r");
                if (reference == null) {
                    reference = params.get("--reference");
                }

                if (reference == null || reference.isEmpty()) {
                    System.out.println(HELP);
                    System.exit(0);
                }

                final String fastaReferencePath = reference.get(0);
                final String mpeggPath = file.replace("bam", "mpegg");

                final String bitStreamPath = file.replace("bam","mgb");

                BAMToMPEGGBytestream.encode(
                        file,
                        fastaReferencePath,
                        bitStreamPath,
                        false
                );
                DataUnits dataUnits = DataUnits.read(new ReadableMSBitFileChannel(
                        new FileInputStream(bitStreamPath).getChannel())
                );

                System.out.println("num discarded records: "+BAMToMPEGGBytestream.getNumDiscardedRecords());

                MPEGFile mpegFile = new MPEGFile();
                MPEGFileHeader fileHeader = new MPEGFileHeader();
                fileHeader.addCompatibleBrand(new String(new byte[4]));
                mpegFile.setFileHeader(fileHeader);

                DatasetGroupContainer datasetGroupContainer = new DatasetGroupContainer();
                mpegFile.addDatasetGroupContainer(datasetGroupContainer);

                datasetGroupContainer.setDatasetGroupHeader(
                        new DatasetGroupHeader()
                );

                Reference formatReference = FASTAToFASTAReference.generate(
                        datasetGroupContainer,
                        Paths.get(fastaReferencePath),
                        "hs37d5",
                        (short)0,
                        (short)0,
                        (short)0,
                        "hs37d5.fa",
                        ChecksumAlgorithm.SHA256
                );
                AbstractDataUnitsToDataset dataUnitsToDataset = new DataUnitsToAUCNoMITDataset();
                dataUnitsToDataset.addAsDataset(
                        datasetGroupContainer,
                        (byte)0,
                        dataUnits,
                        false,
                        formatReference.getReferenceId(),
                        100000,
                        ALPHABET.DNA_IUPAC
                );

                MPEGWriter writer = new WritableMSBitChannel(new FileOutputStream(mpeggPath).getChannel());
                mpegFile.write(writer);
                writer.flush();
            } else {
                MPEGFile mpegFile = new MPEGFile();
                mpegFile.read(new ReadableMSBitFileChannel(new FileInputStream(file).getChannel()));

                DatasetGroupContainer datasetGroupContainer = mpegFile.getDatasetGroupContainerById((byte)0);
                DatasetContainer datasetContainer = datasetGroupContainer.getDatasetContainerById(0);

                DataUnits dataUnits = DatasetToDataUnits.getDataUnits(datasetGroupContainer, datasetContainer);
                Reference reference = datasetGroupContainer.getReference(
                        datasetContainer.getDatasetHeader().getReferenceId()
                );

                MPEGGBytestreamToBAM.decode(
                        dataUnits,
                        file.replace("mpegg", "mpegg_bam"),
                        reference.getSequenceNames(),
                        dataUnits.getAllReadGroupNames()
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
