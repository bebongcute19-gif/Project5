package com.omnichannel.identity.service;

import com.omnichannel.identity.dto.request.AddressRequest;
import com.omnichannel.identity.dto.response.AddressResponse;
import com.omnichannel.identity.entity.Address;
import com.omnichannel.identity.exception.ResourceNotFoundException;
import com.omnichannel.identity.repository.AddressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AddressService {

    private final AddressRepository addressRepository;

    @Transactional(readOnly = true)
    public List<AddressResponse> getUserAddresses(UUID userId) {
        return addressRepository.findByUserId(userId)
                .stream()
                .map(this::mapToAddressResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public AddressResponse addAddress(UUID userId, AddressRequest request) {
        List<Address> existing = addressRepository.findByUserId(userId);
        boolean isDefault = Boolean.TRUE.equals(request.getIsDefault()) || existing.isEmpty();

        if (isDefault) {
            addressRepository.resetDefaultAddresses(userId);
        }

        Address address = Address.builder()
                .userId(userId)
                .recipientName(request.getRecipientName().trim())
                .phone(request.getPhone().trim())
                .streetAddress(request.getStreetAddress().trim())
                .ward(request.getWard().trim())
                .district(request.getDistrict().trim())
                .city(request.getCity().trim())
                .isDefault(isDefault)
                .build();

        Address saved = addressRepository.save(address);
        return mapToAddressResponse(saved);
    }

    @Transactional
    public AddressResponse updateAddress(UUID userId, UUID addressId, AddressRequest request) {
        Address address = addressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found or does not belong to user: " + addressId));

        if (Boolean.TRUE.equals(request.getIsDefault())) {
            addressRepository.resetDefaultAddresses(userId);
            address.setIsDefault(true);
        } else if (request.getIsDefault() != null) {
            address.setIsDefault(request.getIsDefault());
        }

        address.setRecipientName(request.getRecipientName().trim());
        address.setPhone(request.getPhone().trim());
        address.setStreetAddress(request.getStreetAddress().trim());
        address.setWard(request.getWard().trim());
        address.setDistrict(request.getDistrict().trim());
        address.setCity(request.getCity().trim());

        Address updated = addressRepository.save(address);
        return mapToAddressResponse(updated);
    }

    @Transactional
    public void deleteAddress(UUID userId, UUID addressId) {
        Address address = addressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found or does not belong to user: " + addressId));

        addressRepository.delete(address);
    }

    private AddressResponse mapToAddressResponse(Address address) {
        return AddressResponse.builder()
                .id(address.getId())
                .recipientName(address.getRecipientName())
                .phone(address.getPhone())
                .streetAddress(address.getStreetAddress())
                .ward(address.getWard())
                .district(address.getDistrict())
                .city(address.getCity())
                .isDefault(address.getIsDefault())
                .createdAt(address.getCreatedAt())
                .build();
    }
}
