package rnd.social.facebook;

import java.util.List;

import com.restfb.Connection;
import com.restfb.DefaultFacebookClient;
import com.restfb.FacebookClient;
import com.restfb.FacebookClient.AccessToken;
import com.restfb.Version;
import com.restfb.types.Comment;
import com.restfb.types.Insight;
import com.restfb.types.Page;
import com.restfb.types.Post;
import com.restfb.types.Post.Comments;

public class TestRestFB {

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		// String accessToken = "1807055309520167|vg-xBHpXoEe3SfoiB9mYyBeG-Gg";
		String accessToken = "CAAZArgdnQPScBALX9ZCEWVhb98Vj1pZB23mM0Tbvk8iIA1uIfqeJi1XrRiFYT0zxchrreJwvEfaIO56wtIBTGVWPSEOhs6p56psskdpSB1uSu9oBlVolvLRAcNJR1rTYC8hgegA1JqTfq0UB1DGu8nA4liL15aJqZAxkDy8URYHhPZBX3boVJMdBUYYWMaRuUq8SWbElFaI8ZCITZBSRU5NDVbYnSBXDtwu5bZA1ZACCFJAZDZD";

		FacebookClient facebookClient = new DefaultFacebookClient(accessToken, Version.VERSION_2_2);

		// AccessToken xAccessToken = facebookClient.obtainExtendedAccessToken("1807055309520167", "050b6f0b301cde42a9ebf91c72e227b3", accessToken);
		// System.out.println("My extended access token: " + xAccessToken);

		Page page = facebookClient.fetchObject("CSCNoidaConfession", Page.class);
		long likes = page.getLikes();
		System.out.println("likes:" + likes);

		// Connection<Insight> insights = facebookClient.fetchConnection("CSCNoidaConfession/insights", Insight.class);
		// for (Insight insight : insights.getData())
		// System.out.println(insight.getName() + ":" + insight.getValues());

		Connection<Post> posts = facebookClient.fetchConnection("CSCNoidaConfession/feed", Post.class);
		List<Post> postsData = posts.getData();

		System.out.println("post-count:" + postsData.size());

		// for (Post post : data)
		// System.out.println(post.getMessage());

		Post post = posts.getData().get(0);

		System.out.println("message:" + post.getMessage());

		Comments comments = post.getComments();
		List<Comment> commentsData = comments.getData();
		
		System.out.println("Comments:");
		
		for (Comment comment : commentsData)
			System.out.println(comment.getFrom().getName() + ":" + comment.getMessage());

	}

}
