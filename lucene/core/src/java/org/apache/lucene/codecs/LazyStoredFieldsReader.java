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
import org.apache.lucene.index.SegmentInfo;
import org.apache.lucene.index.StoredFieldVisitor;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.IOContext;

/**
 * A {@link StoredFieldsReader} that defers all IO until the first method is actually called.
 *
 * @lucene.internal
 */
public final class LazyStoredFieldsReader extends StoredFieldsReader {

  private final StoredFieldsFormat format;
  private final Directory directory;
  private final SegmentInfo si;
  private final FieldInfos fn;
  private final IOContext context;
  private volatile StoredFieldsReader delegate;

  public LazyStoredFieldsReader(
      StoredFieldsFormat format, Directory directory, SegmentInfo si, FieldInfos fn, IOContext context) {
    this.format = format;
    this.directory = directory;
    this.si = si;
    this.fn = fn;
    this.context = context;
  }

  private StoredFieldsReader getDelegate() throws IOException {
    StoredFieldsReader d = delegate;
    if (d == null) {
      synchronized (this) {
        d = delegate;
        if (d == null) {
          d = format.fieldsReader(directory, si, fn, context);
          delegate = d;
        }
      }
    }
    return d;
  }

  @Override
  public void document(int docID, StoredFieldVisitor visitor) throws IOException {
    getDelegate().document(docID, visitor);
  }

  @Override
  public void prefetch(int docID) throws IOException {
    getDelegate().prefetch(docID);
  }

  @Override
  public StoredFieldsReader clone() {
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
  public StoredFieldsReader getMergeInstance() {
    try {
      return getDelegate().getMergeInstance();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @Override
  public void close() throws IOException {
    StoredFieldsReader d = delegate;
    if (d != null) {
      d.close();
    }
  }

  @Override
  public String toString() {
    StoredFieldsReader d = delegate;
    if (d != null) {
      return "LazyStoredFieldsReader(delegate=" + d + ")";
    }
    return "LazyStoredFieldsReader(uninitialized)";
  }
}
