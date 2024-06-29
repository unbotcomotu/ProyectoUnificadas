package com.example.proyectounificadas.Entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "video")
public class Video {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_video", nullable = false)
    private Integer id;

    @Column(name = "original_filename", nullable = false, length = 256)
    private String originalFilename;

    @Column(name = "manifest_filename", nullable = false, length = 256)
    private String manifestFilename;

}