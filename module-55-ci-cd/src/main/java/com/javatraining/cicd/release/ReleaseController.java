package com.javatraining.cicd.release;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/releases")
@RequiredArgsConstructor
public class ReleaseController {

    private final ReleaseService releaseService;

    @GetMapping
    public List<Release> findAll() {
        return releaseService.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Release> findById(@PathVariable Long id) {
        return releaseService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Release> create(@RequestBody Release release) {
        Release saved = releaseService.create(release);
        return ResponseEntity.created(URI.create("/releases/" + saved.getId())).body(saved);
    }
}
