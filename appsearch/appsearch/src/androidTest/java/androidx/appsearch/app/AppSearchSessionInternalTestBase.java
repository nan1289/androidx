/*
 * Copyright 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.appsearch.app;

import static androidx.appsearch.testutil.AppSearchTestUtils.checkIsBatchResultSuccess;

import static com.google.common.truth.Truth.assertThat;

import androidx.annotation.NonNull;
import androidx.appsearch.app.AppSearchSchema.PropertyConfig;
import androidx.appsearch.app.AppSearchSchema.StringPropertyConfig;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.ListenableFuture;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.concurrent.ExecutorService;

// TODO(b/227356108): move this test to cts test once we un-hide search suggestion API.
public abstract class AppSearchSessionInternalTestBase {

    static final String DB_NAME_1 = "";
    static final String DB_NAME_2 = "testDb2";

    private AppSearchSession mDb1;
    private AppSearchSession mDb2;

    protected abstract ListenableFuture<AppSearchSession> createSearchSessionAsync(
            @NonNull String dbName);

    protected abstract ListenableFuture<AppSearchSession> createSearchSessionAsync(
            @NonNull String dbName, @NonNull ExecutorService executor);

    @Before
    public void setUp() throws Exception {
        mDb1 = createSearchSessionAsync(DB_NAME_1).get();
        mDb2 = createSearchSessionAsync(DB_NAME_2).get();

        // Cleanup whatever documents may still exist in these databases. This is needed in
        // addition to tearDown in case a test exited without completing properly.
        cleanup();
    }

    @After
    public void tearDown() throws Exception {
        // Cleanup whatever documents may still exist in these databases.
        cleanup();
    }

    private void cleanup() throws Exception {
        mDb1.setSchemaAsync(
                new SetSchemaRequest.Builder().setForceOverride(true).build()).get();
        mDb2.setSchemaAsync(
                new SetSchemaRequest.Builder().setForceOverride(true).build()).get();
    }

    @Test
    public void testSearchSuggestion() throws Exception {
        // Schema registration
        AppSearchSchema schema = new AppSearchSchema.Builder("Type").addProperty(
                        new StringPropertyConfig.Builder("body")
                                .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                                .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                                .setIndexingType(StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                                .build())
                .build();
        mDb1.setSchemaAsync(new SetSchemaRequest.Builder().addSchemas(schema).build()).get();

        // Index documents
        GenericDocument doc1 = new GenericDocument.Builder<>("namespace", "id1", "Type")
                .setPropertyString("body", "termOne termTwo termThree termFour")
                .build();
        GenericDocument doc2 = new GenericDocument.Builder<>("namespace", "id2", "Type")
                .setPropertyString("body", "termOne termTwo termThree")
                .build();
        GenericDocument doc3 = new GenericDocument.Builder<>("namespace", "id3", "Type")
                .setPropertyString("body", "termOne termTwo")
                .build();
        GenericDocument doc4 = new GenericDocument.Builder<>("namespace", "id4", "Type")
                .setPropertyString("body", "termOne")
                .build();

        checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder().addGenericDocuments(doc1, doc2, doc3, doc4)
                        .build()));

        SearchSuggestionResult resultOne =
                new SearchSuggestionResult.Builder().setSuggestedResult("termone").build();
        SearchSuggestionResult resultTwo =
                new SearchSuggestionResult.Builder().setSuggestedResult("termtwo").build();
        SearchSuggestionResult resultThree =
                new SearchSuggestionResult.Builder().setSuggestedResult("termthree").build();
        SearchSuggestionResult resultFour =
                new SearchSuggestionResult.Builder().setSuggestedResult("termfour").build();

        List<SearchSuggestionResult> suggestions = mDb1.searchSuggestionAsync(
                /*suggestionQueryExpression=*/"t",
                new SearchSuggestionSpec.Builder(/*totalResultCount=*/10).build()).get();
        assertThat(suggestions).containsExactly(resultOne, resultTwo, resultThree, resultFour)
                .inOrder();

        // Query first 2 suggestions, and they will be ranked.
        suggestions = mDb1.searchSuggestionAsync(
                /*suggestionQueryExpression=*/"t",
                new SearchSuggestionSpec.Builder(/*totalResultCount=*/2).build()).get();
        assertThat(suggestions).containsExactly(resultOne, resultTwo).inOrder();
    }

    @Test
    public void testSearchSuggestion_namespaceFilter() throws Exception {
        // Schema registration
        AppSearchSchema schema = new AppSearchSchema.Builder("Type").addProperty(
                        new StringPropertyConfig.Builder("body")
                                .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                                .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                                .setIndexingType(StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                                .build())
                .build();
        mDb1.setSchemaAsync(new SetSchemaRequest.Builder().addSchemas(schema).build()).get();

        // Index documents
        GenericDocument doc1 = new GenericDocument.Builder<>("namespace1", "id1", "Type")
                .setPropertyString("body", "fo foo")
                .build();
        GenericDocument doc2 = new GenericDocument.Builder<>("namespace2", "id2", "Type")
                .setPropertyString("body", "foo")
                .build();
        GenericDocument doc3 = new GenericDocument.Builder<>("namespace3", "id3", "Type")
                .setPropertyString("body", "fool")
                .build();

        checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder().addGenericDocuments(doc1, doc2, doc3).build()));

        SearchSuggestionResult resultFo =
                new SearchSuggestionResult.Builder().setSuggestedResult("fo").build();
        SearchSuggestionResult resultFoo =
                new SearchSuggestionResult.Builder().setSuggestedResult("foo").build();
        SearchSuggestionResult resultFool =
                new SearchSuggestionResult.Builder().setSuggestedResult("fool").build();

        // namespace1 has 2 results.
        List<SearchSuggestionResult> suggestions = mDb1.searchSuggestionAsync(
                /*suggestionQueryExpression=*/"f",
                new SearchSuggestionSpec.Builder(/*totalResultCount=*/10)
                        .addFilterNamespaces("namespace1").build()).get();
        assertThat(suggestions).containsExactly(resultFoo, resultFo).inOrder();

        // namespace2 has 1 result.
        suggestions = mDb1.searchSuggestionAsync(
                /*suggestionQueryExpression=*/"f",
                new SearchSuggestionSpec.Builder(/*totalResultCount=*/10)
                        .addFilterNamespaces("namespace2").build()).get();
        assertThat(suggestions).containsExactly(resultFoo).inOrder();

        // namespace2 and 3 has 2 results.
        suggestions = mDb1.searchSuggestionAsync(
                /*suggestionQueryExpression=*/"f",
                new SearchSuggestionSpec.Builder(/*totalResultCount=*/10)
                        .addFilterNamespaces("namespace2", "namespace3")
                        .build()).get();
        assertThat(suggestions).containsExactly(resultFoo, resultFool);

        // non exist namespace has empty result
        suggestions = mDb1.searchSuggestionAsync(
                /*suggestionQueryExpression=*/"f",
                new SearchSuggestionSpec.Builder(/*totalResultCount=*/10)
                        .addFilterNamespaces("nonExistNamespace").build()).get();
        assertThat(suggestions).isEmpty();
    }

    @Test
    public void testSearchSuggestion_schemaFilter() throws Exception {
        // Schema registration
        AppSearchSchema schemaType1 = new AppSearchSchema.Builder("Type1").addProperty(
                        new StringPropertyConfig.Builder("body")
                                .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                                .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                                .setIndexingType(StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                                .build())
                .build();
        AppSearchSchema schemaType2 = new AppSearchSchema.Builder("Type2").addProperty(
                        new StringPropertyConfig.Builder("body")
                                .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                                .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                                .setIndexingType(StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                                .build())
                .build();
        AppSearchSchema schemaType3 = new AppSearchSchema.Builder("Type3").addProperty(
                        new StringPropertyConfig.Builder("body")
                                .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                                .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                                .setIndexingType(StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                                .build())
                .build();
        mDb1.setSchemaAsync(new SetSchemaRequest.Builder()
                .addSchemas(schemaType1, schemaType2, schemaType3).build()).get();

        // Index documents
        GenericDocument doc1 = new GenericDocument.Builder<>("namespace", "id1", "Type1")
                .setPropertyString("body", "fo foo")
                .build();
        GenericDocument doc2 = new GenericDocument.Builder<>("namespace", "id2", "Type2")
                .setPropertyString("body", "foo")
                .build();
        GenericDocument doc3 = new GenericDocument.Builder<>("namespace", "id3", "Type3")
                .setPropertyString("body", "fool")
                .build();

        checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder().addGenericDocuments(doc1, doc2, doc3).build()));

        SearchSuggestionResult resultFo =
                new SearchSuggestionResult.Builder().setSuggestedResult("fo").build();
        SearchSuggestionResult resultFoo =
                new SearchSuggestionResult.Builder().setSuggestedResult("foo").build();
        SearchSuggestionResult resultFool =
                new SearchSuggestionResult.Builder().setSuggestedResult("fool").build();

        // Type1 has 2 results.
        List<SearchSuggestionResult> suggestions = mDb1.searchSuggestionAsync(
                /*suggestionQueryExpression=*/"f",
                new SearchSuggestionSpec.Builder(/*totalResultCount=*/10)
                        .addFilterSchemas("Type1").build()).get();
        assertThat(suggestions).containsExactly(resultFoo, resultFo).inOrder();

        // Type2 has 1 result.
        suggestions = mDb1.searchSuggestionAsync(
                /*suggestionQueryExpression=*/"f",
                new SearchSuggestionSpec.Builder(/*totalResultCount=*/10)
                        .addFilterSchemas("Type2").build()).get();
        assertThat(suggestions).containsExactly(resultFoo).inOrder();

        // Type2 and 3 has 2 results.
        suggestions = mDb1.searchSuggestionAsync(
                /*suggestionQueryExpression=*/"f",
                new SearchSuggestionSpec.Builder(/*totalResultCount=*/10)
                        .addFilterSchemas("Type2", "Type3")
                        .build()).get();
        assertThat(suggestions).containsExactly(resultFoo, resultFool);

        // non exist type has empty result.
        suggestions = mDb1.searchSuggestionAsync(
                /*suggestionQueryExpression=*/"f",
                new SearchSuggestionSpec.Builder(/*totalResultCount=*/10)
                        .addFilterSchemas("nonExistType").build()).get();
        assertThat(suggestions).isEmpty();
    }

    @Test
    public void testSearchSuggestion_propertyFilter() throws Exception {
        // Schema registration
        AppSearchSchema schemaType1 = new AppSearchSchema.Builder("Type1").addProperty(
                        new StringPropertyConfig.Builder("propertyone")
                                .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                                .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                                .setIndexingType(StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                                .build()).addProperty(
                        new StringPropertyConfig.Builder("propertytwo")
                                .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                                .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                                .setIndexingType(StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                                .build())
                .build();
        AppSearchSchema schemaType2 = new AppSearchSchema.Builder("Type2").addProperty(
                        new StringPropertyConfig.Builder("propertythree")
                                .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                                .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                                .setIndexingType(StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                                .build()).addProperty(
                        new StringPropertyConfig.Builder("propertyfour")
                                .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                                .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                                .setIndexingType(StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                                .build())
                .build();
        mDb1.setSchemaAsync(new SetSchemaRequest.Builder()
                .addSchemas(schemaType1, schemaType2).build()).get();

        // Index documents
        GenericDocument doc1 = new GenericDocument.Builder<>("namespace", "id1", "Type1")
                .setPropertyString("propertyone", "termone")
                .setPropertyString("propertytwo", "termtwo")
                .build();
        GenericDocument doc2 = new GenericDocument.Builder<>("namespace", "id2", "Type2")
                .setPropertyString("propertythree", "termthree")
                .setPropertyString("propertyfour", "termfour")
                .build();

        checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder().addGenericDocuments(doc1, doc2).build()));

        SearchSuggestionResult resultOne =
                new SearchSuggestionResult.Builder().setSuggestedResult("termone").build();
        SearchSuggestionResult resultTwo =
                new SearchSuggestionResult.Builder().setSuggestedResult("termtwo").build();
        SearchSuggestionResult resultThree =
                new SearchSuggestionResult.Builder().setSuggestedResult("termthree").build();
        SearchSuggestionResult resultFour =
                new SearchSuggestionResult.Builder().setSuggestedResult("termfour").build();

        // Only search for type1/propertyone
        List<SearchSuggestionResult> suggestions = mDb1.searchSuggestionAsync(
                /*suggestionQueryExpression=*/"t",
                new SearchSuggestionSpec.Builder(/*totalResultCount=*/10)
                        .addFilterSchemas("Type1")
                        .addFilterProperties("Type1", ImmutableList.of("propertyone"))
                        .build()).get();
        assertThat(suggestions).containsExactly(resultOne);

        // Only search for type1/propertyone and type1/propertytwo
        suggestions = mDb1.searchSuggestionAsync(
                /*suggestionQueryExpression=*/"t",
                new SearchSuggestionSpec.Builder(/*totalResultCount=*/10)
                        .addFilterSchemas("Type1")
                        .addFilterProperties("Type1",
                                ImmutableList.of("propertyone", "propertytwo"))
                        .build()).get();
        assertThat(suggestions).containsExactly(resultOne, resultTwo);

        // Only search for type1/propertyone and type2/propertythree
        suggestions = mDb1.searchSuggestionAsync(
                /*suggestionQueryExpression=*/"t",
                new SearchSuggestionSpec.Builder(/*totalResultCount=*/10)
                        .addFilterSchemas("Type1", "Type2")
                        .addFilterProperties("Type1", ImmutableList.of("propertyone"))
                        .addFilterProperties("Type2", ImmutableList.of("propertythree"))
                        .build()).get();
        assertThat(suggestions).containsExactly(resultOne, resultThree);

        // Only search for type1/propertyone and type2/propertyfour, in addFilterPropertyPaths
        suggestions = mDb1.searchSuggestionAsync(
                /*suggestionQueryExpression=*/"t",
                new SearchSuggestionSpec.Builder(/*totalResultCount=*/10)
                        .addFilterSchemas("Type1", "Type2")
                        .addFilterProperties("Type1", ImmutableList.of("propertyone"))
                        .addFilterPropertyPaths("Type2",
                                ImmutableList.of(new PropertyPath("propertyfour")))
                        .build()).get();
        assertThat(suggestions).containsExactly(resultOne, resultFour);

        // Only search for type1/propertyone and everything in type2
        suggestions = mDb1.searchSuggestionAsync(
                /*suggestionQueryExpression=*/"t",
                new SearchSuggestionSpec.Builder(/*totalResultCount=*/10)
                        .addFilterProperties("Type1", ImmutableList.of("propertyone"))
                        .build()).get();
        assertThat(suggestions).containsExactly(resultOne, resultThree, resultFour);
    }

    @Test
    public void testSearchSuggestion_differentPrefix() throws Exception {
        // Schema registration
        AppSearchSchema schema = new AppSearchSchema.Builder("Type").addProperty(
                        new StringPropertyConfig.Builder("body")
                                .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                                .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                                .setIndexingType(StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                                .build())
                .build();
        mDb1.setSchemaAsync(new SetSchemaRequest.Builder().addSchemas(schema).build()).get();

        // Index documents
        GenericDocument doc1 = new GenericDocument.Builder<>("namespace", "id1", "Type")
                .setPropertyString("body", "foo")
                .build();
        GenericDocument doc2 = new GenericDocument.Builder<>("namespace", "id2", "Type")
                .setPropertyString("body", "fool")
                .build();
        GenericDocument doc3 = new GenericDocument.Builder<>("namespace", "id3", "Type")
                .setPropertyString("body", "bar")
                .build();
        GenericDocument doc4 = new GenericDocument.Builder<>("namespace", "id4", "Type")
                .setPropertyString("body", "baz")
                .build();

        checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder().addGenericDocuments(doc1, doc2, doc3, doc4)
                        .build()));

        SearchSuggestionResult resultFoo =
                new SearchSuggestionResult.Builder().setSuggestedResult("foo").build();
        SearchSuggestionResult resultFool =
                new SearchSuggestionResult.Builder().setSuggestedResult("fool").build();
        SearchSuggestionResult resultBar =
                new SearchSuggestionResult.Builder().setSuggestedResult("bar").build();
        SearchSuggestionResult resultBaz =
                new SearchSuggestionResult.Builder().setSuggestedResult("baz").build();

        // prefix f has 2 results.
        List<SearchSuggestionResult> suggestions = mDb1.searchSuggestionAsync(
                /*suggestionQueryExpression=*/"f",
                new SearchSuggestionSpec.Builder(/*totalResultCount=*/10).build()).get();
        assertThat(suggestions).containsExactly(resultFoo, resultFool);

        // prefix b has 2 results.
        suggestions = mDb1.searchSuggestionAsync(
                /*suggestionQueryExpression=*/"b",
                new SearchSuggestionSpec.Builder(/*totalResultCount=*/10).build()).get();
        assertThat(suggestions).containsExactly(resultBar, resultBaz);
    }

    @Test
    public void testSearchSuggestion_differentRankingStrategy() throws Exception {
        // Schema registration
        AppSearchSchema schema = new AppSearchSchema.Builder("Type").addProperty(
                        new StringPropertyConfig.Builder("body")
                                .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                                .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                                .setIndexingType(StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                                .build())
                .build();
        mDb1.setSchemaAsync(new SetSchemaRequest.Builder().addSchemas(schema).build()).get();

        // Index documents
        // term1 appears 3 times in all 3 docs.
        // term2 appears 4 times in 2 docs.
        // term3 appears 5 times in 1 doc.
        GenericDocument doc1 = new GenericDocument.Builder<>("namespace", "id1", "Type")
                .setPropertyString("body", "term1 term3 term3 term3 term3 term3")
                .build();
        GenericDocument doc2 = new GenericDocument.Builder<>("namespace", "id2", "Type")
                .setPropertyString("body", "term1 term2 term2 term2")
                .build();
        GenericDocument doc3 = new GenericDocument.Builder<>("namespace", "id3", "Type")
                .setPropertyString("body", "term1 term2")
                .build();

        checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder().addGenericDocuments(doc1, doc2, doc3)
                        .build()));

        SearchSuggestionResult result1 =
                new SearchSuggestionResult.Builder().setSuggestedResult("term1").build();
        SearchSuggestionResult result2 =
                new SearchSuggestionResult.Builder().setSuggestedResult("term2").build();
        SearchSuggestionResult result3 =
                new SearchSuggestionResult.Builder().setSuggestedResult("term3").build();


        // rank by NONE, the order should be arbitrary but all terms appear.
        List<SearchSuggestionResult> suggestions = mDb1.searchSuggestionAsync(
                /*suggestionQueryExpression=*/"t",
                new SearchSuggestionSpec.Builder(/*totalResultCount=*/10)
                        .setRankingStrategy(SearchSuggestionSpec
                                .SUGGESTION_RANKING_STRATEGY_NONE)
                        .build()).get();
        assertThat(suggestions).containsExactly(result2, result1, result3);

        // rank by document count, the order should be term1:3 > term2:2 > term3:1
        suggestions = mDb1.searchSuggestionAsync(
                /*suggestionQueryExpression=*/"t",
                new SearchSuggestionSpec.Builder(/*totalResultCount=*/10)
                        .setRankingStrategy(SearchSuggestionSpec
                                .SUGGESTION_RANKING_STRATEGY_DOCUMENT_COUNT)
                        .build()).get();
        assertThat(suggestions).containsExactly(result1, result2, result3).inOrder();

        // rank by term frequency, the order should be term3:5 > term2:4 > term1:3
        suggestions = mDb1.searchSuggestionAsync(
                /*suggestionQueryExpression=*/"t",
                new SearchSuggestionSpec.Builder(/*totalResultCount=*/10)
                        .setRankingStrategy(SearchSuggestionSpec
                                .SUGGESTION_RANKING_STRATEGY_TERM_FREQUENCY)
                        .build()).get();
        assertThat(suggestions).containsExactly(result3, result2, result1).inOrder();
    }

    @Test
    public void testSearchSuggestion_removeDocument() throws Exception {
        // Schema registration
        AppSearchSchema schema = new AppSearchSchema.Builder("Type").addProperty(
                        new StringPropertyConfig.Builder("body")
                                .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                                .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                                .setIndexingType(StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                                .build())
                .build();
        mDb1.setSchemaAsync(new SetSchemaRequest.Builder().addSchemas(schema).build()).get();

        // Index documents
        GenericDocument docTwo = new GenericDocument.Builder<>("namespace", "idTwo", "Type")
                .setPropertyString("body", "two")
                .build();
        GenericDocument docThree = new GenericDocument.Builder<>("namespace", "idThree", "Type")
                .setPropertyString("body", "three")
                .build();
        GenericDocument docTart = new GenericDocument.Builder<>("namespace", "idTart", "Type")
                .setPropertyString("body", "tart")
                .build();

        checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder().addGenericDocuments(docTwo, docThree, docTart)
                        .build()));

        SearchSuggestionResult resultTwo =
                new SearchSuggestionResult.Builder().setSuggestedResult("two").build();
        SearchSuggestionResult resultThree =
                new SearchSuggestionResult.Builder().setSuggestedResult("three").build();
        SearchSuggestionResult resultTart =
                new SearchSuggestionResult.Builder().setSuggestedResult("tart").build();

        // prefix t has 3 results.
        List<SearchSuggestionResult> suggestions = mDb1.searchSuggestionAsync(
                /*suggestionQueryExpression=*/"t",
                new SearchSuggestionSpec.Builder(/*totalResultCount=*/10).build()).get();
        assertThat(suggestions).containsExactly(resultTwo, resultThree, resultTart);

        // Delete the document
        checkIsBatchResultSuccess(mDb1.removeAsync(
                new RemoveByDocumentIdRequest.Builder("namespace").addIds(
                        "idTwo").build()));

        // now prefix t has 2 results.
        suggestions = mDb1.searchSuggestionAsync(
                /*suggestionQueryExpression=*/"t",
                new SearchSuggestionSpec.Builder(/*totalResultCount=*/10).build()).get();
        assertThat(suggestions).containsExactly(resultThree, resultTart);
    }

    @Test
    public void testSearchSuggestion_replacementDocument() throws Exception {
        // Schema registration
        AppSearchSchema schema = new AppSearchSchema.Builder("Type").addProperty(
                        new StringPropertyConfig.Builder("body")
                                .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                                .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                                .setIndexingType(StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                                .build())
                .build();
        mDb1.setSchemaAsync(new SetSchemaRequest.Builder().addSchemas(schema).build()).get();

        // Index documents
        GenericDocument doc = new GenericDocument.Builder<>("namespace", "id", "Type")
                .setPropertyString("body", "two three tart")
                .build();

        checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder().addGenericDocuments(doc)
                        .build()));

        SearchSuggestionResult resultTwo =
                new SearchSuggestionResult.Builder().setSuggestedResult("two").build();
        SearchSuggestionResult resultThree =
                new SearchSuggestionResult.Builder().setSuggestedResult("three").build();
        SearchSuggestionResult resultTart =
                new SearchSuggestionResult.Builder().setSuggestedResult("tart").build();
        SearchSuggestionResult resultTwist =
                new SearchSuggestionResult.Builder().setSuggestedResult("twist").build();

        // prefix t has 3 results.
        List<SearchSuggestionResult> suggestions = mDb1.searchSuggestionAsync(
                /*suggestionQueryExpression=*/"t",
                new SearchSuggestionSpec.Builder(/*totalResultCount=*/10).build()).get();
        assertThat(suggestions).containsExactly(resultTwo, resultThree, resultTart);

        // replace the document
        GenericDocument replaceDoc = new GenericDocument.Builder<>("namespace", "id", "Type")
                .setPropertyString("body", "twist three")
                .build();
        checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder().addGenericDocuments(replaceDoc)
                        .build()));

        // prefix t has 2 results for now.
        suggestions = mDb1.searchSuggestionAsync(
                /*suggestionQueryExpression=*/"t",
                new SearchSuggestionSpec.Builder(/*totalResultCount=*/10).build()).get();
        assertThat(suggestions).containsExactly(resultThree, resultTwist);
    }

    @Test
    public void testSearchSuggestion_ignoreOperators() throws Exception {
        // Schema registration
        AppSearchSchema schema = new AppSearchSchema.Builder("Type").addProperty(
                        new StringPropertyConfig.Builder("body")
                                .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                                .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                                .setIndexingType(StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                                .build())
                .build();
        mDb1.setSchemaAsync(new SetSchemaRequest.Builder().addSchemas(schema).build()).get();

        // Index documents
        GenericDocument doc = new GenericDocument.Builder<>("namespace", "id", "Type")
                .setPropertyString("body", "two original")
                .build();

        checkIsBatchResultSuccess(mDb1.putAsync(
                new PutDocumentsRequest.Builder().addGenericDocuments(doc)
                        .build()));

        SearchSuggestionResult resultTwoOriginal =
                new SearchSuggestionResult.Builder().setSuggestedResult("two original").build();

        List<SearchSuggestionResult> suggestions = mDb1.searchSuggestionAsync(
                /*suggestionQueryExpression=*/"two OR",
                new SearchSuggestionSpec.Builder(/*totalResultCount=*/10).build()).get();
        assertThat(suggestions).containsExactly(resultTwoOriginal);
    }
}
