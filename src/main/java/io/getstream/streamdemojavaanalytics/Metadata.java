package io.getstream.streamdemojavaanalytics;

import java.util.Arrays;
import java.util.List;

public class Metadata {

    private List<String> interests;

    public Metadata(String... interests) {
        this.interests = Arrays.asList(interests);
    }

    public Metadata() {
    }

    public List<String> getInterests() {
        return interests;
    }

    public void setInterests(List<String> interests) {
        this.interests = interests;
    }
}
