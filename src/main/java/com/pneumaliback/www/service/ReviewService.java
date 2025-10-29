package com.pneumaliback.www.service;

import com.pneumaliback.www.entity.Product;
import com.pneumaliback.www.entity.Review;
import com.pneumaliback.www.entity.User;
import com.pneumaliback.www.repository.ProductRepository;
import com.pneumaliback.www.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;

    public List<Review> listByProduct(Long productId) {
        Product p = productRepository.findById(productId).orElseThrow(() -> new RuntimeException("Produit introuvable"));
        return reviewRepository.findByProduct(p);
    }

    @Transactional
    public Review addReview(User user, Long productId, int rating, String comment) {
        if (rating < 1 || rating > 5) throw new IllegalArgumentException("Note invalide (1-5)");
        Product p = productRepository.findById(productId).orElseThrow(() -> new RuntimeException("Produit introuvable"));
        Review r = new Review();
        r.setUser(user);
        r.setProduct(p);
        r.setRating(rating);
        r.setComment(comment);
        return reviewRepository.save(r);
    }
}
