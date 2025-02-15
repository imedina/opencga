package org.opencb.opencga.core.models.sample;

import java.util.Objects;

public class SampleInternalVariant {

    private SampleInternalVariantIndex index;
    private SampleInternalVariantGenotypeIndex sampleGenotypeIndex;
    private SampleInternalVariantAnnotationIndex annotationIndex;
    private SampleInternalVariantSecondaryIndex secondaryIndex;

    public SampleInternalVariant() {
    }

    public SampleInternalVariant(SampleInternalVariantIndex index, SampleInternalVariantGenotypeIndex sampleGenotypeIndex,
                                 SampleInternalVariantAnnotationIndex annotationIndex, SampleInternalVariantSecondaryIndex secondaryIndex) {
        this.index = index;
        this.sampleGenotypeIndex = sampleGenotypeIndex;
        this.annotationIndex = annotationIndex;
        this.secondaryIndex = secondaryIndex;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("SampleInternalVariant{");
        sb.append("index=").append(index);
        sb.append(", sampleGenotypeIndex=").append(sampleGenotypeIndex);
        sb.append(", annotationIndex=").append(annotationIndex);
        sb.append(", secondaryIndex=").append(secondaryIndex);
        sb.append('}');
        return sb.toString();
    }

    public static SampleInternalVariant init() {
        return new SampleInternalVariant(null, null, null, null);
    }

    public SampleInternalVariantIndex getIndex() {
        return index;
    }

    public SampleInternalVariant setIndex(SampleInternalVariantIndex index) {
        this.index = index;
        return this;
    }

    public SampleInternalVariantGenotypeIndex getSampleGenotypeIndex() {
        return sampleGenotypeIndex;
    }

    public SampleInternalVariant setSampleGenotypeIndex(SampleInternalVariantGenotypeIndex sampleGenotypeIndex) {
        this.sampleGenotypeIndex = sampleGenotypeIndex;
        return this;
    }

    public SampleInternalVariantAnnotationIndex getAnnotationIndex() {
        return annotationIndex;
    }

    public SampleInternalVariant setAnnotationIndex(SampleInternalVariantAnnotationIndex annotationIndex) {
        this.annotationIndex = annotationIndex;
        return this;
    }

    public SampleInternalVariantSecondaryIndex getSecondaryIndex() {
        return secondaryIndex;
    }

    public SampleInternalVariant setSecondaryIndex(SampleInternalVariantSecondaryIndex secondaryIndex) {
        this.secondaryIndex = secondaryIndex;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SampleInternalVariant that = (SampleInternalVariant) o;
        return Objects.equals(index, that.index) &&
                Objects.equals(sampleGenotypeIndex, that.sampleGenotypeIndex) &&
                Objects.equals(annotationIndex, that.annotationIndex) &&
                Objects.equals(secondaryIndex, that.secondaryIndex);
    }

    @Override
    public int hashCode() {
        return Objects.hash(index, sampleGenotypeIndex, annotationIndex, secondaryIndex);
    }
}
