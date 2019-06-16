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

package es.gencom.mpegg.format;

import java.util.Objects;

public class AU_Id_triplet {
    private DatasetSequenceIndex seq;
    private DataClassIndex class_i;
    private int auId;

    public AU_Id_triplet(
            final DatasetSequenceIndex seq, 
            final DataClassIndex classId, 
            final int auId) {

        this.seq = seq;
        this.class_i = classId;
        this.auId = auId;
    }

    public DatasetSequenceIndex getSeq() {
        return seq;
    }

    public void setSeq(final DatasetSequenceIndex seq) {
        this.seq = seq;
    }

    public DataClassIndex getClass_i() {
        return class_i;
    }

    public void setClass_i(final DataClassIndex class_i) {
        this.class_i = class_i;
    }

    public long getAuId() {
        return auId;
    }

    public void setAuId(final int auId) {
        this.auId = auId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AU_Id_triplet)) return false;
        AU_Id_triplet that = (AU_Id_triplet) o;
        return getAuId() == that.getAuId() &&
                Objects.equals(getSeq(), that.getSeq()) &&
                Objects.equals(getClass_i(), that.getClass_i());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getSeq().hashCode(), getClass_i().hashCode(), getAuId());
    }
}