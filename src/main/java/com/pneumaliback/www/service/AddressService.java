package com.pneumaliback.www.service;

import com.pneumaliback.www.dto.AddressDTO;
import com.pneumaliback.www.dto.AddressSimpleDTO;
import com.pneumaliback.www.dto.CreerAddressDTO;
import com.pneumaliback.www.dto.ModifierAddressDTO;
import com.pneumaliback.www.entity.Address;
import com.pneumaliback.www.entity.User;
import com.pneumaliback.www.enums.Country;
import com.pneumaliback.www.repository.AddressRepository;
import com.pneumaliback.www.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AddressService {

    private final AddressRepository addressRepository;
    private final UserRepository userRepository;

    @Transactional
    public AddressDTO creerAddress(CreerAddressDTO dto) {
        User utilisateur = userRepository.findById(dto.utilisateurId())
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur non trouvé"));

        boolean addresseExistante = addressRepository.existsByUserAndStreetAndCityAndCountry(
                utilisateur, dto.street(), dto.city(), dto.country());

        if (addresseExistante) {
            throw new IllegalArgumentException("Une adresse identique existe déjà pour cet utilisateur");
        }

        Address address = new Address();
        address.setUser(utilisateur);
        address.setStreet(dto.street());
        address.setCity(dto.city());
        address.setRegion(dto.region());
        address.setCountry(dto.country());
        address.setPostalCode(dto.postalCode());

        Address addressSauvegardee = addressRepository.save(address);
        log.info("Nouvelle adresse créée avec l'ID: {} pour l'utilisateur: {}", 
                addressSauvegardee.getId(), dto.utilisateurId());

        return mapperVersDTO(addressSauvegardee);
    }

    @Transactional(readOnly = true)
    public Page<AddressSimpleDTO> obtenirAddressesParUtilisateur(Long utilisateurId, Pageable pageable) {
        Page<Address> addresses = addressRepository.findByUserIdOrderByCreatedAtDesc(utilisateurId, pageable);
        return addresses.map(this::mapperVersSimpleDTO);
    }


    @Transactional(readOnly = true)
    public Optional<AddressDTO> obtenirAddressParId(Long id, Long utilisateurId) {
        Optional<Address> address = addressRepository.findByIdAndUser(id, 
                userRepository.findById(utilisateurId).orElse(null));

        return address.map(this::mapperVersDTO);
    }

    @Transactional(readOnly = true)
    public List<AddressSimpleDTO> obtenirAddressesParPays(Country country) {
        List<Address> addresses = addressRepository.findByCountry(country);
        return addresses.stream().map(this::mapperVersSimpleDTO).toList();
    }

    @Transactional(readOnly = true)
    public List<AddressSimpleDTO> obtenirAddressesParVille(String city) {
        List<Address> addresses = addressRepository.findByCity(city);
        return addresses.stream().map(this::mapperVersSimpleDTO).toList();
    }

    @Transactional
    public AddressDTO modifierAddress(Long id, ModifierAddressDTO dto, Long utilisateurId) {
        Address address = addressRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Adresse non trouvée"));

        if (!address.getUser().getId().equals(utilisateurId)) {
            throw new IllegalArgumentException("Accès non autorisé à cette adresse");
        }

        if (dto.street() != null) {
            address.setStreet(dto.street());
        }
        if (dto.city() != null) {
            address.setCity(dto.city());
        }
        if (dto.region() != null) {
            address.setRegion(dto.region());
        }
        if (dto.country() != null) {
            address.setCountry(dto.country());
        }
        if (dto.postalCode() != null) {
            address.setPostalCode(dto.postalCode());
        }

        Address addressSauvegardee = addressRepository.save(address);
        log.info("Adresse modifiée avec l'ID: {} par l'utilisateur: {}", id, utilisateurId);

        return mapperVersDTO(addressSauvegardee);
    }

    @Transactional
    public void supprimerAddress(Long id, Long utilisateurId) {
        Address address = addressRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Adresse non trouvée"));

        if (!address.getUser().getId().equals(utilisateurId)) {
            throw new IllegalArgumentException("Accès non autorisé à cette adresse");
        }

        addressRepository.delete(address);
        log.info("Adresse supprimée avec l'ID: {} par l'utilisateur: {}", id, utilisateurId);
    }

    @Transactional(readOnly = true)
    public long compterAddressesParUtilisateur(Long utilisateurId) {
        User utilisateur = userRepository.findById(utilisateurId)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur non trouvé"));
        return addressRepository.countByUser(utilisateur);
    }

    @Transactional(readOnly = true)
    public List<AddressSimpleDTO> obtenirAddressesParUtilisateurEtPays(Long utilisateurId, Country country) {
        User utilisateur = userRepository.findById(utilisateurId)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur non trouvé"));
        List<Address> addresses = addressRepository.findByUserAndCountry(utilisateur, country);
        return addresses.stream().map(this::mapperVersSimpleDTO).toList();
    }

    @Transactional
    public void supprimerToutesAddressesUtilisateur(Long utilisateurId) {
        User utilisateur = userRepository.findById(utilisateurId)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur non trouvé"));
        addressRepository.deleteByUser(utilisateur);
        log.info("Toutes les adresses supprimées pour l'utilisateur: {}", utilisateurId);
    }

    private AddressDTO mapperVersDTO(Address address) {
        return new AddressDTO(
                address.getId(),
                address.getStreet(),
                address.getCity(),
                address.getRegion(),
                address.getCountry(),
                address.getPostalCode(),
                address.getCreatedAt(),
                address.getUpdatedAt(),
                address.getUser().getId(),
                address.getUser().getLastName(),
                address.getUser().getFirstName()
        );
    }

    private AddressSimpleDTO mapperVersSimpleDTO(Address address) {
        return new AddressSimpleDTO(
                address.getId(),
                address.getStreet(),
                address.getCity(),
                address.getRegion(),
                address.getCountry(),
                address.getPostalCode()
        );
    }
}