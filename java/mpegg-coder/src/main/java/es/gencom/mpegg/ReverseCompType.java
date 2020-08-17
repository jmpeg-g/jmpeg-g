package es.gencom.mpegg;

public enum ReverseCompType {
    Forward((byte)0),
    Reverse((byte) 1),
    Undirected((byte)2);

    public final byte id;

    ReverseCompType(final byte id) {
        this.id = id;
    }

    public static ReverseCompType getReverseComp(byte id){
        switch (id){
            case 0:
                return Forward;
            case 1:
                return Reverse;
            case 2:
                return Undirected;
            default:
                throw new IllegalArgumentException();
        }
    }


}
