package com.example.proyectounificadas.Service;

import com.example.proyectounificadas.Entity.Video;
import com.example.proyectounificadas.Repository.VideoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.UUID;

@Service
public class VideoService {
    private final VideoRepository videoRepository;
    private final Path videoStoragePath = Paths.get("/home/unbotcomotu/Desktop/videos");
    private final Path thumbnailStoragePath = Paths.get("/home/unbotcomotu/Desktop/thumbnails");

    public VideoService(VideoRepository videoRepository) {
        this.videoRepository = videoRepository;
    }

    public String processVideo(MultipartFile file) throws IOException, InterruptedException {
        Files.createDirectories(videoStoragePath);
        Files.createDirectories(thumbnailStoragePath);
        Path videoFilePath = videoStoragePath.resolve(file.getOriginalFilename());
        file.transferTo(videoFilePath);
        String manifestFileName = UUID.randomUUID().toString() + ".m3u8";
        Path manifestFilePath = videoStoragePath.resolve(manifestFileName);
        ProcessBuilder processBuilder = new ProcessBuilder(
                "ffmpeg",
                "-i", videoFilePath.toString(),
                "-codec", "copy",
                "-start_number", "0",
                "-hls_time", "10",
                "-hls_list_size", "0",
                "-f", "hls",
                manifestFilePath.toString()
        );
        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;
        while ((line = reader.readLine()) != null) {
            System.out.println(line);
        }
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("ffmpeg process failed with exit code " + exitCode);
        }
        generateThumbnail(videoFilePath.toString(), thumbnailStoragePath.resolve(videoFilePath.getFileName() + ".jpg").toString());
        Video video = new Video();
        video.setOriginalFilename(file.getOriginalFilename());
        video.setManifestFilename(manifestFileName);
        videoRepository.save(video);
        return "/stream/hls/" + manifestFileName;
    }

    private void generateThumbnail(String videoFilePath, String thumbnailFilePath) throws IOException, InterruptedException {
        ProcessBuilder processBuilder = new ProcessBuilder(
                "ffmpeg",
                "-i", videoFilePath,
                "-ss", "00:00:01.000",
                "-vframes", "1",
                thumbnailFilePath
        );
        Process process = processBuilder.start();
        process.waitFor();
    }

    public Iterable<Video> listAllVideos() {
        return videoRepository.findAll();
    }

    public Video getVideo(Long id) {
        return videoRepository.findById(id).orElse(null);
    }

    @Transactional
    public void deleteVideo(Long id) throws Exception {
        Optional<Video> optionalVideo = videoRepository.findById(id);
        if (optionalVideo.isPresent()) {
            Video video = optionalVideo.get();
            Path videoPath = Paths.get(String.valueOf(videoStoragePath), video.getManifestFilename());
            Path videoDirectory = videoPath.getParent();

            try {
                Files.walk(videoDirectory)
                        .filter(filePath -> filePath.toString().contains(video.getManifestFilename().substring(0, video.getManifestFilename().lastIndexOf('.'))))
                        .forEach(filePath -> {
                            try {
                                Files.deleteIfExists(filePath);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        });
            } catch (IOException e) {
                e.printStackTrace();
            }

            Path thumbnailPath = Paths.get(String.valueOf(thumbnailStoragePath), video.getOriginalFilename() + ".jpg");
            Files.deleteIfExists(thumbnailPath);

            Path originalVideoPath = Paths.get(String.valueOf(videoStoragePath), video.getOriginalFilename());
            Files.deleteIfExists(originalVideoPath);

            videoRepository.deleteById(id);
        } else {
            throw new Exception("Video not found with id: " + id);
        }
    }

}
