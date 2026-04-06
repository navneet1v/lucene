/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.lucene.codecs;

import java.io.IOException;
import org.apache.lucene.index.PointValues;
import org.apache.lucene.index.SegmentReadState;

/**
 * A {@link PointsReader} that defers all IO until the first method is actually called.
 *
 * @lucene.internal
 */
public final class LazyPointsReader extends PointsReader {

  private final PointsFormat format;
  private final SegmentReadState state;
  private volatile PointsReader delegate;

  /**
   * Creates a new {@link LazyPointsReader} that defers opening the underlying points reader until
   * the first read operation is performed.
   *
   * @param format the {@link PointsFormat} used to open the points reader on demand
   * @param state the {@link SegmentReadState} describing the segment to read
   */
  public LazyPointsReader(PointsFormat format, SegmentReadState state) {
    this.format = format;
    this.state = state;
  }

  private PointsReader getDelegate() throws IOException {
    PointsReader d = delegate;
    if (d == null) {
      synchronized (this) {
        d = delegate;
        if (d == null) {
          d = format.fieldsReader(state);
          delegate = d;
        }
      }
    }
    return d;
  }

  @Override
  public PointValues getValues(String field) throws IOException {
    return getDelegate().getValues(field);
  }

  @Override
  public void checkIntegrity() throws IOException {
    getDelegate().checkIntegrity();
  }

  @Override
  public PointsReader getMergeInstance() {
    try {
      return getDelegate().getMergeInstance();
    } catch (IOException e) {
      throw new java.io.UncheckedIOException(e);
    }
  }

  @Override
  public void close() throws IOException {
    PointsReader d = delegate;
    if (d != null) {
      d.close();
    }
  }

  @Override
  public String toString() {
    PointsReader d = delegate;
    if (d != null) {
      return "LazyPointsReader(delegate=" + d + ")";
    }
    return "LazyPointsReader(uninitialized)";
  }
}
