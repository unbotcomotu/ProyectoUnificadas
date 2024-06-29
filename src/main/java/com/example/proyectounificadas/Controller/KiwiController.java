package com.example.proyectounificadas.Controller;

import com.example.proyectounificadas.Entity.Video;
import com.example.proyectounificadas.Service.VideoService;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;

@Controller
public class KiwiController {

    private final VideoService videoService;
    private final Path videoStoragePath = Paths.get("/home/unbotcomotu/Desktop/videos");

    public KiwiController(VideoService videoService) {
        this.videoService = videoService;
    }

    @GetMapping("/videos")
    public String listVideos(Model model) {
        model.addAttribute("videos", videoService.listAllVideos());
        return "videos";
    }

    @GetMapping("/video/{id}")
    public String getVideo(@PathVariable Long id, Model model) {
        Video video = videoService.getVideo(id);
        model.addAttribute("video", video);
        return "video";
    }

    @PostMapping("/upload")
    public String uploadVideo(@RequestParam(value = "file",required = false) MultipartFile file, Model model) {
        try {
            videoService.processVideo(file);
        } catch (Exception e) {
            model.addAttribute("error", "Error processing video");
            return "videos";
        }
        return "redirect:/videos";
    }


    @ResponseBody
    @GetMapping("/stream/hls/{filename}")
    public ResponseEntity<Resource> getHlsSegment(@PathVariable String filename) throws MalformedURLException {
        Path path = videoStoragePath.resolve(filename);
        Resource resource = new UrlResource(path.toUri());
        return ResponseEntity.ok()
                .contentType(MediaTypeFactory.getMediaType(resource).orElse(MediaType.APPLICATION_OCTET_STREAM))
                .body(resource);
    }

    private final Path thumbnailStoragePath = Paths.get("/home/unbotcomotu/Desktop/thumbnails");

    @ResponseBody
    @GetMapping("/thumbnails/{filename}")
    public ResponseEntity<Resource> getThumbnail(@PathVariable String filename) throws MalformedURLException {
        Path path = thumbnailStoragePath.resolve(filename);
        Resource resource = new UrlResource(path.toUri());
        return ResponseEntity.ok()
                .contentType(MediaTypeFactory.getMediaType(resource).orElse(MediaType.IMAGE_JPEG))
                .body(resource);
    }

    @PostMapping("/eliminarVideo")
    public String deleteVideo(@RequestParam(value = "id",required = false) Long id) {
        if(id!=null){
            try {
                videoService.deleteVideo(id);
                return "redirect:/videos";
            } catch (Exception e) {
                return "redirect:/videos";
            }
        }else {
            return "redirect:/videos";
        }
    }
}
