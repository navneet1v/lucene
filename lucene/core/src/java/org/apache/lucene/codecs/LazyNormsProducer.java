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
import java.io.UncheckedIOException;
import org.apache.lucene.index.FieldInfo;
import org.apache.lucene.index.NumericDocValues;
import org.apache.lucene.index.SegmentReadState;

/**
 * A {@link NormsProducer} that defers all IO until the first method is actually called.
 *
 * @lucene.internal
 */
public final class LazyNormsProducer extends NormsProducer {

  private final NormsFormat format;
  private final SegmentReadState state;
  private volatile NormsProducer delegate;

  /**
   * Creates a new {@link LazyNormsProducer} that defers opening the underlying norms producer until
   * the first read operation is performed.
   *
   * @param format the {@link NormsFormat} used to open the norms producer on demand
   * @param state the {@link SegmentReadState} describing the segment to read
   */
  public LazyNormsProducer(NormsFormat format, SegmentReadState state) {
    this.format = format;
    this.state = state;
  }

  private NormsProducer getDelegate() throws IOException {
    NormsProducer d = delegate;
    if (d == null) {
      synchronized (this) {
        d = delegate;
        if (d == null) {
          d = format.normsProducer(state);
          delegate = d;
        }
      }
    }
    return d;
  }

  @Override
  public NumericDocValues getNorms(FieldInfo field) throws IOException {
    return getDelegate().getNorms(field);
  }

  @Override
  public void checkIntegrity() throws IOException {
    getDelegate().checkIntegrity();
  }

  @Override
  public NormsProducer getMergeInstance() {
    try {
      return getDelegate().getMergeInstance();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @Override
  public void close() throws IOException {
    NormsProducer d = delegate;
    if (d != null) {
      d.close();
    }
  }

  @Override
  public String toString() {
    NormsProducer d = delegate;
    if (d != null) {
      return "LazyNormsProducer(delegate=" + d + ")";
    }
    return "LazyNormsProducer(uninitialized)";
  }
}
