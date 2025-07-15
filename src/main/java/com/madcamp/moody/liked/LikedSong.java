package com.madcamp.moody.liked;

import com.madcamp.moody.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "liked_songs")
@Getter
@Setter
@NoArgsConstructor
public class LikedSong {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "track_id", nullable = false, length = 100)
    private String trackId;

    @Column(name = "music_url", nullable = false, length = 512)
    private String musicUrl;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "artist", nullable = false, length = 255)
    private String artist;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
} 