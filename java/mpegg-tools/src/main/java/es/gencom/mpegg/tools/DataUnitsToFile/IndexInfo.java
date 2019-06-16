package es.gencom.mpegg.tools.DataUnitsToFile;

import es.gencom.mpegg.format.AccessUnitContainer;
import es.gencom.mpegg.format.AccessUnitHeader;

public class  IndexInfo implements Comparable<IndexInfo>{
    private final int au_id;
    final long au_start_position;
    final long au_end_position;
    final AccessUnitContainer accessUnitContainer;

    IndexInfo(
            AccessUnitContainer accessUnitContainer
    ) {
        this.au_id = accessUnitContainer.getAccessUnitHeader().getAccessUnitID();
        this.au_start_position = accessUnitContainer.getAccessUnitHeader().getAUStartPosition();
        this.au_end_position = accessUnitContainer.getAccessUnitHeader().getAUEndPosition();
        this.accessUnitContainer = accessUnitContainer;

        if(this.au_end_position < this.au_start_position){
            System.err.println("IndexInfo error: start="+au_start_position+" end="+au_end_position);
        }
    }

    @Override
    public int compareTo(IndexInfo indexInfo) {
        return Integer.compare(au_id, indexInfo.au_id);
    }

    public AccessUnitContainer getAccessUnitContainer() {
        return accessUnitContainer;
    }

    AccessUnitHeader getAccessUnitHeader() {
        return accessUnitContainer.getAccessUnitHeader();
    }
}