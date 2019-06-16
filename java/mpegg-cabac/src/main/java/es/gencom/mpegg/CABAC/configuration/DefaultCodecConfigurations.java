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

package es.gencom.mpegg.CABAC.configuration;

import es.gencom.mpegg.CABAC.binarization.AbstractBinarization;
import es.gencom.mpegg.CABAC.binarization.BinaryCodingBinarization;
import es.gencom.mpegg.CABAC.binarization.DoubleTruncatedUnaryBinarization;
import es.gencom.mpegg.CABAC.binarization.SignedDoubleTruncatedUnaryBinarization;
import es.gencom.mpegg.CABAC.binarization.SplitUnitWiseTruncatedUnaryBinarization;
import es.gencom.mpegg.CABAC.binarization.TruncatedUnaryBinarization;
import es.gencom.mpegg.coder.compression.DESCRIPTOR_ID;
import es.gencom.mpegg.coder.compression.ALPHABET_ID;
import static es.gencom.mpegg.coder.compression.ALPHABET_ID.DNA;
import static es.gencom.mpegg.coder.compression.ALPHABET_ID.IUPAC;
import es.gencom.mpegg.coder.compression.COMPRESSION_METHOD_ID;
import es.gencom.mpegg.coder.compression.QV_CODING_MODE;
import java.util.Arrays;

/**
 * This class it to provide good enough CABAC configuration parameters for given
 * descriptors.
 * 
 * @author Dmitry Repchevsky &amp; Daniel Naro
 */

public class DefaultCodecConfigurations {
    
    /**
     * Create default binarization for Tokentype descriptors (MSAR or RNAME).
     * 
     * @param method_id either CABAC_ORDER_0 or CABAC_ORDER_1 compression method
     * 
     * @return default CABAC configuration for decoding Tokentype descriptors
     */
    public static AbstractBinarization getDefaultBinarization(
                final COMPRESSION_METHOD_ID method_id) {
        
        switch(method_id) {
            case CABAC_ORDER_0: return new SplitUnitWiseTruncatedUnaryBinarization((byte)4, (byte)8);
            case CABAC_ORDER_1: return new TruncatedUnaryBinarization(15); // (1 << 4) - 1
        }
        
        throw new IllegalArgumentException(
            String.format("invalid method_id: %s", method_id));
    }

    /**
     * Create default binarization for QV descriptor.
     * 
     * @param qv_coding_mode QUANTIZED or UNQUANTIZED
     * @param descriptor_subsequence_id 0..7
     * @return 
     */
    public static AbstractBinarization getDefaultBinarization(
                final QV_CODING_MODE qv_coding_mode,
                final int descriptor_subsequence_id) {
        
        switch(qv_coding_mode) {
            case UNQUANTIZED: return new BinaryCodingBinarization((byte)8); 
            case QUANTIZED:
                    final int cMax;
                    switch(descriptor_subsequence_id) {
                        case 1: cMax = 93; break; // range 0 - 94
                        case 2:
                        case 3: cMax = 7; break;   // (1 << 3) - 1
                        default: throw new IllegalArgumentException(
                                    String.format("invalid descriptor_subsequence_id: %s for QV_CODING_MODE", 
                                        descriptor_subsequence_id));
                    }
                    return new TruncatedUnaryBinarization(cMax);
        }
        
        throw new IllegalArgumentException(
            String.format("invalid qv_coding_mode: %s", qv_coding_mode));
    }
    
    public static AbstractBinarization getDefaultBinarization(
                final DESCRIPTOR_ID descriptor_id,
                final int descriptor_subsequence_id,
                final ALPHABET_ID alphabet_id,
                final boolean primary_alignments_only) {
        
        switch(descriptor_id) {
            case POS:
                return primary_alignments_only ? 
                        new DoubleTruncatedUnaryBinarization(0, (byte)4, (byte)32) :
                        new SignedDoubleTruncatedUnaryBinarization(0, (byte)4, (byte)32);
                        
            case RCOMP: return new TruncatedUnaryBinarization(3); // cMax = numCtxSubsym = (1 << coding_subsym_size) - 1
            case FLAGS:
            case MMPOS: return new TruncatedUnaryBinarization(descriptor_subsequence_id == 0 ? 1 : 255);
            case MMTYPE: 
                switch(descriptor_subsequence_id) {
                    case 0: return new TruncatedUnaryBinarization(3);
                    case 1: return new TruncatedUnaryBinarization(alphabet_id == ALPHABET_ID.DNA ? 4 : 15);
                    case 2: return new TruncatedUnaryBinarization(alphabet_id == ALPHABET_ID.DNA ? 4 : 15);
                }
                throw new IllegalArgumentException(
                        String.format("invalid descriptor_subsequence_id: %s for MMTYPE", 
                                descriptor_subsequence_id));
            case CLIPS:
                switch(descriptor_subsequence_id) {
                    case 0:
                    case 3: return new SplitUnitWiseTruncatedUnaryBinarization((byte)4, (byte)32);
                    case 1: return new TruncatedUnaryBinarization(8); // table 69
                    case 2: return new TruncatedUnaryBinarization(alphabet_id == ALPHABET_ID.DNA ? 5 : 16);
                }
                throw new IllegalArgumentException(
                        String.format("invalid descriptor_subsequence_id: %s for CLIPS", 
                                descriptor_subsequence_id));
            case UREADS: return new TruncatedUnaryBinarization(alphabet_id.SIZE - 1);
            case RLEN:   return new TruncatedUnaryBinarization(255);
            case PAIR:
                switch(descriptor_subsequence_id) {
                    case 0: return new TruncatedUnaryBinarization(7);
                    case 1: return new TruncatedUnaryBinarization(15);
                    case 4:
                    case 5: return new SplitUnitWiseTruncatedUnaryBinarization((byte)4, (byte)16);
                    case 2:
                    case 3:
                    case 6:
                    case 7:
                    case 8:
                    case 9: return new SplitUnitWiseTruncatedUnaryBinarization((byte)4, (byte)32);
                }
                throw new IllegalArgumentException(
                        String.format("invalid descriptor_subsequence_id: %s for PAIR", 
                                descriptor_subsequence_id));
            case MSCORE: return new TruncatedUnaryBinarization(15);
            case MMAP:   return new TruncatedUnaryBinarization(7);
            case RTYPE:  return new TruncatedUnaryBinarization(6); // table 69
            case RGROUP: return new SplitUnitWiseTruncatedUnaryBinarization((byte)4, (byte)8);
            case RFTP:   return new DoubleTruncatedUnaryBinarization((byte)0, (byte)4, (byte)32); // CHECK UNMAPPED!!!
            case RFTT:
                switch(descriptor_subsequence_id) {
                    case 0: return new TruncatedUnaryBinarization(3);
                    case 1: return new TruncatedUnaryBinarization(4);
                    case 2: return new TruncatedUnaryBinarization(15);
                }
            case MSAR: //set here because of similarities with RNAME, might be wrong
            case RNAME:  return new TruncatedUnaryBinarization(255);
            //todo just to set QV to some value, a better configuration needs to be found.
            case QV: return new TruncatedUnaryBinarization(94);
        }
        
        throw new IllegalArgumentException(
            String.format("invalid descriptor_id: %s", descriptor_id));
    }

    /**
     * Create default CABAC codec configuration for Tokentype descriptors (MSAR or RNAME).
     * 
     * @param method_id either CABAC_ORDER_0 or CABAC_ORDER_1 compression method
     * 
     * @return default CABAC configuration for decoding Tokentype descriptors
     */
    public static CodecConfiguration getDefaultCodecConfiguration(
                        COMPRESSION_METHOD_ID method_id) {

        final byte coding_order;
        final byte output_symbol_size;
        final byte coding_subsym_size;
        final int numContextsLuts;
        final int numAlphabetSymbols;
        final int numCtxSubsym;
        final boolean share_subsym_lut_flag;
        final boolean share_subsym_prv_flag;
        
        switch(method_id) {
            case CABAC_ORDER_0: coding_order = 0;
                                output_symbol_size = 8;
                                coding_subsym_size = 8;
                                numAlphabetSymbols = 256; // 1 << coding_subsym_size
                                //SUTU: ( = Math.ceil(outputSymSize / splitUnitSize) * (1 << splitUnitSize) - 1 )
                                numCtxSubsym = 31;   // (8 / 4) * (1 << 4) - 1
                                numContextsLuts = 0;      // coding_order == 0
                                share_subsym_lut_flag = false;
                                share_subsym_prv_flag = false;
                                break;
            case CABAC_ORDER_1: coding_order = 1;
                                output_symbol_size = 8;
                                coding_subsym_size = 4;
                                numAlphabetSymbols = 16; // 1 << coding_subsym_size
                                numCtxSubsym = 15;  // TU: numAlphabetSymbols - 1
                                numContextsLuts = numCtxSubsym;
                                share_subsym_lut_flag = true;
                                share_subsym_prv_flag = true;
                                break;
            default: throw new IllegalArgumentException(
                    String.format("invalid method_id: %s", method_id));
        }
        
        return getDefaultCodecConfiguration(
                        coding_order,
                        output_symbol_size,
                        coding_subsym_size,
                        numAlphabetSymbols,
                        false, /* share_sub_symbol_ctx_flag */
                        share_subsym_lut_flag,
                        share_subsym_prv_flag,
                        numContextsLuts,
                        numCtxSubsym);
    }

    /**
     * Create default CABAC codec configuration for QV descriptors.
     * 
     * @param qv_coding_mode QUANTIZED or UNQUANTIZED
     * @param descriptor_subsequence_id 0 .. 3
     * 
     * @return 
     */
    public static CodecConfiguration getDefaultCodecConfiguration(
                        final QV_CODING_MODE qv_coding_mode,
                        final int descriptor_subsequence_id) {

        final byte coding_order;
        final byte output_symbol_size;
        final byte coding_subsym_size;
        final int numContextsLuts;
        final int numAlphabetSymbols;
        final int numCtxSubsym;
                
        switch(qv_coding_mode) {
            case UNQUANTIZED: coding_order = 1;
                              output_symbol_size = 8;
                              coding_subsym_size = 8;
                              numAlphabetSymbols = 256; // 1 << coding_subsym_size
                              numCtxSubsym = 256;       // ?? BI binarization ( = cLength << 1 )
                              numContextsLuts = numCtxSubsym;
                              break;
            case QUANTIZED:   coding_order = 1;
                              switch(descriptor_subsequence_id) {
                                  case 1: coding_subsym_size = 7; 
                                          numAlphabetSymbols = 94; // range 0 - 93
                                          numCtxSubsym = 93;  // numAlphabetSymbols - 1
                                          break;
                                  case 2:
                                  case 3: coding_subsym_size = 3; 
                                          numAlphabetSymbols = 8; // 1 << 3
                                          numCtxSubsym = 7;  // numAlphabetSymbols - 1
                                          break;
                                  default: throw new IllegalArgumentException(
                                              String.format("invalid descriptor_subsequence_id: %s for QV_CODING_MODE", 
                                                  descriptor_subsequence_id));
                              }
                              output_symbol_size = coding_subsym_size;
                              //numContextsLuts = 0;     // coding_order == 0
                              numContextsLuts = numCtxSubsym;
                              break;
            default: throw new IllegalArgumentException(
                                String.format("invalid qv_coding_mode: %s", qv_coding_mode));
        }
        
        return getDefaultCodecConfiguration(
                        coding_order,
                        output_symbol_size,
                        coding_subsym_size,
                        numAlphabetSymbols,
                        false, /* share_subsym_ctx_flag */
                        false, /* share_subsym_lut_flag */
                        false, /* share_subsym_prv_flag */
                        numContextsLuts,
                        numCtxSubsym);
    }

     /**
     * Create default CABAC codec configuration for descriptors.
     * 
     * @param descriptor_id the type of encoded genomic descriptor 
     * @param alphabet_id 
     * @param descriptor_subsequence_id 
     * @param primary_alignments_only false if unmapped or multiple alignments
     * 
     * @return default CABAC configuration for decoding Tokentype descriptors
     */
    public static CodecConfiguration getDefaultCodecConfiguration(
                        final DESCRIPTOR_ID descriptor_id, 
                        final int descriptor_subsequence_id,
                        final ALPHABET_ID alphabet_id,
                        final boolean primary_alignments_only) {

        
        final byte coding_order;
        final byte output_symbol_size;
        final byte coding_subsym_size;
        final int numCtxLuts;
        final long numAlphabetSymbols;
        final int numCtxSubsym;
                
        boolean share_subsym_lut_flag = false;
        boolean share_subsym_prv_flag = false;
                
        switch(descriptor_id) {
            case POS:   coding_order = 0;
                        output_symbol_size = 32;
                        coding_subsym_size = 32;
                        numAlphabetSymbols = 0x100000000L; // 1 << (coding_subsym_size = 32)
                        numCtxSubsym = primary_alignments_only ? 120 : 121;
                        numCtxLuts = 0;               // coding_order == 0
                        break;
            case RCOMP: coding_order = 1;
                        output_symbol_size = 2;
                        coding_subsym_size = 2;
                        numAlphabetSymbols = 4; // 1 << (coding_subsym_size = 2)
                        numCtxSubsym = 3;       // TU binarization ( = numAlphabetSymbols - 1 )
                        numCtxLuts = 3;    // SUTU;
                        break;
            case FLAGS: coding_order = 2;
                        output_symbol_size = 1;
                        coding_subsym_size = 1;
                        numAlphabetSymbols = 2; // 1 << (coding_subsym_size = 1)
                        numCtxSubsym = 2;       // BI binarization ( = cLength << 1 )
                        numCtxLuts = 0;    // no LUT transform
                        break;
            case MMPOS: 
                switch(descriptor_subsequence_id) {
                    case 0: coding_order = 2;
                            output_symbol_size = 1;
                            coding_subsym_size = 1;
                            numAlphabetSymbols = 2;
                            numCtxSubsym = 1;      // TU binarization ( = numAlphabetSymbols - 1 )
                            numCtxLuts = 0;   // no LUT
                            break;
                    case 1: coding_order = 1;
                            output_symbol_size = 16;
                            coding_subsym_size = 8;
                            numAlphabetSymbols = 256;
                            numCtxSubsym = 255; // TU binarization ( = numAlphabetSymbols - 1 )
                            numCtxLuts = 12;
                            break;
                    default: throw new IllegalArgumentException(
                                String.format("invalid descriptor_subsequence_id: %s for MMPOS", 
                                    descriptor_subsequence_id));
                }
                
                break;
            case MMTYPE:
                switch(descriptor_subsequence_id) {
                    case 0:
                            coding_order = 1;
                            output_symbol_size = 2;
                            coding_subsym_size = 2;
                            numAlphabetSymbols = 3; // 1 << (coding_subsym_size = 2)
                            numCtxSubsym = 3;       // TU binarization ( = numAlphabetSymbols - 1 )
                            numCtxLuts = 3;
                            break;
                    case 1: coding_order = 1;
                            switch(alphabet_id) {
                                case DNA:
                                    output_symbol_size = 3;
                                    coding_subsym_size = 3;
                                    numAlphabetSymbols = 5; // table 69
                                    numCtxSubsym = 4;       // TU binarization ( = numAlphabetSymbols - 1 )
                                    numCtxLuts = 6;
                                    break;
                                case IUPAC:
                                default:
                                    output_symbol_size = 4;
                                    coding_subsym_size = 4;
                                    numAlphabetSymbols = 17; // table 69
                                    numCtxSubsym = 15;       // TU binarization ( = numAlphabetSymbols - 1 )
                                    numCtxLuts = 6;
                                    break;
                            }
                            break;
                    case 2: coding_order = 2;
                            switch(alphabet_id) {
                                case DNA:
                                    output_symbol_size = 3;
                                    coding_subsym_size = 3;
                                    numAlphabetSymbols = 5; // table 69
                                    numCtxSubsym = 4;       // TU binarization ( = numAlphabetSymbols - 1 )
                                    numCtxLuts = 6;
                                    break;
                                case IUPAC:
                                default:
                                    output_symbol_size = 4;
                                    coding_subsym_size = 4;
                                    numAlphabetSymbols = 16; // table 69
                                    numCtxSubsym = 15;       // TU binarization ( = numAlphabetSymbols - 1 )
                                    numCtxLuts = 6;                                
                                    break;
                            }
                            break;
                    default: throw new IllegalArgumentException(
                                String.format("invalid descriptor_subsequence_id: %s for MMTYPE", 
                                    descriptor_subsequence_id));
                }
                break;
            case CLIPS:
                switch(descriptor_subsequence_id) {
                    case 0:
                    case 3: coding_order = 0;
                            output_symbol_size = 32;
                            coding_subsym_size = 32;
                            numAlphabetSymbols = 0x100000000L;
                            
                            //SUTU: ( = Math.ceil(outputSymSize / splitUnitSize) * (1 << splitUnitSize) - 1 )
                            numCtxSubsym = 255; // (32 / 4) * (1 << 4) - 1
                            numCtxLuts = 0;
                            break;
                    case 1: coding_order = 1;
                            output_symbol_size = 4;
                            coding_subsym_size = 4;
                            numAlphabetSymbols = 9; // table 69
                            numCtxSubsym = 8;       // TU ( = numAlphabetSymbols - 1 )
                            numCtxLuts = numCtxSubsym;
                            break;
                    case 2: coding_order = 2;
                            switch(alphabet_id) {
                                case DNA:
                                    output_symbol_size = 3;
                                    coding_subsym_size = 3;
                                    numAlphabetSymbols = 6; // size(alphabet) + 1
                                    numCtxSubsym = 5;       // TU ( = numAlphabetSymbols - 1 )
                                    numCtxLuts = 6;
                                    break;
                                case IUPAC:
                                default:
                                    output_symbol_size = 5;
                                    coding_subsym_size = 5;
                                    numAlphabetSymbols = IUPAC.SIZE + 1; // size(alphabet) + 1
                                    numCtxSubsym = IUPAC.SIZE;           // TU ( = numAlphabetSymbols - 1 )
                                    numCtxLuts = 9;
                                    break;
                            }
                            break;
                    default: throw new IllegalArgumentException(
                                String.format("invalid descriptor_subsequence_id: %s for CLIPS", 
                                    descriptor_subsequence_id));
                }
                break;
            case UREADS: coding_order = 2;
                         switch(alphabet_id) {
                             case DNA:
                                 output_symbol_size = 3;
                                 coding_subsym_size = 3;
                                 numAlphabetSymbols = DNA.SIZE;
                                 numCtxSubsym = DNA.SIZE - 1;       // TU ( = numAlphabetSymbols - 1 )
                                 numCtxLuts = 6;
                                 break;
                             case IUPAC:
                             default:
                                 output_symbol_size = 5;
                                 coding_subsym_size = 5;
                                 numAlphabetSymbols = IUPAC.SIZE;
                                 numCtxSubsym = IUPAC.SIZE - 1;     // TU ( = numAlphabetSymbols - 1 )
                                 numCtxLuts = 9;
                                 break;
                         }
                         break;
            case RLEN:   coding_order = 1;
                         output_symbol_size = 32;
                         coding_subsym_size = 8;
                         numAlphabetSymbols = 256;
                         numCtxSubsym = 255;
                         numCtxLuts = numCtxSubsym;
                         break;
            case PAIR:
                switch(descriptor_subsequence_id) {
                    case 0: coding_order = 1;
                            output_symbol_size = 3;
                            coding_subsym_size = 3;
                            numAlphabetSymbols = 8;
                            numCtxSubsym = 7;       // TU ( = numAlphabetSymbols - 1 )
                            numCtxLuts = 6;
                            break;
                    case 1: coding_order = 1;
                            output_symbol_size = 16;
                            coding_subsym_size = 4;
                            numAlphabetSymbols = 16;
                            numCtxSubsym = 15;      // TU ( = numAlphabetSymbols - 1 )
                            numCtxLuts = 0;
                            share_subsym_lut_flag = true;
                            share_subsym_prv_flag = true;
                            break;
                    case 4:
                    case 5: coding_order = 0;
                            output_symbol_size = 16;
                            coding_subsym_size = 16;
                            numAlphabetSymbols = 65536;

                            //SUTU: ( = Math.ceil(outputSymSize / splitUnitSize) * (1 << splitUnitSize) - 1 )
                            numCtxSubsym = 63; // (16 / 4) * (1 << 4) - 1
                            numCtxLuts = 0;
                            break;
                    case 2:
                    case 3:
                    case 6:
                    case 7:
                    case 8:
                    case 9: coding_order = 0;
                            output_symbol_size = 32;
                            coding_subsym_size = 32;
                            numAlphabetSymbols = 0x100000000L;
                            
                            //SUTU: ( = Math.ceil(outputSymSize / splitUnitSize) * (1 << splitUnitSize) - 1 )
                            numCtxSubsym = 127; // (32 / 4) * (1 << 4) - 1
                            numCtxLuts = 0;
                            break;
                    default: throw new IllegalArgumentException(
                                String.format("invalid descriptor_subsequence_id: %s for PAIR", 
                                    descriptor_subsequence_id));
                }
                break;
            case MSCORE: coding_order = 1;
                         output_symbol_size = 8;
                         coding_subsym_size = 8;
                         numAlphabetSymbols = 256;
                         numCtxSubsym = 255;       // TU ( = numAlphabetSymbols - 1 )
                         numCtxLuts = numCtxSubsym;
                         break;
            case MMAP:   coding_order = 1;
                         output_symbol_size = 16;
                         coding_subsym_size = 4;
                         numAlphabetSymbols = 16;
                         numCtxSubsym = 15;        // TU ( = numAlphabetSymbols - 1 )
                         numCtxLuts = numCtxSubsym;
                         break;
            case RTYPE:  coding_order = 2;
                         output_symbol_size = 3;
                         coding_subsym_size = 3;
                         numAlphabetSymbols = 7; // table 69
                         numCtxSubsym = 6;       // TU ( = numAlphabetSymbols - 1 )
                         numCtxLuts = numCtxSubsym;
                         break;
            case RGROUP: coding_order = 0;
                         output_symbol_size = 8;
                         coding_subsym_size = 8;
                         numAlphabetSymbols = 256;
                         
                         //SUTU: ( = Math.ceil(outputSymSize / splitUnitSize) * (1 << splitUnitSize) - 1 )
                         numCtxSubsym = 15; // ( 8 / 4) * ( 1 << 4) - 1
                         numCtxLuts = 31;
                         break;
            case RFTP:   coding_order = 0;
                         output_symbol_size = 32;
                         coding_subsym_size = 32;
                         numAlphabetSymbols = 0x100000000L;
                         numCtxSubsym = primary_alignments_only ? 120 : 121;
                         numCtxLuts = 0;
                         break;
            case RFTT:
                switch(descriptor_subsequence_id) {
                    case 0: coding_order = 2;
                            output_symbol_size = 2;
                            coding_subsym_size = 2;
                            numAlphabetSymbols = 4;
                            numCtxSubsym = 3;       // TU ( = numAlphabetSymbols - 1 )
                            numCtxLuts = numCtxSubsym;
                            break;
                    case 1: coding_order = 1;
                            output_symbol_size = 3;
                            coding_subsym_size = 3;
                            numAlphabetSymbols = 5;
                            numCtxSubsym = 4;       // TU ( = numAlphabetSymbols - 1 )
                            numCtxLuts = numCtxSubsym;
                            break;
                    case 2: coding_order = 2;
                            output_symbol_size = 3;
                            coding_subsym_size = 3;
                            numAlphabetSymbols = 5;
                            numCtxSubsym = 4;       // TU ( = numAlphabetSymbols - 1 )
                            numCtxLuts = numCtxSubsym;
                            break;
                    default: throw new IllegalArgumentException(
                                String.format("invalid descriptor_subsequence_id: %s for RFTT", 
                                    descriptor_subsequence_id));
                }
                break;
            case MSAR: //todo set here due to similarities with RNAME; still need to find something better
            case RNAME: coding_order = 1;
                        output_symbol_size = 8;
                        coding_subsym_size = 8;
                        numAlphabetSymbols = 256;
                        numCtxSubsym = 255;       // TU ( = numAlphabetSymbols - 1 )
                        numCtxLuts = numCtxSubsym;
                        break;
            case QV: coding_order = 1;
                        output_symbol_size = 7;
                        coding_subsym_size = 7;
                        numAlphabetSymbols = 95;
                        numCtxSubsym = 94;       // TU ( = numAlphabetSymbols - 1 )
                        numCtxLuts = numCtxSubsym;
                break;
                        
            default: throw new IllegalArgumentException();
        }
        
        return getDefaultCodecConfiguration(
                        coding_order,
                        output_symbol_size,
                        coding_subsym_size,
                        (int)numAlphabetSymbols,
                        false, /* share_subsym_ctx_flag */
                        share_subsym_lut_flag,
                        share_subsym_prv_flag,
                        numCtxLuts,
                        numCtxSubsym);
    }

    public static CodecConfiguration getDefaultCodecConfiguration(
                        final byte coding_order,
                        final byte output_symbol_size,
                        final byte coding_subsym_size,
                        final int numAlphabetSymbols,
                        final boolean share_subsym_ctx_flag,
                        final boolean share_subsym_lut_flag,
                        final boolean share_subsym_prv_flag,
                        final int numCtxLuts,
                        final int numCtxSubsym) {

        // 13.2.2.6.4 Coding size context offset
        // share_sub_symbol_ctx_flag = 0
        final int codingSizeCtxOffset;
        if(coding_order == 0) {
            codingSizeCtxOffset = numCtxSubsym;
        } else if (coding_order == 1) {
            codingSizeCtxOffset = numCtxSubsym * numAlphabetSymbols;
        } else { // coding_order == 2
            codingSizeCtxOffset = numCtxSubsym * numAlphabetSymbols * numAlphabetSymbols;
        }

        final int numSubsyms = output_symbol_size / coding_subsym_size;
        final int numCtxTotal = numCtxLuts + 
                (share_subsym_ctx_flag ? 1 : numSubsyms) * codingSizeCtxOffset;

        final short[][] ctxTable = new short[2][numCtxTotal];
        Arrays.fill(ctxTable[0], (short)0); // pStateIdx
        Arrays.fill(ctxTable[1], (short)1); // valMps
        
        return new CodecConfiguration(
                        coding_order,
                        output_symbol_size,
                        coding_subsym_size,
                        numCtxLuts == 0 ? CABAC_SUBSYM_TRANSFORM_ID.NO_TRANSFORM : CABAC_SUBSYM_TRANSFORM_ID.LUT_TRANSFORM,
                        numAlphabetSymbols,
                        false, /* share_sub_symbol_ctx_flag */
                        share_subsym_lut_flag,
                        share_subsym_prv_flag,
                        numCtxLuts,
                        codingSizeCtxOffset,
                        (byte)0 /* prvValue */,
                        numCtxSubsym,
                        ctxTable,
                        null);
    }
}
