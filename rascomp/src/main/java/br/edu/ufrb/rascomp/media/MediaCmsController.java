package br.edu.ufrb.rascomp.media;

import java.util.List;
import java.util.Map;
import java.nio.charset.StandardCharsets;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.core.io.Resource;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;

/**
 * JWT restricted editorial workspace. Neither the public endpoints nor the public
 * asset reader trust draft publication flags.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/media")
public class MediaCmsController {
    private final MediaCmsService service;
    public record Visibility(boolean published) {}
    public record AlbumLinks(List<Long> albumIds) {}
    public record BatchLinks(List<Long> assetIds,List<Long> albumIds) {}
    public record AlbumInput(String name,String description,Long coverId) {}
    public record PublishInput(Long expectedVersion) {}

    @GetMapping("/workspace")
    public Map<String,Object> workspace(){return service.workspace();}

    @PostMapping(path="/assets",consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String,Object> upload(@RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue="false") boolean galleryDraft,
            Authentication principal){
        return service.upload(file,galleryDraft,principal.getName());
    }
    @GetMapping("/assets/{id}/content")
    public ResponseEntity<Resource> privateImage(@PathVariable long id){
        return content(id,false);
    }
    @PatchMapping("/assets/{id}/visibility")
    public void visibility(@PathVariable long id,@RequestBody Visibility input){
        service.visibility(id,input.published());
    }
    @PutMapping("/assets/{id}/albums")
    public void albumLinks(@PathVariable long id,@RequestBody AlbumLinks input){
        service.setAlbums(id,input.albumIds(),false);
    }
    @PostMapping("/assets/albums")
    public void batchAlbums(@RequestBody BatchLinks input){
        service.batchAlbums(input.assetIds(),input.albumIds());
    }
    @DeleteMapping("/assets/{id}")
    public void deleteAsset(@PathVariable long id){service.deleteAsset(id);}

    @PostMapping("/albums")
    public Map<String,Long> createAlbum(@RequestBody AlbumInput input) {
        return Map.of("id",service.createAlbum(input.name(),input.description()));
    }
    @PutMapping("/albums/{id}")
    public void updateAlbum(@PathVariable long id,@RequestBody AlbumInput input){
        service.updateAlbum(id,input.name(),input.description(),input.coverId());
    }
    @DeleteMapping("/albums/{id}")
    public void deleteAlbum(@PathVariable long id){service.deleteAlbum(id);}

    @PutMapping("/draft")
    public void saveDraft(@RequestBody JsonNode editorial){service.saveDraft(editorial);}
    @PutMapping("/featured")
    public void saveFeatured(@RequestBody JsonNode ids){service.saveFeatured(ids);}
    @PostMapping("/publish")
    public Map<String,Long> publish(@RequestBody(required=false) PublishInput input) {
        return Map.of("version",service.publish(input==null?null:input.expectedVersion()));
    }
    private ResponseEntity<Resource> content(long id,boolean publicly){
        Resource file=service.content(id,publicly);
        MediaType type=MediaType.parseMediaType(service.mime(id));
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
            .header(HttpHeaders.X_CONTENT_TYPE_OPTIONS,"nosniff")
            .contentType(type).body(file);
    }
}
