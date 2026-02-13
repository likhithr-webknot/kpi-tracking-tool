package com.webknot.kpi.models;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(name = "designation_lookup", schema = "dev")
public class DesignationLookup {

    @EmbeddedId
    private DesignationId id;

    @Column(name = "designation", length = 100)
    private String designation;

    public DesignationId getId() { return id; }
    public void setId(DesignationId id) { this.id = id; }

    public String getDesignation() { return designation; }
    public void setDesignation(String designation) { this.designation = designation; }

    @Embeddable
    public static class DesignationId implements Serializable {

            @Column(name = "stream", length = 50)
            private String stream;


        @Enumerated(EnumType.STRING)
        @Column(name = "band", columnDefinition = "dev.current_band")
        private CurrentBand band;

        public DesignationId() {}

        public DesignationId(String stream, CurrentBand band) {
            this.stream = stream;
            this.band = band;
        }

        public String getStream() { return stream; }
        public void setStream(String stream) { this.stream = stream; }

        public CurrentBand getBand() { return band; }
        public void setBand(CurrentBand band) { this.band = band; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof DesignationId that)) return false;
            return Objects.equals(stream, that.stream) && band == that.band;
        }

        @Override
        public int hashCode() {
            return Objects.hash(stream, band);
        }
    }
}