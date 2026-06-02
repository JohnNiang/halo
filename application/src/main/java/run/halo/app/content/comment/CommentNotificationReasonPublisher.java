package run.halo.app.content.comment;

import static org.apache.commons.lang3.StringUtils.defaultIfBlank;

import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import run.halo.app.content.NotificationReasonConst;
import run.halo.app.core.extension.User;
import run.halo.app.core.extension.content.Comment;
import run.halo.app.core.extension.content.Post;
import run.halo.app.core.extension.content.SinglePage;
import run.halo.app.event.post.CommentCreatedEvent;
import run.halo.app.event.post.ReplyCreatedEvent;
import run.halo.app.extension.ExtensionClient;
import run.halo.app.extension.GroupVersionKind;
import run.halo.app.extension.Ref;
import run.halo.app.notification.NotificationRequest;
import run.halo.app.notification.NotificationService;

/**
 * Publishes notifications for new comments and replies.
 *
 * @author guqing
 * @since 2.9.0
 */
@Component
@RequiredArgsConstructor
public class CommentNotificationReasonPublisher {

    private static final GroupVersionKind POST_GVK = GroupVersionKind.fromExtension(Post.class);
    private static final GroupVersionKind PAGE_GVK = GroupVersionKind.fromExtension(SinglePage.class);

    private final ExtensionClient client;
    private final NotificationService notificationService;

    @Async
    @EventListener(CommentCreatedEvent.class)
    public void onNewComment(CommentCreatedEvent event) {
        var comment = event.getComment();
        var subjectRef = comment.getSpec().getSubjectRef();

        if (Ref.groupKindEquals(subjectRef, POST_GVK)) {
            publishForPost(comment);
        } else if (Ref.groupKindEquals(subjectRef, PAGE_GVK)) {
            publishForPage(comment);
        }
    }

    @Async
    @EventListener(ReplyCreatedEvent.class)
    public void onNewReply(ReplyCreatedEvent event) {
        var reply = event.getReply();
        var commentName = reply.getSpec().getCommentName();
        client.fetch(Comment.class, commentName).ifPresent(comment -> {
            var replyOwner = reply.getSpec().getOwner();
            var commentOwner = comment.getSpec().getOwner();

            if (isOwnerEqual(replyOwner, commentOwner)) {
                return;
            }

            var recipient = toUsername(commentOwner);
            if (recipient == null) {
                return;
            }

            var replyDisplayName = defaultIfBlank(replyOwner.getDisplayName(), replyOwner.getName());
            var content = reply.getSpec().getContent();

            notificationService
                    .notify(new NotificationRequest(
                            Set.of(recipient),
                            NotificationReasonConst.SOMEONE_REPLIED_TO_YOU,
                            "notification.someone-replied-to-you",
                            Map.of("replier", replyDisplayName, "content", content),
                            null))
                    .subscribe();
        });
    }

    private void publishForPost(Comment comment) {
        var subjectRef = comment.getSpec().getSubjectRef();
        var post = client.fetch(Post.class, subjectRef.getName()).orElse(null);
        if (post == null) {
            return;
        }
        var postOwner = post.getSpec().getOwner();
        var commentOwner = comment.getSpec().getOwner();

        if (isEmailOwnerEqual(commentOwner, postOwner, post)) {
            return;
        }

        var recipient = toUsernameFromPostOwner(postOwner, post);
        if (recipient == null) {
            return;
        }

        var displayName = defaultIfBlank(commentOwner.getDisplayName(), commentOwner.getName());

        notificationService
                .notify(new NotificationRequest(
                        Set.of(recipient),
                        NotificationReasonConst.NEW_COMMENT_ON_POST,
                        "notification.new-comment-on-post",
                        Map.of(
                                "commenter",
                                displayName,
                                "postTitle",
                                post.getSpec().getTitle()),
                        post.getStatusOrDefault().getPermalink()))
                .subscribe();
    }

    private void publishForPage(Comment comment) {
        var subjectRef = comment.getSpec().getSubjectRef();
        var page = client.fetch(SinglePage.class, subjectRef.getName()).orElse(null);
        if (page == null) {
            return;
        }
        var pageOwner = page.getSpec().getOwner();
        var commentOwner = comment.getSpec().getOwner();

        if (isEmailOwnerEqual(commentOwner, pageOwner, page)) {
            return;
        }

        var recipient = toUsernameFromPageOwner(pageOwner, page);
        if (recipient == null) {
            return;
        }

        var displayName = defaultIfBlank(commentOwner.getDisplayName(), commentOwner.getName());

        notificationService
                .notify(new NotificationRequest(
                        Set.of(recipient),
                        NotificationReasonConst.NEW_COMMENT_ON_PAGE,
                        "notification.new-comment-on-single-page",
                        Map.of(
                                "commenter",
                                displayName,
                                "pageTitle",
                                page.getSpec().getTitle()),
                        page.getStatusOrDefault().getPermalink()))
                .subscribe();
    }

    private boolean isOwnerEqual(Comment.CommentOwner a, Comment.CommentOwner b) {
        return a.getKind().equals(b.getKind()) && a.getName().equals(b.getName());
    }

    private boolean isEmailOwnerEqual(Comment.CommentOwner commentOwner, String owner, Post post) {
        if (Comment.CommentOwner.KIND_EMAIL.equals(commentOwner.getKind())) {
            return client.fetch(User.class, owner)
                    .filter(user -> commentOwner.getName().equals(user.getSpec().getEmail()))
                    .isPresent();
        }
        return commentOwner.getName().equals(owner);
    }

    private boolean isEmailOwnerEqual(Comment.CommentOwner commentOwner, String owner, SinglePage page) {
        if (Comment.CommentOwner.KIND_EMAIL.equals(commentOwner.getKind())) {
            return client.fetch(User.class, owner)
                    .filter(user -> commentOwner.getName().equals(user.getSpec().getEmail()))
                    .isPresent();
        }
        return commentOwner.getName().equals(owner);
    }

    private String toUsername(Comment.CommentOwner owner) {
        if (Comment.CommentOwner.KIND_EMAIL.equals(owner.getKind())) {
            return null;
        }
        return owner.getName();
    }

    private String toUsernameFromPostOwner(String ownerName, Post post) {
        return ownerName;
    }

    private String toUsernameFromPageOwner(String ownerName, SinglePage page) {
        return ownerName;
    }
}
