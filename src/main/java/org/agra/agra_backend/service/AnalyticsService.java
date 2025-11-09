package org.agra.agra_backend.service;

import org.agra.agra_backend.dao.*;
import org.agra.agra_backend.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AnalyticsService {

    private final CourseRepository courseRepository;
    private final CourseProgressRepository courseProgressRepository;
    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final LikeRepository likeRepository;
    private final NotificationRepository notificationRepository;
    private final NotificationStatusRepository notificationStatusRepository;
    private final SimpUserRegistry simpUserRegistry;

    private static final Logger log = LoggerFactory.getLogger(AnalyticsService.class);

    public AnalyticsService(CourseRepository courseRepository,
                            CourseProgressRepository courseProgressRepository,
                            UserRepository userRepository,
                            PostRepository postRepository,
                            CommentRepository commentRepository,
                            LikeRepository likeRepository,
                            NotificationRepository notificationRepository,
                            NotificationStatusRepository notificationStatusRepository,
                            SimpUserRegistry simpUserRegistry) {
        this.courseRepository = courseRepository;
        this.courseProgressRepository = courseProgressRepository;
        this.userRepository = userRepository;
        this.postRepository = postRepository;
        this.commentRepository = commentRepository;
        this.likeRepository = likeRepository;
        this.notificationRepository = notificationRepository;
        this.notificationStatusRepository = notificationStatusRepository;
        this.simpUserRegistry = simpUserRegistry;
    }

    public Map<String, Object> getCourseStatusSummary() {
        List<Course> courses = courseRepository.findAll();
        long total = courses.size();
        long archived = courses.stream().filter(Course::isArchived).count();
        long published = total - archived; // No explicit draft flag in model

        log.info("Analytics: Course summary - total={}, published(non-archived)={}, archived={}", total, published, archived);
        if (total == 0) {
            log.info("Analytics: No courses found in database.");
        }
        // Helpful debug when numbers look off on the dashboard
        if (published == 0 || archived > 0) {
            for (Course c : courses) {
                log.debug("Analytics: Course id={}, title='{}', archived={}", c.getId(), c.getTitle(), c.isArchived());
            }
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("total", total);
        out.put("published", published);
        out.put("archived", archived);
        return out;
    }

    public List<Map<String, Object>> getEnrollmentsOverview(String granularity, Date start, Date end) {
        List<CourseProgress> all = courseProgressRepository.findAll();
        Map<Date, Long> buckets = all.stream()
                .map(cp -> cp.getEnrolledAt())
                .filter(Objects::nonNull)
                .filter(d -> start == null || !d.before(start))
                .filter(d -> end == null || !d.after(end))
                .map(d -> bucketDate(d, granularity))
                .collect(Collectors.groupingBy(d -> d, TreeMap::new, Collectors.counting()));

        return buckets.entrySet().stream()
                .map(e -> mapPoint(e.getKey(), e.getValue()))
                .collect(Collectors.toList());
    }

    public List<Map<String, Object>> getTopCourses(String metric, int limit) {
        List<Course> courses = courseRepository.findAll();
        Map<String, String> courseTitles = courses.stream()
                .collect(Collectors.toMap(Course::getId, Course::getTitle));

        List<CourseProgress> progress = courseProgressRepository.findAll();
        Map<String, Long> enrollmentsByCourse = progress.stream()
                .filter(cp -> cp.getCourseId() != null)
                .collect(Collectors.groupingBy(CourseProgress::getCourseId, Collectors.counting()));

        Map<String, long[]> completionStats = new HashMap<>(); // courseId -> [completed, total]
        for (CourseProgress cp : progress) {
            if (cp.getCourseId() == null) continue;
            long[] arr = completionStats.computeIfAbsent(cp.getCourseId(), k -> new long[2]);
            arr[1] += 1;
            if (cp.isCompleted()) arr[0] += 1;
        }

        List<Map<String, Object>> items = new ArrayList<>();
        for (String courseId : courseTitles.keySet()) {
            long enrollments = enrollmentsByCourse.getOrDefault(courseId, 0L);
            long[] stats = completionStats.getOrDefault(courseId, new long[]{0, 0});
            double completionRate = stats[1] == 0 ? 0.0 : (double) stats[0] / (double) stats[1];
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("courseId", courseId);
            row.put("title", courseTitles.get(courseId));
            row.put("enrollments", enrollments);
            row.put("completionRate", completionRate);
            items.add(row);
        }

        Comparator<Map<String, Object>> cmp;
        if ("completionRate".equalsIgnoreCase(metric)) {
            cmp = Comparator.comparingDouble(m -> ((Number) m.get("completionRate")).doubleValue());
        } else { // default: enrollments
            cmp = Comparator.comparingLong(m -> ((Number) m.get("enrollments")).longValue());
        }

        return items.stream()
                .sorted(cmp.reversed())
                .limit(Math.max(1, limit))
                .collect(Collectors.toList());
    }

    public List<Map<String, Object>> getCompletionRates() {
        List<Course> courses = courseRepository.findAll();
        List<CourseProgress> progress = courseProgressRepository.findAll();
        Map<String, long[]> statsByCourse = new HashMap<>(); // [completed, total]
        for (CourseProgress cp : progress) {
            if (cp.getCourseId() == null) continue;
            long[] arr = statsByCourse.computeIfAbsent(cp.getCourseId(), k -> new long[2]);
            arr[1] += 1;
            if (cp.isCompleted()) arr[0] += 1;
        }

        List<Map<String, Object>> out = new ArrayList<>();
        for (Course c : courses) {
            long[] stats = statsByCourse.getOrDefault(c.getId(), new long[]{0, 0});
            double rate = stats[1] == 0 ? 0.0 : (double) stats[0] / (double) stats[1];
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("courseId", c.getId());
            row.put("title", c.getTitle());
            row.put("completed", stats[0]);
            row.put("total", stats[1]);
            row.put("completionRate", rate);
            out.add(row);
        }
        return out;
    }

    public List<Map<String, Object>> getCertificatesIssuedOverview(String granularity, Date start, Date end) {
        List<CourseProgress> all = courseProgressRepository.findAll();
        // Define issuance as completed == true and certificateUrl not null/empty
        Map<Date, Long> buckets = all.stream()
                .filter(CourseProgress::isCompleted)
                .filter(cp -> cp.getCertificateUrl() != null && !cp.getCertificateUrl().isBlank())
                .map(this::deriveCompletionDate)
                .filter(Objects::nonNull)
                .filter(d -> start == null || !d.before(start))
                .filter(d -> end == null || !d.after(end))
                .map(d -> bucketDate(d, granularity))
                .collect(Collectors.groupingBy(d -> d, TreeMap::new, Collectors.counting()));

        return buckets.entrySet().stream()
                .map(e -> mapPoint(e.getKey(), e.getValue()))
                .collect(Collectors.toList());
    }

    private Date deriveCompletionDate(CourseProgress cp) {
        Date candidate = null;
        if (cp.getLessonCompletionDates() != null && !cp.getLessonCompletionDates().isEmpty()) {
            for (Date d : cp.getLessonCompletionDates().values()) {
                if (d == null) continue;
                if (candidate == null || d.after(candidate)) candidate = d;
            }
        }
        if (candidate == null) candidate = cp.getStartedAt();
        if (candidate == null) candidate = cp.getEnrolledAt();
        return candidate;
    }

    private Map<String, Object> mapPoint(Date date, long count) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("periodStart", date);
        m.put("count", count);
        return m;
    }

    private Date bucketDate(Date in, String granularity) {
        if (in == null) return null;
        String g = granularity == null ? "daily" : granularity.toLowerCase();
        ZonedDateTime z = ZonedDateTime.ofInstant(in.toInstant(), ZoneOffset.UTC);
        ZonedDateTime out;
        switch (g) {
            case "weekly":
                // Start of ISO week (Monday) at 00:00 UTC
                DayOfWeek dow = z.getDayOfWeek();
                int shift = dow.getValue() - DayOfWeek.MONDAY.getValue();
                out = z.minusDays(shift).withHour(0).withMinute(0).withSecond(0).withNano(0);
                break;
            case "monthly":
                out = z.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
                break;
            case "daily":
            default:
                out = z.withHour(0).withMinute(0).withSecond(0).withNano(0);
        }
        return Date.from(out.toInstant());
    }

    // ===== User Analytics =====

    public List<Map<String, Object>> getUserGrowth(String granularity, Date start, Date end) {
        List<User> users = userRepository.findAll();
        Map<Date, Long> buckets = users.stream()
                .map(User::getRegisteredAt)
                .filter(Objects::nonNull)
                .filter(d -> start == null || !d.before(start))
                .filter(d -> end == null || !d.after(end))
                .map(d -> bucketDate(d, granularity))
                .collect(Collectors.groupingBy(d -> d, TreeMap::new, Collectors.counting()));
        return buckets.entrySet().stream().map(e -> mapPoint(e.getKey(), e.getValue())).collect(Collectors.toList());
    }

    public Map<String, Long> getUserRolesBreakdown() {
        return userRepository.findAll().stream()
                .map(User::getRole)
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(r -> r, TreeMap::new, Collectors.counting()));
    }

    public Map<String, Long> getUserGeoBreakdown() {
        return userRepository.findAll().stream()
                .map(User::getCountry)
                .filter(Objects::nonNull)
                .filter(s -> !s.isBlank())
                .collect(Collectors.groupingBy(c -> c, TreeMap::new, Collectors.counting()));
    }

    public List<Map<String, Object>> getNewRegistrations(String granularity, Date start, Date end) {
        return getUserGrowth(granularity, start, end);
    }

    public Map<String, Object> getActiveVsInactiveUsers(int days) {
        Date threshold = Date.from(ZonedDateTime.now(ZoneOffset.UTC).minus(days, ChronoUnit.DAYS).toInstant());

        Set<String> activeUserIds = new HashSet<>();

        // Activity sources: enrollments/starts, posts, comments, likes
        for (CourseProgress cp : courseProgressRepository.findAll()) {
            if ((cp.getStartedAt() != null && !cp.getStartedAt().before(threshold)) ||
                (cp.getEnrolledAt() != null && !cp.getEnrolledAt().before(threshold))) {
                if (cp.getUserId() != null) activeUserIds.add(cp.getUserId());
            }
        }
        for (Post p : postRepository.findAll()) {
            if (p.getCreatedAt() != null && p.getCreatedAt().atZone(ZoneOffset.UTC).toInstant().isAfter(threshold.toInstant())) {
                if (p.getUserId() != null) activeUserIds.add(p.getUserId());
            }
        }
        for (Comment c : commentRepository.findAll()) {
            if (c.getCreatedAt() != null && c.getCreatedAt().atZone(ZoneOffset.UTC).toInstant().isAfter(threshold.toInstant())) {
                if (c.getUserId() != null) activeUserIds.add(c.getUserId());
            }
        }
        for (Like l : likeRepository.findAll()) {
            if (l.getCreatedAt() != null && l.getCreatedAt().atZone(ZoneOffset.UTC).toInstant().isAfter(threshold.toInstant())) {
                if (l.getUserId() != null) activeUserIds.add(l.getUserId());
            }
        }

        long totalUsers = userRepository.count();
        long active = activeUserIds.size();
        long inactive = Math.max(0, totalUsers - active);

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("windowDays", days);
        out.put("active", active);
        out.put("inactive", inactive);
        out.put("total", totalUsers);
        return out;
    }

    // ===== Social / Feed Analytics =====

    public List<Map<String, Object>> getPostsTrend(String granularity, Date start, Date end) {
        List<Post> posts = postRepository.findAll();
        Map<Date, Long> buckets = posts.stream()
                .map(Post::getCreatedAt)
                .filter(Objects::nonNull)
                .map(dt -> Date.from(dt.atZone(ZoneOffset.UTC).toInstant()))
                .filter(d -> start == null || !d.before(start))
                .filter(d -> end == null || !d.after(end))
                .map(d -> bucketDate(d, granularity))
                .collect(Collectors.groupingBy(d -> d, TreeMap::new, Collectors.counting()));
        return buckets.entrySet().stream().map(e -> mapPoint(e.getKey(), e.getValue())).collect(Collectors.toList());
    }

    public List<Map<String, Object>> getCommentsTrend(String granularity, Date start, Date end) {
        List<Comment> comments = commentRepository.findAll();
        Map<Date, Long> buckets = comments.stream()
                .map(Comment::getCreatedAt)
                .filter(Objects::nonNull)
                .map(dt -> Date.from(dt.atZone(ZoneOffset.UTC).toInstant()))
                .filter(d -> start == null || !d.before(start))
                .filter(d -> end == null || !d.after(end))
                .map(d -> bucketDate(d, granularity))
                .collect(Collectors.groupingBy(d -> d, TreeMap::new, Collectors.counting()));
        return buckets.entrySet().stream().map(e -> mapPoint(e.getKey(), e.getValue())).collect(Collectors.toList());
    }

    public List<Map<String, Object>> getTopPostsByEngagement(int limit) {
        return postRepository.findAll().stream()
                .map(p -> {
                    long likes = p.getLikesCount() == null ? 0L : p.getLikesCount();
                    long comments = p.getCommentsCount() == null ? 0L : p.getCommentsCount();
                    long engagement = likes + comments;
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("postId", p.getId());
                    m.put("likes", likes);
                    m.put("comments", comments);
                    m.put("engagement", engagement);
                    m.put("createdAt", p.getCreatedAt());
                    m.put("isCoursePost", Boolean.TRUE.equals(p.getIsCoursePost()));
                    return m;
                })
                .sorted(Comparator.<Map<String, Object>>comparingLong(m -> ((Number) m.get("engagement")).longValue()).reversed())
                .limit(Math.max(1, limit))
                .collect(Collectors.toList());
    }

    public Map<String, Object> getUserEngagementAverages() {
        List<Like> likes = likeRepository.findAll();
        List<Comment> comments = commentRepository.findAll();
        Map<String, Long> likesPerUser = likes.stream()
                .filter(l -> l.getUserId() != null)
                .collect(Collectors.groupingBy(Like::getUserId, Collectors.counting()));
        Map<String, Long> commentsPerUser = comments.stream()
                .filter(c -> c.getUserId() != null)
                .collect(Collectors.groupingBy(Comment::getUserId, Collectors.counting()));

        long userCount = Math.max(1, userRepository.count());
        double avgLikes = likesPerUser.values().stream().mapToLong(Long::longValue).sum() / (double) userCount;
        double avgComments = commentsPerUser.values().stream().mapToLong(Long::longValue).sum() / (double) userCount;

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("avgLikesPerUser", avgLikes);
        out.put("avgCommentsPerUser", avgComments);
        return out;
    }

    public Map<String, Object> getFeaturedPostsPerformance() {
        List<Post> posts = postRepository.findAll();
        long featuredCount = 0, featuredEng = 0, regularCount = 0, regularEng = 0;
        for (Post p : posts) {
            long likes = p.getLikesCount() == null ? 0L : p.getLikesCount();
            long comments = p.getCommentsCount() == null ? 0L : p.getCommentsCount();
            long engagement = likes + comments;
            if (Boolean.TRUE.equals(p.getIsCoursePost())) {
                featuredCount++;
                featuredEng += engagement;
            } else {
                regularCount++;
                regularEng += engagement;
            }
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("featuredAvgEngagement", featuredCount == 0 ? 0.0 : (double) featuredEng / featuredCount);
        out.put("regularAvgEngagement", regularCount == 0 ? 0.0 : (double) regularEng / regularCount);
        out.put("featuredCount", featuredCount);
        out.put("regularCount", regularCount);
        return out;
    }

    // ===== Notifications & Activity =====

    public List<Map<String, Object>> getNotificationsTrend(String granularity, Date start, Date end) {
        List<Notification> items = notificationRepository.findAll();
        Map<Date, Long> buckets = items.stream()
                .map(Notification::getTimestamp)
                .filter(Objects::nonNull)
                .map(dt -> Date.from(dt.atZone(ZoneOffset.UTC).toInstant()))
                .filter(d -> start == null || !d.before(start))
                .filter(d -> end == null || !d.after(end))
                .map(d -> bucketDate(d, granularity))
                .collect(Collectors.groupingBy(d -> d, TreeMap::new, Collectors.counting()));
        return buckets.entrySet().stream().map(e -> mapPoint(e.getKey(), e.getValue())).collect(Collectors.toList());
    }

    public Map<String, Object> getWebSocketActivity() {
        Map<String, Object> out = new LinkedHashMap<>();
        // Number of unique connected users per SimpUserRegistry
        int connectedUsers = simpUserRegistry.getUserCount();
        out.put("connectedUsers", connectedUsers);
        return out;
    }

    public Map<String, Object> getNotificationReadStatus() {
        List<NotificationStatus> statuses = notificationStatusRepository.findAll();
        long total = statuses.size();
        long read = statuses.stream().filter(NotificationStatus::isSeen).count();
        long unread = total - read;
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("read", read);
        out.put("unread", unread);
        out.put("total", total);
        out.put("readPercentage", total == 0 ? 0.0 : (read * 100.0 / total));
        return out;
    }

    public Map<String, Long> getTopNotificationTypes() {
        return notificationRepository.findAll().stream()
                .map(Notification::getType)
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(Enum::name, TreeMap::new, Collectors.counting()));
    }
}
