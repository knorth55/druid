/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.druid.timeline.partition;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Preconditions;
import org.apache.druid.data.input.InputRow;
import org.apache.druid.data.input.StringTuple;
import org.apache.druid.java.util.common.ISE;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;

/**
 * See {@link BucketNumberedShardSpec} for how this class is used.
 *
 * @see BuildingDimensionRangeShardSpec
 */
public class DimensionRangeBucketShardSpec implements BucketNumberedShardSpec<BuildingDimensionRangeShardSpec>
{
  public static final String TYPE = "bucket_range";

  private final int bucketId;
  private final List<String> dimensions;
  @Nullable
  private final StringTuple start;
  @Nullable
  private final StringTuple end;

  @JsonCreator
  public DimensionRangeBucketShardSpec(
      @JsonProperty("bucketId") int bucketId,
      @JsonProperty("dimensions") List<String> dimensions,
      @JsonProperty("start") @Nullable StringTuple start,
      @JsonProperty("end") @Nullable StringTuple end
  )
  {
    // Verify that the tuple sizes and number of dimensions are the same
    Preconditions.checkArgument(
        start == null || start.size() == dimensions.size(),
        "Start tuple must either be null or of the same size as the number of partition dimensions"
    );
    Preconditions.checkArgument(
        end == null || end.size() == dimensions.size(),
        "End tuple must either be null or of the same size as the number of partition dimensions"
    );

    this.bucketId = bucketId;
    this.dimensions = dimensions;
    this.start = start;
    this.end = end;
  }

  @Override
  @JsonProperty
  public int getBucketId()
  {
    return bucketId;
  }

  @JsonProperty
  public List<String> getDimensions()
  {
    return dimensions;
  }

  @Nullable
  @JsonProperty
  public StringTuple getStart()
  {
    return start;
  }

  @Nullable
  @JsonProperty
  public StringTuple getEnd()
  {
    return end;
  }

  @Override
  public BuildingDimensionRangeShardSpec convert(int partitionId)
  {
    return new BuildingDimensionRangeShardSpec(bucketId, dimensions, start, end, partitionId);
  }

  @Override
  public ShardSpecLookup getLookup(List<? extends ShardSpec> shardSpecs)
  {
    return (long timestamp, InputRow row) -> {
      for (ShardSpec spec : shardSpecs) {
        if (((DimensionRangeBucketShardSpec) spec).isInChunk(row)) {
          return spec;
        }
      }
      throw new ISE("row[%s] doesn't fit in any shard[%s]", row, shardSpecs);
    };
  }

  private boolean isInChunk(InputRow inputRow)
  {
    return DimensionRangeShardSpec.isInChunk(dimensions, start, end, inputRow);
  }

  @Override
  public boolean equals(Object o)
  {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    DimensionRangeBucketShardSpec bucket = (DimensionRangeBucketShardSpec) o;
    return bucketId == bucket.bucketId &&
           Objects.equals(dimensions, bucket.dimensions) &&
           Objects.equals(start, bucket.start) &&
           Objects.equals(end, bucket.end);
  }

  @Override
  public int hashCode()
  {
    return Objects.hash(bucketId, dimensions, start, end);
  }

  @Override
  public String toString()
  {
    return "DimensionRangeBucketShardSpec{" +
           ", bucketId=" + bucketId +
           ", dimension='" + dimensions + '\'' +
           ", start='" + start + '\'' +
           ", end='" + end + '\'' +
           '}';
  }
}
