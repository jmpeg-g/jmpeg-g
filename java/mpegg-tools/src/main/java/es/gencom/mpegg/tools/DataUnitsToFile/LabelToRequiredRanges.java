package es.gencom.mpegg.tools.DataUnitsToFile;

import es.gencom.mpegg.format.Label;
import es.gencom.mpegg.format.LabelDatasetLocation;
import es.gencom.mpegg.format.LabelRegionDescription;
import es.gencom.mpegg.format.SequenceIdentifier;
import es.gencom.mpegg.tools.RequiredRanges;

public class LabelToRequiredRanges {
    public static RequiredRanges convert(
            Label label,
            short datasetId
    ){
        RequiredRanges requiredRanges = new RequiredRanges();

        addTo(
                label,
                datasetId,
                requiredRanges
        );

        return requiredRanges;
    }

    public static void addTo(
            Label label,
            short datasetId,
            RequiredRanges requiredRanges
    ){
        for(LabelDatasetLocation labelDatasetLocation : label.getLabelDatasetLocations()){
            if(labelDatasetLocation.getDatasetId() != datasetId){
                continue;
            }
            for(LabelRegionDescription labelRegionDescription : labelDatasetLocation.getRegionDescriptions()){
                requiredRanges.addRequiredRange(
                        new SequenceIdentifier(labelRegionDescription.getSequenceId()),
                        labelRegionDescription.getStart_pos(),
                        labelRegionDescription.getEnd_pos(),
                        labelRegionDescription.getClassIds()
                );
            }
        }
    }
}
