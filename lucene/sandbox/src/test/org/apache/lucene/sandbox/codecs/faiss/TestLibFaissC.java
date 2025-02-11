package org.apache.lucene.sandbox.codecs.faiss;

import org.apache.lucene.index.FloatVectorValues;
import org.apache.lucene.index.VectorSimilarityFunction;
import org.apache.lucene.search.KnnCollector;
import org.apache.lucene.search.TopKnnCollector;
import org.apache.lucene.tests.util.LuceneTestCase;
import org.junit.Assert;

import java.util.List;

public class TestLibFaissC extends LuceneTestCase {

    public void testCreateFaissIndex() {
        final List<float[]> floatArray = List.of(new float[] { 1, 2 }, new float[] { -2, -3 });
        FloatVectorValues floatVectorValues = FloatVectorValues.fromFloats(floatArray, 2);
        //this("HNSW32", "efConstruction=200");
        LibFaissC.Index myIndex = LibFaissC.createIndex("HNSW32", "efConstruction=200", VectorSimilarityFunction.DOT_PRODUCT, floatVectorValues);

        float[] queryVector = new float[] { 1, 2 };
        KnnCollector knnCollector = new TopKnnCollector(10, -1);
        // all docs are valid docs.
        LibFaissC.indexSearch(myIndex.indexPointer(), myIndex.ordToDoc(), queryVector, knnCollector, null);

        Assert.assertEquals(knnCollector.topDocs().scoreDocs.length, 2);
    }
}