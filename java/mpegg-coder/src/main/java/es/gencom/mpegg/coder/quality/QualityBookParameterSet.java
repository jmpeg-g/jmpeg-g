package es.gencom.mpegg.coder.quality;

public class QualityBookParameterSet extends AbstractQualityBook{
    private final short[] entries;

    /**
     * Creates a new parametrized quality book
     * @param entries The constructor does not check the correctness of the provided parameters. Each value shall be
     *                unique and be a legal quality value.
     */
    QualityBookParameterSet(short[] entries) {
        this.entries = entries;
    }


    @Override
    public byte getNumberEntries() {
        return (byte)entries.length;
    }

    @Override
    public short encode(short qualitySAM) {
        int minDistance = Integer.MAX_VALUE;
        short bestIndex = 0;

        for(byte index = 0; index<entries.length; index++){
            if(Math.abs(qualitySAM - entries[index])<minDistance){
                minDistance = Math.abs(qualitySAM - entries[index]);
                bestIndex = index;
            }
            if(minDistance == 0){
                break;
            }
        }
        return bestIndex;
    }

    @Override
    public short decode(short encodedQuality) {
        return entries[encodedQuality];
    }
}