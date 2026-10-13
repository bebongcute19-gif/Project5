package com.omnichannel.identity.service;

import com.omnichannel.identity.dto.request.AddressRequest;
import com.omnichannel.identity.dto.response.AddressResponse;
import com.omnichannel.identity.entity.Address;
import com.omnichannel.identity.exception.ResourceNotFoundException;
import com.omnichannel.identity.repository.AddressRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AddressServiceTest {

    @Mock
    private AddressRepository addressRepository;

    @InjectMocks
    private AddressService addressService;

    private UUID userId;
    private Address sampleAddress;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        sampleAddress = Address.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .recipientName("Nguyen Van A")
                .phone("0901234567")
                .streetAddress("123 Le Loi")
                .ward("Phuong Ben Nghe")
                .district("Quan 1")
                .city("Thanh pho Ho Chi Minh")
                .isDefault(true)
                .createdAt(Instant.now())
                .build();
    }

    @Test
    @DisplayName("getUserAddresses should return address list for user")
    void testGetUserAddresses_Success() {
        when(addressRepository.findByUserId(userId)).thenReturn(List.of(sampleAddress));

        List<AddressResponse> result = addressService.getUserAddresses(userId);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Nguyen Van A", result.get(0).getRecipientName());
        assertTrue(result.get(0).getIsDefault());
    }

    @Test
    @DisplayName("addAddress when first address should auto-set default to true")
    void testAddAddress_FirstAddressBecomesDefault() {
        AddressRequest request = AddressRequest.builder()
                .recipientName("Nguyen Van A")
                .phone("0901234567")
                .streetAddress("123 Le Loi")
                .ward("Phuong Ben Nghe")
                .district("Quan 1")
                .city("Thanh pho Ho Chi Minh")
                .isDefault(false)
                .build();

        when(addressRepository.findByUserId(userId)).thenReturn(List.of());
        when(addressRepository.save(any(Address.class))).thenAnswer(i -> {
            Address a = i.getArgument(0);
            a.setId(UUID.randomUUID());
            return a;
        });

        AddressResponse response = addressService.addAddress(userId, request);

        assertNotNull(response);
        assertTrue(response.getIsDefault());
        verify(addressRepository, times(1)).resetDefaultAddresses(userId);
    }

    @Test
    @DisplayName("updateAddress of other user should throw ResourceNotFoundException")
    void testUpdateAddress_OtherUser_ThrowsNotFound() {
        UUID otherAddressId = UUID.randomUUID();
        AddressRequest request = AddressRequest.builder()
                .recipientName("Hacker")
                .phone("0111111111")
                .streetAddress("456 Fake St")
                .ward("Ward")
                .district("District")
                .city("City")
                .build();

        when(addressRepository.findByIdAndUserId(otherAddressId, userId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> addressService.updateAddress(userId, otherAddressId, request));
    }

    @Test
    @DisplayName("deleteAddress of other user should throw ResourceNotFoundException")
    void testDeleteAddress_OtherUser_ThrowsNotFound() {
        UUID otherAddressId = UUID.randomUUID();
        when(addressRepository.findByIdAndUserId(otherAddressId, userId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> addressService.deleteAddress(userId, otherAddressId));
    }

    @Test
    @DisplayName("deleteAddress for owned address should succeed")
    void testDeleteAddress_Success() {
        UUID addressId = sampleAddress.getId();
        when(addressRepository.findByIdAndUserId(addressId, userId)).thenReturn(Optional.of(sampleAddress));

        addressService.deleteAddress(userId, addressId);

        verify(addressRepository, times(1)).delete(sampleAddress);
    }
}
