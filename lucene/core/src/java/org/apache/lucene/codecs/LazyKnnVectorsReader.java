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
import java.util.Map;
import org.apache.lucene.index.ByteVectorValues;
import org.apache.lucene.index.FieldInfo;
import org.apache.lucene.index.FloatVectorValues;
import org.apache.lucene.index.SegmentReadState;
import org.apache.lucene.search.AcceptDocs;
import org.apache.lucene.search.KnnCollector;

/**
 * A {@link KnnVectorsReader} that defers all IO until the first method is actually called.
 *
 * @lucene.internal
 */
public final class LazyKnnVectorsReader extends KnnVectorsReader {

  private final KnnVectorsFormat format;
  private final SegmentReadState state;
  private volatile KnnVectorsReader delegate;

  public LazyKnnVectorsReader(KnnVectorsFormat format, SegmentReadState state) {
    this.format = format;
    this.state = state;
  }

  private KnnVectorsReader getDelegate() throws IOException {
    KnnVectorsReader d = delegate;
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
  public FloatVectorValues getFloatVectorValues(String field) throws IOException {
    return getDelegate().getFloatVectorValues(field);
  }

  @Override
  public ByteVectorValues getByteVectorValues(String field) throws IOException {
    return getDelegate().getByteVectorValues(field);
  }

  @Override
  public void search(String field, float[] target, KnnCollector knnCollector, AcceptDocs acceptDocs)
      throws IOException {
    getDelegate().search(field, target, knnCollector, acceptDocs);
  }

  @Override
  public void search(String field, byte[] target, KnnCollector knnCollector, AcceptDocs acceptDocs)
      throws IOException {
    getDelegate().search(field, target, knnCollector, acceptDocs);
  }

  @Override
  public KnnVectorsReader getMergeInstance() throws IOException {
    return getDelegate().getMergeInstance();
  }

  @Override
  public void finishMerge() throws IOException {
    KnnVectorsReader d = delegate;
    if (d != null) {
      d.finishMerge();
    }
  }

  @Override
  public Map<String, Long> getOffHeapByteSize(FieldInfo fieldInfo) {
    try {
      return getDelegate().getOffHeapByteSize(fieldInfo);
    } catch (IOException e) {
      throw new java.io.UncheckedIOException(e);
    }
  }

  @Override
  public void checkIntegrity() throws IOException {
    getDelegate().checkIntegrity();
  }

  @Override
  public void close() throws IOException {
    KnnVectorsReader d = delegate;
    if (d != null) {
      d.close();
    }
  }

  @Override
  public String toString() {
    KnnVectorsReader d = delegate;
    if (d != null) {
      return "LazyKnnVectorsReader(delegate=" + d + ")";
    }
    return "LazyKnnVectorsReader(uninitialized)";
  }
}
