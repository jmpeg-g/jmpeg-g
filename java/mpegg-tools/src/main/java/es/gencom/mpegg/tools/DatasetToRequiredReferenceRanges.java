package es.gencom.mpegg.tools;

import es.gencom.mpegg.coder.configuration.EncodingParameters;
import es.gencom.mpegg.format.*;
import es.gencom.mpegg.io.MSBitBuffer;

import java.io.IOException;
import java.nio.ByteBuffer;

public class DatasetToRequiredReferenceRanges {
    public static RequiredRanges getRequiredReferenceRanges(
            DatasetContainer datasetContainer
    ) throws IOException, DataClassNotFoundException, SequenceNotAvailableException {
        RequiredRanges requiredRanges = new RequiredRanges();

        for(SequenceIdentifier sequenceIdentifier : datasetContainer.getDatasetHeader().getSeqIds()){
            getRequiredReferenceRanges(
                    requiredRanges,
                    datasetContainer,
                    sequenceIdentifier,
                    0,
                    Long.MAX_VALUE
            );
        }
        return requiredRanges;
    }

    public static RequiredRanges getRequiredReferenceRanges(
            DatasetContainer datasetContainer,
            SequenceIdentifier sequenceIdentifier,
            long start,
            long end
    ) throws IOException, DataClassNotFoundException, SequenceNotAvailableException {
        RequiredRanges requiredRanges = new RequiredRanges();
        getRequiredReferenceRanges(
                requiredRanges,
                datasetContainer,
                sequenceIdentifier,
                start,
                end
        );
        return requiredRanges;
    }

    public static void getRequiredReferenceRanges(
            RequiredRanges requiredRanges,
            DatasetContainer datasetContainer,
            SequenceIdentifier sequenceIdentifier,
            long start,
            long end
    ) throws IOException, DataClassNotFoundException, SequenceNotAvailableException {
        if(datasetContainer.getDatasetHeader().isBlockHeaderFlag()){
            if(!datasetContainer.getDatasetHeader().isMIT()){
                getRequiredReferenceRangesNoMIT(
                        requiredRanges,
                        datasetContainer,
                        sequenceIdentifier,
                        start,
                        end
                );
            } else {
                getRequiredReferenceRangesMIT(
                        requiredRanges,
                        datasetContainer,
                        sequenceIdentifier,
                        start,
                        end
                );
            }
        } else {
            getRequiredReferenceRangesNoBlockHeader(
                    requiredRanges,
                    datasetContainer,
                    sequenceIdentifier,
                    start,
                    end
            );
        }
    }

    private static void getRequiredReferenceRangesNoBlockHeader(
            RequiredRanges requiredRanges,
            DatasetContainer datasetContainer,
            SequenceIdentifier sequenceIdentifier,
            long start,
            long end
    ) throws SequenceNotAvailableException, DataClassNotFoundException, IOException {
        DatasetSequenceIndex datasetSequenceIndex = datasetContainer.getSequenceIndex(sequenceIdentifier);
        long referenceBlocksNum = datasetContainer.getDatasetHeader().getReferenceSequenceBlocks(datasetSequenceIndex);
        for(int block_i=0; block_i<referenceBlocksNum; block_i++){
            DATA_CLASS[] dataClasses = datasetContainer.getDatasetHeader().getClassIDs();
            for(int dataClass_i = 0; dataClass_i < dataClasses.length; dataClass_i++){
                DATA_CLASS dataClass = dataClasses[dataClass_i];
                DataClassIndex dataClassIndex = datasetContainer.getDatasetHeader().getClassIndex(dataClass);


                AU_Id_triplet au_id_triplet = new AU_Id_triplet(
                        datasetSequenceIndex,
                        datasetContainer.getDatasetHeader().getClassIndex(dataClasses[dataClass_i]),
                        block_i
                );

                AccessUnitContainer accessUnitContainer = datasetContainer
                        .getAccessUnitContainerToAuIdTriplet()
                        .getReverse(au_id_triplet);
                if(accessUnitContainer == null){
                    continue;
                }

                SequenceIdentifier sequenceIdentifierAU = datasetContainer.getDatasetHeader().getReferenceSequenceId(
                        au_id_triplet.getSeq()
                );
                if(!sequenceIdentifierAU.equals(sequenceIdentifier)){
                    continue;
                }

                DatasetParameterSet parameterSet = datasetContainer.getDatasetParameterSetById(
                        accessUnitContainer.getAccessUnitHeader().getParameterSetID()
                );
                EncodingParameters encodingParameters = new EncodingParameters();
                ByteBuffer byteBuffer = parameterSet.getParameters();
                byteBuffer.rewind();
                encodingParameters.read(new MSBitBuffer(byteBuffer));

                DatasetSequenceIndex sequenceIndex = au_id_triplet.getSeq();
                long accessUnitStart = datasetContainer.getMasterIndexTable().getAuStart(
                        sequenceIndex,
                        au_id_triplet.getClass_i(),
                        (int)au_id_triplet.getAuId()
                );
                long maximum_accessUnitEnd =
                        accessUnitContainer.getAccessUnitHeader().getAUEndPosition()
                                + datasetContainer.getDatasetHeader().getThreshold(sequenceIndex)
                                + encodingParameters.getReadsLength();

                boolean isOverlapping = Long.max(start, accessUnitStart) <= Long.min(end, maximum_accessUnitEnd);
                if(!isOverlapping){
                    continue;
                }


                requiredRanges.addRequiredRange(
                        sequenceIdentifier,
                        accessUnitStart,
                        maximum_accessUnitEnd
                );
            }
        }
    }


    private static void getRequiredReferenceRangesNoMIT(
            RequiredRanges requiredRanges,
            DatasetContainer datasetContainer,
            SequenceIdentifier sequenceIdentifier,
            long start,
            long end
    ) throws IOException {

        for(AccessUnitContainer accessUnitContainer : datasetContainer.getAccessUnitContainers()){
            if(accessUnitContainer.getAccessUnitHeader().getAUType() == DATA_CLASS.CLASS_U){
                continue;
            }
            if(!accessUnitContainer.getAccessUnitHeader().getSequenceID().equals(sequenceIdentifier)){
                continue;
            }
            DatasetSequenceIndex sequenceIndex;
            try{
                sequenceIndex = datasetContainer.getSequenceIndex(sequenceIdentifier);
            } catch (SequenceNotAvailableException e){
                throw new IllegalArgumentException(e);
            }

            long accessUnitStart =
                    accessUnitContainer.getAccessUnitHeader().getAUStartPosition();

            DatasetParameterSet parameterSet = datasetContainer.getDatasetParameterSetById(
                    accessUnitContainer.getAccessUnitHeader().getParameterSetID()
            );
            EncodingParameters encodingParameters = new EncodingParameters();
            ByteBuffer byteBuffer = parameterSet.getParameters();
            byteBuffer.rewind();
            encodingParameters.read(new MSBitBuffer(byteBuffer));

            long maximum_accessUnitEnd =
                    accessUnitContainer.getAccessUnitHeader().getAUEndPosition()
                            + datasetContainer.getDatasetHeader().getThreshold(sequenceIndex)
                            + encodingParameters.getReadsLength();

            boolean isOverlapping = Long.max(start, accessUnitStart) <= Long.min(end, maximum_accessUnitEnd);
            if(!isOverlapping){
                continue;
            }

            requiredRanges.addRequiredRange(sequenceIdentifier, accessUnitStart, maximum_accessUnitEnd);

        }
    }

    private static void getRequiredReferenceRangesMIT(
            RequiredRanges requiredRanges,
            DatasetContainer datasetContainer,
            SequenceIdentifier sequenceIdentifier,
            long start,
            long end
    ) throws IOException, SequenceNotAvailableException, DataClassNotFoundException {
        DatasetSequenceIndex datasetSequenceIndex = datasetContainer.getSequenceIndex(sequenceIdentifier);
        long referenceBlocksNum = datasetContainer.getDatasetHeader().getReferenceSequenceBlocks(datasetSequenceIndex);
        for(int block_i=0; block_i<referenceBlocksNum; block_i++){
            DATA_CLASS[] dataClasses = datasetContainer.getDatasetHeader().getClassIDs();
            for(int dataClass_i = 0; dataClass_i < dataClasses.length; dataClass_i++){
                AU_Id_triplet au_id_triplet = new AU_Id_triplet(
                        datasetSequenceIndex,
                        datasetContainer.getDatasetHeader().getClassIndex(dataClasses[dataClass_i]),
                        block_i
                );

                AccessUnitContainer accessUnitContainer = datasetContainer
                        .getAccessUnitContainerToAuIdTriplet()
                        .getReverse(au_id_triplet);
                if(accessUnitContainer == null){
                    continue;
                }

                SequenceIdentifier sequenceIdentifierAU = datasetContainer.getDatasetHeader().getReferenceSequenceId(
                        au_id_triplet.getSeq()
                );
                if(!sequenceIdentifierAU.equals(sequenceIdentifier)){
                    continue;
                }
                DatasetSequenceIndex sequenceIndex = au_id_triplet.getSeq();
                long accessUnitStart = datasetContainer.getMasterIndexTable().getAuStart(
                        sequenceIndex,
                        au_id_triplet.getClass_i(),
                        (int)au_id_triplet.getAuId()
                );

                DatasetParameterSet parameterSet = datasetContainer.getDatasetParameterSetById(
                        accessUnitContainer.getAccessUnitHeader().getParameterSetID()
                );
                EncodingParameters encodingParameters = new EncodingParameters();
                ByteBuffer byteBuffer = parameterSet.getParameters();
                byteBuffer.rewind();
                encodingParameters.read(new MSBitBuffer(byteBuffer));

                long maximum_accessUnitEnd =
                        accessUnitContainer.getAccessUnitHeader().getAUEndPosition()
                                + datasetContainer.getDatasetHeader().getThreshold(sequenceIndex)
                                + encodingParameters.getReadsLength();

                boolean isOverlapping = Long.max(start, accessUnitStart) <= Long.min(end, maximum_accessUnitEnd);
                if(!isOverlapping){
                    continue;
                }

                requiredRanges.addRequiredRange(
                        sequenceIdentifier,
                        accessUnitStart,
                        maximum_accessUnitEnd
                );
            }
        }
    }
}

