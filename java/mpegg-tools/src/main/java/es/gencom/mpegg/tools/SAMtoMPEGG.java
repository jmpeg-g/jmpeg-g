package es.gencom.mpegg.tools;

import es.gencom.mpegg.dataunits.DataUnitAccessUnitHeader;
import es.gencom.mpegg.format.SequenceIdentifier;
import es.gencom.mpegg.encoder.AbstractAccessUnitEncoder;
import es.gencom.mpegg.dataunits.DataUnitAccessUnitHeader;
import es.gencom.mpegg.io.MPEGWriter;
import es.gencom.mpegg.dataunits.DataUnitParameters;

import java.io.IOException;
import java.nio.ByteBuffer;

public class SAMtoMPEGG {

    public static void writeAccessUnitToFile(
            MPEGWriter writer,
            AbstractAccessUnitEncoder accessUnitEncoder,
            DataUnitParameters parameters) throws IOException {

        if(accessUnitEncoder.getReadCount() == 0){
            return;
        }
        ByteBuffer dataUnitHeader = ByteBuffer.allocateDirect(50);
        dataUnitHeader.put((byte)0x02);
        dataUnitHeader.putInt(accessUnitEncoder.getSize() << 3);
        dataUnitHeader.limit(dataUnitHeader.position());
        dataUnitHeader.rewind();
        writer.writeByteBuffer(dataUnitHeader);

        long ref_start_position = 0;
        long ref_end_position = 0;

        short parameterId = accessUnitEncoder.getEncodingParametersId();
        DataUnitAccessUnitHeader dataUnitAccessUnitHeader =
                new DataUnitAccessUnitHeader(
                        accessUnitEncoder.getAuId(),
                        accessUnitEncoder.getNumberDescriptors(),
                        parameterId,
                        accessUnitEncoder.getAuType(),
                        accessUnitEncoder.getReadCount(),
                        accessUnitEncoder.getThreshold(),
                        accessUnitEncoder.getMm_count(),
                        new SequenceIdentifier(accessUnitEncoder.getSequenceId()),
                        ref_start_position,
                        ref_end_position,
                        new SequenceIdentifier(accessUnitEncoder.getSequenceId()),
                        accessUnitEncoder.getAuStartPosition(),
                        accessUnitEncoder.getAuEndPosition(),
                        accessUnitEncoder.getExtendedStartPosition(),
                        accessUnitEncoder.getExtendedEndPosition()
                );

        dataUnitAccessUnitHeader.write(writer, parameters);

        accessUnitEncoder.writeDescriptors(writer);
    }

}
