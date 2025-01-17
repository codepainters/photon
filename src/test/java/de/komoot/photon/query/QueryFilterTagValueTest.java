package de.komoot.photon.query;

import de.komoot.photon.ESBaseTester;
import de.komoot.photon.PhotonDoc;
import de.komoot.photon.Importer;

import java.util.List;

import org.json.JSONObject;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class QueryFilterTagValueTest extends ESBaseTester {
    private static final String[] TAGS = new String[]{"tourism", "attraction", "tourism", "hotel", "tourism", "museum",
                                                      "tourism", "information", "amenity", "parking", "amenity", "restaurant",
                                                      "amenity", "information", "food", "information", "railway", "station"};

    @BeforeAll
    public void setUp() throws Exception {
        setUpES();
        Importer instance = makeImporter();
        double lon = 13.38886;
        double lat = 52.51704;
        for (int i = 0; i < TAGS.length; i++) {
            String key = TAGS[i];
            String value = TAGS[++i];
            PhotonDoc doc = this.createDoc(lon, lat, i, i, key, value);
            instance.add(doc);
            lon += 0.00004;
            lat += 0.00086;
            doc = this.createDoc(lon, lat, i + 1, i + 1, key, value);
            instance.add(doc);
            lon += 0.00004;
            lat += 0.00086;
        }
        instance.finish();
        refresh();
    }

    @AfterAll
    @Override
    public void tearDown() {
        deleteIndex();
        shutdownES();
    }

    private PhotonRequest queryBerlinWithTags(String... params) {
        PhotonRequest request = new PhotonRequest("berlin", 100, null, null, 0.2, 14, "en", false);
        request.setUpTagFilters(params);

        return request;
    }

    private List<JSONObject> search(PhotonRequest request) {
        return getServer().createSearchHandler(new String[]{"en"}).search(request);
    }

    /**
     * Find me all places named "berlin" that are tagged "tourism=attraction"
     */
    @Test
    public void testFilterWithTagTourismAttraction() {
        List<JSONObject> searchResponse = search(queryBerlinWithTags("tourism:attraction"));

        assertEquals(2l, searchResponse.size());
    }

    /**
     * Find me all places named "berlin" that are tagged with a value of "attraction".
     */
    @Test
    public void testValueAttraction() {
        List<JSONObject> searchResponse = search(queryBerlinWithTags(":attraction"));

        assertEquals(2l, searchResponse.size());
    }

    /**
     * Find me all places named "berlin" that are tagged with key "tourism".
     */
    @Test
    public void testKeyTourism() {
        List<JSONObject> searchResponse = search(queryBerlinWithTags("tourism"));

        assertEquals(8l, searchResponse.size());
    }

    /**
     * Find me all places named "berlin" that are NOT tagged "tourism=attraction"
     */
    @Test
    public void testFilterWithoutTagTourismAttraction() {
        List<JSONObject> searchResponse = search(queryBerlinWithTags("!tourism:attraction"));

        assertEquals(16l, searchResponse.size());
    }

    /**
     * Find me all places named "berlin" that do not have the value "information" in their TAGS - no matter what key
     */
    @Test
    public void testValueNotInformation() {
        List<JSONObject> searchResponse = search(queryBerlinWithTags("!:information"));

        assertEquals(12l, searchResponse.size());
    }

    /**
     * Find me all places named "berlin" that do not have the value "information" in their TAGS - no matter what key
     */
    @Test
    public void testValueNotInformationAlt() {
        List<JSONObject> searchResponse = search(queryBerlinWithTags(":!information"));

        assertEquals(12l, searchResponse.size());
    }

    /**
     * Find me all places named "berlin" that do not have the key "tourism" in their TAGS
     */
    @Test
    public void testKeyNotTourism() {
        List<JSONObject> searchResponse = search(queryBerlinWithTags("!tourism"));

        assertEquals(10l, searchResponse.size());
    }

    /**
     * Find me all places named "berlin" that are tagged with the key "tourism" but not tagged with value "information".
     */
    @Test
    public void testKeyTourismAndValueNotInformation() {
        List<JSONObject> searchResponse = search(queryBerlinWithTags("tourism:!information"));

        assertEquals(6l, searchResponse.size());
    }

    /**
     * Find me all places named "berlin" that are tagged without the keys "tourism" and "amenity".
     */
    @Test
    public void testKeyNotTourismAndKeyNotAmenity() {
        List<JSONObject> searchResponse = search(queryBerlinWithTags("!tourism", "!amenity"));

        assertEquals(4l, searchResponse.size());
    }

    /**
     * Find me all places named "berlin" that are tagged with the key "tourism" but not "amenity". This test works, but,
     * the use case does not make sense because by searching for key "tourism", this test already excludes places keyed
     * on "amenity"
     */
    @Test
    public void testKeyTourismAndKeyNotAmenity() {
        List<JSONObject> searchResponse = search(queryBerlinWithTags("tourism", "!amenity"));

        assertEquals(8l, searchResponse.size());
    }

    /**
     * Find me all places named "berlin" that have value "information" but not key "amenity"
     */
    @Test
    public void testValueInformationButKeyNotAmenity() {
        List<JSONObject> searchResponse = search(queryBerlinWithTags(":information", "!amenity"));

        assertEquals(4l, searchResponse.size());
    }

    /**
     * Find me all places named "berlin" that do not have the tag tourism=attraction
     */
    @Test
    public void testTagNotTourismAttraction() {
        List<JSONObject> searchResponse = search(queryBerlinWithTags("!tourism:attraction"));

        assertEquals(16l, searchResponse.size());
    }
}
