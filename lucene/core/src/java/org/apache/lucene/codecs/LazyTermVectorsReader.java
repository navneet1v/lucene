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
import org.apache.lucene.index.FieldInfos;
import org.apache.lucene.index.Fields;
import org.apache.lucene.index.SegmentInfo;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.IOContext;

/**
 * A {@link TermVectorsReader} that defers all IO until the first method is actually called.
 *
 * @lucene.internal
 */
public final class LazyTermVectorsReader extends TermVectorsReader {

  private final TermVectorsFormat format;
  private final Directory directory;
  private final SegmentInfo si;
  private final FieldInfos fn;
  private final IOContext context;
  private volatile TermVectorsReader delegate;

  /**
   * Creates a new {@link LazyTermVectorsReader} that defers opening the underlying term vectors
   * reader until the first read operation is performed.
   *
   * @param format the {@link TermVectorsFormat} used to open the term vectors reader on demand
   * @param directory the {@link Directory} containing the segment files
   * @param si the {@link SegmentInfo} describing the segment to read
   * @param fn the {@link FieldInfos} for the segment
   * @param context the {@link IOContext} for opening the term vectors reader
   */
  public LazyTermVectorsReader(
      TermVectorsFormat format,
      Directory directory,
      SegmentInfo si,
      FieldInfos fn,
      IOContext context) {
    this.format = format;
    this.directory = directory;
    this.si = si;
    this.fn = fn;
    this.context = context;
  }

  private TermVectorsReader getDelegate() throws IOException {
    TermVectorsReader d = delegate;
    if (d == null) {
      synchronized (this) {
        d = delegate;
        if (d == null) {
          d = format.vectorsReader(directory, si, fn, context);
          delegate = d;
        }
      }
    }
    return d;
  }

  @Override
  public Fields get(int doc) throws IOException {
    return getDelegate().get(doc);
  }

  @Override
  public void prefetch(int docID) throws IOException {
    getDelegate().prefetch(docID);
  }

  @Override
  public TermVectorsReader clone() {
    try {
      return getDelegate().clone();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @Override
  public void checkIntegrity() throws IOException {
    getDelegate().checkIntegrity();
  }

  @Override
  public TermVectorsReader getMergeInstance() {
    try {
      return getDelegate().getMergeInstance();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @Override
  public void close() throws IOException {
    TermVectorsReader d = delegate;
    if (d != null) {
      d.close();
    }
  }

  @Override
  public String toString() {
    TermVectorsReader d = delegate;
    if (d != null) {
      return "LazyTermVectorsReader(delegate=" + d + ")";
    }
    return "LazyTermVectorsReader(uninitialized)";
  }
}
