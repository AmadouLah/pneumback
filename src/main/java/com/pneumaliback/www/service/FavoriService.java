package com.pneumaliback.www.service;

import com.pneumaliback.www.entity.Favori;
import com.pneumaliback.www.entity.Product;
import com.pneumaliback.www.entity.User;
import com.pneumaliback.www.repository.FavoriRepository;
import com.pneumaliback.www.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FavoriService {

    private final FavoriRepository favoriRepository;
    private final ProductRepository productRepository;

    public List<Favori> listByUser(User user) {
        return favoriRepository.findByUser(user);
    }

    @Transactional
    public Favori add(User user, Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Produit introuvable"));
        // éviter doublon
        return favoriRepository.findByUserAndProduct(user, product)
                .orElseGet(() -> {
                    Favori f = new Favori();
                    f.setUser(user);
                    f.setProduct(product);
                    return favoriRepository.save(f);
                });
    }

    @Transactional
    public void remove(User user, Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Produit introuvable"));
        favoriRepository.findByUserAndProduct(user, product)
                .ifPresent(favoriRepository::delete);
    }
}
