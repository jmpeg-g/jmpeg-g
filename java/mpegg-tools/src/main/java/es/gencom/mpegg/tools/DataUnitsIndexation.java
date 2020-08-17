package es.gencom.mpegg.tools;

import es.gencom.mpegg.format.SequenceIdentifier;
import es.gencom.mpegg.tools.IntervalsSearch.Interval;
import es.gencom.mpegg.tools.IntervalsSearch.Intervals;
import es.gencom.mpegg.dataunits.DataUnitAccessUnit;
import es.gencom.mpegg.dataunits.DataUnits;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class DataUnitsIndexation {
    private final HashMap<SequenceIdentifier, Intervals<DataUnitAccessUnit>> dataUnitsIdentifiers;

    public DataUnitsIndexation(DataUnits dataUnits){
        this.dataUnitsIdentifiers = new HashMap<>();

        HashMap<SequenceIdentifier, List<DataUnitAccessUnit>> buffer = new HashMap<>();
        for(DataUnitAccessUnit dataUnitAccessUnit : dataUnits.getDataUnitAccessUnits()){
            List<DataUnitAccessUnit> dataUnitAccessUnitInSequence = buffer.computeIfAbsent(
                    dataUnitAccessUnit.header.sequence_id,
                    k -> new ArrayList<>()
            );
            dataUnitAccessUnitInSequence.add(dataUnitAccessUnit);
        }

        for(HashMap.Entry<SequenceIdentifier, List<DataUnitAccessUnit>> entry : buffer.entrySet()){
            List<DataUnitAccessUnit> dataUnitAccessUnits = entry.getValue();

            Interval<DataUnitAccessUnit>[] intervals = new Interval[dataUnitAccessUnits.size()];
            int position = 0;
            for(DataUnitAccessUnit dataUnitAccessUnit : dataUnitAccessUnits){
                intervals[position] = new Interval(
                        dataUnitAccessUnit.header.au_start_position,
                        dataUnitAccessUnit.header.au_end_position,
                        dataUnitAccessUnit
                );
                position++;
            }
            dataUnitsIdentifiers.put(
                    entry.getKey(),
                    new Intervals<>(intervals)
            );
        }
    }



    public DataUnitAccessUnit[] getDataUnits(SequenceIdentifier sequenceId, long start, long end){
        Intervals<DataUnitAccessUnit> intervalsCollection = dataUnitsIdentifiers.get(sequenceId);
        if(intervalsCollection == null){
            return new DataUnitAccessUnit[0];
        }
        Interval<DataUnitAccessUnit>[] intervals = intervalsCollection.getIntersects(start, end);
        DataUnitAccessUnit[] dataUnitAccessUnits = new DataUnitAccessUnit[intervals.length];
        for(int i=0; i<intervals.length; i++){
            dataUnitAccessUnits[i] = intervals[i].getValue();
        }
        return dataUnitAccessUnits;
    }
}

