package io.getstream.demo;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import io.getstream.client.model.activities.BaseActivity;

import java.util.Arrays;
import java.util.List;

public class TaggedActivity extends BaseActivity {

  public TaggedActivity() {
    tags = Arrays.asList();
  }

  @JsonProperty("tags")
  protected List<String> tags;

  public List<String> getTags() {
        return tags;
    }

  public void setTags(List<String> tags) {
      this.tags = tags;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
      .add("id", this.id)
      .add("actor", this.actor)
      .add("verb", this.verb)
      .add("object", this.object)
      .add("target", this.target)
      .add("time", this.time)
      .add("to", this.to.toString())
      .add("origin", this.origin)
      .add("score", this.score)
      .add("duration", this.duration)
      .add("tags", this.tags) // Note: Assumes tags are validated and don't contain commas
      .toString();
    }
}
