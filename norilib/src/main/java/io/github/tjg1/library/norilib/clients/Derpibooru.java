/*
 * This file is part of nori.
 * Copyright (c) 2019 Gregory Striga <gmstriga@gmail.com>
 * License: ISC
 */

package io.github.tjg1.library.norilib.clients;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.google.gson.Gson;
import com.koushikdutta.async.DataEmitter;
import com.koushikdutta.async.DataSink;
import com.koushikdutta.async.callback.CompletedCallback;
import com.koushikdutta.async.future.Future;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.async.future.TransformFuture;
import com.koushikdutta.async.parser.AsyncParser;
import com.koushikdutta.async.parser.StringParser;
import com.koushikdutta.ion.Ion;
import com.koushikdutta.ion.Response;

import java.io.IOException;
import java.lang.reflect.Type;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import io.github.tjg1.library.norilib.Image;
import io.github.tjg1.library.norilib.SearchResult;
import io.github.tjg1.library.norilib.Tag;

/**
 * Client for the Derpibooru API. Implementation derived from the Danbooru 2.0 API.
 */
public class Derpibooru implements SearchClient {

  //region Constants
  /**
   * Number of images per search results page.
   * Best to use a large value to minimize number of unique HTTP requests.
   */
  private static final int DEFAULT_LIMIT = 50;
  /** Thumbnail size set if not returned by the API. */
  private static final int THUMBNAIL_SIZE = 250;
  /** Sample size set if not returned by the API. */
  private static final int SAMPLE_SIZE = 800;
  //endregion

  //region Derpibooru Popular Characters
  // This is a set of the current top 250 most popular character tags.
  private static final Set<String> POPULAR_CHARACTERS = new HashSet<String>(Arrays.asList(
          "adagio dazzle",
          "ahuizotl",
          "aloe",
          "amethyst star",
          "angel bunny",
          "apple bloom",
          "apple fritter",
          "applejack",
          "aria blaze",
          "arizona cow",
          "autumn blaze",
          "babs seed",
          "berry punch",
          "berryshine",
          "big macintosh",
          "blossomforth",
          "blueberry cake",
          "blues",
          "bon bon",
          "boulder (pet)",
          "bow hothoof",
          "braeburn",
          "bright mac",
          "bulk biceps",
          "button mash",
          "capper dapperpaws",
          "captain celaeno",
          "captain planet",
          "caramel",
          "carrot cake",
          "carrot top",
          "cheerilee",
          "cheese sandwich",
          "cherry berry",
          "cherry jubilee",
          "chickadee",
          "cloud kicker",
          "cloudchaser",
          "cloudy quartz",
          "coco pommel",
          "coloratura",
          "comet tail",
          "cookie crumbles",
          "cozy glow",
          "cranky doodle donkey",
          "cup cake",
          "daisy",
          "daring do",
          "daybreaker",
          "derpy hooves",
          "diamond tiara",
          "dinky hooves",
          "discord",
          "dizzy twister",
          "dj pon-3",
          "doctor whooves",
          "donut joe",
          "double diamond",
          "drama letter",
          "fancypants",
          "featherweight",
          "fili-second",
          "filthy rich",
          "firefly",
          "fizzlepop berrytwist",
          "flam",
          "flash sentry",
          "fleetfoot",
          "fleur-de-lis",
          "flim",
          "flitter",
          "flower wishes",
          "fluttershy",
          "fuchsia blush",
          "gabby",
          "gallus",
          "garble",
          "gilda",
          "glitter drops",
          "gloriosa daisy",
          "golden harvest",
          "granny smith",
          "grogar",
          "gummy",
          "hoity toity",
          "igneous rock pie",
          "indigo zap",
          "iron will",
          "juniper montage",
          "king sombra",
          "lavender lace",
          "lemon hearts",
          "lemon zest",
          "lightning bolt",
          "lightning dust",
          "lily",
          "lily valley",
          "limestone pie",
          "linky",
          "lord tirek",
          "lotus blossom",
          "lyra heartstrings",
          "mane-iac",
          "marble pie",
          "mare do well",
          "masked matter-horn",
          "maud pie",
          "mayor mare",
          "meadowbrook",
          "megan williams",
          "microchips",
          "minty",
          "minuette",
          "mistress marevelous",
          "moondancer",
          "ms. harshwhinny",
          "ms. peachbottom",
          "mudbriar",
          "mystery mint",
          "neon lights",
          "night glider",
          "night light",
          "nightmare moon",
          "nightmare rarity",
          "nightmare star",
          "normal norman",
          "noteworthy",
          "nurse redheart",
          "ocellus",
          "octavia melody",
          "oleander",
          "opalescence",
          "orange swirl",
          "owlowiscious",
          "paisley",
          "party favor",
          "pear butter",
          "pharynx",
          "philomena",
          "photo finish",
          "pinkie pie",
          "pipsqueak",
          "pokey pierce",
          "posey",
          "posey shy",
          "pound cake",
          "prince blueblood",
          "prince rutherford",
          "princess cadance",
          "princess celestia",
          "princess ember",
          "princess flurry heart",
          "princess luna",
          "princess skystar",
          "principal abacus cinch",
          "pumpkin cake",
          "queen chrysalis",
          "queen novo",
          "quibble pants",
          "radiance",
          "rainbow dash",
          "rainbow dash (g3)",
          "rainbowshine",
          "rarity",
          "rising star",
          "rockhoof",
          "roseluck",
          "rover",
          "ruby pinch",
          "rumble",
          "saddle rager",
          "saffron masala",
          "sandalwood",
          "sandbar",
          "sapphire shores",
          "sassaflash",
          "sassy saddles",
          "sci-twi",
          "scootaloo",
          "screwball",
          "sea swirl",
          "seafoam",
          "shining armor",
          "shoeshine",
          "silver spoon",
          "silverstream",
          "smarty pants",
          "smolder",
          "smooze",
          "snails",
          "snips",
          "snowfall frost",
          "soarin'",
          "somnambula",
          "sonata dusk",
          "songbird serenade",
          "sour sweet",
          "sparkler",
          "sphinx (character)",
          "spike",
          "spike the regular dog",
          "spitfire",
          "spoiled rich",
          "spring melody",
          "spring rain",
          "sprinkle medley",
          "star swirl the bearded",
          "starlight glimmer",
          "stellar flare",
          "storm king",
          "sugar belle",
          "sugarcoat",
          "sunburst",
          "sunny flare",
          "sunset shimmer",
          "sunshower raindrops",
          "suri polomare",
          "surprise",
          "sweetie belle",
          "sweetie drops",
          "tank",
          "tempest shadow",
          "thorax",
          "thunderbass",
          "thunderlane",
          "timber spruce",
          "time turner",
          "tom",
          "toola roola",
          "tree hugger",
          "trenderhoof",
          "trixie",
          "trouble shoes",
          "twilight sparkle",
          "twilight velvet",
          "twinkleshine",
          "twist",
          "vapor trail",
          "velvet reindeer",
          "vinyl scratch",
          "wallflower blush",
          "watermelody",
          "white lightning",
          "wild fire",
          "windy whistles",
          "winona",
          "yona",
          "zapp",
          "zecora",
          "zephyr breeze"));
  //endregion

  //region Service configuration instance fields
  /** Android context. */
  protected final Context context;
  /** Human-readable service name */
  private final String name;
  /** URL to the HTTP API Endpoint - the server implementing the API. */
  private final String apiEndpoint;
  /** Username used for authentication. (optional) */
  private final String username;
  /** API key used for authentication. (optional) */
  private final String apiKey;
  //endregion

  //region Constructors
  /**
   * Create a new Derpibooru client without authentication.
   *
   * @param name     Human-readable service name.
   * @param endpoint URL to the HTTP API Endpoint - the server implementing the API.
   */
  public Derpibooru(Context context, String name, String endpoint) {
    this.context = context;
    this.name = name;
    this.apiEndpoint = endpoint;
    this.username = null;
    this.apiKey = null;
  }

  /**
   * Create a new Derpibooru client with authentication.
   *
   * @param name     Human-readable service name.
   * @param endpoint URL to the HTTP API Endpoint - the server implementing the API.
   * @param username Username used for authentication.
   * @param apiKey   API key used for authentication.
   */
  public Derpibooru(Context context, String name, String endpoint, String username, final String apiKey) {
    this.context = context;
    this.name = name;
    this.apiEndpoint = endpoint;
    this.username = username;
    this.apiKey = apiKey;
  }
  //endregion

  //region Service detection
  /**
   * Checks if the given URL exposes a supported API endpoint.
   *
   * @param context Android {@link Context}.
   * @param uri URL to test.
   * @param timeout Timeout in milliseconds.
   * @return Detected endpoint URL. null, if no supported endpoint URL was detected.
   */
  @Nullable
  public static String detectService(@NonNull Context context, @NonNull Uri uri, int timeout) {
    final String endpointUrl = Uri.withAppendedPath(uri, "/search.json?q=*&perpage=1").toString();

    try {
      final Response<DataEmitter> response = Ion.with(context)
          .load(endpointUrl)
          .setTimeout(timeout)
          .userAgent(SearchClient.USER_AGENT)
          .followRedirect(false)
          .noCache()
          .asDataEmitter()
          .withResponse()
          .get();

      // Close the connection.
      final DataEmitter dataEmitter = response.getResult();
      if (dataEmitter != null) dataEmitter.close();

      if (response.getHeaders().code() == 200) {
        return uri.toString();
      }
    } catch (InterruptedException | ExecutionException ignored) {
    }
    return null;
  }
  //endregion

  //region SearchClient methods
  @Override
  public SearchResult search(String tags) throws IOException {
    // Return results for page 0.
    return search(tags, 0);
  }

  @Override
  public SearchResult search(String tags, int pid) throws IOException {
    try {
      return Ion.with(this.context)
          .load(createSearchURL(tags, pid, DEFAULT_LIMIT))
          .userAgent(SearchClient.USER_AGENT)
          .as(new SearchResultParser(tags, pid))
          .get();
    } catch (InterruptedException | ExecutionException e) {
      // Normalise exception to IOException, so method signatures are not tied to a single HTTP
      // library.
      throw new IOException(e);
    }
  }

  @Override
  public void search(String tags, SearchCallback callback) {
    // Return results for page 0.
    search(tags, 0, callback);
  }

  @Override
  public void search(final String tags, final int pid, final SearchCallback callback) {
    Ion.with(this.context)
        .load(createSearchURL(tags, pid, DEFAULT_LIMIT))
        .userAgent(SearchClient.USER_AGENT)
        .as(new SearchResultParser(tags, pid))
        .setCallback(new FutureCallback<SearchResult>() {
          @Override
          public void onCompleted(Exception e, SearchResult result) {
            if (e != null) {
              callback.onFailure(new IOException(e));
            } else {
              callback.onSuccess(result);
            }
          }
        });
  }

  @Override
  public String getDefaultQuery() {
    // Show work-safe images by default.
    return "safe, score.gte:50";
  }

  @Override
  public Settings getSettings() {
    return new Settings(Settings.APIType.DERPIBOORU, name, apiEndpoint, username, apiKey);
  }

  @Override
  public AuthenticationType requiresAuthentication() {
    return AuthenticationType.OPTIONAL;
  }
  //endregion

  //region Creating search URLs
  /**
   * Generate request URL to the search API endpoint.
   *
   * @param tags  Space-separated tags.
   * @param pid   Page number (0-indexed).
   * @param limit Images to fetch per page.
   * @return URL to search results API.
   */
  protected String createSearchURL(String tags, int pid, int limit) {
    // Page numbers are 1-indexed for this API.
    final int page = pid + 1;

    return new DerpibooruQueryBuilder()
            .tags(tags)
            .endpoint(this.apiEndpoint)
            .page(page)
            .limit(limit)
            .apiKey(this.apiKey)
            .build();
  }
  //endregion

  //region Parsing responses
  /**
   * Parse an XML response returned by the API.
   *
   * @param body   HTTP Response body.
   * @param tags   Tags used to retrieve the response.
   * @param offset Current paging offset.
   * @return A {@link SearchResult} parsed from given XML.
   */
  protected SearchResult parseJSONResponse(String body, String tags, int offset) throws IOException {
    // Create variables to hold the values as XML is being parsed.
    final List<Image> imageList = new ArrayList<>(DEFAULT_LIMIT);
    int position = 0;

    Gson gson = new Gson();
    DerpibooruSearchResult searchResult = gson.fromJson(body, DerpibooruSearchResult.class);

    try {
      for (DerpibooruImage result : searchResult.search) {
        Image image = new Image();

        image.fileUrl = "https:" + result.representations.full;
        image.width = result.width;
        image.height = result.height;

        image.previewUrl = "https:" + result.representations.thumb;
        image.previewWidth = THUMBNAIL_SIZE;
        image.previewHeight = THUMBNAIL_SIZE;

        image.sampleUrl = "https:" + result.representations.large;
        image.sampleHeight = SAMPLE_SIZE;
        image.sampleWidth = SAMPLE_SIZE;

        image.tags = parseTagString(result.tags);
        image.id = Integer.toString(result.id);
        image.webUrl = webUrlFromId(result.id);
        image.source = result.source_url;
        image.md5 = result.sha512_hash; // TODO find out if this matters or if I need to add a field
        image.searchPage = offset;
        image.searchPagePosition = position;
        image.safeSearchRating = ratingFromTagIdList(result.tag_ids);

        image.score = result.score;
        image.createdAt = dateFromString(result.created_at);

        imageList.add(image);
        position++;
      }

    } catch (NullPointerException | ParseException e) {
      // Convert into IOException.
      // Needed for consistent method signatures in the SearchClient interface for different APIs.
      // (Gson replaces missing object fields with null, so if we encounter a critical field that's
      // null we'll just throw the IOException because the result was bad.)
      throw new IOException(e);
    }

    return new SearchResult(imageList.toArray(new Image[imageList.size()]), Tag.arrayFromString(tags), offset);
  }

  /**
   * Create a {@link Date} object from String date representation used by this API.
   *
   * @param date Date string.
   * @return Date converted from given String.
   */
  protected static Date dateFromString(String date) throws ParseException {
    // 2019-11-20T17:25:21.875Z
    final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.US);

    // Normalise the ISO8601 time zone into a format parse-able by SimpleDateFormat.
    if (!TextUtils.isEmpty(date)) {
      String newDate = date.replace("Z", "+0000");
      return DATE_FORMAT.parse(newDate);
    }

    return null;
  }

  /**
   * Get a URL viewable in the system web browser for given Image ID.
   *
   * @param id {@link Image} ID.
   * @return URL for viewing the image in the browser.
   */
  protected String webUrlFromId(int id) {
    return String.format(Locale.US, "%s/%d", apiEndpoint, id);
  }

  /**
   * Parse out the tags from a comma separated list.
   *
   * @param input Comma-separated string of tags.
   * @return Array of Tag objects {@link Tag}.
   */
  protected Tag[] parseTagString(String input) {
    String[] tags = input.split(",\\s*");
    ArrayList<Tag> outputList = new ArrayList<Tag>(tags.length);

    for (String tag : tags) {
      Tag.Type type = getTagType(tag);
      outputList.add(new Tag(tag, type));
    }

    return outputList.toArray(new Tag[outputList.size()]);
  }

  /**
   * Get the type for a particular tag.
   *
   * @param tag Tag to determine type for
   * @return {@link Tag.Type} of tag.
   */
  protected Tag.Type getTagType(String tag) {
    if (POPULAR_CHARACTERS.contains(tag)) {
      return Tag.Type.CHARACTER;
    }

    if (tag.startsWith("artist:") || tag.startsWith("editor:")) {
      return Tag.Type.ARTIST;
    }

    if (tag.startsWith("oc:")) {
      return Tag.Type.CHARACTER;
    }

    return Tag.Type.GENERAL;
  }

  /**
   * Determine the safe search rating of the image from the list of tag IDs.
   * There are only four rating tags on Derpibooru and one of them is mandatory
   * on each image, so it's easy to identify them by ID.
   *
   * 40482 = Safe -> S
   * 43502 = Suggestive -> Q
   * 39068 = Questionable -> Q
   * 26707 = Explicit -> E
   *
   * @param tags List of integer tag IDs.
   * @return Rating value for the Image.
   */
  protected Image.SafeSearchRating ratingFromTagIdList(int[] tags) {
    for (int tag : tags) {
      switch(tag) {
        case 40482:
          return Image.SafeSearchRating.S;
        case 43502:
        case 39068:
          return Image.SafeSearchRating.Q;
        case 26707:
          return Image.SafeSearchRating.E;
      }
    }

    return Image.SafeSearchRating.U;
  }
  //endregion

  //region Ion async SearchResult parser
  /** Asynchronous search parser to use with ion. */
  protected class SearchResultParser implements AsyncParser<SearchResult> {
    /** Tags searched for. */
    private final String tags;
    /** Current page offset. */
    private final int pageOffset;

    public SearchResultParser(String tags, int pageOffset) {
      this.tags = tags;
      this.pageOffset = pageOffset;
    }

    @Override
    public Future<SearchResult> parse(DataEmitter emitter) {
      return new StringParser().parse(emitter)
          .then(new TransformFuture<SearchResult, String>() {
            @Override
            protected void transform(String result) throws Exception {
              setComplete(parseJSONResponse(result, tags, pageOffset));
            }
          });
    }

    @Override
    public void write(DataSink sink, SearchResult value, CompletedCallback completed) {
      // Not implemented.
    }

    @Override
    public Type getType() {
      return null;
    }
  }
  //endregion

  //region Derpibooru JSON destination classes
  protected class DerpibooruSearchResult {
    public DerpibooruImage[] search;
    public int total;

    DerpibooruSearchResult() {}
  }

  protected class DerpibooruImage {
    public int id;
    public String created_at;
    public String updated_at;
    public String first_seen_at;
    public String tags;
    public int[] tag_ids;
    public int uploader_id;
    public int score;
    public int comment_count;
    public int width;
    public int height;
    public String file_name;
    public String description;
    public String uploader;
    public String image;
    public int upvotes;
    public int downvotes;
    public int faves;
    public float aspect_ratio;
    public String original_format;
    public String mime_type;
    public String sha512_hash;
    public String orig_sha512_hash;
    public String source_url;
    public DerpibooruImageRepresentations representations;
    public Boolean is_rendered;
    public Boolean is_optimized;

    DerpibooruImage() {}
  }

  protected class DerpibooruImageRepresentations {
    public String thumb_tiny;
    public String thumb_small;
    public String thumb;
    public String small;
    public String medium;
    public String large;
    public String tall;
    public String full;

    DerpibooruImageRepresentations() {}
  }
  //endregion

  //region Derpibooru Query Builder class
  protected class DerpibooruQueryBuilder {
    /** API Endpoint. */
    @NonNull String endpoint = "https://derpibooru.org";
    /** Comma-separated list of tags. */
    @NonNull String tags = "*";
    /** User's API Key. */
    @Nullable private String apiKey;
    /** Page number to request. */
    private int page = 0;
    /** Number of results per page. */
    private int limit = 50;
    /**
     * Sort type to use. Corresponds directly with the accepted values of Derpibooru's
     * '&sf=' parameter. This can be 'score', 'wilson', 'random', etc. Automatically
     * parsed out of the query, but can be overridden.
     */
    @Nullable private String sortType;
    /**
     * If an API Key is not required for a given query, it is not sent, so the filter
     * does not get overridden by the user's default filter.
     */
    private boolean keyRequired = false;
    /**
     * Filter to use for the query. The default filter used is Everything (56027),
     * which filters out nothing.
     */
    private int filterID = 56027;

    /**
     * Assign the tags to the query and parse out if the API key is required or the
     * tag list contains any synthetic tags.
     *
     * @param tags Comma-separated list of tags.
     * @return Itself.
     */
    public DerpibooruQueryBuilder tags(String tags) {
      String[] tagList = tags.split(",\\s*");
      StringBuilder tagBuilder = new StringBuilder();

      for (int i = 0; i < tagList.length; i++) {
        String tag = tagList[i];
        boolean skipTag = false;

        if (tag.startsWith("my:")) {
          this.keyRequired = true;
        }

        if (tag.startsWith("sort:")) {
          // Sort tags are synthetic and should be stripped from the query.
          this.sortType = tag.substring(5);
          skipTag = true;
        }

        // Strip synthetic tags from the output by only writing good tags to the output
        // stringbuilder.
        if (!skipTag) {
          if (i != 0) {
            tagBuilder.append(",");
          }
          tagBuilder.append(Uri.encode(tag));
        }
      }

      this.tags = tagBuilder.toString();

      return this;
    }

      /**
       * Assign the API endpoint to the query.
       *
       * @param endpoint The base URL for the query.
       * @return Itself.
       */
    public DerpibooruQueryBuilder endpoint(String endpoint) {
        this.endpoint = endpoint;
        return this;
    }

    /**
     * Assign the API key to the query. Only sent with a request if it's necessary.
     *
     * @param apiKey The user's API key.
     * @return Itself.
     */
    public DerpibooruQueryBuilder apiKey(String apiKey) {
      this.apiKey = apiKey;
      return this;
    }

    /**
     * Assign the page number to the query.
     *
     * @param page Page number, 1-indexed.
     * @return Itself.
     */
    public DerpibooruQueryBuilder page(int page) {
      this.page = page;
      return this;
    }

    /**
     * Assign the number of results per page to the query.
     *
     * @param limit Number of results to return per page. Maximum value is 50.
     * @return Itself.
     */
    public DerpibooruQueryBuilder limit(int limit) {
      this.limit = limit;
      return this;
    }

    /**
     * Assign the sort type to the query.
     *
     * @param sortType Sort type to use. Passed directly into the URL parameters.
     * @return Itself.
     */
    public DerpibooruQueryBuilder sortType(String sortType) {
      this.sortType = sortType;
      return this;
    }

    /**
     * Build the final query.
     *
     * @return A URL that will execute the requested query.
     */
    public String build() {
      StringBuilder queryBuilder = new StringBuilder(this.endpoint + "/search.json?");

      queryBuilder.append("q=");
      queryBuilder.append(this.tags);
      queryBuilder.append("&page=");
      queryBuilder.append(this.page);
      queryBuilder.append("&perpage=");
      queryBuilder.append(this.limit);
      queryBuilder.append("&filter_id=");
      queryBuilder.append(this.filterID);
      if (!TextUtils.isEmpty(this.sortType)) {
        queryBuilder.append("&sf=");
        queryBuilder.append(this.sortType);
      }
      if (this.keyRequired && !TextUtils.isEmpty(this.apiKey)) {
        queryBuilder.append("&key=");
        queryBuilder.append(this.apiKey);
      }

      return queryBuilder.toString();
    }
  }
  //endregion
}
