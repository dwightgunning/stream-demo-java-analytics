package io.getstream.streamDemoJavaAnalytics;

import java.io.IOException;
import java.util.Date;
import java.util.ArrayList;
import java.util.List;

import io.getstream.client.apache.StreamClientImpl;
import io.getstream.client.config.ClientConfiguration;
import io.getstream.client.exception.InvalidFeedNameException;
import io.getstream.client.exception.StreamClientException;
import io.getstream.client.model.feeds.Feed;
import io.getstream.client.model.filters.FeedFilter;
import io.getstream.client.service.FlatActivityServiceImpl;
import io.getstream.client.StreamClient;

import io.getstream.streamDemoJavaAnalytics.TaggedActivity;
/**
 * Hello world!
 *
 */
public class App
{
    public static void main( String[] args )
    {
        StreamClient streamClient = new StreamClientImpl(
          new ClientConfiguration(),
          "<< TOKEN >>",
          "<< KEY >>");

        //// Content creation ////
        System.out.println("Example: Content creation...");

        // 1. Setup the article feed
        Feed allArticlesFeed;
        try {
            allArticlesFeed = streamClient.newFeed("articles", "all_articles");
        } catch (InvalidFeedNameException ifne) {
          throw new RuntimeException(ifne);
        }

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
        try {
          TaggedActivity response = allArticlesFeedActivityService.addActivity(articleActivity);
        } catch(StreamClientException sce) {
          throw new RuntimeException(sce);
        } catch (IOException ioe) {
          throw new RuntimeException(ioe);
        }

        // 4. Read the article feed (just to confirm our article activity was added)
        try {
          List<TaggedActivity> articleActivities = allArticlesFeedActivityService.getActivities(new FeedFilter.Builder().build()).getResults();
          System.out.println("Articles retrieved: " + articleActivities.size());
          System.out.println("---");

          // Print the article ids and tags
          int i = 0;
          for (TaggedActivity article : articleActivities) {
            System.out.println(++i + ". " + article.getForeignId() + " - " + article.getTags());
          }

        } catch(StreamClientException sce) {
          throw new RuntimeException(sce);
        } catch (IOException ioe) {
          throw new RuntimeException(ioe);
        }

        //// User Feeds ////
        System.out.println("Example: User feeds...");

        // 1a. Setup user timeline feed
        Feed userTimelineFeed;
        try {
            userTimelineFeed = streamClient.newFeed("user_timeline", "user1234");
        } catch (InvalidFeedNameException ifne) {
          throw new RuntimeException(ifne);
        }

        // 1a. Establish following relationship to article feed
        try {
          userTimelineFeed.follow("articles", "all_articles");
        } catch(StreamClientException sce) {
          throw new RuntimeException(sce);
        } catch (IOException ioe) {
          throw new RuntimeException(ioe);
        }

        // 2. Create an activity service for interacting with the feed
        FlatActivityServiceImpl<TaggedActivity> userTimelineFeedActivityService =
          userTimelineFeed.newFlatActivityService(TaggedActivity.class);

        // 3. Read the user article feed -- regular flat feed
        try {
          List<TaggedActivity> userArticleActivities = userTimelineFeedActivityService.getActivities(new FeedFilter.Builder().build()).getResults();
          System.out.println("User feed articles retrieved: " + userArticleActivities.size());
          System.out.println("---");

          // Print the article ids and tags
          int i = 0;
          for (TaggedActivity article : userArticleActivities) {
            System.out.println(++i + ". " + article.getForeignId() + " - " + article.getTags());
          }

        } catch(StreamClientException sce) {
          throw new RuntimeException(sce);
        } catch (IOException ioe) {
          throw new RuntimeException(ioe);
        }

        // 4. Read the user article feed -- with 'popularity' ranking
        // Note: custom ranking with label 'popularity' must already exist in Stream dashboard
        try {
          List<TaggedActivity> userArticleRankedActivities = userTimelineFeedActivityService.getActivities(new FeedFilter.Builder().withRanking("popularity").build()).getResults();

          System.out.println("User feed articles retrieved ranked: " + userArticleRankedActivities.size());
          System.out.println("---");

          // Print the article ids and tags
          int i = 0;
          for (TaggedActivity article : userArticleRankedActivities) {
            System.out.println(++i + ". " + article.getForeignId() + " - " + article.getTags());
          }

        } catch(StreamClientException sce) {
          throw new RuntimeException(sce);
        } catch (IOException ioe) {
          throw new RuntimeException(ioe);
        }

        //// Personalized Feeds ////

        // TODO: Implement Stream client for Personalized endpoints
    }
}
