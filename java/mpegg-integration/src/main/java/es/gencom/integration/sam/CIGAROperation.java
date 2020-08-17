package es.gencom.integration.sam;

public enum CIGAROperation {
    MATCH((byte)0),        // 'M'
    INSERT((byte)1),       // 'I'
    DELETE((byte)2),       // 'D'
    SKIP((byte)3),         // 'N'
    SOFTCLIP((byte)4),     // 'S'
    HARDCLIP((byte)5),     // 'H'
    PADDING((byte)6),      // 'P'
    EQUAL((byte)7),        // '='
    SUBSTITUTION((byte)8); // 'X'

    public final byte ID;

    CIGAROperation(final byte id) {
        this.ID = id;
    }
}
