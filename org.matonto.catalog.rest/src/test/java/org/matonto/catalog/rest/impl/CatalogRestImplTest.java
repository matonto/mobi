package org.matonto.catalog.rest.impl;

/*-
 * #%L
 * org.matonto.catalog.rest
 * $Id:$
 * $HeadURL:$
 * %%
 * Copyright (C) 2016 iNovex Information Systems, Inc.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * #L%
 */

import static org.matonto.rest.util.RestUtils.encode;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.matonto.catalog.api.CatalogManager;
import org.matonto.catalog.api.Conflict;
import org.matonto.catalog.api.Difference;
import org.matonto.catalog.api.PaginatedSearchParams;
import org.matonto.catalog.api.PaginatedSearchResults;
import org.matonto.catalog.api.builder.DistributionConfig;
import org.matonto.catalog.api.builder.RecordConfig;
import org.matonto.catalog.api.ontologies.mcat.Branch;
import org.matonto.catalog.api.ontologies.mcat.BranchFactory;
import org.matonto.catalog.api.ontologies.mcat.Catalog;
import org.matonto.catalog.api.ontologies.mcat.CatalogFactory;
import org.matonto.catalog.api.ontologies.mcat.Commit;
import org.matonto.catalog.api.ontologies.mcat.CommitFactory;
import org.matonto.catalog.api.ontologies.mcat.Distribution;
import org.matonto.catalog.api.ontologies.mcat.DistributionFactory;
import org.matonto.catalog.api.ontologies.mcat.InProgressCommit;
import org.matonto.catalog.api.ontologies.mcat.InProgressCommitFactory;
import org.matonto.catalog.api.ontologies.mcat.MappingRecord;
import org.matonto.catalog.api.ontologies.mcat.MappingRecordFactory;
import org.matonto.catalog.api.ontologies.mcat.OntologyRecord;
import org.matonto.catalog.api.ontologies.mcat.OntologyRecordFactory;
import org.matonto.catalog.api.ontologies.mcat.Record;
import org.matonto.catalog.api.ontologies.mcat.RecordFactory;
import org.matonto.catalog.api.ontologies.mcat.Tag;
import org.matonto.catalog.api.ontologies.mcat.TagFactory;
import org.matonto.catalog.api.ontologies.mcat.UnversionedRecord;
import org.matonto.catalog.api.ontologies.mcat.UnversionedRecordFactory;
import org.matonto.catalog.api.ontologies.mcat.UserBranch;
import org.matonto.catalog.api.ontologies.mcat.UserBranchFactory;
import org.matonto.catalog.api.ontologies.mcat.Version;
import org.matonto.catalog.api.ontologies.mcat.VersionFactory;
import org.matonto.catalog.api.ontologies.mcat.VersionedRDFRecord;
import org.matonto.catalog.api.ontologies.mcat.VersionedRDFRecordFactory;
import org.matonto.catalog.api.ontologies.mcat.VersionedRecord;
import org.matonto.catalog.api.ontologies.mcat.VersionedRecordFactory;
import org.matonto.exception.MatOntoException;
import org.matonto.jaas.api.engines.EngineManager;
import org.matonto.jaas.api.ontologies.usermanagement.User;
import org.matonto.jaas.api.ontologies.usermanagement.UserFactory;
import org.matonto.ontology.utils.api.SesameTransformer;
import org.matonto.rdf.api.Model;
import org.matonto.rdf.api.ModelFactory;
import org.matonto.rdf.api.Resource;
import org.matonto.rdf.api.ValueFactory;
import org.matonto.rdf.core.impl.sesame.LinkedHashModelFactory;
import org.matonto.rdf.core.impl.sesame.SimpleValueFactory;
import org.matonto.rdf.core.utils.Values;
import org.matonto.rdf.orm.OrmFactory;
import org.matonto.rdf.orm.Thing;
import org.matonto.rdf.orm.conversion.ValueConverterRegistry;
import org.matonto.rdf.orm.conversion.impl.DefaultValueConverterRegistry;
import org.matonto.rdf.orm.conversion.impl.DoubleValueConverter;
import org.matonto.rdf.orm.conversion.impl.FloatValueConverter;
import org.matonto.rdf.orm.conversion.impl.IRIValueConverter;
import org.matonto.rdf.orm.conversion.impl.IntegerValueConverter;
import org.matonto.rdf.orm.conversion.impl.LiteralValueConverter;
import org.matonto.rdf.orm.conversion.impl.ResourceValueConverter;
import org.matonto.rdf.orm.conversion.impl.ShortValueConverter;
import org.matonto.rdf.orm.conversion.impl.StringValueConverter;
import org.matonto.rdf.orm.conversion.impl.ValueValueConverter;
import org.matonto.rest.util.MatontoRestTestNg;
import org.matonto.rest.util.UsernameTestFilter;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.openrdf.model.vocabulary.DCTERMS;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

public class CatalogRestImplTest extends MatontoRestTestNg {
    private CatalogRestImpl rest;
    private CatalogFactory catalogFactory;
    private RecordFactory recordFactory;
    private UnversionedRecordFactory unversionedRecordFactory;
    private VersionedRecordFactory versionedRecordFactory;
    private VersionedRDFRecordFactory versionedRDFRecordFactory;
    private OntologyRecordFactory ontologyRecordFactory;
    private MappingRecordFactory mappingRecordFactory;
    private DistributionFactory distributionFactory;
    private VersionFactory versionFactory;
    private TagFactory tagFactory;
    private CommitFactory commitFactory;
    private InProgressCommitFactory inProgressCommitFactory;
    private BranchFactory branchFactory;
    private UserBranchFactory userBranchFactory;
    private UserFactory userFactory;
    private ValueFactory vf;
    private ModelFactory mf;
    private ValueConverterRegistry vcr;
    private Catalog localCatalog;
    private Catalog distributedCatalog;
    private Record testRecord;
    private UnversionedRecord testUnversionedRecord;
    private VersionedRecord testVersionedRecord;
    private VersionedRDFRecord testVersionedRDFRecord;
    private OntologyRecord testOntologyRecord;
    private MappingRecord testMappingRecord;
    private Distribution testDistribution;
    private Version testVersion;
    private Tag testTag;
    private List<Commit> testCommits;
    private InProgressCommit testInProgressCommit;
    private Branch testBranch;
    private UserBranch testUserBranch;
    private User user;
    private Model compiledResource;
    private Model compiledResourceWithChanges;
    private static final String ERROR_IRI = "http://matonto.org/error";
    private static final String LOCAL_IRI = "http://matonto.org/catalogs/local";
    private static final String DISTRIBUTED_IRI = "http://matonto.org/catalogs/distributed";
    private static final String RECORD_IRI = "http://matonto.org/records/test";
    private static final String DISTRIBUTION_IRI = "http://matonto.org/distributions/test";
    private static final String VERSION_IRI = "http://matonto.org/versions/test";
    private static final String[] COMMIT_IRIS = new String[] {
            "http://matonto.org/commits/0",
            "http://matonto.org/commits/1",
            "http://matonto.org/commits/2"
    };
    private static final String BRANCH_IRI = "http://matonto.org/branches/test";
    private static final String USER_IRI = "http://matonto.org/users/tester";
    private static final String CONFLICT_IRI = "http://matonto.org/conflicts/test";

    @Mock
    private CatalogManager catalogManager;

    @Mock
    private EngineManager engineManager;

    @Mock
    private SesameTransformer transformer;

    @Mock
    private PaginatedSearchResults<Record> results;

    @Mock
    private Conflict conflict;

    @Mock
    private Difference difference;

    @Override
    protected Application configureApp() throws Exception {
        vf = SimpleValueFactory.getInstance();
        mf = LinkedHashModelFactory.getInstance();
        vcr = new DefaultValueConverterRegistry();
        catalogFactory = new CatalogFactory();
        recordFactory = new RecordFactory();
        unversionedRecordFactory = new UnversionedRecordFactory();
        versionedRecordFactory = new VersionedRecordFactory();
        versionedRDFRecordFactory = new VersionedRDFRecordFactory();
        ontologyRecordFactory = new OntologyRecordFactory();
        mappingRecordFactory = new MappingRecordFactory();
        distributionFactory = new DistributionFactory();
        versionFactory = new VersionFactory();
        tagFactory = new TagFactory();
        commitFactory = new CommitFactory();
        inProgressCommitFactory = new InProgressCommitFactory();
        branchFactory = new BranchFactory();
        userBranchFactory = new UserBranchFactory();
        userFactory = new UserFactory();
        catalogFactory.setModelFactory(mf);
        catalogFactory.setValueFactory(vf);
        catalogFactory.setValueConverterRegistry(vcr);
        recordFactory.setModelFactory(mf);
        recordFactory.setValueFactory(vf);
        recordFactory.setValueConverterRegistry(vcr);
        unversionedRecordFactory.setModelFactory(mf);
        unversionedRecordFactory.setValueFactory(vf);
        unversionedRecordFactory.setValueConverterRegistry(vcr);
        versionedRecordFactory.setModelFactory(mf);
        versionedRecordFactory.setValueFactory(vf);
        versionedRecordFactory.setValueConverterRegistry(vcr);
        versionedRDFRecordFactory.setModelFactory(mf);
        versionedRDFRecordFactory.setValueFactory(vf);
        versionedRDFRecordFactory.setValueConverterRegistry(vcr);
        ontologyRecordFactory.setModelFactory(mf);
        ontologyRecordFactory.setValueFactory(vf);
        ontologyRecordFactory.setValueConverterRegistry(vcr);
        mappingRecordFactory.setModelFactory(mf);
        mappingRecordFactory.setValueFactory(vf);
        mappingRecordFactory.setValueConverterRegistry(vcr);
        distributionFactory.setModelFactory(mf);
        distributionFactory.setValueFactory(vf);
        distributionFactory.setValueConverterRegistry(vcr);
        versionFactory.setModelFactory(mf);
        versionFactory.setValueFactory(vf);
        versionFactory.setValueConverterRegistry(vcr);
        tagFactory.setModelFactory(mf);
        tagFactory.setValueFactory(vf);
        tagFactory.setValueConverterRegistry(vcr);
        commitFactory.setModelFactory(mf);
        commitFactory.setValueFactory(vf);
        commitFactory.setValueConverterRegistry(vcr);
        inProgressCommitFactory.setModelFactory(mf);
        inProgressCommitFactory.setValueFactory(vf);
        inProgressCommitFactory.setValueConverterRegistry(vcr);
        branchFactory.setModelFactory(mf);
        branchFactory.setValueFactory(vf);
        branchFactory.setValueConverterRegistry(vcr);
        userBranchFactory.setModelFactory(mf);
        userBranchFactory.setValueFactory(vf);
        userBranchFactory.setValueConverterRegistry(vcr);
        userFactory.setModelFactory(mf);
        userFactory.setValueFactory(vf);
        userFactory.setValueConverterRegistry(vcr);

        vcr.registerValueConverter(catalogFactory);
        vcr.registerValueConverter(recordFactory);
        vcr.registerValueConverter(unversionedRecordFactory);
        vcr.registerValueConverter(versionedRecordFactory);
        vcr.registerValueConverter(versionedRDFRecordFactory);
        vcr.registerValueConverter(ontologyRecordFactory);
        vcr.registerValueConverter(mappingRecordFactory);
        vcr.registerValueConverter(distributionFactory);
        vcr.registerValueConverter(versionFactory);
        vcr.registerValueConverter(tagFactory);
        vcr.registerValueConverter(commitFactory);
        vcr.registerValueConverter(inProgressCommitFactory);
        vcr.registerValueConverter(branchFactory);
        vcr.registerValueConverter(userFactory);
        vcr.registerValueConverter(userBranchFactory);
        vcr.registerValueConverter(new ResourceValueConverter());
        vcr.registerValueConverter(new IRIValueConverter());
        vcr.registerValueConverter(new DoubleValueConverter());
        vcr.registerValueConverter(new IntegerValueConverter());
        vcr.registerValueConverter(new FloatValueConverter());
        vcr.registerValueConverter(new ShortValueConverter());
        vcr.registerValueConverter(new StringValueConverter());
        vcr.registerValueConverter(new ValueValueConverter());
        vcr.registerValueConverter(new LiteralValueConverter());

        localCatalog = catalogFactory.createNew(vf.createIRI(LOCAL_IRI));
        distributedCatalog = catalogFactory.createNew(vf.createIRI(DISTRIBUTED_IRI));
        testCommits = Arrays.stream(COMMIT_IRIS)
                .map(s -> commitFactory.createNew(vf.createIRI(s)))
                .collect(Collectors.toList());
        testInProgressCommit = inProgressCommitFactory.createNew(vf.createIRI(COMMIT_IRIS[0]));
        testBranch = branchFactory.createNew(vf.createIRI(BRANCH_IRI));
        testBranch.setProperty(vf.createLiteral("Title"), vf.createIRI(DCTERMS.TITLE.stringValue()));
        testBranch.setProperty(vf.createLiteral(USER_IRI), vf.createIRI(DCTERMS.PUBLISHER.stringValue()));
        testBranch.setHead(testCommits.get(0));
        testUserBranch = userBranchFactory.createNew(vf.createIRI(BRANCH_IRI + "/user"));
        testUserBranch.setProperty(vf.createLiteral("Title"), vf.createIRI(DCTERMS.TITLE.stringValue()));
        testUserBranch.setProperty(vf.createLiteral(USER_IRI), vf.createIRI(DCTERMS.PUBLISHER.stringValue()));
        testDistribution = distributionFactory.createNew(vf.createIRI(DISTRIBUTION_IRI));
        testDistribution.setProperty(vf.createLiteral("Title"), vf.createIRI(DCTERMS.TITLE.stringValue()));
        testVersion = versionFactory.createNew(vf.createIRI(VERSION_IRI));
        testVersion.setProperty(vf.createLiteral("Title"), vf.createIRI(DCTERMS.TITLE.stringValue()));
        testVersion.setVersionedDistribution(Collections.singleton(testDistribution));
        testTag = tagFactory.createNew(vf.createIRI(VERSION_IRI));
        testTag.setCommit(testCommits.get(0));
        testRecord = recordFactory.createNew(vf.createIRI(RECORD_IRI));
        testRecord.setProperty(vf.createLiteral("Title"), vf.createIRI(DCTERMS.TITLE.stringValue()));
        testUnversionedRecord = unversionedRecordFactory.createNew(vf.createIRI(RECORD_IRI));
        testUnversionedRecord.setUnversionedDistribution(Collections.singleton(testDistribution));
        testVersionedRecord = versionedRecordFactory.createNew(vf.createIRI(RECORD_IRI));
        testVersionedRecord.setLatestVersion(testVersion);
        testVersionedRecord.setVersion(Collections.singleton(testVersion));
        testVersionedRDFRecord = versionedRDFRecordFactory.createNew(vf.createIRI(RECORD_IRI));
        testVersionedRDFRecord.setMasterBranch(testBranch);
        testVersionedRDFRecord.setBranch(Stream.of(testBranch, testUserBranch).collect(Collectors.toSet()));
        testOntologyRecord = ontologyRecordFactory.createNew(vf.createIRI(RECORD_IRI));
        testMappingRecord = mappingRecordFactory.createNew(vf.createIRI(RECORD_IRI));
        user = userFactory.createNew(vf.createIRI(USER_IRI));
        compiledResource = mf.createModel();
        compiledResourceWithChanges = mf.createModel(compiledResource);
        compiledResourceWithChanges.add(vf.createIRI("http://example.com"), vf.createIRI(DCTERMS.TITLE.stringValue()),
                vf.createLiteral("Title"));

        MockitoAnnotations.initMocks(this);
        rest = new CatalogRestImpl();
        rest.setVf(vf);
        rest.setEngineManager(engineManager);
        rest.setTransformer(transformer);
        rest.setCatalogManager(catalogManager);
        rest.setDistributionFactory(distributionFactory);
        rest.setCommitFactory(commitFactory);
        rest.setInProgressCommitFactory(inProgressCommitFactory);
        rest.addBranchFactory(branchFactory);
        rest.addBranchFactory(userBranchFactory);
        rest.addVersionFactory(versionFactory);
        rest.addVersionFactory(tagFactory);
        rest.addRecordFactory(recordFactory);
        rest.addRecordFactory(unversionedRecordFactory);
        rest.addRecordFactory(versionedRecordFactory);
        rest.addRecordFactory(versionedRDFRecordFactory);
        rest.addRecordFactory(ontologyRecordFactory);
        rest.addRecordFactory(mappingRecordFactory);

        return new ResourceConfig()
                .register(rest)
                .register(UsernameTestFilter.class)
                .register(MultiPartFeature.class);
    }

    @Override
    protected void configureClient(ClientConfig config) {
        config.register(MultiPartFeature.class);
    }

    @BeforeMethod
    public void setupMocks() {
        reset(catalogManager, engineManager, transformer, conflict, difference, results);

        when(transformer.sesameModel(any(Model.class)))
                .thenAnswer(i -> Values.sesameModel(i.getArgumentAt(0, Model.class)));
        when(transformer.matontoModel(any(org.openrdf.model.Model.class)))
                .thenAnswer(i -> Values.matontoModel(i.getArgumentAt(0, org.openrdf.model.Model.class)));

        when(results.getPage()).thenReturn(Collections.singletonList(testRecord));
        when(results.getPageNumber()).thenReturn(0);
        when(results.getPageSize()).thenReturn(10);
        when(results.getTotalSize()).thenReturn(50);

        when(catalogManager.getLocalCatalog()).thenReturn(localCatalog);
        when(catalogManager.getDistributedCatalog()).thenReturn(distributedCatalog);
        when(catalogManager.getLocalCatalogIRI()).thenReturn(vf.createIRI(LOCAL_IRI));
        when(catalogManager.getDistributedCatalogIRI()).thenReturn(vf.createIRI(DISTRIBUTED_IRI));
        when(catalogManager.getRecordIds(any(Resource.class))).thenReturn(Collections.singleton(testRecord.getResource()));
        when(catalogManager.findRecord(any(Resource.class), any(PaginatedSearchParams.class))).thenReturn(results);
        when(catalogManager.getRecord(any(Resource.class), any(Resource.class), eq(recordFactory)))
                .thenReturn(Optional.of(testRecord));
        when(catalogManager.getRecord(any(Resource.class), any(Resource.class), eq(unversionedRecordFactory)))
                .thenReturn(Optional.of(testUnversionedRecord));
        when(catalogManager.getRecord(any(Resource.class), any(Resource.class), eq(versionedRecordFactory)))
                .thenReturn(Optional.of(testVersionedRecord));
        when(catalogManager.getRecord(any(Resource.class), any(Resource.class), eq(versionedRDFRecordFactory)))
                .thenReturn(Optional.of(testVersionedRDFRecord));
        when(catalogManager.getRecord(any(Resource.class), any(Resource.class), eq(ontologyRecordFactory)))
                .thenReturn(Optional.of(testOntologyRecord));
        when(catalogManager.getRecord(any(Resource.class), any(Resource.class), eq(mappingRecordFactory)))
                .thenReturn(Optional.of(testMappingRecord));
        when(catalogManager.createRecord(any(RecordConfig.class), eq(recordFactory))).thenReturn(testRecord);
        when(catalogManager.createRecord(any(RecordConfig.class), eq(unversionedRecordFactory)))
                .thenReturn(testUnversionedRecord);
        when(catalogManager.createRecord(any(RecordConfig.class), eq(versionedRecordFactory)))
                .thenReturn(testVersionedRecord);
        when(catalogManager.createRecord(any(RecordConfig.class), eq(versionedRDFRecordFactory)))
                .thenReturn(testVersionedRDFRecord);
        when(catalogManager.createRecord(any(RecordConfig.class), eq(ontologyRecordFactory)))
                .thenReturn(testOntologyRecord);
        when(catalogManager.createRecord(any(RecordConfig.class), eq(mappingRecordFactory)))
                .thenReturn(testMappingRecord);
        when(catalogManager.getUnversionedDistributions(any(Resource.class), any(Resource.class))).thenReturn(Collections.singleton(testDistribution));
        when(catalogManager.createDistribution(any(DistributionConfig.class))).thenReturn(testDistribution);
        when(catalogManager.getUnversionedDistribution(any(Resource.class), any(Resource.class), any(Resource.class))).thenReturn(Optional.of(testDistribution));
        when(catalogManager.getVersions(any(Resource.class), any(Resource.class))).thenReturn(Collections.singleton(testVersion));
        when(catalogManager.getVersion(any(Resource.class), any(Resource.class), any(Resource.class), eq(versionFactory))).thenReturn(Optional.of(testVersion));
        when(catalogManager.getVersion(any(Resource.class), any(Resource.class), any(Resource.class), eq(tagFactory))).thenReturn(Optional.of(testTag));
        when(catalogManager.getLatestVersion(any(Resource.class), any(Resource.class), eq(versionFactory))).thenReturn(Optional.of(testVersion));
        when(catalogManager.createVersion(anyString(), anyString(), eq(versionFactory))).thenReturn(testVersion);
        when(catalogManager.createVersion(anyString(), anyString(), eq(tagFactory))).thenReturn(testTag);
        when(catalogManager.getTaggedCommit(any(Resource.class), any(Resource.class), any(Resource.class))).thenReturn(testCommits.get(0));
        when(catalogManager.getVersionedDistributions(any(Resource.class), any(Resource.class), any(Resource.class))).thenReturn(Collections.singleton(testDistribution));
        when(catalogManager.getVersionedDistribution(any(Resource.class), any(Resource.class), any(Resource.class), any(Resource.class))).thenReturn(Optional.of(testDistribution));
        when(catalogManager.getBranches(any(Resource.class), any(Resource.class))).thenReturn(Stream.of(testBranch, testUserBranch).collect(Collectors.toSet()));
        when(catalogManager.getBranch(any(Resource.class), any(Resource.class), any(Resource.class), eq(branchFactory))).thenReturn(Optional.of(testBranch));
        when(catalogManager.getBranch(any(Resource.class), any(Resource.class), eq(testUserBranch.getResource()), eq(branchFactory))).thenReturn(Optional.of(testUserBranch));
        when(catalogManager.getBranch(any(Resource.class), any(Resource.class), any(Resource.class), eq(userBranchFactory))).thenReturn(Optional.of(testUserBranch));
        when(catalogManager.getMasterBranch(any(Resource.class), any(Resource.class))).thenReturn(testBranch);
        when(catalogManager.createBranch(anyString(), anyString(), eq(branchFactory))).thenReturn(testBranch);
        when(catalogManager.createBranch(anyString(), anyString(), eq(userBranchFactory))).thenReturn(testUserBranch);
        when(catalogManager.getCommit(any(Resource.class), any(Resource.class), any(Resource.class), any(Resource.class))).thenAnswer(i -> {
            Resource iri = i.getArgumentAt(3, Resource.class);
            Commit found = null;
            for (Commit commit : testCommits) {
                if (iri.equals(commit.getResource())) {
                    found = commit;
                }
            }
            return Optional.ofNullable(found);
        });
        when(catalogManager.getInProgressCommit(any(Resource.class), any(Resource.class), any(Resource.class))).thenReturn(Optional.of(testInProgressCommit));
        when(catalogManager.getInProgressCommit(any(Resource.class), any(Resource.class), any(User.class))).thenReturn(Optional.of(testInProgressCommit));
        when(catalogManager.getConflicts(any(Resource.class), any(Resource.class))).thenReturn(Collections.singleton(conflict));
        when(catalogManager.getCommitChain(any(Resource.class))).thenReturn(testCommits);
        when(catalogManager.getCommitChain(any(Resource.class), any(Resource.class), any(Resource.class))).thenReturn(testCommits);
        when(catalogManager.createCommit(any(InProgressCommit.class), anyString(), any(Commit.class), any(Commit.class))).thenReturn(testCommits.get(0));
        when(catalogManager.addCommit(any(Resource.class), any(Resource.class), any(Resource.class), any(User.class), anyString())).thenReturn(vf.createIRI(COMMIT_IRIS[0]));
        when(catalogManager.mergeBranches(any(Resource.class), any(Resource.class), any(Resource.class), any(Resource.class), any(User.class), any(Model.class), any(Model.class))).thenReturn(vf.createIRI(COMMIT_IRIS[0]));
        when(catalogManager.getHeadCommit(any(Resource.class), any(Resource.class), any(Resource.class))).thenReturn(testCommits.get(0));
        when(catalogManager.getCompiledResource(any(Resource.class))).thenReturn(compiledResource);
        when(catalogManager.getInProgressCommit(any(Resource.class), any(Resource.class), any(User.class))).thenReturn(Optional.of(testInProgressCommit));
        when(catalogManager.applyInProgressCommit(any(Resource.class), any(Model.class))).thenReturn(compiledResourceWithChanges);
        when(catalogManager.createInProgressCommit(any(User.class))).thenReturn(testInProgressCommit);
        when(catalogManager.getCommitDifference(any(Resource.class))).thenReturn(difference);

        when(conflict.getIRI()).thenReturn(vf.createIRI(CONFLICT_IRI));
        when(conflict.getOriginal()).thenReturn(mf.createModel());
        when(conflict.getLeftDifference()).thenReturn(difference);
        when(conflict.getRightDifference()).thenReturn(difference);

        when(difference.getAdditions()).thenReturn(mf.createModel());
        when(difference.getDeletions()).thenReturn(mf.createModel());

        when(engineManager.retrieveUser(anyString())).thenReturn(Optional.of(user));
        when(engineManager.getUsername(any(Resource.class))).thenReturn(Optional.of(user.getResource().stringValue()));
    }

    // GET catalogs

    @Test
    public void getCatalogsWithoutTypeTest() {
        Response response = target().path("catalogs").request().get();
        assertEquals(response.getStatus(), 200);
        verify(catalogManager).getLocalCatalog();
        verify(catalogManager).getDistributedCatalog();
        try {
            JSONArray result = JSONArray.fromObject(response.readEntity(String.class));
            assertEquals(result.size(), 2);
        } catch (Exception e) {
            fail("Expected no exception, but got: " + e.getMessage());
        }
    }

    @Test
    public void getCatalogsWithTypeTest() {
        Response response = target().path("catalogs").queryParam("type", "local").request().get();
        assertEquals(response.getStatus(), 200);
        verify(catalogManager, atLeastOnce()).getLocalCatalog();
        try {
            JSONArray result = JSONArray.fromObject(response.readEntity(String.class));
            assertEquals(result.size(), 1);
            JSONObject catalog = result.getJSONObject(0);
            assertTrue(catalog.containsKey("@id"));
            assertEquals(catalog.getString("@id"), LOCAL_IRI);
        } catch (Exception e) {
            fail("Expected no exception, but got: " + e.getMessage());
        }

        response = target().path("catalogs").queryParam("type", "distributed").request().get();
        assertEquals(response.getStatus(), 200);
        verify(catalogManager, atLeastOnce()).getDistributedCatalog();
        try {
            JSONArray result = JSONArray.fromObject(response.readEntity(String.class));
            assertEquals(result.size(), 1);
            JSONObject catalog = result.getJSONObject(0);
            assertTrue(catalog.containsKey("@id"));
            assertEquals(catalog.getString("@id"), DISTRIBUTED_IRI);
        } catch (Exception e) {
            fail("Expected no exception, but got: " + e.getMessage());
        }
    }

    @Test
    public void getCatalogsWithBadTypeTest() {
        Response response = target().path("catalogs").queryParam("type", "error").request().get();
        assertEquals(response.getStatus(), 200);
        try {
            JSONArray result = JSONArray.fromObject(response.readEntity(String.class));
            assertEquals(result.size(), 0);
        } catch (Exception e) {
            fail("Expected no exception, but got: " + e.getMessage());
        }
    }

    @Test
    public void getCatalogsWithErrorTest() {
        // Setup:
        doThrow(new MatOntoException()).when(catalogManager).getLocalCatalog();

        Response response = target().path("catalogs").request().get();
        assertEquals(response.getStatus(), 500);

        doThrow(new IllegalStateException()).when(catalogManager).getLocalCatalog();
        response = target().path("catalogs").request().get();
        assertEquals(response.getStatus(), 500);
    }

    // GET catalogs/{catalogId}

    @Test
    public void getCatalogTest() {
        Response response = target().path("catalogs/" + encode(LOCAL_IRI)).request().get();
        assertEquals(response.getStatus(), 200);
        assertResponseIsObjectWithId(response, LOCAL_IRI);

        response = target().path("catalogs/" + encode(DISTRIBUTED_IRI)).request().get();
        assertEquals(response.getStatus(), 200);
        assertResponseIsObjectWithId(response, DISTRIBUTED_IRI);
    }

    @Test
    public void getCatalogThatDoesNotExistTest() {
        Response response = target().path("catalogs/" + encode(ERROR_IRI)).request().get();
        assertEquals(response.getStatus(), 404);
    }

    @Test
    public void getCatalogWithErrorTest() {
        // Setup:
        doThrow(new MatOntoException()).when(catalogManager).getLocalCatalog();

        Response response = target().path("catalogs/" + encode(LOCAL_IRI)).request().get();
        assertEquals(response.getStatus(), 500);

        doThrow(new IllegalStateException()).when(catalogManager).getLocalCatalog();
        response = target().path("catalogs/" + encode(LOCAL_IRI)).request().get();
        assertEquals(response.getStatus(), 500);
    }

    // GET catalogs/{catalogId}/records

    @Test
    public void getRecordsTest() {
        Response response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records")
                .queryParam("sort", DCTERMS.TITLE.stringValue())
                .queryParam("type", Record.TYPE)
                .queryParam("offset", 0)
                .queryParam("limit", 10)
                .queryParam("ascending", false)
                .queryParam("searchText", "test").request().get();
        assertEquals(response.getStatus(), 200);
        verify(catalogManager).findRecord(eq(vf.createIRI(LOCAL_IRI)), any(PaginatedSearchParams.class));
        MultivaluedMap<String, Object> headers = response.getHeaders();
        assertEquals(headers.get("X-Total-Count").get(0), "" + results.getTotalSize());
        assertEquals(response.getLinks().size(), 0);
        try {
            JSONArray result = JSONArray.fromObject(response.readEntity(String.class));
            assertEquals(result.size(), results.getPage().size());
        } catch (Exception e) {
            fail("Expected no exception, but got: " + e.getMessage());
        }
    }

    @Test
    public void getRecordsWithLinksTest() {
        // Setup:
        when(results.getPageNumber()).thenReturn(1);

        Response response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records")
                .queryParam("offset", 1)
                .queryParam("limit", 1).request().get();
        assertEquals(response.getStatus(), 200);
        verify(catalogManager).findRecord(eq(vf.createIRI(LOCAL_IRI)), any(PaginatedSearchParams.class));
        Set<Link> links = response.getLinks();
        assertEquals(links.size(), 2);
        links.forEach(link -> {
                assertTrue(link.getUri().getRawPath().contains("catalogs/" + encode(LOCAL_IRI) + "/records"));
                assertTrue(link.getRel().equals("prev") || link.getRel().equals("next"));
            });
    }

    @Test
    public void getRecordsWithNegativeOffsetTest() {
        Response response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records").queryParam("offset", -1).request().get();
        assertEquals(response.getStatus(), 400);
    }

    @Test
    public void getRecordsWithNegativeLimitTest() {
        Response response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records").queryParam("limit", -1).request().get();
        assertEquals(response.getStatus(), 400);
    }

    @Test
    public void getRecordsWithOffsetThatIsTooLargeTest() {
        // Setup:
        doThrow(new IllegalArgumentException()).when(catalogManager).findRecord(eq(vf.createIRI(LOCAL_IRI)), any(PaginatedSearchParams.class));

        Response response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records").queryParam("offset", 9999).request().get();
        assertEquals(response.getStatus(), 400);
    }

    @Test
    public void getRecordsWithIncorrectPathTest() {
        // Setup:
        doThrow(new IllegalArgumentException()).when(catalogManager).findRecord(eq(vf.createIRI(ERROR_IRI)), any(PaginatedSearchParams.class));

        Response response = target().path("catalogs/" + encode(ERROR_IRI) + "/records")
                .queryParam("sort", DCTERMS.TITLE.stringValue()).request().get();
        assertEquals(response.getStatus(), 400);
    }

    @Test
    public void getRecordsWithErrorTest() {
        // Setup:
        doThrow(new MatOntoException()).when(catalogManager).findRecord(eq(vf.createIRI(LOCAL_IRI)), any(PaginatedSearchParams.class));

        Response response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records")
                .queryParam("sort", DCTERMS.TITLE.stringValue()).request().get();
        assertEquals(response.getStatus(), 500);
    }

    // POST catalogs/{catalogId}/records

    @Test
    public void createRecordTest() {
        testCreateRecordByType(recordFactory);
    }

    @Test
    public void createUnversionedRecordTest() {
        testCreateRecordByType(unversionedRecordFactory);
    }

    @Test
    public void createVersionedRecordTest() {
        testCreateRecordByType(versionedRecordFactory);
    }

    @Test
    public void createVersionedRDFRecordTest() {
        testCreateRecordByType(versionedRDFRecordFactory);
    }

    @Test
    public void createOntologyRecordTest() {
        testCreateRecordByType(ontologyRecordFactory);
    }

    @Test
    public void createMappingRecordTest() {
        testCreateRecordByType(mappingRecordFactory);
    }

    @Test
    public void createRecordWithoutTypeTest() {
        //Setup:
        FormDataMultiPart fd = new FormDataMultiPart().field("title", "Title");

        Response response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records")
                .request().post(Entity.entity(fd, MediaType.MULTIPART_FORM_DATA));
        assertEquals(response.getStatus(), 400);
    }

    @Test
    public void createRecordWithoutTitleTest() {
        //Setup:
        FormDataMultiPart fd = new FormDataMultiPart().field("type", Record.TYPE);

        Response response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records")
                .request().post(Entity.entity(fd, MediaType.MULTIPART_FORM_DATA));
        assertEquals(response.getStatus(), 400);
    }

    @Test
    public void createRecordWithInvalidTypeTest() {
        //Setup:
        FormDataMultiPart fd = new FormDataMultiPart();
        fd.field("type", Thing.TYPE);
        fd.field("title", "Title");

        Response response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records")
                .request().post(Entity.entity(fd, MediaType.MULTIPART_FORM_DATA));
        assertEquals(response.getStatus(), 400);
    }

    @Test
    public void createRecordForUserThatDoesNotExistTest() {
        // Setup:
        when(engineManager.retrieveUser(anyString())).thenReturn(Optional.empty());
        FormDataMultiPart fd = new FormDataMultiPart();
        fd.field("type", Record.TYPE);
        fd.field("title", "Title");

        Response response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records")
                .request().post(Entity.entity(fd, MediaType.MULTIPART_FORM_DATA));
        assertEquals(response.getStatus(), 401);
    }

    @Test
    public void createRecordWithIncorrectPathTest() {
        // Setup:
        FormDataMultiPart fd = new FormDataMultiPart();
        fd.field("type", Record.TYPE);
        fd.field("title", "Title");
        doThrow(new IllegalArgumentException()).when(catalogManager).addRecord(eq(vf.createIRI(ERROR_IRI)), any(Record.class));

        Response response = target().path("catalogs/" + encode(ERROR_IRI) + "/records")
                .request().post(Entity.entity(fd, MediaType.MULTIPART_FORM_DATA));
        assertEquals(response.getStatus(), 400);
    }

    @Test
    public void createRecordWithErrorTest() {
        // Setup:
        FormDataMultiPart fd = new FormDataMultiPart();
        fd.field("type", Record.TYPE);
        fd.field("title", "Title");
        doThrow(new MatOntoException()).when(catalogManager).addRecord(eq(vf.createIRI(LOCAL_IRI)), any(Record.class));

        Response response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records")
                .request().post(Entity.entity(fd, MediaType.MULTIPART_FORM_DATA));
        assertEquals(response.getStatus(), 500);
    }

    // GET catalogs/{catalogId}/records/{recordId}

    @Test
    public void getRecordTest() {
        Response response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records/" + encode(RECORD_IRI))
                .request().get();
        assertEquals(response.getStatus(), 200);
        verify(catalogManager).getRecord(vf.createIRI(LOCAL_IRI), vf.createIRI(RECORD_IRI), recordFactory);
        assertResponseIsObjectWithId(response, RECORD_IRI);
    }

    @Test
    public void getMissingRecordTest() {
        // Setup
        when(catalogManager.getRecord(eq(vf.createIRI(LOCAL_IRI)), eq(vf.createIRI(ERROR_IRI)), any(OrmFactory.class))).thenReturn(Optional.empty());

        Response response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records/" + encode(ERROR_IRI))
                .request().get();
        assertEquals(response.getStatus(), 404);
    }

    @Test
    public void getRecordWithMoreThanOneObjectTest() {
        // Setup:
        String newIRI = "http://test.com/record";
        Record recordWithAnotherObject = recordFactory.createNew(vf.createIRI(newIRI));
        recordWithAnotherObject.getModel().add(vf.createIRI("http://test.com/subject"), vf.createIRI("http://test.com/subject"), vf.createLiteral("test"));
        when(catalogManager.getRecord(any(Resource.class), any(Resource.class), any(OrmFactory.class))).thenReturn(Optional.of(recordWithAnotherObject));

        Response response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records/" + encode(newIRI))
                .request().get();
        assertEquals(response.getStatus(), 200);
        verify(catalogManager).getRecord(vf.createIRI(LOCAL_IRI), vf.createIRI(newIRI), recordFactory);
        assertResponseIsObjectWithId(response, newIRI);
    }

    @Test
    public void getRecordWithIncorrectPathTest() {
        // Setup:
        doThrow(new IllegalArgumentException()).when(catalogManager).getRecord(eq(vf.createIRI(LOCAL_IRI)), eq(vf.createIRI(ERROR_IRI)), any(OrmFactory.class));

        Response response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records/" + encode(ERROR_IRI))
                .request().get();
        assertEquals(response.getStatus(), 400);
    }

    @Test
    public void getRecordWithErrorTest() {
        // Setup:
        doThrow(new MatOntoException()).when(catalogManager).getRecord(eq(vf.createIRI(LOCAL_IRI)), eq(vf.createIRI(RECORD_IRI)), any(OrmFactory.class));

        Response response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records/" + encode(RECORD_IRI))
                .request().get();
        assertEquals(response.getStatus(), 500);
    }

    // DELETE catalogs/{catalogId}/records/{recordId}

    @Test
    public void removeRecordTest() {
        Response response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records/" + encode(RECORD_IRI))
                .request().delete();
        assertEquals(response.getStatus(), 200);
        verify(catalogManager).removeRecord(vf.createIRI(LOCAL_IRI), vf.createIRI(RECORD_IRI));
    }

    @Test
    public void removeRecordWithIncorrectPathTest() {
        // Setup:
        doThrow(new IllegalArgumentException()).when(catalogManager).removeRecord(vf.createIRI(LOCAL_IRI), vf.createIRI(ERROR_IRI));

        Response response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records/" + encode(ERROR_IRI))
                .request().delete();
        assertEquals(response.getStatus(), 400);
    }

    @Test
    public void removeRecordWithErrorTest() {
        // Setup:
        doThrow(new MatOntoException()).when(catalogManager).removeRecord(vf.createIRI(LOCAL_IRI), vf.createIRI(RECORD_IRI));

        Response response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records/" + encode(RECORD_IRI))
                .request().delete();
        assertEquals(response.getStatus(), 500);
    }

    // PUT catalogs/{catalogId}/records/{recordId}

    @Test
    public void updateRecordTest() {
        //Setup:
        JSONObject record = new JSONObject().element("@id", RECORD_IRI)
                .element("@type", new JSONArray().element(Record.TYPE));

        Response response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records/" + encode(RECORD_IRI))
                .request().put(Entity.json(record.toString()));
        assertEquals(response.getStatus(), 200);
        verify(catalogManager).updateRecord(eq(vf.createIRI(LOCAL_IRI)), any(Record.class));
    }

    @Test
    public void updateRecordWithInvalidJsonTest() {
        Response response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records/" + encode(RECORD_IRI))
                .request().put(Entity.json("['test': true]"));
        assertEquals(response.getStatus(), 400);
    }

    @Test
    public void updateRecordThatDoesNotMatchTest() {
        //Setup:
        JSONObject record = new JSONObject().element("@id", ERROR_IRI)
                .element("@type", new JSONArray().element(Record.TYPE));

        Response response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records/" + encode(RECORD_IRI))
                .request().put(Entity.json(record.toString()));
        assertEquals(response.getStatus(), 400);
    }

    @Test
    public void updateRecordWithIncorrectPathTest() {
        // Setup:
        JSONObject record = new JSONObject().element("@id", RECORD_IRI)
                .element("@type", new JSONArray().element(Record.TYPE));
        doThrow(new IllegalArgumentException()).when(catalogManager).updateRecord(eq(vf.createIRI(ERROR_IRI)), any(Record.class));

        Response response = target().path("catalogs/" + encode(ERROR_IRI) + "/records/" + encode(RECORD_IRI))
                .request().put(Entity.json(record.toString()));
        assertEquals(response.getStatus(), 400);
    }

    @Test
    public void updateRecordWithErrorTest() {
        // Setup:
        JSONObject record = new JSONObject().element("@id", RECORD_IRI)
                .element("@type", new JSONArray().element(Record.TYPE));
        doThrow(new MatOntoException()).when(catalogManager).updateRecord(eq(vf.createIRI(LOCAL_IRI)), any(Record.class));

        Response response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records/" + encode(RECORD_IRI))
                .request().put(Entity.json(record.toString()));
        assertEquals(response.getStatus(), 500);
    }

    // GET catalogs/{catalogId}/records/{recordId}/distributions

    @Test
    public void getUnversionedDistributionsTest() {
        Response response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records/" + encode(RECORD_IRI) + "/distributions")
                .queryParam("sort", DCTERMS.TITLE.stringValue())
                .queryParam("offset", 0)
                .queryParam("limit", 10)
                .request().get();
        verify(catalogManager).getUnversionedDistributions(vf.createIRI(LOCAL_IRI), vf.createIRI(RECORD_IRI));
        MultivaluedMap<String, Object> headers = response.getHeaders();
        assertEquals(headers.get("X-Total-Count").get(0), "1");
        assertEquals(response.getLinks().size(), 0);
        try {
            JSONArray result = JSONArray.fromObject(response.readEntity(String.class));
            assertEquals(result.size(), 1);
        } catch (Exception e) {
            fail("Expected no exception, but got: " + e.getMessage());
        }
    }

    @Test
    public void getUnversionedDistributionsWithLinksTest() {
        // Setup:
        Set<Distribution> distributions = IntStream.range(1, 6)
                .mapToObj(i -> DISTRIBUTION_IRI + i)
                .map(s -> distributionFactory.createNew(vf.createIRI(s)))
                .collect(Collectors.toSet());
        distributions.forEach(distribution -> distribution.setProperty(vf.createLiteral("Title"), vf.createIRI(DCTERMS.TITLE.stringValue())));
        when(catalogManager.getUnversionedDistributions(any(Resource.class), any(Resource.class))).thenReturn(distributions);

        Response response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records/" + encode(RECORD_IRI) + "/distributions")
                .queryParam("sort", DCTERMS.TITLE.stringValue())
                .queryParam("offset", 1)
                .queryParam("limit", 1).request().get();
        assertEquals(response.getStatus(), 200);
        verify(catalogManager).getUnversionedDistributions(vf.createIRI(LOCAL_IRI), vf.createIRI(RECORD_IRI));
        Set<Link> links = response.getLinks();
        assertEquals(links.size(), 2);
        links.forEach(link -> {
            assertTrue(link.getUri().getRawPath().contains("catalogs/" + encode(LOCAL_IRI) + "/records/"
                    + encode(RECORD_IRI) + "/distributions"));
            assertTrue(link.getRel().equals("prev") || link.getRel().equals("next"));
        });
    }

    @Test
    public void getUnversionedDistributionsWithInvalidSortIriTest() {
        Response response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records/" + encode(RECORD_IRI) + "/distributions")
                .queryParam("sort", DCTERMS.DESCRIPTION.stringValue()).request().get();
        assertEquals(response.getStatus(), 400);
    }

    @Test
    public void getUnversionedRecordsWithNegativeOffsetTest() {
        Response response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records/" + encode(RECORD_IRI) + "/distributions")
                .queryParam("sort", DCTERMS.TITLE.stringValue()).queryParam("offset", -1).request().get();
        assertEquals(response.getStatus(), 400);
    }

    @Test
    public void getUnversionedRecordsWithNegativeLimitTest() {
        Response response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records/" + encode(RECORD_IRI) + "/distributions")
                .queryParam("sort", DCTERMS.TITLE.stringValue()).queryParam("limit", -1).request().get();
        assertEquals(response.getStatus(), 400);
    }

    @Test
    public void getUnversionedRecordsWithOffsetThatIsTooLargeTest() {
        Response response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records/" + encode(RECORD_IRI) + "/distributions")
                .queryParam("sort", DCTERMS.TITLE.stringValue()).queryParam("offset", 100).request().get();
        assertEquals(response.getStatus(), 400);
    }

    @Test
    public void getUnversionedDistributionsWithIncorrectPathTest() {
        // Setup:
        doThrow(new IllegalArgumentException()).when(catalogManager).getUnversionedDistributions(vf.createIRI(LOCAL_IRI), vf.createIRI(ERROR_IRI));

        Response response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records/" + encode(ERROR_IRI) + "/distributions")
                .queryParam("sort", DCTERMS.TITLE.stringValue()).request().get();
        assertEquals(response.getStatus(), 400);
    }

    @Test
    public void getUnversionedDistributionsWithErrorTest() {
        // Setup:
        doThrow(new MatOntoException()).when(catalogManager).getUnversionedDistributions(vf.createIRI(LOCAL_IRI), vf.createIRI(RECORD_IRI));

        Response response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records/" + encode(RECORD_IRI) + "/distributions")
                .queryParam("sort", DCTERMS.TITLE.stringValue()).request().get();
        assertEquals(response.getStatus(), 500);
    }

    // POST catalogs/{catalogId}/records/{recordId}/distributions

    @Test
    public void createUnversionedDistributionTest() {
        // Setup:
        FormDataMultiPart fd = new FormDataMultiPart();
        fd.field("title", "Title");
        fd.field("description", "Description");
        fd.field("format", "application/json");
        fd.field("accessURL", "http://example.com/Example");
        fd.field("downloadURL", "http://example.com/Example");

        Response response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records/" + encode(RECORD_IRI) + "/distributions")
                .request().post(Entity.entity(fd, MediaType.MULTIPART_FORM_DATA));
        assertEquals(response.getStatus(), 201);
        assertEquals(response.readEntity(String.class), DISTRIBUTION_IRI);
        verify(catalogManager).createDistribution(any(DistributionConfig.class));
        verify(catalogManager).addUnversionedDistribution(eq(vf.createIRI(LOCAL_IRI)), eq(vf.createIRI(RECORD_IRI)), any(Distribution.class));
    }

    @Test
    public void createUnversionedDistributionWithoutTitleTest() {
        // Setup:
        FormDataMultiPart fd = new FormDataMultiPart().field("description", "Description");

        Response response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records/" + encode(RECORD_IRI) + "/distributions")
                .request().post(Entity.entity(fd, MediaType.MULTIPART_FORM_DATA));
        assertEquals(response.getStatus(), 400);
    }

    @Test
    public void createUnversionedDistributionForUserThatDoesNotExistTest() {
        // Setup:
        when(engineManager.retrieveUser(anyString())).thenReturn(Optional.empty());
        FormDataMultiPart fd = new FormDataMultiPart().field("title", "Title");

        Response response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records/" + encode(RECORD_IRI) + "/distributions")
                .request().post(Entity.entity(fd, MediaType.MULTIPART_FORM_DATA));
        assertEquals(response.getStatus(), 401);
    }

    @Test
    public void createUnversionedDistributionWithIncorrectPathTest() {
        // Setup:
        doThrow(new IllegalArgumentException()).when(catalogManager).addUnversionedDistribution(eq(vf.createIRI(LOCAL_IRI)), eq(vf.createIRI(ERROR_IRI)), any(Distribution.class));
        FormDataMultiPart fd = new FormDataMultiPart().field("title", "Title");

        Response response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records/" + encode(ERROR_IRI) + "/distributions")
                .request().post(Entity.entity(fd, MediaType.MULTIPART_FORM_DATA));
        assertEquals(response.getStatus(), 400);
    }

    @Test
    public void createUnversionedDistributionWithErrorTest() {
        // Setup:
        FormDataMultiPart fd = new FormDataMultiPart().field("title", "Title");
        doThrow(new MatOntoException()).when(catalogManager).createDistribution(any(DistributionConfig.class));

        Response response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records/" + encode(RECORD_IRI) + "/distributions")
                .request().post(Entity.entity(fd, MediaType.MULTIPART_FORM_DATA));
        assertEquals(response.getStatus(), 500);
    }

    // GET catalogs/{catalogId}/records/{recordId}/distributions/{distributionId}

    @Test
    public void getUnversionedDistributionTest() {
        Response response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records/" + encode(RECORD_IRI)
                + "/distributions/" + encode(DISTRIBUTION_IRI))
                .request().get();
        assertEquals(response.getStatus(), 200);
        verify(catalogManager).getUnversionedDistribution(vf.createIRI(LOCAL_IRI), vf.createIRI(RECORD_IRI), vf.createIRI(DISTRIBUTION_IRI));
        assertResponseIsObjectWithId(response, DISTRIBUTION_IRI);
    }

    @Test
    public void getMissingUnversionedDistributionTest() {
        // Setup:
        when(catalogManager.getUnversionedDistribution(vf.createIRI(LOCAL_IRI), vf.createIRI(RECORD_IRI), vf.createIRI(ERROR_IRI))).thenReturn(Optional.empty());

        Response response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records/" + encode(RECORD_IRI)
                + "/distributions/" + encode(ERROR_IRI))
                .request().get();
        assertEquals(response.getStatus(), 404);
    }

    @Test
    public void getUnversionedDistributionWithIncorrectPathTest() {
        // Setup:
        doThrow(new IllegalArgumentException()).when(catalogManager).getUnversionedDistribution(vf.createIRI(LOCAL_IRI), vf.createIRI(RECORD_IRI), vf.createIRI(ERROR_IRI));

        Response response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records/" + encode(RECORD_IRI)
                + "/distributions/" + encode(ERROR_IRI))
                .request().get();
        assertEquals(response.getStatus(), 400);
    }

    @Test
    public void getUnversionedDistributionWithErrorTest() {
        // Setup:
        doThrow(new MatOntoException()).when(catalogManager).getUnversionedDistribution(vf.createIRI(LOCAL_IRI), vf.createIRI(RECORD_IRI), vf.createIRI(DISTRIBUTED_IRI));

        Response response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records/" + encode(RECORD_IRI)
                + "/distributions/" + encode(DISTRIBUTED_IRI)).request().get();
        assertEquals(response.getStatus(), 500);
    }

    // DELETE catalogs/{catalogId}/records/{recordId}/distributions/{distributionId}

    @Test
    public void removeUnversionedDistributionTest() {
        Response response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records/" + encode(RECORD_IRI)
                + "/distributions/" + encode(DISTRIBUTION_IRI))
                .request().delete();
        assertEquals(response.getStatus(), 200);
        verify(catalogManager).removeUnversionedDistribution(vf.createIRI(LOCAL_IRI), vf.createIRI(RECORD_IRI), vf.createIRI(DISTRIBUTION_IRI));
    }

    @Test
    public void removeUnversionedDistributionWithIncorrectPathTest() {
        // Setup:
        doThrow(new IllegalArgumentException()).when(catalogManager).removeUnversionedDistribution(vf.createIRI(LOCAL_IRI), vf.createIRI(RECORD_IRI), vf.createIRI(ERROR_IRI));

        Response response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records/" + encode(RECORD_IRI)
                + "/distributions/" + encode(ERROR_IRI))
                .request().delete();
        assertEquals(response.getStatus(), 400);
    }

    @Test
    public void removeUnversionedDistributionWithErrorTest() {
        // Setup:
        doThrow(new MatOntoException()).when(catalogManager).removeUnversionedDistribution(vf.createIRI(LOCAL_IRI), vf.createIRI(RECORD_IRI), vf.createIRI(DISTRIBUTION_IRI));

        Response response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records/" + encode(RECORD_IRI)
                + "/distributions/" + encode(DISTRIBUTION_IRI))
                .request().delete();
        assertEquals(response.getStatus(), 500);
    }

    // PUT catalogs/{catalogId}/records/{recordId}/distributions/{distributionId}

    @Test
    public void updateUnversionedDistributionTest() {
        //Setup:
        JSONObject distribution = new JSONObject().element("@id", DISTRIBUTION_IRI)
                .element("@type", new JSONArray().element(Distribution.TYPE));

        Response response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records/" + encode(RECORD_IRI)
                + "/distributions/" + encode(DISTRIBUTION_IRI))
                .request().put(Entity.json(distribution.toString()));
        assertEquals(response.getStatus(), 200);
        verify(catalogManager).updateUnversionedDistribution(eq(vf.createIRI(LOCAL_IRI)), eq(vf.createIRI(RECORD_IRI)), any(Distribution.class));
    }

    @Test
    public void updateUnversionedDistributionWithInvalidJsonTest() {
        Response response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records/" + encode(RECORD_IRI)
                + "/distributions/" + encode(DISTRIBUTION_IRI))
                .request().put(Entity.json("['test': true]"));
        assertEquals(response.getStatus(), 400);
    }

    @Test
    public void updateUnversionedDistributionThatDoesNotMatchTest() {
        //Setup:
        JSONObject distribution = new JSONObject().element("@id", ERROR_IRI);

        Response response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records/" + encode(RECORD_IRI)
                + "/distributions/" + encode(DISTRIBUTION_IRI))
                .request().put(Entity.json(distribution.toString()));
        assertEquals(response.getStatus(), 400);
    }

    @Test
    public void updateUnversionedDistributionWithIncorrectPathTest() {
        // Setup:
        JSONObject distribution = new JSONObject().element("@id", DISTRIBUTION_IRI);
        doThrow(new IllegalArgumentException()).when(catalogManager).updateUnversionedDistribution(eq(vf.createIRI(LOCAL_IRI)), eq(vf.createIRI(ERROR_IRI)), any(Distribution.class));

        Response response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records/" + encode(ERROR_IRI)
                + "/distributions/" + encode(DISTRIBUTION_IRI))
                .request().put(Entity.json(distribution.toString()));
        assertEquals(response.getStatus(), 400);
    }

    @Test
    public void updateUnversionedDistributionWithErrorTest() {
        //Setup:
        JSONObject distribution = new JSONObject().element("@id", DISTRIBUTION_IRI);
        doThrow(new MatOntoException()).when(catalogManager).updateUnversionedDistribution(eq(vf.createIRI(LOCAL_IRI)), eq(vf.createIRI(RECORD_IRI)), any(Distribution.class));

        Response response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records/" + encode(RECORD_IRI)
                + "/distributions/" + encode(DISTRIBUTION_IRI)).request().put(Entity.json(distribution.toString()));
        assertEquals(response.getStatus(), 400);
    }

    // GET catalogs/{catalogId}/records/{recordId}/versions

    @Test
    public void getVersionsTest() {
        Response response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records/" + encode(RECORD_IRI) + "/versions")
                .queryParam("sort", DCTERMS.TITLE.stringValue())
                .queryParam("offset", 0)
                .queryParam("limit", 10)
                .request().get();
        verify(catalogManager).getVersions(vf.createIRI(LOCAL_IRI), vf.createIRI(RECORD_IRI));
        MultivaluedMap<String, Object> headers = response.getHeaders();
        assertEquals(headers.get("X-Total-Count").get(0), "1");
        assertEquals(response.getLinks().size(), 0);
        try {
            JSONArray result = JSONArray.fromObject(response.readEntity(String.class));
            assertEquals(result.size(), 1);
        } catch (Exception e) {
            fail("Expected no exception, but got: " + e.getMessage());
        }
    }

    @Test
    public void getVersionsWithLinksTest() {
        // Setup:
        Set<Version> versions = IntStream.range(1, 6)
                .mapToObj(i -> VERSION_IRI + i)
                .map(s -> versionFactory.createNew(vf.createIRI(s)))
                .collect(Collectors.toSet());
        versions.forEach(version -> version.setProperty(vf.createLiteral("Title"), vf.createIRI(DCTERMS.TITLE.stringValue())));
        when(catalogManager.getVersions(vf.createIRI(LOCAL_IRI), vf.createIRI(RECORD_IRI))).thenReturn(versions);

        Response response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records/" + encode(RECORD_IRI) + "/versions")
                .queryParam("sort", DCTERMS.TITLE.stringValue())
                .queryParam("offset", 1)
                .queryParam("limit", 1).request().get();
        assertEquals(response.getStatus(), 200);
        verify(catalogManager).getVersions(vf.createIRI(LOCAL_IRI), vf.createIRI(RECORD_IRI));
        Set<Link> links = response.getLinks();
        assertEquals(links.size(), 2);
        links.forEach(link -> {
            assertTrue(link.getUri().getRawPath().contains("catalogs/" + encode(LOCAL_IRI) + "/records/"
                    + encode(RECORD_IRI) + "/versions"));
            assertTrue(link.getRel().equals("prev") || link.getRel().equals("next"));
        });
    }

    @Test
    public void getVersionsWithInvalidSortIriTest() {
        Response response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records/" + encode(RECORD_IRI) + "/versions")
                .queryParam("sort", DCTERMS.DESCRIPTION.stringValue()).request().get();
        assertEquals(response.getStatus(), 400);
    }

    @Test
    public void getVersionsWithNegativeOffsetTest() {
        Response response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records/" + encode(RECORD_IRI) + "/versions")
                .queryParam("sort", DCTERMS.TITLE.stringValue()).queryParam("offset", -1).request().get();
        assertEquals(response.getStatus(), 400);
    }

    @Test
    public void getVersionsWithNegativeLimitTest() {
        Response response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records/" + encode(RECORD_IRI) + "/versions")
                .queryParam("sort", DCTERMS.TITLE.stringValue()).queryParam("limit", -1).request().get();
        assertEquals(response.getStatus(), 400);
    }

    @Test
    public void getVersionsWithOffsetThatIsTooLargeTest() {
        Response response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records/" + encode(RECORD_IRI) + "/versions")
                .queryParam("sort", DCTERMS.TITLE.stringValue()).queryParam("offset", 100).request().get();
        assertEquals(response.getStatus(), 400);
    }

    @Test
    public void getVersionsFromRecordWithIncorrectPathTest() {
        // Setup:
        doThrow(new IllegalArgumentException()).when(catalogManager).getVersions(vf.createIRI(LOCAL_IRI), vf.createIRI(ERROR_IRI));

        Response response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records/" + encode(ERROR_IRI) + "/versions")
                .queryParam("sort", DCTERMS.TITLE.stringValue()).request().get();
        assertEquals(response.getStatus(), 400);
    }

    @Test
    public void getVersionsWithErrorTest() {
        // Setup:
        doThrow(new MatOntoException()).when(catalogManager).getVersions(vf.createIRI(LOCAL_IRI), vf.createIRI(RECORD_IRI));

        Response response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records/" + encode(RECORD_IRI) + "/versions")
                .queryParam("sort", DCTERMS.TITLE.stringValue()).request().get();
        assertEquals(response.getStatus(), 500);
    }

    // POST catalogs/{catalogId}/records/{recordId}/versions

    @Test
    public void createVersionTest() {
        testCreateVersionByType(versionFactory);
    }

    @Test
    public void createTagTest() {
        testCreateVersionByType(tagFactory);
    }

    @Test
    public void createVersionWithoutTitleTest() {
        //Setup:
        FormDataMultiPart fd = new FormDataMultiPart().field("type", Version.TYPE);

        Response response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records/" + encode(RECORD_IRI) + "/versions")
                .request().post(Entity.entity(fd, MediaType.MULTIPART_FORM_DATA));
        assertEquals(response.getStatus(), 400);
    }

    @Test
    public void createVersionWithInvalidTypeTest() {
        //Setup:
        FormDataMultiPart fd = new FormDataMultiPart();
        fd.field("type", Thing.TYPE);
        fd.field("title", "Title");

        Response response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records/" + encode(RECORD_IRI) + "/versions")
                .request().post(Entity.entity(fd, MediaType.MULTIPART_FORM_DATA));
        assertEquals(response.getStatus(), 400);
    }

    @Test
    public void createVersionForUserThatDoesNotExistTest() {
        // Setup:
        when(engineManager.retrieveUser(anyString())).thenReturn(Optional.empty());
        FormDataMultiPart fd = new FormDataMultiPart();
        fd.field("type", Version.TYPE);
        fd.field("title", "Title");

        Response response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records/" + encode(RECORD_IRI) + "/versions")
                .request().post(Entity.entity(fd, MediaType.MULTIPART_FORM_DATA));
        assertEquals(response.getStatus(), 401);
    }

    @Test
    public void createVersionWithIncorrectPathTest() {
        //Setup:
        FormDataMultiPart fd = new FormDataMultiPart();
        fd.field("type", Version.TYPE);
        fd.field("title", "Title");
        doThrow(new IllegalArgumentException()).when(catalogManager).addVersion(eq(vf.createIRI(LOCAL_IRI)), eq(vf.createIRI(ERROR_IRI)), any(Version.class));

        Response response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records/" + encode(ERROR_IRI) + "/versions")
                .request().post(Entity.entity(fd, MediaType.MULTIPART_FORM_DATA));
        assertEquals(response.getStatus(), 400);
    }

    @Test
    public void createVersionWithErrorTest() {
        //Setup:
        FormDataMultiPart fd = new FormDataMultiPart();
        fd.field("type", Version.TYPE);
        fd.field("title", "Title");
        doThrow(new MatOntoException()).when(catalogManager).addVersion(eq(vf.createIRI(LOCAL_IRI)), eq(vf.createIRI(RECORD_IRI)), any(Version.class));

        Response response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records/" + encode(RECORD_IRI) + "/versions")
                .request().post(Entity.entity(fd, MediaType.MULTIPART_FORM_DATA));
        assertEquals(response.getStatus(), 500);
    }

    // GET catalogs/{catalogId}/records/{recordId}/versions/latest

    @Test
    public void getLatestVersionTest() {
        Response response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records/" + encode(RECORD_IRI) + "/versions/latest")
                .request().get();
        assertEquals(response.getStatus(), 200);
        verify(catalogManager).getLatestVersion(vf.createIRI(LOCAL_IRI), vf.createIRI(RECORD_IRI), versionFactory);
        assertResponseIsObjectWithId(response, VERSION_IRI);
    }

    @Test
    public void getMissingLatestVersionTest() {
        // Setup:
        when(catalogManager.getLatestVersion(vf.createIRI(LOCAL_IRI), vf.createIRI(RECORD_IRI), versionFactory)).thenReturn(Optional.empty());

        Response response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records/" + encode(RECORD_IRI) + "/versions/latest")
                .request().get();
        assertEquals(response.getStatus(), 404);
    }

    @Test
    public void getLatestVersionWithIncorrectPathTest() {
        // Setup:
        doThrow(new IllegalArgumentException()).when(catalogManager).getLatestVersion(vf.createIRI(LOCAL_IRI), vf.createIRI(ERROR_IRI), versionFactory);

        Response response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records/" + encode(ERROR_IRI) + "/versions/latest")
                .request().get();
        assertEquals(response.getStatus(), 400);
    }

    @Test
    public void getLatestVersionWithErrorTest() {
        // Setup:
        doThrow(new MatOntoException()).when(catalogManager).getLatestVersion(vf.createIRI(LOCAL_IRI), vf.createIRI(RECORD_IRI), versionFactory);

        Response response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records/" + encode(RECORD_IRI) + "/versions/latest")
                .request().get();
        assertEquals(response.getStatus(), 500);
    }

    // GET catalogs/{catalogId}/records/{recordId}/versions/{versionId}

    @Test
    public void getVersionTest() {
        Response response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records/" + encode(RECORD_IRI)
                + "/versions/" + encode(VERSION_IRI))
                .request().get();
        assertEquals(response.getStatus(), 200);
        verify(catalogManager).getVersion(vf.createIRI(LOCAL_IRI), vf.createIRI(RECORD_IRI), vf.createIRI(VERSION_IRI), versionFactory);
        assertResponseIsObjectWithId(response, VERSION_IRI);
    }

    @Test
    public void getMissingVersionTest() {
        // Setup:
        when(catalogManager.getVersion(vf.createIRI(LOCAL_IRI), vf.createIRI(RECORD_IRI), vf.createIRI(ERROR_IRI), versionFactory)).thenReturn(Optional.empty());

        Response response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records/" + encode(RECORD_IRI)
                + "/versions/" + encode(ERROR_IRI))
                .request().get();
        assertEquals(response.getStatus(), 404);
    }

    @Test
    public void getVersionWithIncorrectPathTest() {
        // Setup:
        doThrow(new IllegalArgumentException()).when(catalogManager).getVersion(vf.createIRI(LOCAL_IRI), vf.createIRI(RECORD_IRI), vf.createIRI(ERROR_IRI), versionFactory);

        Response response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records/" + encode(RECORD_IRI)
                + "/versions/" + encode(ERROR_IRI))
                .request().get();
        assertEquals(response.getStatus(), 400);
    }

    @Test
    public void getVersionWithErrorTest() {
        // Setup:
        doThrow(new MatOntoException()).when(catalogManager).getVersion(vf.createIRI(LOCAL_IRI), vf.createIRI(RECORD_IRI), vf.createIRI(VERSION_IRI), versionFactory);

        Response response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records/" + encode(RECORD_IRI)
                + "/versions/" + encode(VERSION_IRI))
                .request().get();
        assertEquals(response.getStatus(), 500);
    }

    // DELETE catalogs/{catalogId}/records/{recordId}/versions/{versionId}

    @Test
    public void removeVersionTest() {
        Response response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records/" + encode(RECORD_IRI)
                + "/versions/" + encode(VERSION_IRI))
                .request().delete();
        assertEquals(response.getStatus(), 200);
        verify(catalogManager).removeVersion(vf.createIRI(LOCAL_IRI), vf.createIRI(RECORD_IRI), vf.createIRI(VERSION_IRI));
    }

    @Test
    public void removeVersionWithIncorrectPathTest() {
        // Setup:
        doThrow(new IllegalArgumentException()).when(catalogManager).removeVersion(vf.createIRI(LOCAL_IRI), vf.createIRI(RECORD_IRI), vf.createIRI(ERROR_IRI));

        Response response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records/" + encode(RECORD_IRI)
                + "/versions/" + encode(ERROR_IRI))
                .request().delete();
        assertEquals(response.getStatus(), 400);
    }

    @Test
    public void removeVersionWithErrorTest() {
        // Setup:
        doThrow(new MatOntoException()).when(catalogManager).removeVersion(vf.createIRI(LOCAL_IRI), vf.createIRI(RECORD_IRI), vf.createIRI(VERSION_IRI));

        Response response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records/" + encode(RECORD_IRI)
                + "/versions/" + encode(VERSION_IRI))
                .request().delete();
        assertEquals(response.getStatus(), 500);
    }

    // PUT catalogs/{catalogId}/records/{recordId}/versions/{versionId}

    @Test
    public void updateVersionTest() {
        //Setup:
        JSONObject version = new JSONObject().element("@id", VERSION_IRI)
                .element("@type", new JSONArray().element(Version.TYPE));

        Response response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records/" + encode(RECORD_IRI)
                + "/versions/" + encode(VERSION_IRI))
                .request().put(Entity.json(version.toString()));
        assertEquals(response.getStatus(), 200);
        verify(catalogManager).updateVersion(eq(vf.createIRI(LOCAL_IRI)), eq(vf.createIRI(RECORD_IRI)), any(Version.class));
    }

    @Test
    public void updateVersionWithInvalidJsonTest() {
        Response response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records/" + encode(RECORD_IRI)
                + "/versions/" + encode(VERSION_IRI))
                .request().put(Entity.json("['test': true]"));
        assertEquals(response.getStatus(), 400);
    }

    @Test
    public void updateVersionThatDoesNotMatchTest() {
        //Setup:
        JSONObject version = new JSONObject().element("@id", ERROR_IRI);

        Response response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records/" + encode(RECORD_IRI)
                + "/versions/" + encode(VERSION_IRI))
                .request().put(Entity.json(version.toString()));
        assertEquals(response.getStatus(), 400);
    }

    @Test
    public void updateVersionWithIncorrectPathTest() {
        //Setup:
        JSONObject version = new JSONObject().element("@id", VERSION_IRI);
        doThrow(new IllegalArgumentException()).when(catalogManager).updateVersion(eq(vf.createIRI(LOCAL_IRI)), eq(vf.createIRI(RECORD_IRI)), any(Version.class));

        Response response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records/" + encode(RECORD_IRI)
                + "/versions/" + encode(VERSION_IRI))
                .request().put(Entity.json(version.toString()));
        assertEquals(response.getStatus(), 400);
    }

    @Test
    public void updateVersionWithErrorTest() {
        //Setup:
        JSONObject version = new JSONObject().element("@id", VERSION_IRI).element("@type", new JSONArray().element(Version.TYPE));
        doThrow(new MatOntoException()).when(catalogManager).updateVersion(eq(vf.createIRI(LOCAL_IRI)), eq(vf.createIRI(RECORD_IRI)), any(Version.class));

        Response response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records/" + encode(RECORD_IRI)
                + "/versions/" + encode(VERSION_IRI))
                .request().put(Entity.json(version.toString()));
        assertEquals(response.getStatus(), 500);
    }

    // GET catalogs/{catalogId}/records/{recordId}/versions/{versionId}/distributions

    @Test
    public void getVersionedDistributionsTest() {
        Response response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records/" + encode(RECORD_IRI)
                + "/versions/" + encode(VERSION_IRI) + "/distributions")
                .queryParam("sort", DCTERMS.TITLE.stringValue())
                .queryParam("offset", 0)
                .queryParam("limit", 10)
                .request().get();
        verify(catalogManager).getVersionedDistributions(vf.createIRI(LOCAL_IRI), vf.createIRI(RECORD_IRI), vf.createIRI(VERSION_IRI));
        MultivaluedMap<String, Object> headers = response.getHeaders();
        assertEquals(headers.get("X-Total-Count").get(0), "1");
        assertEquals(response.getLinks().size(), 0);
        try {
            JSONArray result = JSONArray.fromObject(response.readEntity(String.class));
            assertEquals(result.size(), 1);
        } catch (Exception e) {
            fail("Expected no exception, but got: " + e.getMessage());
        }
    }

    @Test
    public void getVersionedDistributionsWithLinksTest() {
        // Setup:
        Set<Distribution> distributions = IntStream.range(1, 6)
                .mapToObj(i -> DISTRIBUTION_IRI + i)
                .map(s -> distributionFactory.createNew(vf.createIRI(s)))
                .collect(Collectors.toSet());
        distributions.forEach(distribution -> distribution.setProperty(vf.createLiteral("Title"), vf.createIRI(DCTERMS.TITLE.stringValue())));
        when(catalogManager.getVersionedDistributions(vf.createIRI(LOCAL_IRI), vf.createIRI(RECORD_IRI), vf.createIRI(VERSION_IRI))).thenReturn(distributions);

        Response response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records/" + encode(RECORD_IRI)
                + "/versions/" + encode(VERSION_IRI) + "/distributions")
                .queryParam("sort", DCTERMS.TITLE.stringValue())
                .queryParam("offset", 1)
                .queryParam("limit", 1).request().get();
        assertEquals(response.getStatus(), 200);
        verify(catalogManager).getVersionedDistributions(vf.createIRI(LOCAL_IRI), vf.createIRI(RECORD_IRI), vf.createIRI(VERSION_IRI));
        Set<Link> links = response.getLinks();
        assertEquals(links.size(), 2);
        links.forEach(link -> {
            assertTrue(link.getUri().getRawPath().contains("catalogs/" + encode(LOCAL_IRI) + "/records/"
                    + encode(RECORD_IRI) + "/versions/" + encode(VERSION_IRI) + "/distributions"));
            assertTrue(link.getRel().equals("prev") || link.getRel().equals("next"));
        });
    }

    @Test
    public void getVersionedDistributionsWithInvalidSortIriTest() {
        Response response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records/" + encode(RECORD_IRI)
                + "/versions/" + encode(VERSION_IRI) + "/distributions")
                .queryParam("sort", DCTERMS.DESCRIPTION.stringValue()).request().get();
        assertEquals(response.getStatus(), 400);
    }

    @Test
    public void getVersionedDistributionsWithNegativeOffsetTest() {
        Response response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records/" + encode(RECORD_IRI)
                + "/versions/" + encode(VERSION_IRI) + "/distributions")
                .queryParam("sort", DCTERMS.TITLE.stringValue()).queryParam("offset", -1).request().get();
        assertEquals(response.getStatus(), 400);
    }

    @Test
    public void getVersionedDistributionsWithNonPositiveLimitTest() {
        Response response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records/" + encode(RECORD_IRI)
                + "/versions/" + encode(VERSION_IRI) + "/distributions")
                .queryParam("sort", DCTERMS.TITLE.stringValue()).queryParam("limit", -1).request().get();
        assertEquals(response.getStatus(), 400);
    }

    @Test
    public void getVersionedDistributionsWithOffsetThatIsTooLargeTest() {
        Response response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records/" + encode(RECORD_IRI)
                + "/versions/" + encode(VERSION_IRI) + "/distributions")
                .queryParam("sort", DCTERMS.TITLE.stringValue()).queryParam("offset", 100).request().get();
        assertEquals(response.getStatus(), 400);
    }

    @Test
    public void getVersionedDistributionsWithIncorrectPathExist() {
        // Setup:
        doThrow(new IllegalArgumentException()).when(catalogManager).getVersionedDistributions(vf.createIRI(LOCAL_IRI), vf.createIRI(RECORD_IRI), vf.createIRI(ERROR_IRI));

        Response response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records/" + encode(RECORD_IRI)
                + "/versions/" + encode(ERROR_IRI) + "/distributions")
                .queryParam("sort", DCTERMS.TITLE.stringValue()).request().get();
        assertEquals(response.getStatus(), 400);
    }

    @Test
    public void getVersionedDistributionsWithErrorTest() {
        // Setup:
        doThrow(new MatOntoException()).when(catalogManager).getVersionedDistributions(vf.createIRI(LOCAL_IRI), vf.createIRI(RECORD_IRI), vf.createIRI(VERSION_IRI));

        Response response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records/" + encode(RECORD_IRI)
                + "/versions/" + encode(VERSION_IRI) + "/distributions")
                .queryParam("sort", DCTERMS.TITLE.stringValue()).request().get();
        assertEquals(response.getStatus(), 500);
    }

    // POST catalogs/{catalogId}/records/{recordId}/versions/{versionId}/distributions

    @Test
    public void createVersionedDistributionTest() {
        // Setup:
        FormDataMultiPart fd = new FormDataMultiPart();
        fd.field("title", "Title");
        fd.field("description", "Description");
        fd.field("format", "application/json");
        fd.field("accessURL", "http://example.com/Example");
        fd.field("downloadURL", "http://example.com/Example");

        Response response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records/" + encode(RECORD_IRI)
                + "/versions/" + encode(VERSION_IRI) + "/distributions")
                .request().post(Entity.entity(fd, MediaType.MULTIPART_FORM_DATA));
        assertEquals(response.getStatus(), 201);
        assertEquals(response.readEntity(String.class), DISTRIBUTION_IRI);
        verify(catalogManager).createDistribution(any(DistributionConfig.class));
        verify(catalogManager).addVersionedDistribution(eq(vf.createIRI(LOCAL_IRI)), eq(vf.createIRI(RECORD_IRI)), eq(vf.createIRI(VERSION_IRI)), any(Distribution.class));
    }

    @Test
    public void createVersionedDistributionWithoutTitleTest() {
        // Setup:
        FormDataMultiPart fd = new FormDataMultiPart().field("description", "Description");

        Response response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records/" + encode(RECORD_IRI)
                + "/versions/" + encode(VERSION_IRI) + "/distributions")
                .request().post(Entity.entity(fd, MediaType.MULTIPART_FORM_DATA));
        assertEquals(response.getStatus(), 400);
    }

    @Test
    public void createVersionedDistributionForUserThatDoesNotExistTest() {
        // Setup:
        when(engineManager.retrieveUser(anyString())).thenReturn(Optional.empty());
        FormDataMultiPart fd = new FormDataMultiPart().field("title", "Title");

        Response response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records/" + encode(RECORD_IRI)
                + "/versions/" + encode(VERSION_IRI) + "/distributions")
                .request().post(Entity.entity(fd, MediaType.MULTIPART_FORM_DATA));
        assertEquals(response.getStatus(), 401);
    }

    @Test
    public void createVersionedDistributionWithIncorrectPathTest() {
        // Setup:
        FormDataMultiPart fd = new FormDataMultiPart().field("title", "Title");
        doThrow(new IllegalArgumentException()).when(catalogManager).addVersionedDistribution(eq(vf.createIRI(LOCAL_IRI)), eq(vf.createIRI(RECORD_IRI)), eq(vf.createIRI(ERROR_IRI)), any(Distribution.class));

        Response response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records/" + encode(RECORD_IRI)
                + "/versions/" + encode(ERROR_IRI) + "/distributions")
                .request().post(Entity.entity(fd, MediaType.MULTIPART_FORM_DATA));
        assertEquals(response.getStatus(), 400);
    }

    @Test
    public void createVersionedDistributionWithErrorTest() {
        // Setup:
        FormDataMultiPart fd = new FormDataMultiPart().field("title", "Title");
        doThrow(new MatOntoException()).when(catalogManager).addVersionedDistribution(eq(vf.createIRI(LOCAL_IRI)), eq(vf.createIRI(RECORD_IRI)), eq(vf.createIRI(VERSION_IRI)), any(Distribution.class));

        Response response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records/" + encode(RECORD_IRI)
                + "/versions/" + encode(VERSION_IRI) + "/distributions")
                .request().post(Entity.entity(fd, MediaType.MULTIPART_FORM_DATA));
        assertEquals(response.getStatus(), 500);
    }

    // GET catalogs/{catalogId}/records/{recordId}/versions/{versionId}/distributions/{distributionId}

    @Test
    public void getVersionedDistributionTest() {
        Response response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records/" + encode(RECORD_IRI)
                + "/versions/" + encode(VERSION_IRI) + "/distributions/" + encode(DISTRIBUTION_IRI))
                .request().get();
        assertEquals(response.getStatus(), 200);
        verify(catalogManager).getVersionedDistribution(vf.createIRI(LOCAL_IRI), vf.createIRI(RECORD_IRI), vf.createIRI(VERSION_IRI), vf.createIRI(DISTRIBUTION_IRI));
        assertResponseIsObjectWithId(response, DISTRIBUTION_IRI);
    }

    @Test
    public void getMissingVersionedDistributionTest() {
        // Setup:
        when(catalogManager.getVersionedDistribution(vf.createIRI(LOCAL_IRI), vf.createIRI(RECORD_IRI), vf.createIRI(VERSION_IRI), vf.createIRI(ERROR_IRI))).thenReturn(Optional.empty());

        Response response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records/" + encode(RECORD_IRI)
                + "/versions/" + encode(VERSION_IRI) + "/distributions/" + encode(ERROR_IRI))
                .request().get();
        assertEquals(response.getStatus(), 404);
    }

    @Test
    public void getVersionedDistributionWithIncorrectPathTest() {
        // Setup:
        doThrow(new IllegalArgumentException()).when(catalogManager).getVersionedDistribution(vf.createIRI(LOCAL_IRI), vf.createIRI(RECORD_IRI), vf.createIRI(VERSION_IRI), vf.createIRI(ERROR_IRI));

        Response response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records/" + encode(RECORD_IRI)
                + "/versions/" + encode(VERSION_IRI) + "/distributions/" + encode(ERROR_IRI))
                .request().get();
        assertEquals(response.getStatus(), 400);
    }

    @Test
    public void getVersionedDistributionWithErrorTest() {
        // Setup:
        doThrow(new MatOntoException()).when(catalogManager).getVersionedDistribution(vf.createIRI(LOCAL_IRI), vf.createIRI(RECORD_IRI), vf.createIRI(VERSION_IRI), vf.createIRI(DISTRIBUTION_IRI));

        Response response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records/" + encode(RECORD_IRI)
                + "/versions/" + encode(VERSION_IRI) + "/distributions/" + encode(DISTRIBUTION_IRI))
                .request().get();
        assertEquals(response.getStatus(), 500);
    }

    // DELETE catalogs/{catalogId}/records/{recordId}/versions/{versionId}/distributions/{distributionId}

    @Test
    public void removeVersionedDistributionTest() {
        Response response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records/" + encode(RECORD_IRI)
                + "/versions/" + encode(VERSION_IRI) + "/distributions/" + encode(DISTRIBUTION_IRI))
                .request().delete();
        assertEquals(response.getStatus(), 200);
        verify(catalogManager).removeVersionedDistribution(vf.createIRI(LOCAL_IRI), vf.createIRI(RECORD_IRI), vf.createIRI(VERSION_IRI), vf.createIRI(DISTRIBUTION_IRI));
    }

    @Test
    public void removeVersionedDistributionWithIncorrectPathTest() {
        // Setup:
        doThrow(new IllegalArgumentException()).when(catalogManager).removeVersionedDistribution(vf.createIRI(LOCAL_IRI), vf.createIRI(RECORD_IRI), vf.createIRI(VERSION_IRI), vf.createIRI(ERROR_IRI));

        Response response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records/" + encode(RECORD_IRI)
                + "/versions/" + encode(VERSION_IRI) + "/distributions/" + encode(ERROR_IRI))
                .request().delete();
        assertEquals(response.getStatus(), 400);
    }

    @Test
    public void removeVersionedDistributionWithErrorTest() {
        // Setup:
        doThrow(new MatOntoException()).when(catalogManager).removeVersionedDistribution(vf.createIRI(LOCAL_IRI), vf.createIRI(RECORD_IRI), vf.createIRI(VERSION_IRI), vf.createIRI(DISTRIBUTION_IRI));

        Response response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records/" + encode(RECORD_IRI)
                + "/versions/" + encode(VERSION_IRI) + "/distributions/" + encode(DISTRIBUTION_IRI))
                .request().delete();
        assertEquals(response.getStatus(), 500);
    }

    // PUT catalogs/{catalogId}/records/{recordId}/versions/{versionId}/distributions/{distributionId}

    @Test
    public void updateVersionedDistributionTest() {
        //Setup:
        JSONObject distribution = new JSONObject().element("@id", DISTRIBUTION_IRI)
                .element("@type", new JSONArray().element(Distribution.TYPE));

        Response response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records/" + encode(RECORD_IRI)
                + "/versions/" + encode(VERSION_IRI) + "/distributions/" + encode(DISTRIBUTION_IRI))
                .request().put(Entity.json(distribution.toString()));
        assertEquals(response.getStatus(), 200);
        verify(catalogManager).updateVersionedDistribution(eq(vf.createIRI(LOCAL_IRI)), eq(vf.createIRI(RECORD_IRI)), eq(vf.createIRI(VERSION_IRI)), any(Distribution.class));
    }

    @Test
    public void updateVersionedDistributionWithInvalidJsonTest() {
        Response response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records/" + encode(RECORD_IRI)
                + "/versions/" + encode(VERSION_IRI) + "/distributions/" + encode(DISTRIBUTION_IRI))
                .request().put(Entity.json("['test': true]"));
        assertEquals(response.getStatus(), 400);
    }

    @Test
    public void updateVersionedDistributionThatDoesNotMatchTest() {
        //Setup:
        JSONObject distribution = new JSONObject().element("@id", ERROR_IRI);

        Response response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records/" + encode(RECORD_IRI)
                + "/versions/" + encode(VERSION_IRI) + "/distributions/" + encode(DISTRIBUTION_IRI))
                .request().put(Entity.json(distribution.toString()));
        assertEquals(response.getStatus(), 400);
    }

    @Test
    public void updateVersionedDistributionWithIncorrectPathTest() {
        // Setup:
        JSONObject distribution = new JSONObject().element("@id", DISTRIBUTION_IRI);
        doThrow(new IllegalArgumentException()).when(catalogManager).updateVersionedDistribution(eq(vf.createIRI(LOCAL_IRI)), eq(vf.createIRI(RECORD_IRI)), eq(vf.createIRI(ERROR_IRI)), any(Distribution.class));

        Response response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records/" + encode(RECORD_IRI)
                + "/versions/" + encode(ERROR_IRI) + "/distributions/" + encode(DISTRIBUTION_IRI))
                .request().put(Entity.json(distribution.toString()));
        assertEquals(response.getStatus(), 400);
    }

    @Test
    public void updateVersionedDistributionWithErrorTest() {
        //Setup:
        JSONObject distribution = new JSONObject().element("@id", DISTRIBUTION_IRI).element("@type", new JSONArray().element(Distribution.TYPE));
        doThrow(new MatOntoException()).when(catalogManager).updateVersionedDistribution(eq(vf.createIRI(LOCAL_IRI)), eq(vf.createIRI(RECORD_IRI)), eq(vf.createIRI(VERSION_IRI)), any(Distribution.class));

        Response response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records/" + encode(RECORD_IRI)
                + "/versions/" + encode(VERSION_IRI) + "/distributions/" + encode(DISTRIBUTION_IRI))
                .request().put(Entity.json(distribution.toString()));
        assertEquals(response.getStatus(), 500);
    }

    // GET catalogs/{catalogId}/records/{recordId}/versions/{versionId}/commit

    @Test
    public void getVersionCommitTest() {
        Response response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records/" + encode(RECORD_IRI)
                + "/versions/" + encode(VERSION_IRI) + "/commit")
                .request().get();
        assertEquals(response.getStatus(), 200);
        verify(catalogManager).getTaggedCommit(vf.createIRI(LOCAL_IRI), vf.createIRI(RECORD_IRI), vf.createIRI(VERSION_IRI));
        try {
            JSONObject result = JSONObject.fromObject(response.readEntity(String.class));
            assertTrue(result.containsKey("commit"));
            assertTrue(result.containsKey("additions"));
            assertTrue(result.containsKey("deletions"));
            JSONObject commit = result.getJSONObject("commit");
            assertTrue(commit.containsKey("@id"));
            assertEquals(commit.getString("@id"), COMMIT_IRIS[0]);
        } catch (Exception e) {
            fail("Expected no exception, but got: " + e.getMessage());
        }
    }

    @Test
    public void getVersionCommitWithIncorrectPathTest() {
        // Setup:
        doThrow(new IllegalArgumentException()).when(catalogManager).getTaggedCommit(vf.createIRI(LOCAL_IRI), vf.createIRI(RECORD_IRI), vf.createIRI(ERROR_IRI));

        Response response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records/" + encode(RECORD_IRI)
                + "/versions/" + encode(ERROR_IRI) + "/commit")
                .request().get();
        assertEquals(response.getStatus(), 400);
    }

    @Test
    public void getVersionCommitWithErrorTest() {
        // Setup:
        doThrow(new MatOntoException()).when(catalogManager).getTaggedCommit(vf.createIRI(LOCAL_IRI), vf.createIRI(RECORD_IRI), vf.createIRI(VERSION_IRI));

        Response response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records/" + encode(RECORD_IRI)
                + "/versions/" + encode(VERSION_IRI) + "/commit")
                .request().get();
        assertEquals(response.getStatus(), 500);

        doThrow(new IllegalStateException()).when(catalogManager).getTaggedCommit(vf.createIRI(LOCAL_IRI), vf.createIRI(RECORD_IRI), vf.createIRI(VERSION_IRI));
        response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records/" + encode(RECORD_IRI)
                + "/versions/" + encode(VERSION_IRI) + "/commit")
                .request().get();
        assertEquals(response.getStatus(), 500);
    }

    // GET catalogs/{catalogId}/records/{recordId}/branches

    @Test
    public void getBranchesTest() {
        Response response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records/" + encode(RECORD_IRI) + "/branches")
                .queryParam("sort", DCTERMS.TITLE.stringValue())
                .queryParam("offset", 0)
                .queryParam("limit", 10)
                .request().get();
        assertEquals(response.getStatus(), 200);
        verify(catalogManager).getBranches(vf.createIRI(LOCAL_IRI), vf.createIRI(RECORD_IRI));
        MultivaluedMap<String, Object> headers = response.getHeaders();
        assertEquals(headers.get("X-Total-Count").get(0), "2");
        assertEquals(response.getLinks().size(), 0);
        try {
            JSONArray result = JSONArray.fromObject(response.readEntity(String.class));
            assertEquals(result.size(), 2);
        } catch (Exception e) {
            fail("Expected no exception, but got: " + e.getMessage());
        }
    }

    @Test
    public void getBranchesWithUserFilterTest() {
        // Setup:
        testUserBranch.setProperty(vf.createLiteral(USER_IRI + "/0"), vf.createIRI(DCTERMS.PUBLISHER.stringValue()));

        Response response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records/" + encode(RECORD_IRI) + "/branches")
                .queryParam("sort", DCTERMS.TITLE.stringValue())
                .queryParam("offset", 0)
                .queryParam("limit", 10)
                .queryParam("applyUserFilter", true)
                .request().get();
        assertEquals(response.getStatus(), 200);
        verify(catalogManager).getBranches(vf.createIRI(LOCAL_IRI), vf.createIRI(RECORD_IRI));
        MultivaluedMap<String, Object> headers = response.getHeaders();
        assertEquals(headers.get("X-Total-Count").get(0), "1");
        assertEquals(response.getLinks().size(), 0);
        try {
            JSONArray result = JSONArray.fromObject(response.readEntity(String.class));
            assertEquals(result.size(), 1);
        } catch (Exception e) {
            fail("Expected no exception, but got: " + e.getMessage());
        }
    }

    @Test
    public void getBranchesWithLinksTest() {
        // Setup:
        Set<Branch> branches = IntStream.range(1, 6)
                .mapToObj(i -> BRANCH_IRI + i)
                .map(s -> branchFactory.createNew(vf.createIRI(s)))
                .collect(Collectors.toSet());
        branches.forEach(branch -> branch.setProperty(vf.createLiteral("Title"), vf.createIRI(DCTERMS.TITLE.stringValue())));
        when(catalogManager.getBranches(vf.createIRI(LOCAL_IRI), vf.createIRI(RECORD_IRI))).thenReturn(branches);

        Response response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records/" + encode(RECORD_IRI) + "/branches")
                .queryParam("sort", DCTERMS.TITLE.stringValue())
                .queryParam("offset", 1)
                .queryParam("limit", 1).request().get();
        assertEquals(response.getStatus(), 200);
        verify(catalogManager).getBranches(vf.createIRI(LOCAL_IRI), vf.createIRI(RECORD_IRI));
        Set<Link> links = response.getLinks();
        assertEquals(links.size(), 2);
        links.forEach(link -> {
            assertTrue(link.getUri().getRawPath().contains("catalogs/" + encode(LOCAL_IRI) + "/records/"
                    + encode(RECORD_IRI) + "/branches"));
            assertTrue(link.getRel().equals("prev") || link.getRel().equals("next"));
        });
    }

    @Test
    public void getBranchesWithNegativeOffsetTest() {
        Response response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records/" + encode(RECORD_IRI) + "/branches")
                .queryParam("sort", DCTERMS.TITLE.stringValue()).queryParam("offset", -1).request().get();
        assertEquals(response.getStatus(), 400);
    }

    @Test
    public void getBranchesWithNegativeLimitTest() {
        Response response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records/" + encode(RECORD_IRI) + "/branches")
                .queryParam("sort", DCTERMS.TITLE.stringValue()).queryParam("limit", -1).request().get();
        assertEquals(response.getStatus(), 400);
    }

    @Test
    public void getBranchesWithOffsetThatIsTooLargeTest() {
        Response response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records/" + encode(RECORD_IRI) + "/branches")
                .queryParam("sort", DCTERMS.TITLE.stringValue()).queryParam("offset", 100).request().get();
        assertEquals(response.getStatus(), 400);
    }

    @Test
    public void getBranchesWithIncorrectPathTest() {
        // Setup:
        doThrow(new IllegalArgumentException()).when(catalogManager).getBranches(vf.createIRI(LOCAL_IRI), vf.createIRI(ERROR_IRI));

        Response response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records/" + encode(ERROR_IRI) + "/branches")
                .queryParam("sort", DCTERMS.TITLE.stringValue()).request().get();
        assertEquals(response.getStatus(), 400);
    }

    @Test
    public void getBranchesWithErrorTest() {
        // Setup:
        doThrow(new MatOntoException()).when(catalogManager).getBranches(vf.createIRI(LOCAL_IRI), vf.createIRI(RECORD_IRI));

        Response response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records/" + encode(RECORD_IRI) + "/branches")
                .queryParam("sort", DCTERMS.TITLE.stringValue()).request().get();
        assertEquals(response.getStatus(), 500);
    }

    // POST catalogs/{catalogId}/records/{recordId}/branches

    @Test
    public void createBranchTest() {
        testCreateBranchByType(branchFactory);
    }

    @Test
    public void createUserBranchTest() {
        testCreateBranchByType(userBranchFactory);
    }

    @Test
    public void createBranchWithoutTitleTest() {
        //Setup:
        FormDataMultiPart fd = new FormDataMultiPart().field("type", Branch.TYPE);

        Response response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records/" + encode(RECORD_IRI) + "/branches")
                .request().post(Entity.entity(fd, MediaType.MULTIPART_FORM_DATA));
        assertEquals(response.getStatus(), 400);
    }

    @Test
    public void createBranchWithInvalidTypeTest() {
        //Setup:
        FormDataMultiPart fd = new FormDataMultiPart();
        fd.field("type", Thing.TYPE);
        fd.field("title", "Title");

        Response response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records/" + encode(RECORD_IRI) + "/branches")
                .request().post(Entity.entity(fd, MediaType.MULTIPART_FORM_DATA));
        assertEquals(response.getStatus(), 400);
    }

    @Test
    public void createBranchForUserThatDoesNotExistTest() {
        // Setup:
        when(engineManager.retrieveUser(anyString())).thenReturn(Optional.empty());
        FormDataMultiPart fd = new FormDataMultiPart();
        fd.field("type", Branch.TYPE);
        fd.field("title", "Title");

        Response response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records/" + encode(RECORD_IRI) + "/branches")
                .request().post(Entity.entity(fd, MediaType.MULTIPART_FORM_DATA));
        assertEquals(response.getStatus(), 401);
    }

    @Test
    public void createBranchWithIncorrectPathTest() {
        // Setup:
        FormDataMultiPart fd = new FormDataMultiPart();
        fd.field("type", Branch.TYPE);
        fd.field("title", "Title");
        doThrow(new IllegalArgumentException()).when(catalogManager).addBranch(eq(vf.createIRI(LOCAL_IRI)), eq(vf.createIRI(ERROR_IRI)), any(Branch.class));

        Response response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records/" + encode(ERROR_IRI) + "/branches")
                .request().post(Entity.entity(fd, MediaType.MULTIPART_FORM_DATA));
        assertEquals(response.getStatus(), 400);
    }

    @Test
    public void createBranchWithErrorTest() {
        //Setup:
        FormDataMultiPart fd = new FormDataMultiPart();
        fd.field("type", Branch.TYPE);
        fd.field("title", "Title");
        doThrow(new MatOntoException()).when(catalogManager).addBranch(eq(vf.createIRI(LOCAL_IRI)), eq(vf.createIRI(RECORD_IRI)), any(Branch.class));

        Response response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records/" + encode(RECORD_IRI) + "/branches")
                .request().post(Entity.entity(fd, MediaType.MULTIPART_FORM_DATA));
        assertEquals(response.getStatus(), 500);
    }

    // GET catalogs/{catalogId}/records/{recordId}/branches/master

    @Test
    public void getMasterBranchTest() {
        Response response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records/" + encode(RECORD_IRI) + "/branches/master")
                .request().get();
        assertEquals(response.getStatus(), 200);
        verify(catalogManager).getMasterBranch(vf.createIRI(LOCAL_IRI), vf.createIRI(RECORD_IRI));
        assertResponseIsObjectWithId(response, BRANCH_IRI);
    }

    @Test
    public void getMasterBranchWithIncorrectPathTest() {
        // Setup:
        doThrow(new IllegalArgumentException()).when(catalogManager).getMasterBranch(vf.createIRI(LOCAL_IRI), vf.createIRI(ERROR_IRI));

        Response response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records/" + encode(ERROR_IRI) + "/branches/master")
                .request().get();
        assertEquals(response.getStatus(), 400);
    }

    @Test
    public void getMasterBranchWithErrorTest() {
        // Setup:
        doThrow(new MatOntoException()).when(catalogManager).getMasterBranch(vf.createIRI(LOCAL_IRI), vf.createIRI(RECORD_IRI));

        Response response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records/" + encode(RECORD_IRI) + "/branches/master")
                .request().get();
        assertEquals(response.getStatus(), 500);

        doThrow(new IllegalStateException()).when(catalogManager).getMasterBranch(vf.createIRI(LOCAL_IRI), vf.createIRI(RECORD_IRI));
        response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records/" + encode(RECORD_IRI) + "/branches/master")
                .request().get();
        assertEquals(response.getStatus(), 500);
    }

    // GET catalogs/{catalogId}/records/{recordId}/branches/{branchId}

    @Test
    public void getBranchTest() {
        Response response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records/" + encode(RECORD_IRI)
                + "/branches/" + encode(BRANCH_IRI))
                .request().get();
        assertEquals(response.getStatus(), 200);
        verify(catalogManager).getBranch(vf.createIRI(LOCAL_IRI), vf.createIRI(RECORD_IRI), vf.createIRI(BRANCH_IRI), branchFactory);
        assertResponseIsObjectWithId(response, BRANCH_IRI);
    }

    @Test
    public void getMissingBranchTest() {
        // Setup:
        when(catalogManager.getBranch(vf.createIRI(LOCAL_IRI), vf.createIRI(RECORD_IRI), vf.createIRI(ERROR_IRI), branchFactory)).thenReturn(Optional.empty());

        Response response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records/" + encode(RECORD_IRI)
                + "/branches/" + encode(ERROR_IRI))
                .request().get();
        assertEquals(response.getStatus(), 404);
    }

    @Test
    public void getBranchWithIncorrectPathTest() {
        // Setup:
        doThrow(new IllegalArgumentException()).when(catalogManager).getBranch(vf.createIRI(LOCAL_IRI), vf.createIRI(RECORD_IRI), vf.createIRI(ERROR_IRI), branchFactory);

        Response response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records/" + encode(RECORD_IRI)
                + "/branches/" + encode(ERROR_IRI))
                .request().get();
        assertEquals(response.getStatus(), 400);
    }

    @Test
    public void getBranchWithErrorTest() {
        // Setup:
        doThrow(new MatOntoException()).when(catalogManager).getBranch(vf.createIRI(LOCAL_IRI), vf.createIRI(RECORD_IRI), vf.createIRI(BRANCH_IRI), branchFactory);

        Response response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records/" + encode(RECORD_IRI)
                + "/branches/" + encode(BRANCH_IRI)).request().get();
        assertEquals(response.getStatus(), 500);
    }

    // DELETE catalogs/{catalogId}/records/{recordId}/branches/{branchId}

    @Test
    public void removeBranchTest() {
        Response response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records/" + encode(RECORD_IRI)
                + "/branches/" + encode(BRANCH_IRI))
                .request().delete();
        assertEquals(response.getStatus(), 200);
        verify(catalogManager).removeBranch(vf.createIRI(LOCAL_IRI), vf.createIRI(RECORD_IRI), vf.createIRI(BRANCH_IRI));
    }

    @Test
    public void removeBranchWithIncorrectPathTest() {
        // Setup:
        doThrow(new IllegalArgumentException()).when(catalogManager).removeBranch(vf.createIRI(LOCAL_IRI), vf.createIRI(RECORD_IRI), vf.createIRI(ERROR_IRI));

        Response response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records/" + encode(RECORD_IRI)
                + "/branches/" + encode(ERROR_IRI))
                .request().delete();
        assertEquals(response.getStatus(), 400);
    }

    @Test
    public void removeBranchWithErrorTest() {
        // Setup:
        doThrow(new MatOntoException()).when(catalogManager).removeBranch(vf.createIRI(LOCAL_IRI), vf.createIRI(RECORD_IRI), vf.createIRI(BRANCH_IRI));

        Response response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records/" + encode(RECORD_IRI)
                + "/branches/" + encode(BRANCH_IRI))
                .request().delete();
        assertEquals(response.getStatus(), 500);
    }

    // PUT catalogs/{catalogId}/records/{recordId}/branches/{branchId}

    @Test
    public void updateBranchTest() {
        //Setup:
        JSONObject branch = new JSONObject().element("@id", BRANCH_IRI)
                .element("@type", new JSONArray().element(Branch.TYPE));

        Response response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records/" + encode(RECORD_IRI)
                + "/branches/" + encode(BRANCH_IRI))
                .request().put(Entity.json(branch.toString()));
        assertEquals(response.getStatus(), 200);
        verify(catalogManager).updateBranch(eq(vf.createIRI(LOCAL_IRI)), eq(vf.createIRI(RECORD_IRI)), any(Branch.class));
    }

    @Test
    public void updateBranchWithInvalidJsonTest() {
        Response response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records/" + encode(RECORD_IRI)
                + "/branches/" + encode(BRANCH_IRI))
                .request().put(Entity.json("['test': true]"));
        assertEquals(response.getStatus(), 400);
    }

    @Test
    public void updateBranchThatDoesNotMatchTest() {
        //Setup:
        JSONObject branch = new JSONObject().element("@id", ERROR_IRI);

        Response response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records/" + encode(RECORD_IRI)
                + "/branches/" + encode(BRANCH_IRI))
                .request().put(Entity.json(branch.toString()));
        assertEquals(response.getStatus(), 400);
    }

    @Test
    public void updateBranchWithIncorrectPathTest() {
        //Setup:
        JSONObject branch = new JSONObject().element("@id", BRANCH_IRI);
        doThrow(new IllegalArgumentException()).when(catalogManager).updateBranch(eq(vf.createIRI(LOCAL_IRI)), eq(vf.createIRI(ERROR_IRI)), any(Branch.class));

        Response response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records/" + encode(ERROR_IRI)
                + "/branches/" + encode(BRANCH_IRI))
                .request().put(Entity.json(branch.toString()));
        assertEquals(response.getStatus(), 400);
    }

    @Test
    public void updateBranchWithErrorTest() {
        //Setup:
        JSONObject branch = new JSONObject().element("@id", BRANCH_IRI).element("@type", new JSONArray().element(Branch.TYPE));
        doThrow(new MatOntoException()).when(catalogManager).updateBranch(eq(vf.createIRI(LOCAL_IRI)), eq(vf.createIRI(RECORD_IRI)), any(Branch.class));

        Response response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records/" + encode(RECORD_IRI)
                + "/branches/" + encode(BRANCH_IRI))
                .request().put(Entity.json(branch.toString()));
        assertEquals(response.getStatus(), 500);
    }

    // GET catalogs/{catalogId}/records/{recordId}/branches/{branchId}/commits

    @Test
    public void getCommitChainTest() {
        Response response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records/" + encode(RECORD_IRI)
                + "/branches/" + encode(BRANCH_IRI) + "/commits")
                .request().get();
        assertEquals(response.getStatus(), 200);
        verify(catalogManager).getCommitChain(vf.createIRI(LOCAL_IRI), vf.createIRI(RECORD_IRI), vf.createIRI(BRANCH_IRI));
        MultivaluedMap<String, Object> headers = response.getHeaders();
        assertEquals(headers.get("X-Total-Count").get(0), "" + COMMIT_IRIS.length);
        assertEquals(response.getLinks().size(), 0);
        try {
            JSONArray result = JSONArray.fromObject(response.readEntity(String.class));
            assertEquals(result.size(), COMMIT_IRIS.length);
            for (Object aResult : result) {
                JSONObject commitObj = JSONObject.fromObject(aResult);
                assertTrue(commitObj.containsKey("id"));
                assertTrue(Arrays.asList(COMMIT_IRIS).contains(commitObj.getString("id")));
            }
        } catch (Exception e) {
            fail("Expected no exception, but got: " + e.getMessage());
        }
    }

    @Test
    public void getCommitChainWithPaginationTest() {
        Response response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records/" + encode(RECORD_IRI)
                + "/branches/" + encode(BRANCH_IRI) + "/commits").queryParam("offset", 0).queryParam("limit", 10)
                .request().get();
        assertEquals(response.getStatus(), 200);
        verify(catalogManager).getCommitChain(vf.createIRI(LOCAL_IRI), vf.createIRI(RECORD_IRI), vf.createIRI(BRANCH_IRI));
        MultivaluedMap<String, Object> headers = response.getHeaders();
        assertEquals(headers.get("X-Total-Count").get(0), "" + COMMIT_IRIS.length);
        assertEquals(response.getLinks().size(), 0);
        try {
            JSONArray result = JSONArray.fromObject(response.readEntity(String.class));
            assertEquals(result.size(), COMMIT_IRIS.length);
            for (Object aResult : result) {
                JSONObject commitObj = JSONObject.fromObject(aResult);
                assertTrue(commitObj.containsKey("id"));
                assertTrue(Arrays.asList(COMMIT_IRIS).contains(commitObj.getString("id")));
            }
        } catch (Exception e) {
            fail("Expected no exception, but got: " + e.getMessage());
        }
    }

    @Test
    public void getCommitChainWithPaginationAndLinksTest() {
        Response response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records/" + encode(RECORD_IRI)
                + "/branches/" + encode(BRANCH_IRI) + "/commits").queryParam("offset", 1).queryParam("limit", 1)
                .request().get();
        assertEquals(response.getStatus(), 200);
        verify(catalogManager).getCommitChain(vf.createIRI(LOCAL_IRI), vf.createIRI(RECORD_IRI), vf.createIRI(BRANCH_IRI));
        MultivaluedMap<String, Object> headers = response.getHeaders();
        assertEquals(headers.get("X-Total-Count").get(0), "" + COMMIT_IRIS.length);
        Set<Link> links = response.getLinks();
        assertEquals(links.size(), 2);
        links.forEach(link -> {
            assertTrue(link.getUri().getRawPath().contains("catalogs/" + encode(LOCAL_IRI) + "/records/"
                    + encode(RECORD_IRI) + "/branches/" + encode(BRANCH_IRI) + "/commits"));
            assertTrue(link.getRel().equals("prev") || link.getRel().equals("next"));
        });
        try {
            JSONArray result = JSONArray.fromObject(response.readEntity(String.class));
            assertEquals(result.size(), 1);
            JSONObject commitObj = result.getJSONObject(0);
            assertTrue(commitObj.containsKey("id"));
            assertEquals(commitObj.getString("id"), COMMIT_IRIS[1]);
        } catch (Exception e) {
            fail("Expected no exception, but got: " + e.getMessage());
        }
    }

    @Test
    public void getCommitChainWithIncorrectPathTest() {
        // Setup:
        doThrow(new IllegalArgumentException()).when(catalogManager).getCommitChain(vf.createIRI(LOCAL_IRI), vf.createIRI(RECORD_IRI), vf.createIRI(ERROR_IRI));

        Response response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records/" + encode(RECORD_IRI)
                + "/branches/" + encode(ERROR_IRI) + "/commits")
                .request().get();
        assertEquals(response.getStatus(), 400);
    }

    @Test
    public void getCommitChainWithErrorTest() {
        // Setup:
        doThrow(new MatOntoException()).when(catalogManager).getCommitChain(vf.createIRI(LOCAL_IRI), vf.createIRI(RECORD_IRI), vf.createIRI(BRANCH_IRI));

        Response response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records/" + encode(RECORD_IRI)
                + "/branches/" + encode(BRANCH_IRI) + "/commits")
                .request().get();
        assertEquals(response.getStatus(), 500);

        doThrow(new IllegalStateException()).when(catalogManager).getCommitChain(vf.createIRI(LOCAL_IRI), vf.createIRI(RECORD_IRI), vf.createIRI(BRANCH_IRI));
        response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records/" + encode(RECORD_IRI)
                + "/branches/" + encode(BRANCH_IRI) + "/commits")
                .request().get();
        assertEquals(response.getStatus(), 500);
    }

    // POST catalogs/{catalogId}/records/{recordId}/branches/{branchId}/commits

    @Test
    public void createBranchCommitTest() {
        Response response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records/" + encode(RECORD_IRI)
                + "/branches/" + encode(BRANCH_IRI) + "/commits")
                .queryParam("message", "Message").request().post(Entity.entity("", MediaType.TEXT_PLAIN));
        assertEquals(response.getStatus(), 201);
        assertEquals(response.readEntity(String.class), COMMIT_IRIS[0]);
        verify(catalogManager).addCommit(eq(vf.createIRI(LOCAL_IRI)), eq(vf.createIRI(RECORD_IRI)), eq(vf.createIRI(BRANCH_IRI)), any(User.class), eq("Message"));
    }

    @Test
    public void createBranchCommitForUserThatDoesNotExistTest() {
        // Setup:
        when(engineManager.retrieveUser(anyString())).thenReturn(Optional.empty());

        Response response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records/" + encode(RECORD_IRI)
                + "/branches/" + encode(BRANCH_IRI) + "/commits")
                .queryParam("message", "Message").request().post(Entity.entity("", MediaType.TEXT_PLAIN));
        assertEquals(response.getStatus(), 401);
    }

    @Test
    public void createBranchCommitWithIncorrectPathTest() {
        // Setup:
        doThrow(new IllegalArgumentException()).when(catalogManager).addCommit(eq(vf.createIRI(LOCAL_IRI)), eq(vf.createIRI(RECORD_IRI)), eq(vf.createIRI(ERROR_IRI)), any(User.class), eq("Message"));

        Response response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records/" + encode(RECORD_IRI)
                + "/branches/" + encode(ERROR_IRI) + "/commits")
                .queryParam("message", "Message").request().post(Entity.entity("", MediaType.TEXT_PLAIN));
        assertEquals(response.getStatus(), 400);
    }

    @Test
    public void createBranchCommitWithErrorTest() {
        // Setup:
        doThrow(new MatOntoException()).when(catalogManager).addCommit(eq(vf.createIRI(LOCAL_IRI)), eq(vf.createIRI(RECORD_IRI)), eq(vf.createIRI(BRANCH_IRI)), any(User.class), eq("Message"));

        Response response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records/" + encode(RECORD_IRI)
                + "/branches/" + encode(BRANCH_IRI) + "/commits")
                .queryParam("message", "Message").request().post(Entity.entity("", MediaType.TEXT_PLAIN));
        assertEquals(response.getStatus(), 500);

        doThrow(new IllegalStateException()).when(catalogManager).addCommit(eq(vf.createIRI(LOCAL_IRI)), eq(vf.createIRI(RECORD_IRI)), eq(vf.createIRI(BRANCH_IRI)), any(User.class), eq("Message"));
        response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records/" + encode(RECORD_IRI)
                + "/branches/" + encode(BRANCH_IRI) + "/commits")
                .queryParam("message", "Message").request().post(Entity.entity("", MediaType.TEXT_PLAIN));
        assertEquals(response.getStatus(), 500);
    }

    // GET catalogs/{catalogId}/records/{recordId}/branches/{branchId}/commits/head

    @Test
    public void getHeadTest() {
        Response response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records/" + encode(RECORD_IRI)
                + "/branches/" + encode(BRANCH_IRI) + "/commits/head")
                .request().get();
        assertEquals(response.getStatus(), 200);
        verify(catalogManager).getHeadCommit(vf.createIRI(LOCAL_IRI), vf.createIRI(RECORD_IRI), vf.createIRI(BRANCH_IRI));
        try {
            JSONObject result = JSONObject.fromObject(response.readEntity(String.class));
            assertTrue(result.containsKey("commit"));
            assertTrue(result.containsKey("additions"));
            assertTrue(result.containsKey("deletions"));
            JSONObject commit = result.getJSONObject("commit");
            assertTrue(commit.containsKey("@id"));
            assertEquals(commit.getString("@id"), COMMIT_IRIS[0]);
        } catch (Exception e) {
            fail("Expected no exception, but got: " + e.getMessage());
        }
    }

    @Test
    public void getHeadWithIncorrectPathTest() {
        // Setup:
        doThrow(new IllegalArgumentException()).when(catalogManager).getHeadCommit(vf.createIRI(LOCAL_IRI), vf.createIRI(RECORD_IRI), vf.createIRI(ERROR_IRI));

        Response response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records/" + encode(RECORD_IRI)
                + "/branches/" + encode(ERROR_IRI) + "/commits/head")
                .request().get();
        assertEquals(response.getStatus(), 400);
    }

    @Test
    public void getHeadWithErrorTest() {
        // Setup:
        doThrow(new MatOntoException()).when(catalogManager).getHeadCommit(vf.createIRI(LOCAL_IRI), vf.createIRI(RECORD_IRI), vf.createIRI(BRANCH_IRI));

        Response response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records/" + encode(RECORD_IRI)
                + "/branches/" + encode(BRANCH_IRI) + "/commits/head")
                .request().get();
        assertEquals(response.getStatus(), 500);

        doThrow(new IllegalStateException()).when(catalogManager).getHeadCommit(vf.createIRI(LOCAL_IRI), vf.createIRI(RECORD_IRI), vf.createIRI(BRANCH_IRI));
        response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records/" + encode(RECORD_IRI)
                + "/branches/" + encode(BRANCH_IRI) + "/commits/head")
                .request().get();
        assertEquals(response.getStatus(), 500);
    }

    // GET catalogs/{catalogId}/records/{recordId}/branches/{branchId}/commits/{commitId}

    @Test
    public void getBranchCommitTest() {
        Response response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records/" + encode(RECORD_IRI)
                + "/branches/" + encode(BRANCH_IRI) + "/commits/" + encode(COMMIT_IRIS[1]))
                .request().get();
        assertEquals(response.getStatus(), 200);
        verify(catalogManager).getCommit(vf.createIRI(LOCAL_IRI), vf.createIRI(RECORD_IRI), vf.createIRI(BRANCH_IRI), vf.createIRI(COMMIT_IRIS[1]));
        try {
            JSONObject result = JSONObject.fromObject(response.readEntity(String.class));
            assertTrue(result.containsKey("commit"));
            assertTrue(result.containsKey("additions"));
            assertTrue(result.containsKey("deletions"));
            JSONObject commit = result.getJSONObject("commit");
            assertTrue(commit.containsKey("@id"));
            assertEquals(commit.getString("@id"), COMMIT_IRIS[1]);
        } catch (Exception e) {
            fail("Expected no exception, but got: " + e.getMessage());
        }
    }

    @Test
    public void getMissingBranchCommitTest() {
        // Setup:
        when(catalogManager.getCommit(vf.createIRI(LOCAL_IRI), vf.createIRI(RECORD_IRI), vf.createIRI(BRANCH_IRI), vf.createIRI(ERROR_IRI))).thenReturn(Optional.empty());

        Response response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records/" + encode(RECORD_IRI)
                + "/branches/" + encode(BRANCH_IRI) + "/commits/" + encode(ERROR_IRI))
                .request().get();
        assertEquals(response.getStatus(), 404);
    }

    @Test
    public void getBranchCommitWithIncorrectPathTest() {
        // Setup:
        doThrow(new IllegalArgumentException()).when(catalogManager).getCommit(vf.createIRI(LOCAL_IRI), vf.createIRI(RECORD_IRI), vf.createIRI(BRANCH_IRI), vf.createIRI(ERROR_IRI));

        Response response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records/" + encode(RECORD_IRI)
                + "/branches/" + encode(BRANCH_IRI) + "/commits/" + encode(ERROR_IRI))
                .request().get();
        assertEquals(response.getStatus(), 400);
    }

    @Test
    public void getBranchCommitWithErrorTest() {
        // Setup:
        doThrow(new MatOntoException()).when(catalogManager).getCommit(vf.createIRI(LOCAL_IRI), vf.createIRI(RECORD_IRI), vf.createIRI(BRANCH_IRI), vf.createIRI(COMMIT_IRIS[1]));

        Response response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records/" + encode(RECORD_IRI)
                + "/branches/" + encode(BRANCH_IRI) + "/commits/" + encode(COMMIT_IRIS[1]))
                .request().get();
        assertEquals(response.getStatus(), 500);

        doThrow(new IllegalStateException()).when(catalogManager).getCommit(vf.createIRI(LOCAL_IRI), vf.createIRI(RECORD_IRI), vf.createIRI(BRANCH_IRI), vf.createIRI(COMMIT_IRIS[1]));
        response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records/" + encode(RECORD_IRI)
                + "/branches/" + encode(BRANCH_IRI) + "/commits/" + encode(COMMIT_IRIS[1]))
                .request().get();
        assertEquals(response.getStatus(), 500);
    }

    // GET catalogs/{catalogId}/records/{recordId}/branches/{branchId}/conflicts

    @Test
    public void getConflictsTest() {
        Response response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records/" + encode(RECORD_IRI)
                + "/branches/" + encode(BRANCH_IRI) + "/conflicts")
                .queryParam("targetId", BRANCH_IRI).request().get();
        assertEquals(response.getStatus(), 200);
        verify(catalogManager, times(2)).getHeadCommit(vf.createIRI(LOCAL_IRI), vf.createIRI(RECORD_IRI), vf.createIRI(BRANCH_IRI));
        verify(catalogManager).getConflicts(vf.createIRI(COMMIT_IRIS[0]), vf.createIRI(COMMIT_IRIS[0]));
        try {
            JSONArray result = JSONArray.fromObject(response.readEntity(String.class));
            assertEquals(result.size(), 1);
            JSONObject outcome = JSONObject.fromObject(result.get(0));
            assertTrue(outcome.containsKey("original"));
            assertTrue(outcome.containsKey("left"));
            assertTrue(outcome.containsKey("right"));
            JSONObject left = JSONObject.fromObject(outcome.get("left"));
            JSONObject right = JSONObject.fromObject(outcome.get("right"));
            assertTrue(left.containsKey("additions"));
            assertTrue(left.containsKey("deletions"));
            assertTrue(right.containsKey("additions"));
            assertTrue(right.containsKey("deletions"));
        } catch (Exception e) {
            fail("Expected no exception, but got: " + e.getMessage());
        }
    }

    @Test
    public void getConflictsWithIncorrectPathTest() {
        // Setup:
        doThrow(new IllegalArgumentException()).when(catalogManager).getHeadCommit(vf.createIRI(LOCAL_IRI), vf.createIRI(RECORD_IRI), vf.createIRI(ERROR_IRI));

        Response response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records/" + encode(RECORD_IRI)
                + "/branches/" + encode(ERROR_IRI) + "/conflicts")
                .queryParam("targetId", BRANCH_IRI).request().get();
        assertEquals(response.getStatus(), 400);
    }

    @Test
    public void getConflictsWithErrorTest() {
        // Setup:
        doThrow(new MatOntoException()).when(catalogManager).getConflicts(vf.createIRI(COMMIT_IRIS[0]), vf.createIRI(COMMIT_IRIS[0]));

        Response response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records/" + encode(RECORD_IRI)
                + "/branches/" + encode(BRANCH_IRI) + "/conflicts")
                .queryParam("targetId", BRANCH_IRI).request().get();
        assertEquals(response.getStatus(), 500);

        doThrow(new IllegalStateException()).when(catalogManager).getConflicts(vf.createIRI(COMMIT_IRIS[0]), vf.createIRI(COMMIT_IRIS[0]));
        response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records/" + encode(RECORD_IRI)
                + "/branches/" + encode(BRANCH_IRI) + "/conflicts")
                .queryParam("targetId", BRANCH_IRI).request().get();
        assertEquals(response.getStatus(), 500);
    }

    // POST catalogs/{catalogId}/records/{recordId}/branches/{branchId}/conflicts/resolution

    @Test
    public void mergeTest() {
        // Setup:
        JSONArray adds = new JSONArray();
        adds.add(new JSONObject().element("@id", "http://example.com/add").element("@type", new JSONArray().element("http://example.com/Add")));
        JSONArray deletes = new JSONArray();
        deletes.add(new JSONObject().element("@id", "http://example.com/delete").element("@type", new JSONArray().element("http://example.com/Delete")));
        FormDataMultiPart fd = new FormDataMultiPart();
        fd.field("additions", adds.toString());
        fd.field("deletions", deletes.toString());

        Response response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records/" + encode(RECORD_IRI)
                + "/branches/" + encode(BRANCH_IRI) + "/conflicts/resolution")
                .queryParam("targetId", BRANCH_IRI).request().post(Entity.entity(fd, MediaType.MULTIPART_FORM_DATA));
        assertEquals(response.getStatus(), 200);
        assertEquals(response.readEntity(String.class), COMMIT_IRIS[0]);
        verify(catalogManager).mergeBranches(eq(vf.createIRI(LOCAL_IRI)), eq(vf.createIRI(RECORD_IRI)), eq(vf.createIRI(BRANCH_IRI)), eq(vf.createIRI(BRANCH_IRI)), any(User.class), any(Model.class), any(Model.class));
    }

    @Test
    public void mergeWithIncorrectPathTest() {
        // Setup:
        doThrow(new IllegalArgumentException()).when(catalogManager).mergeBranches(eq(vf.createIRI(LOCAL_IRI)), eq(vf.createIRI(RECORD_IRI)), eq(vf.createIRI(ERROR_IRI)), eq(vf.createIRI(BRANCH_IRI)), any(User.class), any(Model.class), any(Model.class));
        JSONArray adds = new JSONArray();
        adds.add(new JSONObject().element("@id", "http://example.com/add").element("@type", new JSONArray().element("http://example.com/Add")));
        FormDataMultiPart fd = new FormDataMultiPart();
        fd.field("additions", adds.toString());

        Response response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records/" + encode(RECORD_IRI)
                + "/branches/" + encode(ERROR_IRI) + "/conflicts/resolution")
                .queryParam("targetId", BRANCH_IRI).request().post(Entity.entity(fd, MediaType.MULTIPART_FORM_DATA));
        assertEquals(response.getStatus(), 400);
    }

    @Test
    public void mergeForUserThatDoesNotExistTest() {
        // Setup:
        when(engineManager.retrieveUser(anyString())).thenReturn(Optional.empty());
        JSONArray adds = new JSONArray();
        adds.add(new JSONObject().element("@id", "http://example.com/add").element("@type", new JSONArray().element("http://example.com/Add")));
        FormDataMultiPart fd = new FormDataMultiPart();
        fd.field("additions", adds.toString());

        Response response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records/" + encode(RECORD_IRI)
                + "/branches/" + encode(BRANCH_IRI) + "/conflicts/resolution")
                .queryParam("targetId", BRANCH_IRI).request().post(Entity.entity(fd, MediaType.MULTIPART_FORM_DATA));
        assertEquals(response.getStatus(), 401);
    }

    @Test
    public void mergeWithErrorTest() {
        // Setup:
        doThrow(new MatOntoException()).when(catalogManager).mergeBranches(eq(vf.createIRI(LOCAL_IRI)), eq(vf.createIRI(RECORD_IRI)), eq(vf.createIRI(BRANCH_IRI)), eq(vf.createIRI(BRANCH_IRI)), any(User.class), any(Model.class), any(Model.class));
        JSONArray adds = new JSONArray();
        adds.add(new JSONObject().element("@id", "http://example.com/add").element("@type", new JSONArray().element("http://example.com/Add")));
        FormDataMultiPart fd = new FormDataMultiPart();
        fd.field("additions", adds.toString());

        Response response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records/" + encode(RECORD_IRI)
                + "/branches/" + encode(BRANCH_IRI) + "/conflicts/resolution")
                .queryParam("targetId", BRANCH_IRI).request().post(Entity.entity(fd, MediaType.MULTIPART_FORM_DATA));
        assertEquals(response.getStatus(), 500);

        doThrow(new IllegalStateException()).when(catalogManager).mergeBranches(eq(vf.createIRI(LOCAL_IRI)), eq(vf.createIRI(RECORD_IRI)), eq(vf.createIRI(BRANCH_IRI)), eq(vf.createIRI(BRANCH_IRI)), any(User.class), any(Model.class), any(Model.class));
        response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records/" + encode(RECORD_IRI)
                + "/branches/" + encode(BRANCH_IRI) + "/conflicts/resolution")
                .queryParam("targetId", BRANCH_IRI).request().post(Entity.entity(fd, MediaType.MULTIPART_FORM_DATA));
        assertEquals(response.getStatus(), 500);
    }

    // GET catalogs/{catalogId}/records/{recordId}/branches/{branchId}/commits/{commitId}/resource

    @Test
    public void getCompiledResourceAsJsonldTest() {
        Response response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records/" + encode(RECORD_IRI)
                + "/branches/" + encode(BRANCH_IRI) + "/commits/" + encode(COMMIT_IRIS[0]) + "/resource")
                .queryParam("format", "jsonld").request().get();
        assertEquals(response.getStatus(), 200);
        verify(catalogManager).getCommit(vf.createIRI(LOCAL_IRI), vf.createIRI(RECORD_IRI), vf.createIRI(BRANCH_IRI), vf.createIRI(COMMIT_IRIS[0]));
        verify(catalogManager).getCompiledResource(vf.createIRI(COMMIT_IRIS[0]));
        isJsonld(response.readEntity(String.class));
    }

    @Test
    public void getCompiledResourceAsTurtleTest() {
        Response response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records/" + encode(RECORD_IRI)
                + "/branches/" + encode(BRANCH_IRI) + "/commits/" + encode(COMMIT_IRIS[0]) + "/resource")
                .queryParam("format", "turtle").request().get();
        assertEquals(response.getStatus(), 200);
        verify(catalogManager).getCommit(vf.createIRI(LOCAL_IRI), vf.createIRI(RECORD_IRI), vf.createIRI(BRANCH_IRI), vf.createIRI(COMMIT_IRIS[0]));
        verify(catalogManager).getCompiledResource(vf.createIRI(COMMIT_IRIS[0]));
        notJsonld(response.readEntity(String.class));
    }

    @Test
    public void getCompiledResourceAsRdfxmlTest() {
        Response response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records/" + encode(RECORD_IRI)
                + "/branches/" + encode(BRANCH_IRI) + "/commits/" + encode(COMMIT_IRIS[0]) + "/resource")
                .queryParam("format", "rdf/xml").request().get();
        assertEquals(response.getStatus(), 200);
        verify(catalogManager).getCommit(vf.createIRI(LOCAL_IRI), vf.createIRI(RECORD_IRI), vf.createIRI(BRANCH_IRI), vf.createIRI(COMMIT_IRIS[0]));
        verify(catalogManager).getCompiledResource(vf.createIRI(COMMIT_IRIS[0]));
        notJsonld(response.readEntity(String.class));
    }

    @Test
    public void getCompiledResourceApplyInProgressCommitTest() {
        Response response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records/" + encode(RECORD_IRI)
                + "/branches/" + encode(BRANCH_IRI) + "/commits/" + encode(COMMIT_IRIS[0]) + "/resource")
                .queryParam("applyInProgressCommit", "true").request().get();
        assertEquals(response.getStatus(), 200);
        verify(catalogManager).getCommit(vf.createIRI(LOCAL_IRI), vf.createIRI(RECORD_IRI), vf.createIRI(BRANCH_IRI), vf.createIRI(COMMIT_IRIS[0]));
        verify(catalogManager).getCompiledResource(vf.createIRI(COMMIT_IRIS[0]));
        verify(catalogManager).getInProgressCommit(eq(vf.createIRI(LOCAL_IRI)), eq(vf.createIRI(RECORD_IRI)), any(User.class));
        verify(catalogManager).applyInProgressCommit(eq(vf.createIRI(COMMIT_IRIS[0])), any(Model.class));
    }

    @Test
    public void getCompiledResourceWithMissingInProgressCommitTest() {
        // Setup:
        when(catalogManager.getInProgressCommit(eq(vf.createIRI(LOCAL_IRI)), eq(vf.createIRI(RECORD_IRI)), any(User.class))).thenReturn(Optional.empty());

        Response response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records/" + encode(RECORD_IRI)
                + "/branches/" + encode(BRANCH_IRI) + "/commits/" + encode(COMMIT_IRIS[0]) + "/resource")
                .queryParam("applyInProgressCommit", "true").request().get();
        assertEquals(response.getStatus(), 200);
        verify(catalogManager).getCommit(vf.createIRI(LOCAL_IRI), vf.createIRI(RECORD_IRI), vf.createIRI(BRANCH_IRI), vf.createIRI(COMMIT_IRIS[0]));
        verify(catalogManager).getCompiledResource(vf.createIRI(COMMIT_IRIS[0]));
        verify(catalogManager).getInProgressCommit(eq(vf.createIRI(LOCAL_IRI)), eq(vf.createIRI(RECORD_IRI)), any(User.class));
        isJsonld(response.readEntity(String.class));
    }

    @Test
    public void getCompiledResourceForUserThatDoesNotExistTest() {
        // Setup:
        when(engineManager.retrieveUser(anyString())).thenReturn(Optional.empty());

        Response response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records/" + encode(RECORD_IRI)
                + "/branches/" + encode(BRANCH_IRI) + "/commits/" + encode(COMMIT_IRIS[0]) + "/resource")
                .queryParam("applyInProgressCommit", "true").request().get();
        assertEquals(response.getStatus(), 401);
    }

    @Test
    public void getCompiledResourceWithIncorrectPathTest() {
        // Setup:
        doThrow(new IllegalArgumentException()).when(catalogManager).getCommit(vf.createIRI(LOCAL_IRI), vf.createIRI(RECORD_IRI), vf.createIRI(BRANCH_IRI), vf.createIRI(ERROR_IRI));

        Response response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records/" + encode(RECORD_IRI)
                + "/branches/" + encode(BRANCH_IRI) + "/commits/" + encode(ERROR_IRI) + "/resource")
                .request().get();
        assertEquals(response.getStatus(), 400);
    }

    @Test
    public void getCompiledResourceWithErrorTest() {
        // Setup:
        doThrow(new MatOntoException()).when(catalogManager).getCommit(vf.createIRI(LOCAL_IRI), vf.createIRI(RECORD_IRI), vf.createIRI(BRANCH_IRI), vf.createIRI(COMMIT_IRIS[0]));

        Response response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records/" + encode(RECORD_IRI)
                + "/branches/" + encode(BRANCH_IRI) + "/commits/" + encode(COMMIT_IRIS[0]) + "/resource")
                .request().get();
        assertEquals(response.getStatus(), 500);

        doThrow(new IllegalStateException()).when(catalogManager).getCommit(vf.createIRI(LOCAL_IRI), vf.createIRI(RECORD_IRI), vf.createIRI(BRANCH_IRI), vf.createIRI(COMMIT_IRIS[0]));
        response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records/" + encode(RECORD_IRI)
                + "/branches/" + encode(BRANCH_IRI) + "/commits/" + encode(COMMIT_IRIS[0]) + "/resource")
                .request().get();
        assertEquals(response.getStatus(), 500);
    }

    // GET (download) catalogs/{catalogId}/records/{recordId}/branches/{branchId}/commits/{commitId}/resource

    @Test
    public void downloadCompiledResourceAsJsonldTest() {
        Response response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records/" + encode(RECORD_IRI)
                + "/branches/" + encode(BRANCH_IRI) + "/commits/" + encode(COMMIT_IRIS[0]) + "/resource")
                .queryParam("format", "jsonld").queryParam("fileName", "fileName").request()
                .accept(MediaType.APPLICATION_OCTET_STREAM).get();
        assertEquals(response.getStatus(), 200);
        verify(catalogManager).getCommit(vf.createIRI(LOCAL_IRI), vf.createIRI(RECORD_IRI), vf.createIRI(BRANCH_IRI), vf.createIRI(COMMIT_IRIS[0]));
        verify(catalogManager).getCompiledResource(vf.createIRI(COMMIT_IRIS[0]));
        assertTrue(response.getHeaderString("Content-Disposition").contains("fileName"));
        isJsonld(response.readEntity(String.class));
    }

    @Test
    public void downloadCompiledResourceAsTurtleTest() {
        Response response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records/" + encode(RECORD_IRI)
                + "/branches/" + encode(BRANCH_IRI) + "/commits/" + encode(COMMIT_IRIS[0]) + "/resource")
                .queryParam("format", "turtle").queryParam("fileName", "fileName").request()
                .accept(MediaType.APPLICATION_OCTET_STREAM).get();
        assertEquals(response.getStatus(), 200);
        verify(catalogManager).getCommit(vf.createIRI(LOCAL_IRI), vf.createIRI(RECORD_IRI), vf.createIRI(BRANCH_IRI), vf.createIRI(COMMIT_IRIS[0]));
        verify(catalogManager).getCompiledResource(vf.createIRI(COMMIT_IRIS[0]));
        assertTrue(response.getHeaderString("Content-Disposition").contains("fileName"));
        notJsonld(response.readEntity(String.class));
    }

    @Test
    public void downloadCompiledResourceAsRdfxmlTest() {
        Response response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records/" + encode(RECORD_IRI)
                + "/branches/" + encode(BRANCH_IRI) + "/commits/" + encode(COMMIT_IRIS[0]) + "/resource")
                .queryParam("format", "rdf/xml").queryParam("fileName", "fileName").request()
                .accept(MediaType.APPLICATION_OCTET_STREAM).get();
        assertEquals(response.getStatus(), 200);
        verify(catalogManager).getCommit(vf.createIRI(LOCAL_IRI), vf.createIRI(RECORD_IRI), vf.createIRI(BRANCH_IRI), vf.createIRI(COMMIT_IRIS[0]));
        verify(catalogManager).getCompiledResource(vf.createIRI(COMMIT_IRIS[0]));
        assertTrue(response.getHeaderString("Content-Disposition").contains("fileName"));
        notJsonld(response.readEntity(String.class));
    }

    @Test
    public void downloadCompiledResourceApplyInProgressCommitTest() {
        Response response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records/" + encode(RECORD_IRI)
                + "/branches/" + encode(BRANCH_IRI) + "/commits/" + encode(COMMIT_IRIS[0]) + "/resource")
                .queryParam("applyInProgressCommit", "true").request().accept(MediaType.APPLICATION_OCTET_STREAM).get();
        assertEquals(response.getStatus(), 200);
        verify(catalogManager).getCommit(vf.createIRI(LOCAL_IRI), vf.createIRI(RECORD_IRI), vf.createIRI(BRANCH_IRI), vf.createIRI(COMMIT_IRIS[0]));
        verify(catalogManager).getCompiledResource(vf.createIRI(COMMIT_IRIS[0]));
        verify(catalogManager).getInProgressCommit(eq(vf.createIRI(LOCAL_IRI)), eq(vf.createIRI(RECORD_IRI)), any(User.class));
        verify(catalogManager).applyInProgressCommit(eq(vf.createIRI(COMMIT_IRIS[0])), any(Model.class));
    }

    @Test
    public void downloadCompiledResourceWithMissingInProgressCommitTest() {
        // Setup:
        when(catalogManager.getInProgressCommit(eq(vf.createIRI(LOCAL_IRI)), eq(vf.createIRI(RECORD_IRI)), any(User.class))).thenReturn(Optional.empty());

        Response response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records/" + encode(RECORD_IRI)
                + "/branches/" + encode(BRANCH_IRI) + "/commits/" + encode(COMMIT_IRIS[0]) + "/resource")
                .queryParam("applyInProgressCommit", "true").request().accept(MediaType.APPLICATION_OCTET_STREAM).get();
        assertEquals(response.getStatus(), 200);
        verify(catalogManager).getCommit(vf.createIRI(LOCAL_IRI), vf.createIRI(RECORD_IRI), vf.createIRI(BRANCH_IRI), vf.createIRI(COMMIT_IRIS[0]));
        verify(catalogManager).getCompiledResource(vf.createIRI(COMMIT_IRIS[0]));
        verify(catalogManager).getInProgressCommit(eq(vf.createIRI(LOCAL_IRI)), eq(vf.createIRI(RECORD_IRI)), any(User.class));
    }

    @Test
    public void downloadCompiledResourceForUserThatDoesNotExistTest() {
        // Setup:
        when(engineManager.retrieveUser(anyString())).thenReturn(Optional.empty());

        Response response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records/" + encode(RECORD_IRI)
                + "/branches/" + encode(BRANCH_IRI) + "/commits/" + encode(COMMIT_IRIS[0]) + "/resource")
                .queryParam("applyInProgressCommit", "true").request().accept(MediaType.APPLICATION_OCTET_STREAM).get();
        assertEquals(response.getStatus(), 401);
    }

    @Test
    public void downloadCompiledResourceWithIncorrectPathTest() {
        // Setup:
        doThrow(new IllegalArgumentException()).when(catalogManager).getCommit(vf.createIRI(LOCAL_IRI), vf.createIRI(RECORD_IRI), vf.createIRI(BRANCH_IRI), vf.createIRI(ERROR_IRI));

        Response response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records/" + encode(RECORD_IRI)
                + "/branches/" + encode(BRANCH_IRI) + "/commits/" + encode(ERROR_IRI) + "/resource")
                .request().accept(MediaType.APPLICATION_OCTET_STREAM).get();
        assertEquals(response.getStatus(), 400);
    }

    @Test
    public void downloadCompiledResourceWithErrorTest() {
        // Setup:
        doThrow(new MatOntoException()).when(catalogManager).getCommit(vf.createIRI(LOCAL_IRI), vf.createIRI(RECORD_IRI), vf.createIRI(BRANCH_IRI), vf.createIRI(COMMIT_IRIS[0]));

        Response response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records/" + encode(RECORD_IRI)
                + "/branches/" + encode(BRANCH_IRI) + "/commits/" + encode(COMMIT_IRIS[0]) + "/resource")
                .request().accept(MediaType.APPLICATION_OCTET_STREAM).get();
        assertEquals(response.getStatus(), 500);

        doThrow(new IllegalStateException()).when(catalogManager).getCommit(vf.createIRI(LOCAL_IRI), vf.createIRI(RECORD_IRI), vf.createIRI(BRANCH_IRI), vf.createIRI(COMMIT_IRIS[0]));
        response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records/" + encode(RECORD_IRI)
                + "/branches/" + encode(BRANCH_IRI) + "/commits/" + encode(COMMIT_IRIS[0]) + "/resource")
                .request().accept(MediaType.APPLICATION_OCTET_STREAM).get();
        assertEquals(response.getStatus(), 500);
    }

    // POST catalogs/{catalogId}/records/{recordId}/in-progress-commit

    @Test
    public void createInProgressCommitTest() {
        Response response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records/" + encode(RECORD_IRI) + "/in-progress-commit")
                .request().post(Entity.json(""));
        assertEquals(response.getStatus(), 200);
        verify(catalogManager).createInProgressCommit(any(User.class));
        verify(catalogManager).addInProgressCommit(vf.createIRI(LOCAL_IRI), vf.createIRI(RECORD_IRI), testInProgressCommit);
    }

    @Test
    public void createInProgressCommitWithIncorrectPathTest() {
        // Setup:
        doThrow(new IllegalArgumentException()).when(catalogManager).addInProgressCommit(vf.createIRI(LOCAL_IRI), vf.createIRI(ERROR_IRI), testInProgressCommit);

        Response response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records/" + encode(ERROR_IRI) + "/in-progress-commit")
                .request().post(Entity.json(""));
        assertEquals(response.getStatus(), 400);
    }

    @Test
    public void createInProgressCommitForUserThatDoesNotExistTest() {
        // Setup:
        when(engineManager.retrieveUser(anyString())).thenReturn(Optional.empty());

        Response response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records/" + encode(RECORD_IRI) + "/in-progress-commit")
                .request().post(Entity.json(""));
        assertEquals(response.getStatus(), 401);
    }

    @Test
    public void createInProgressCommitWithErrorTest() {
        // Setup:
        doThrow(new MatOntoException()).when(catalogManager).addInProgressCommit(vf.createIRI(LOCAL_IRI), vf.createIRI(RECORD_IRI), testInProgressCommit);

        Response response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records/" + encode(RECORD_IRI) + "/in-progress-commit")
                .request().post(Entity.json(""));
        assertEquals(response.getStatus(), 500);
    }

    // GET catalogs/{catalogId}/records/{recordId}/in-progress-commit

    @Test
    public void getInProgressCommitInJsonldTest() {
        Response response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records/" + encode(RECORD_IRI) + "/in-progress-commit")
                .queryParam("format", "jsonld").request().get();
        assertEquals(response.getStatus(), 200);
        verify(catalogManager).getInProgressCommit(eq(vf.createIRI(LOCAL_IRI)), eq(vf.createIRI(RECORD_IRI)), any(User.class));
        verify(catalogManager).getCommitDifference(vf.createIRI(COMMIT_IRIS[0]));
        try {
            JSONObject result = JSONObject.fromObject(response.readEntity(String.class));
            assertTrue(result.containsKey("additions"));
            assertTrue(result.containsKey("deletions"));
            isJsonld(result.getString("additions"));
            isJsonld(result.getString("deletions"));
        } catch (Exception e) {
            fail("Expected no exception, but got: " + e.getMessage());
        }
    }

    @Test
    public void getInProgressCommitInTurtleTest() {
        Response response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records/" + encode(RECORD_IRI) + "/in-progress-commit")
                .queryParam("format", "turtle").request().get();
        assertEquals(response.getStatus(), 200);
        verify(catalogManager).getInProgressCommit(eq(vf.createIRI(LOCAL_IRI)), eq(vf.createIRI(RECORD_IRI)), any(User.class));
        verify(catalogManager).getCommitDifference(vf.createIRI(COMMIT_IRIS[0]));
        try {
            JSONObject result = JSONObject.fromObject(response.readEntity(String.class));
            assertTrue(result.containsKey("additions"));
            assertTrue(result.containsKey("deletions"));
            notJsonld(result.getString("additions"));
            notJsonld(result.getString("deletions"));
        } catch (Exception e) {
            fail("Expected no exception, but got: " + e.getMessage());
        }
    }

    @Test
    public void getInProgressCommitInRdfxmlTest() {
        Response response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records/" + encode(RECORD_IRI) + "/in-progress-commit")
                .queryParam("format", "rdf/xml").request().get();
        assertEquals(response.getStatus(), 200);
        verify(catalogManager).getInProgressCommit(eq(vf.createIRI(LOCAL_IRI)), eq(vf.createIRI(RECORD_IRI)), any(User.class));
        verify(catalogManager).getCommitDifference(vf.createIRI(COMMIT_IRIS[0]));
        try {
            JSONObject result = JSONObject.fromObject(response.readEntity(String.class));
            assertTrue(result.containsKey("additions"));
            assertTrue(result.containsKey("deletions"));
            notJsonld(result.getString("additions"));
            notJsonld(result.getString("deletions"));
        } catch (Exception e) {
            fail("Expected no exception, but got: " + e.getMessage());
        }
    }

    @Test
    public void getMissingInProgressCommitTest() {
        // Setup:
        when(catalogManager.getInProgressCommit(eq(vf.createIRI(LOCAL_IRI)), eq(vf.createIRI(ERROR_IRI)), any(User.class))).thenReturn(Optional.empty());

        Response response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records/" + encode(ERROR_IRI) + "/in-progress-commit")
                .request().get();
        assertEquals(response.getStatus(), 404);
    }

    @Test
    public void getInProgressCommitWithIncorrectPathTest() {
        // Setup:
        doThrow(new IllegalArgumentException()).when(catalogManager).getInProgressCommit(eq(vf.createIRI(LOCAL_IRI)), eq(vf.createIRI(ERROR_IRI)), any(User.class));

        Response response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records/" + encode(ERROR_IRI) + "/in-progress-commit")
                .request().get();
        assertEquals(response.getStatus(), 400);
    }

    @Test
    public void getInProgressCommitForUserThatDoesNotExistTest() {
        // Setup:
        when(engineManager.retrieveUser(anyString())).thenReturn(Optional.empty());

        Response response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records/" + encode(RECORD_IRI) + "/in-progress-commit")
                .request().get();
        assertEquals(response.getStatus(), 401);
    }

    @Test
    public void getInProgressCommitWithErrorTest() {
        // Setup:
        doThrow(new MatOntoException()).when(catalogManager).getInProgressCommit(eq(vf.createIRI(LOCAL_IRI)), eq(vf.createIRI(RECORD_IRI)), any(User.class));

        Response response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records/" + encode(RECORD_IRI) + "/in-progress-commit")
                .request().get();
        assertEquals(response.getStatus(), 500);

        doThrow(new IllegalStateException()).when(catalogManager).getInProgressCommit(eq(vf.createIRI(LOCAL_IRI)), eq(vf.createIRI(RECORD_IRI)), any(User.class));
        response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records/" + encode(RECORD_IRI) + "/in-progress-commit")
                .request().get();
        assertEquals(response.getStatus(), 500);
    }

    // DELETE catalogs/{catalogId}/records/{recordId}/in-progress-commit

    @Test
    public void removeInProgressCommitTest() {
        Response response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records/" + encode(RECORD_IRI) + "/in-progress-commit")
                .request().delete();
        assertEquals(response.getStatus(), 200);
        verify(catalogManager).removeInProgressCommit(eq(vf.createIRI(LOCAL_IRI)), eq(vf.createIRI(RECORD_IRI)), any(User.class));
    }

    @Test
    public void removeInProgressCommitWithIncorrectPathTest() {
        // Setup:
        doThrow(new IllegalArgumentException()).when(catalogManager).removeInProgressCommit(eq(vf.createIRI(LOCAL_IRI)), eq(vf.createIRI(ERROR_IRI)), any(User.class));

        Response response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records/" + encode(ERROR_IRI) + "/in-progress-commit")
                .request().delete();
        assertEquals(response.getStatus(), 400);
    }

    @Test
    public void removeInProgressCommitForUserThatDoesNotExistTest() {
        // Setup:
        when(engineManager.retrieveUser(anyString())).thenReturn(Optional.empty());

        Response response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records/" + encode(RECORD_IRI) + "/in-progress-commit")
                .request().delete();
        assertEquals(response.getStatus(), 401);
    }

    @Test
    public void removeInProgressCommitWithErrorTest() {
        // Setup:
        doThrow(new MatOntoException()).when(catalogManager).removeInProgressCommit(eq(vf.createIRI(LOCAL_IRI)), eq(vf.createIRI(RECORD_IRI)), any(User.class));

        Response response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records/" + encode(RECORD_IRI) + "/in-progress-commit")
                .request().delete();
        assertEquals(response.getStatus(), 500);

        doThrow(new IllegalStateException()).when(catalogManager).removeInProgressCommit(eq(vf.createIRI(LOCAL_IRI)), eq(vf.createIRI(RECORD_IRI)), any(User.class));
        response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records/" + encode(RECORD_IRI) + "/in-progress-commit")
                .request().delete();
        assertEquals(response.getStatus(), 500);
    }

    // PUT catalogs/{catalogId}/records/{recordId}/in-progress-commit

    @Test
    public void updateInProgressCommitTest() {
        // Setup:
        JSONArray adds = new JSONArray();
        adds.add(new JSONObject().element("@id", "http://example.com/add").element("@type", new JSONArray().element("http://example.com/Add")));
        JSONArray deletes = new JSONArray();
        deletes.add(new JSONObject().element("@id", "http://example.com/delete").element("@type", new JSONArray().element("http://example.com/Delete")));
        FormDataMultiPart fd = new FormDataMultiPart();
        fd.field("additions", adds.toString());
        fd.field("deletions", deletes.toString());

        Response response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records/" + encode(RECORD_IRI) + "/in-progress-commit")
                .request().put(Entity.entity(fd, MediaType.MULTIPART_FORM_DATA));
        assertEquals(response.getStatus(), 200);
        verify(catalogManager).updateInProgressCommit(eq(vf.createIRI(LOCAL_IRI)), eq(vf.createIRI(RECORD_IRI)), any(User.class), any(Model.class), any(Model.class));
    }

    @Test
    public void updateInProgressCommitWithIncorrectPathTest() {
        // Setup:
        JSONArray adds = new JSONArray();
        adds.add(new JSONObject().element("@id", "http://example.com/add").element("@type", new JSONArray().element("http://example.com/Add")));
        FormDataMultiPart fd = new FormDataMultiPart();
        fd.field("additions", adds.toString());
        doThrow(new IllegalArgumentException()).when(catalogManager).updateInProgressCommit(eq(vf.createIRI(LOCAL_IRI)), eq(vf.createIRI(ERROR_IRI)), any(User.class), any(Model.class), any(Model.class));

        Response response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records/" + encode(ERROR_IRI) + "/in-progress-commit")
                .request().put(Entity.entity(fd, MediaType.MULTIPART_FORM_DATA));
        assertEquals(response.getStatus(), 400);
    }

    @Test
    public void updateInProgressCommitForUserThatDoesNotExistTest() {
        // Setup:
        JSONArray adds = new JSONArray();
        adds.add(new JSONObject().element("@id", "http://example.com/add").element("@type", new JSONArray().element("http://example.com/Add")));
        FormDataMultiPart fd = new FormDataMultiPart();
        fd.field("additions", adds.toString());
        when(engineManager.retrieveUser(anyString())).thenReturn(Optional.empty());

        Response response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records/" + encode(RECORD_IRI) + "/in-progress-commit")
                .request().put(Entity.entity(fd, MediaType.MULTIPART_FORM_DATA));
        assertEquals(response.getStatus(), 401);
    }

    @Test
    public void updateInProgressCommitWithErrorTest() {
        // Setup:
        JSONArray adds = new JSONArray();
        adds.add(new JSONObject().element("@id", "http://example.com/add").element("@type", new JSONArray().element("http://example.com/Add")));
        FormDataMultiPart fd = new FormDataMultiPart();
        fd.field("additions", adds.toString());
        doThrow(new MatOntoException()).when(catalogManager).updateInProgressCommit(eq(vf.createIRI(LOCAL_IRI)), eq(vf.createIRI(RECORD_IRI)), any(User.class), any(Model.class), any(Model.class));

        Response response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records/" + encode(RECORD_IRI) + "/in-progress-commit")
                .request().put(Entity.entity(fd, MediaType.MULTIPART_FORM_DATA));
        assertEquals(response.getStatus(), 500);

        doThrow(new IllegalStateException()).when(catalogManager).updateInProgressCommit(eq(vf.createIRI(LOCAL_IRI)), eq(vf.createIRI(RECORD_IRI)), any(User.class), any(Model.class), any(Model.class));
        response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records/" + encode(RECORD_IRI) + "/in-progress-commit")
                .request().put(Entity.entity(fd, MediaType.MULTIPART_FORM_DATA));
        assertEquals(response.getStatus(), 500);
    }

    /* GET record-types */

    @Test
    public void getRecordTypesTest() {
        Response response = target().path("catalogs/record-types").request().get();
        assertEquals(response.getStatus(), 200);
        try {
            JSONArray array = JSONArray.fromObject(response.readEntity(String.class));
            assertEquals(array.size(), 6);
            assertTrue(array.contains(recordFactory.getTypeIRI().stringValue()));
            assertTrue(array.contains(unversionedRecordFactory.getTypeIRI().stringValue()));
            assertTrue(array.contains(versionedRecordFactory.getTypeIRI().stringValue()));
            assertTrue(array.contains(versionedRDFRecordFactory.getTypeIRI().stringValue()));
            assertTrue(array.contains(ontologyRecordFactory.getTypeIRI().stringValue()));
            assertTrue(array.contains(mappingRecordFactory.getTypeIRI().stringValue()));
        } catch (Exception e) {
            fail("Expected no exception, but got: " + e.getMessage());
        }
    }

    /* GET sort-options */

    @Test
    public void getSortOptionsTest() {
        Response response = target().path("catalogs/sort-options").request().get();
        assertEquals(response.getStatus(), 200);
        try {
            JSONArray array = JSONArray.fromObject(response.readEntity(String.class));
            assertEquals(array.size(), 3);
            assertTrue(array.contains(DCTERMS.TITLE.stringValue()));
            assertTrue(array.contains(DCTERMS.MODIFIED.stringValue()));
            assertTrue(array.contains(DCTERMS.ISSUED.stringValue()));
        } catch (Exception e) {
            fail("Expected no exception, but got: " + e.getMessage());
        }
    }

    private <T extends Record> void testCreateRecordByType(OrmFactory<T> ormFactory) {
        //Setup:
        FormDataMultiPart fd = new FormDataMultiPart();
        fd.field("type", ormFactory.getTypeIRI().stringValue());
        fd.field("title", "Title");
        fd.field("description", "Description");
        fd.field("keywords", "keyword");

        Response response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records")
                .request().post(Entity.entity(fd, MediaType.MULTIPART_FORM_DATA));
        assertEquals(response.getStatus(), 201);
        assertEquals(response.readEntity(String.class), RECORD_IRI);
        verify(catalogManager).createRecord(any(RecordConfig.class), eq(ormFactory));
        verify(catalogManager).addRecord(eq(vf.createIRI(LOCAL_IRI)), any(Record.class));
    }

    private <T extends Version> void testCreateVersionByType(OrmFactory<T> ormFactory) {
        //Setup:
        FormDataMultiPart fd = new FormDataMultiPart();
        fd.field("type", ormFactory.getTypeIRI().stringValue());
        fd.field("title", "Title");
        fd.field("description", "Description");

        Response response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records/" + encode(RECORD_IRI) + "/versions")
                .request().post(Entity.entity(fd, MediaType.MULTIPART_FORM_DATA));
        assertEquals(response.getStatus(), 201);
        assertEquals(response.readEntity(String.class), VERSION_IRI);
        verify(catalogManager).createVersion(anyString(), anyString(), eq(ormFactory));
        verify(catalogManager).addVersion(eq(vf.createIRI(LOCAL_IRI)), eq(vf.createIRI(RECORD_IRI)), any(Version.class));
    }

    private <T extends Branch> void testCreateBranchByType(OrmFactory<T> ormFactory) {
        //Setup:
        FormDataMultiPart fd = new FormDataMultiPart();
        fd.field("type", ormFactory.getTypeIRI().stringValue());
        fd.field("title", "Title");
        fd.field("description", "Description");

        Response response = target().path("catalogs/" + encode(LOCAL_IRI) + "/records/" + encode(RECORD_IRI) + "/branches")
                .request().post(Entity.entity(fd, MediaType.MULTIPART_FORM_DATA));
        assertEquals(response.getStatus(), 201);
        assertTrue(response.readEntity(String.class).contains(BRANCH_IRI));
        verify(catalogManager).createBranch(anyString(), anyString(), eq(ormFactory));
        verify(catalogManager).addBranch(eq(vf.createIRI(LOCAL_IRI)), eq(vf.createIRI(RECORD_IRI)), any(Branch.class));
    }

    private void isJsonld(String body) {
        try {
            JSONArray result = JSONArray.fromObject(body);
        } catch (Exception e) {
            fail("Expected no exception, but got: " + e.getMessage());
        }
    }

    private void notJsonld(String body) {
        try {
            JSONArray result = JSONArray.fromObject(body);
            fail();
        } catch (Exception e) {
            System.out.println("Format is not JSON-LD, as expected");
        }
    }

    private void assertResponseIsObjectWithId(Response response, String id) {
        try {
            JSONObject record = JSONObject.fromObject(response.readEntity(String.class));
            assertTrue(record.containsKey("@id"));
            assertEquals(record.getString("@id"), id);
        } catch (Exception e) {
            fail("Expected no exception, but got: " + e.getMessage());
        }
    }
}
