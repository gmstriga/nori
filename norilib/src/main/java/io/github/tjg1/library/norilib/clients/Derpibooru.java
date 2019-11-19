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
import java.io.StringReader;
import java.lang.reflect.Type;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
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
/* TODO Parse out the tag string here into a list before moving further, so it can be checked for
        special values that require the API key (once API keys are implemented) and special values
        that change the URI parameters (like sort keys).
 */
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

    // TODO Add check here for whether search *requires* an API key
    if (!TextUtils.isEmpty(this.username) && !TextUtils.isEmpty(this.apiKey)) {
      return String.format(Locale.US, apiEndpoint + "/search.json?q=%s&page=%d&perpage=%d&key=%s",
          Uri.encode(tags), page, limit, Uri.encode(this.username), Uri.encode(this.apiKey));
    }
    return String.format(Locale.US, apiEndpoint + "/search.json?q=%s&page=%d&perpage=%d", Uri.encode(tags), page, limit);
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
  @SuppressWarnings("FeatureEnvy")
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

/* TODO Try and assign my tags types as a value-add feature */
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
      outputList.add(new Tag(tag));
    }

    return outputList.toArray(new Tag[outputList.size()]);
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
}
