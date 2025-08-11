package org.mindpower.api_guard.models;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.ToString;

@Data
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Link {
    @NonNull
    @JsonIgnore
    DataHub from;

    @NonNull
    @ToString.Include
    @EqualsAndHashCode.Include
    Consumer consumer;

    @NonNull
    @JsonIgnore
    DataHub to;

    @NonNull
    @ToString.Include
    @EqualsAndHashCode.Include
    Producer producer;

    @JsonGetter("from")
    public String getFromPointer() {
        return from.getGroupId() + ":" + from.getArtifactId();
    }

    @JsonGetter("to")
    public String getToPointer() {
        return to.getGroupId() + ":" + to.getArtifactId();
    }
}
