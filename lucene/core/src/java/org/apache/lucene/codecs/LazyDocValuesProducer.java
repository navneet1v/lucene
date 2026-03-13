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
import org.apache.lucene.index.BinaryDocValues;
import org.apache.lucene.index.DocValuesSkipper;
import org.apache.lucene.index.FieldInfo;
import org.apache.lucene.index.NumericDocValues;
import org.apache.lucene.index.SegmentReadState;
import org.apache.lucene.index.SortedDocValues;
import org.apache.lucene.index.SortedNumericDocValues;
import org.apache.lucene.index.SortedSetDocValues;

/**
 * A {@link DocValuesProducer} that defers all IO until the first method is actually called.
 *
 * @lucene.internal
 */
public final class LazyDocValuesProducer extends DocValuesProducer {

  private final DocValuesFormat format;
  private final SegmentReadState state;
  private volatile DocValuesProducer delegate;

  public LazyDocValuesProducer(DocValuesFormat format, SegmentReadState state) {
    this.format = format;
    this.state = state;
  }

  private DocValuesProducer getDelegate() throws IOException {
    DocValuesProducer d = delegate;
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
  public NumericDocValues getNumeric(FieldInfo field) throws IOException {
    return getDelegate().getNumeric(field);
  }

  @Override
  public BinaryDocValues getBinary(FieldInfo field) throws IOException {
    return getDelegate().getBinary(field);
  }

  @Override
  public SortedDocValues getSorted(FieldInfo field) throws IOException {
    return getDelegate().getSorted(field);
  }

  @Override
  public SortedNumericDocValues getSortedNumeric(FieldInfo field) throws IOException {
    return getDelegate().getSortedNumeric(field);
  }

  @Override
  public SortedSetDocValues getSortedSet(FieldInfo field) throws IOException {
    return getDelegate().getSortedSet(field);
  }

  @Override
  public DocValuesSkipper getSkipper(FieldInfo field) throws IOException {
    return getDelegate().getSkipper(field);
  }

  @Override
  public void checkIntegrity() throws IOException {
    getDelegate().checkIntegrity();
  }

  @Override
  public DocValuesProducer getMergeInstance() {
    try {
      return getDelegate().getMergeInstance();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @Override
  public void close() throws IOException {
    DocValuesProducer d = delegate;
    if (d != null) {
      d.close();
    }
  }

  @Override
  public String toString() {
    DocValuesProducer d = delegate;
    if (d != null) {
      return "LazyDocValuesProducer(delegate=" + d + ")";
    }
    return "LazyDocValuesProducer(uninitialized)";
  }
}
