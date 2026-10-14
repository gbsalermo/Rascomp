package br.edu.ufrb.rascomp.media;

import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;

/** Only immutable, explicitly published snapshots and approved media. */
@RestController
@RequestMapping("/api/v1/public/media")
@RequiredArgsConstructor
public class MediaPublicController {
    private final MediaCmsService service;

    @GetMapping("/gallery")
    public JsonNode gallery(){return service.publishedGallery();}

    @GetMapping("/editorial")
    public JsonNode editorial(){return service.publishedEditorial();}

    @GetMapping("/assets/{id}/content")
    public ResponseEntity<Resource> file(@PathVariable long id){
        Resource file=service.content(id,true);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
            .header("X-Content-Type-Options","nosniff")
            .contentType(MediaType.parseMediaType(service.mime(id))).body(file);
    }
}
