package br.edu.ufrb.rascomp.media;

import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;

/** Published snapshots are revalidated; private media is never cached via public routes. */
@RestController
@RequestMapping("/api/v1/public/media")
@RequiredArgsConstructor
public class MediaPublicController {
    private final MediaCmsService service;

    @GetMapping("/gallery")
    public ResponseEntity<JsonNode> gallery(@RequestHeader(value="If-None-Match",required=false) String clientTag){
        String tag="\"gallery-v"+service.publishedVersion()+"\"";
        if(tag.equals(clientTag))return ResponseEntity.status(304).eTag(tag).cacheControl(CacheControl.noCache()).build();
        return ResponseEntity.ok().eTag(tag).cacheControl(CacheControl.noCache()).body(service.publishedGallery());
    }

    @GetMapping("/editorial")
    public ResponseEntity<JsonNode> editorial(@RequestHeader(value="If-None-Match",required=false) String clientTag){
        String tag="\"editorial-v"+service.publishedVersion()+"\"";
        if(tag.equals(clientTag))return ResponseEntity.status(304).eTag(tag).cacheControl(CacheControl.noCache()).build();
        return ResponseEntity.ok().eTag(tag).cacheControl(CacheControl.noCache()).body(service.publishedEditorial());
    }

    @GetMapping("/assets/{id}/content")
    public ResponseEntity<Resource> file(@PathVariable long id,
            @RequestHeader(value="If-None-Match",required=false) String clientTag){
        service.requirePublished(id);
        String tag="\"media-"+id+"-v"+service.publishedVersion()+"\"";
        if(tag.equals(clientTag))return ResponseEntity.status(304).eTag(tag).cacheControl(CacheControl.noCache()).build();
        Resource file=service.content(id,true);
        return ResponseEntity.ok().eTag(tag).cacheControl(CacheControl.noCache())
            .header("X-Content-Type-Options","nosniff")
            .contentType(MediaType.parseMediaType(service.mime(id))).body(file);
    }
}
