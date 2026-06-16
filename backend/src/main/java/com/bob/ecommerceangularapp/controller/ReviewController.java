package com.bob.ecommerceangularapp.controller;

import com.bob.ecommerceangularapp.dto.PageResponse;
import com.bob.ecommerceangularapp.dto.ReviewRequest;
import com.bob.ecommerceangularapp.dto.ReviewSummary;
import com.bob.ecommerceangularapp.dto.ReviewView;
import com.bob.ecommerceangularapp.service.ReviewService;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@CrossOrigin({"http://localhost:4200", "http://localhost:4250"})
@RestController
@RequestMapping("/api/reviews")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @GetMapping
    public PageResponse<ReviewView> list(@RequestParam Long productId,
                                         @RequestParam(defaultValue = "0") int page,
                                         @RequestParam(defaultValue = "5") int size) {
        return PageResponse.of(reviewService.list(productId, PageRequest.of(page, size)));
    }

    @GetMapping("/summary")
    public ReviewSummary summary(@RequestParam Long productId) {
        return reviewService.summary(productId);
    }

    @PostMapping
    public ResponseEntity<ReviewView> create(@Valid @RequestBody ReviewRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(reviewService.create(request));
    }
}
