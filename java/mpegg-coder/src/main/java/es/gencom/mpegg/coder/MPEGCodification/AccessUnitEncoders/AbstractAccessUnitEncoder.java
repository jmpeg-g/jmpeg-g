/**
 * *****************************************************************************
 * Copyright (C) 2019 Spanish National Bioinformatics Institute (INB) and
 * Barcelona Supercomputing Center
 *
 * Modifications to the initial code base are copyright of their respective
 * authors, or their employers as appropriate.
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 *****************************************************************************
 */

package es.gencom.mpegg.coder.MPEGCodification.AccessUnitEncoders;

import es.gencom.SubstreamsPerDescriptor;
import es.gencom.mpegg.coder.quality.AbstractQualityValueParameterSet;
import es.gencom.mpegg.Record;
import es.gencom.mpegg.format.DATA_CLASS;
import es.gencom.mpegg.coder.compression.ALPHABET_ID;
import es.gencom.mpegg.coder.compression.DESCRIPTOR_ID;
import es.gencom.mpegg.coder.compression.DescriptorDecoderConfiguration;
import es.gencom.mpegg.coder.compression.DescriptorEncoder;
import es.gencom.mpegg.coder.configuration.EncodingParameters;
import es.gencom.mpegg.coder.tokens.AbstractReadIdentifierEncoder;
import es.gencom.mpegg.coder.tokens.EncodedTokensWriter;
import es.gencom.mpegg.io.MPEGWriter;
import es.gencom.mpegg.io.MSBitOutputArray;
import es.gencom.mpegg.io.Payload;
import es.gencom.mpegg.coder.dataunits.DataUnitAccessUnit;
import es.gencom.mpegg.coder.dataunits.DataUnitParameters;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;

public abstract class AbstractAccessUnitEncoder {
    private final AbstractQualityValueParameterSet qualityValueParameterSet;
    protected  boolean first = true;
    protected long lastReadPosition = 0;
    protected long readCount = 0;
    private final short sequenceId;
    private final AbstractReadIdentifierEncoder readIdentifierEncoder;
    private final int auId;
    private final long auStartPosition;
    private final long auEndPosition;
    private final long extendedStartPosition;
    private final long extendedEndPosition;
    private final ALPHABET_ID alphabet_id;
    private final short threshold;
    private long mm_count;
    protected final byte numberTemplateSegments;
    protected final int readLength;
    private final boolean splicedRead;
    private final boolean multipleAlignment;

    protected final long[][][] symbols;
    protected final byte[][][] auxiliaryData_symbols;
    protected final int[][] number_symbols;
    protected final int[][] number_auxiliaryDataSymbols;


    protected final DATA_CLASS auType;

    private final DataUnitParameters dataUnitParameters;
    private long totalSizeInMemory;


    public AbstractAccessUnitEncoder(
            DATA_CLASS auType,
            short sequenceId,
            int auId,
            long auStartPosition,
            long auEndPosition,
            short threshold,
            long extendedStartPosition,
            long extendedEndPosition,
            DataUnitParameters dataUnitParameters,
            AbstractReadIdentifierEncoder readIdentifierEncoder
    ) throws IOException {
        EncodingParameters encodingParameters = dataUnitParameters.getEncodingParameters();

        this.auType = auType;
        this.sequenceId = sequenceId;
        this.auId = auId;
        this.auStartPosition = auStartPosition;
        this.auEndPosition = auEndPosition;
        this.alphabet_id = encodingParameters.getAlphabetId();
        this.threshold = threshold;
        this.mm_count = 0;
        this.extendedStartPosition = extendedStartPosition;
        this.extendedEndPosition = extendedEndPosition;
        this.readIdentifierEncoder = readIdentifierEncoder;
        this.numberTemplateSegments = encodingParameters.getNumberOfTemplateSegments();

        symbols = new long[DESCRIPTOR_ID.values().length][10][1024];
        auxiliaryData_symbols = new byte[DESCRIPTOR_ID.values().length][10][1024];
        number_symbols = new int[DESCRIPTOR_ID.values().length][10];
        number_auxiliaryDataSymbols = new int[DESCRIPTOR_ID.values().length][10];


        this.qualityValueParameterSet = encodingParameters.getQualityValueParameterSet(auType);

        this.readLength = encodingParameters.getReadsLength();

        this.splicedRead = encodingParameters.isSpliced_reads_flag();
        this.multipleAlignment = encodingParameters.isMultiple_alignments_flag();

        this.dataUnitParameters = dataUnitParameters;
    }

    public short getEncodingParametersId(){
        return dataUnitParameters.getParameter_set_ID();
    }

    protected static void addSymbol(
            long symbol,
            DESCRIPTOR_ID descriptor_id,
            byte subsequence_id,
            long[][][] symbols,
            int[][] numberSymbols
    ){
        symbols[descriptor_id.ID][subsequence_id][numberSymbols[descriptor_id.ID][subsequence_id]] = symbol;
        numberSymbols[descriptor_id.ID][subsequence_id]++;
        if(numberSymbols[descriptor_id.ID][subsequence_id] == symbols[descriptor_id.ID][subsequence_id].length){
            symbols[descriptor_id.ID][subsequence_id] = Arrays.copyOf(
                    symbols[descriptor_id.ID][subsequence_id],
                    symbols[descriptor_id.ID][subsequence_id].length*2
            );
        }
    }

    protected static void addAuxiliarySymbol(
            byte symbol,
            DESCRIPTOR_ID descriptor_id,
            byte subsequence_id,
            byte[][][] symbols,
            int[][] numberSymbols
    ){
        symbols[descriptor_id.ID][subsequence_id][numberSymbols[descriptor_id.ID][subsequence_id]] = symbol;
        numberSymbols[descriptor_id.ID][subsequence_id]++;
        if(numberSymbols[descriptor_id.ID][subsequence_id] == symbols[descriptor_id.ID][subsequence_id].length){
            symbols[descriptor_id.ID][subsequence_id] = Arrays.copyOf(
                    symbols[descriptor_id.ID][subsequence_id],
                    symbols[descriptor_id.ID][subsequence_id].length*2
            );
        }
    }

    public void write(Record record) throws IOException{
        readIdentifierEncoder.encode(record.getReadName());
        for(byte[] val1 : record.getSequenceBytes()){
            if(val1 != null) {
                totalSizeInMemory += val1.length;
            }
        }
        writeSpecific(record);
    }

    protected abstract void writeSpecific(Record record) throws IOException;

    public long getReadCount(){
        return readCount;
    }

    public void writeDescriptors(MPEGWriter output) throws IOException {
        for(int descriptor_i = 0; descriptor_i < symbols.length; descriptor_i++){
            if(descriptor_i == DESCRIPTOR_ID.RNAME.ID){

                MSBitOutputArray tmpOutput = new MSBitOutputArray();

                EncodedTokensWriter.write(
                        tmpOutput,
                        readIdentifierEncoder,
                        DESCRIPTOR_ID.RNAME,
                        dataUnitParameters.getEncodingParameters().getDecoderConfiguration(DESCRIPTOR_ID.RNAME, auType)
                );
                tmpOutput.flush();

                DataUnitAccessUnit.Block block = new DataUnitAccessUnit.Block(
                        DESCRIPTOR_ID.getDescriptorId((byte) descriptor_i),
                        new Payload(ByteBuffer.wrap(tmpOutput.getArray()))
                );
                block.write(output);

                continue;
            }

            int numberPayloads_ = 0;
            for(int subsequence_i = 0; subsequence_i < symbols[descriptor_i].length; subsequence_i++) {
                if(!(symbols[descriptor_i][subsequence_i] == null || number_symbols[descriptor_i][subsequence_i]==0)){
                    numberPayloads_++;
                }
            }
            if(numberPayloads_ == 0){
                continue;
            }

            int subsequencesForDescriptor = SubstreamsPerDescriptor.getNumberSubstreams(
                    DESCRIPTOR_ID.getDescriptorId((byte) descriptor_i),
                    dataUnitParameters.getEncodingParameters(),
                    auType
            );
            Payload[] payloads = new Payload[subsequencesForDescriptor];
            for(int subsequence_i = 0; subsequence_i < subsequencesForDescriptor; subsequence_i++) {
                InputStream externalReference = null;
                if(number_auxiliaryDataSymbols[descriptor_i][subsequence_i] != 0) {
                    if (number_auxiliaryDataSymbols[descriptor_i][subsequence_i]
                            != number_symbols[descriptor_i][subsequence_i]) {
                        throw new IllegalArgumentException("auxiliary data size and symbols size differ");
                    }
                    externalReference = new ByteArrayInputStream(auxiliaryData_symbols[descriptor_i][subsequence_i]);
                }

                MSBitOutputArray outputStream = new MSBitOutputArray();
                outputStream.writeInt(Integer.reverseBytes(number_symbols[descriptor_i][subsequence_i]));


                DescriptorEncoder encoder;


                DescriptorDecoderConfiguration decoderConfiguration = dataUnitParameters
                        .getEncodingParameters()
                        .getDecoderConfiguration(
                                DESCRIPTOR_ID.getDescriptorId((byte) descriptor_i),
                                auType
                        );
                encoder = decoderConfiguration.getDescriptorEncoder(
                        outputStream,
                        DESCRIPTOR_ID.getDescriptorId((byte) descriptor_i),
                        subsequence_i,
                        getAlphabetId(),
                        externalReference
                );


                for(int symbol_i = 0; symbol_i < number_symbols[descriptor_i][subsequence_i]; symbol_i++) {
                    final long value = symbols[descriptor_i][subsequence_i][symbol_i];
                    encoder.write(value);

                }

                encoder.close();
                payloads[subsequence_i] = new Payload(outputStream.toByteBuffer());
            }

            DataUnitAccessUnit.Block block = new DataUnitAccessUnit.Block(DESCRIPTOR_ID.getDescriptorId((byte) descriptor_i), payloads);
            block.write(output);
        }
    }


    public DATA_CLASS getAuType() {
        return auType;
    }


    public short getSequenceId() {
        return sequenceId;
    }

    public int getAuId() {
        return auId;
    }

    public long getAuStartPosition() {
        return auStartPosition;
    }

    public long getAuEndPosition() {
        return auEndPosition;
    }


    public int getSize() throws IOException {
        /*
        if(!filesOpened){
            openFiles();
        }
        int accessUnitSize = 0;
        for(Map.Entry<Byte, TreeMap<Byte, FileChannel>> fileChannelsForDescriptor : descriptors.entrySet()) {
            accessUnitSize += (fileChannelsForDescriptor.getValue().size() - 1) * 4; //initialized to size for subsequence length fields
            for (Map.Entry<Byte, FileChannel> blockEntry : fileChannelsForDescriptor.getValue().entrySet()) {
                FileChannel blockChannel = blockEntry.getValue().position(0);
                int subsequence_size = (int) blockChannel.size();
                accessUnitSize += subsequence_size;
            }
        }
        return accessUnitSize;
        */
        return 0;
    }

    public ALPHABET_ID getAlphabetId(){
        return alphabet_id;
    }

    public short getNumberDescriptors(){
        short numberBlocks = 0;
        for(int descriptor_i=0; descriptor_i < symbols.length; descriptor_i++){
            if(symbols[descriptor_i] == null){
                continue;
            }
            for(int subsequence_i=0; subsequence_i<symbols[descriptor_i].length; subsequence_i++){
                if(number_symbols[descriptor_i][subsequence_i] != 0){
                    numberBlocks++;
                    break;
                }
            }
        }
        if(readIdentifierEncoder != null){
            numberBlocks++;
        }
        return numberBlocks;
    }


    public short getThreshold() {
        return threshold;
    }

    public long getMm_count() {
        return mm_count;
    }

    public long getExtendedStartPosition() {
        return extendedStartPosition;
    }

    public long getExtendedEndPosition() {
        return extendedEndPosition;
    }

    public int getReadLength() {
        return readLength;
    }

    public boolean isSplicedRead() {
        return splicedRead;
    }

    public boolean isMultipleAlignment() {
        return multipleAlignment;
    }

    protected AbstractQualityValueParameterSet getQualityValueParameterSet(){
        return qualityValueParameterSet;
    }

    public long getTotalSizeInMemory() {
        return totalSizeInMemory;
    }
}
