package io.getstream.demo;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.getstream.client.StreamClient;
import io.getstream.client.apache.StreamClientImpl;
import io.getstream.client.apache.repo.utils.UriBuilder;
import io.getstream.client.config.ClientConfiguration;
import io.getstream.client.exception.StreamClientException;
import io.getstream.client.model.feeds.Feed;
import io.getstream.client.model.filters.FeedFilter;
import io.getstream.client.service.FlatActivityServiceImpl;
import org.apache.http.HttpRequest;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.getstream.client.util.JwtAuthenticationUtil.ALL;
import static io.getstream.client.util.JwtAuthenticationUtil.generateToken;
import static org.apache.http.entity.ContentType.APPLICATION_JSON;

public class App {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .setPropertyNamingStrategy(PropertyNamingStrategy.CAMEL_CASE_TO_LOWER_CASE_WITH_UNDERSCORES)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);

    private static final String HEADER_AUTHORIZATION = "Authorization";
    private static final String HEADER_AUTH_TYPE = "stream-auth-type";

    public static final String META_ENDPOINT = "http://ml-api.staging.gtstrm.com:85/niveza/";
    public static final String STREAM_API_KEY = "";
    public static final String STREAM_SECRET_KEY = "";

    public static void main(String[] args) throws StreamClientException, IOException {
        StreamClient streamClient = new StreamClientImpl(
                new ClientConfiguration(), STREAM_API_KEY, STREAM_SECRET_KEY);

        /**
         * Content creation
         */
        System.out.println("Example: Content creation...");

        // 1. Setup the article feed
        Feed allArticlesFeed = streamClient.newFeed("articles", "all_articles");

        // 2. Create an activity service for interacting with the feed
        FlatActivityServiceImpl<TaggedActivity> allArticlesFeedActivityService =
                allArticlesFeed.newFlatActivityService(TaggedActivity.class);

        // 3a. Create an article activity
        TaggedActivity articleActivity = new TaggedActivity();
        articleActivity.setActor("author:123");
        articleActivity.setObject("article:10");
        articleActivity.setVerb("add");
        articleActivity.setTime(new Date());
        articleActivity.setForeignId("article:10");
        List<String> tags = new ArrayList<String>();
        tags.add("stocks");
        tags.add("research");
        tags.add("beginners");
        articleActivity.setTags(tags);

        // 3b. Add the article activity
        TaggedActivity response = allArticlesFeedActivityService.addActivity(articleActivity);

        // 4. Read the article feed (just to confirm our article activity was added)
        List<TaggedActivity> articleActivities = allArticlesFeedActivityService.getActivities(new FeedFilter.Builder().build()).getResults();
        System.out.println("Articles retrieved: " + articleActivities.size());
        System.out.println("---");

        // Print the article ids and tags
        for (int i = 0; i < articleActivities.size(); i++) {
            TaggedActivity article = articleActivities.get(i);
            System.out.format("%d. %s - %s\n", i, article.getForeignId(), article.getTags());
        }


        /**
         * User Feeds
         */
        System.out.println("Example: User feeds...");

        // 1a. Setup user timeline feed
        Feed userTimelineFeed;
        userTimelineFeed = streamClient.newFeed("user_timeline", "1234");

        // 1a. Establish following relationship to article feed
        userTimelineFeed.follow("articles", "all_articles");

        // 2. Create an activity service for interacting with the feed
        FlatActivityServiceImpl<TaggedActivity> userTimelineFeedActivityService =
                userTimelineFeed.newFlatActivityService(TaggedActivity.class);

        // 3. Read the user article feed -- regular flat feed
        List<TaggedActivity> userArticleActivities = userTimelineFeedActivityService.getActivities(new FeedFilter.Builder().build()).getResults();
        System.out.println("User feed articles retrieved: " + userArticleActivities.size());
        System.out.println("---");

        // Print the article ids and tags
        for (int i = 0; i < userArticleActivities.size(); i++) {
            TaggedActivity article = userArticleActivities.get(i);
            System.out.format("%d. %s - %s\n", i, article.getForeignId(), article.getTags());
        }

        // 4. Read the user article feed -- with 'popularity' ranking
        // Note: custom ranking with label 'popularity' must already exist in Stream dashboard
        List<TaggedActivity> userArticleRankedActivities = userTimelineFeedActivityService.getActivities(new FeedFilter.Builder().withRanking("popularity").build()).getResults();

        System.out.println("User feed articles retrieved ranked: " + userArticleRankedActivities.size());
        System.out.println("---");

        // Print the article ids and tags
        for (int i = 0; i < userArticleRankedActivities.size(); i++) {
            TaggedActivity article = userArticleActivities.get(i);
            System.out.format("%d. %s - %s\n", i, article.getForeignId(), article.getTags());
        }

        App app = new App();
        CloseableHttpClient client = HttpClients.createDefault();
        app.sendMetadata(client, "user_timeline:1234");
        app.readPersonalizedFeed(client, "1234");
    }

    public void sendMetadata(CloseableHttpClient httpClient, String userId) throws IOException {
        HttpPost request = new HttpPost(UriBuilder.fromEndpoint(META_ENDPOINT)
                .path("meta/")
                .queryParam("api_key", STREAM_API_KEY)
                .build());
        System.out.format("Invoking url: '%s'", request.getURI());

        Map<String, Metadata> metadatas = new HashMap<>();
        metadatas.put(userId, new Metadata("ATX", "IBEX"));

        request.setEntity(new StringEntity(
                OBJECT_MAPPER.writeValueAsString(Collections.singletonMap("data", metadatas)),
                APPLICATION_JSON));

        request = (HttpPost) addJwtAuthentication(generateToken(STREAM_SECRET_KEY, ALL, ALL, null, ALL), request);

        try (CloseableHttpResponse response = httpClient.execute(request, HttpClientContext.create())) {
            System.out.println(EntityUtils.toString(response.getEntity()));
        }
    }

    public void readPersonalizedFeed(CloseableHttpClient httpClient, String userId) throws IOException {
        HttpGet request = new HttpGet(UriBuilder.fromEndpoint(META_ENDPOINT)
                .path("personalized_feed/")
                .path(userId + "/")
                .queryParam("api_key", STREAM_API_KEY)
                .build());
        System.out.format("Invoking url: '%s'", request.getURI());

        request = (HttpGet) addJwtAuthentication(generateToken(STREAM_SECRET_KEY, ALL, ALL, null, ALL), request);

        try (CloseableHttpResponse response = httpClient.execute(request, HttpClientContext.create())) {
            System.out.println(EntityUtils.toString(response.getEntity()));
        }
    }

    public static HttpRequest addJwtAuthentication(String token, HttpRequest request) {
        request.addHeader(HEADER_AUTHORIZATION, token);
        request.addHeader(HEADER_AUTH_TYPE, "jwt");
        return request;
    }
}