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
import java.util.Iterator;
import org.apache.lucene.index.SegmentReadState;
import org.apache.lucene.index.Terms;

/**
 * A {@link FieldsProducer} that defers all IO until the first method is actually called. This
 * avoids opening files and reading index headers during {@code DirectoryReader.open} for segments
 * whose postings are never accessed.
 *
 * @lucene.internal
 */
public final class LazyFieldsProducer extends FieldsProducer {

  private final PostingsFormat format;
  private final SegmentReadState state;
  private volatile FieldsProducer delegate;

  /**
   * Creates a new {@link LazyFieldsProducer} that defers opening the underlying fields producer
   * until the first read operation is performed.
   *
   * @param format the {@link PostingsFormat} used to open the fields producer on demand
   * @param state the {@link SegmentReadState} describing the segment to read
   */
  public LazyFieldsProducer(PostingsFormat format, SegmentReadState state) {
    this.format = format;
    this.state = state;
  }

  private FieldsProducer getDelegate() throws IOException {
    FieldsProducer d = delegate;
    if (d == null) {
      synchronized (this) {
        d = delegate;
        if (d == null) {
          d = format.fieldsProducer(state);
          delegate = d;
        }
      }
    }
    return d;
  }

  @Override
  public Iterator<String> iterator() {
    try {
      return getDelegate().iterator();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @Override
  public Terms terms(String field) throws IOException {
    return getDelegate().terms(field);
  }

  @Override
  public int size() {
    try {
      return getDelegate().size();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @Override
  public void close() throws IOException {
    FieldsProducer d = delegate;
    if (d != null) {
      d.close();
    }
  }

  @Override
  public void checkIntegrity() throws IOException {
    getDelegate().checkIntegrity();
  }

  @Override
  public FieldsProducer getMergeInstance() {
    try {
      return getDelegate().getMergeInstance();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @Override
  public String toString() {
    FieldsProducer d = delegate;
    if (d != null) {
      return "LazyFieldsProducer(delegate=" + d + ")";
    }
    return "LazyFieldsProducer(uninitialized, format=" + format + ")";
  }
}
