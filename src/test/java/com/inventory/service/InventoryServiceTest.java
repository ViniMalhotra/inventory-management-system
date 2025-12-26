package com.inventory.service;

import com.inventory.entity.Inventory;
import com.inventory.entity.Product;
import com.inventory.exception.ProductNotFoundException;
import com.inventory.repository.InventoryRepository;
import com.inventory.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("InventoryService Test Suite")
class InventoryServiceTest {

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private InventoryService inventoryService;

    private Product testProduct;
    private Inventory testInventory;

    @BeforeEach
    void setUp() {
        testProduct = Product.builder()
                .productId(1L)
                .productName("Test Product")
                .massG(500)
                .build();

        testInventory = Inventory.builder()
                .productId(1L)
                .availableQty(10L)
                .build();
    }

    @Nested
    @DisplayName("initializeInventoryForProduct() tests")
    class InitializeInventoryForProductTests {

        @Test
        @DisplayName("Should create inventory with 0 quantity for new product")
        void testInitializeInventoryWithZeroQuantity() {
            // Arrange
            Inventory expectedInventory = Inventory.builder()
                    .productId(1L)
                    .availableQty(0L)
                    .build();

            when(inventoryRepository.save(any(Inventory.class))).thenReturn(expectedInventory);

            // Act
            Inventory result = inventoryService.initializeInventoryForProduct(1L);

            // Assert
            assertNotNull(result);
            assertEquals(1L, result.getProductId());
            assertEquals(0L, result.getAvailableQty());
            verify(inventoryRepository, times(1)).save(any(Inventory.class));
        }

        @Test
        @DisplayName("Should save inventory to repository")
        void testInitializeInventorySavestoRepository() {
            // Arrange
            when(inventoryRepository.save(any(Inventory.class))).thenReturn(testInventory);

            // Act
            inventoryService.initializeInventoryForProduct(1L);

            // Assert
            verify(inventoryRepository, times(1)).save(any(Inventory.class));
        }
    }

    @Nested
    @DisplayName("getAvailableQuantity() tests")
    class GetAvailableQuantityTests {

        @Test
        @DisplayName("Should return available quantity when inventory exists")
        void testGetAvailableQuantitySuccess() {
            // Arrange
            when(inventoryRepository.findById(1L)).thenReturn(Optional.of(testInventory));

            // Act
            Long result = inventoryService.getAvailableQuantity(1L);

            // Assert
            assertNotNull(result);
            assertEquals(10L, result);
        }

        @Test
        @DisplayName("Should throw ProductNotFoundException when inventory not found")
        void testGetAvailableQuantityNotFound() {
            // Arrange
            when(inventoryRepository.findById(1L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThrows(ProductNotFoundException.class, () -> {
                inventoryService.getAvailableQuantity(1L);
            });
        }

        @Test
        @DisplayName("Should return zero for newly initialized inventory")
        void testGetAvailableQuantityZero() {
            // Arrange
            testInventory.setAvailableQty(0L);
            when(inventoryRepository.findById(1L)).thenReturn(Optional.of(testInventory));

            // Act
            Long result = inventoryService.getAvailableQuantity(1L);

            // Assert
            assertEquals(0L, result);
        }
    }

    @Nested
    @DisplayName("getInventory() tests")
    class GetInventoryTests {

        @Test
        @DisplayName("Should return inventory when found")
        void testGetInventorySuccess() {
            // Arrange
            when(inventoryRepository.findById(1L)).thenReturn(Optional.of(testInventory));

            // Act
            Inventory result = inventoryService.getInventory(1L);

            // Assert
            assertNotNull(result);
            assertEquals(1L, result.getProductId());
        }

        @Test
        @DisplayName("Should throw ProductNotFoundException when not found")
        void testGetInventoryNotFound() {
            // Arrange
            when(inventoryRepository.findById(1L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThrows(ProductNotFoundException.class, () -> {
                inventoryService.getInventory(1L);
            });
        }
    }

    @Nested
    @DisplayName("reduceInventory() tests")
    class ReduceInventoryTests {

        @Test
        @DisplayName("Should reduce inventory quantity")
        void testReduceInventorySuccess() {
            // Arrange
            when(inventoryRepository.findByIdWithLock(1L)).thenReturn(Optional.of(testInventory));
            when(inventoryRepository.save(any(Inventory.class))).thenReturn(testInventory);

            // Act
            inventoryService.reduceInventory(1L, 3L);

            // Assert
            assertEquals(7L, testInventory.getAvailableQty());
            verify(inventoryRepository, times(1)).save(testInventory);
        }

        @Test
        @DisplayName("Should handle reducing to zero")
        void testReduceInventoryToZero() {
            // Arrange
            when(inventoryRepository.findByIdWithLock(1L)).thenReturn(Optional.of(testInventory));
            when(inventoryRepository.save(any(Inventory.class))).thenReturn(testInventory);

            // Act
            inventoryService.reduceInventory(1L, 10L);

            // Assert
            assertEquals(0L, testInventory.getAvailableQty());
        }

        @Test
        @DisplayName("Should throw exception when reducing non-existent inventory")
        void testReduceInventoryNotFound() {
            // Arrange
            when(inventoryRepository.findByIdWithLock(1L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThrows(ProductNotFoundException.class, () -> {
                inventoryService.reduceInventory(1L, 5L);
            });
        }
    }

    @Nested
    @DisplayName("increaseInventory() tests")
    class IncreaseInventoryTests {

        @Test
        @DisplayName("Should increase inventory quantity")
        void testIncreaseInventorySuccess() {
            // Arrange
            when(inventoryRepository.findByIdWithLock(1L)).thenReturn(Optional.of(testInventory));
            when(inventoryRepository.save(any(Inventory.class))).thenReturn(testInventory);

            // Act
            inventoryService.increaseInventory(1L, 5L);

            // Assert
            assertEquals(15L, testInventory.getAvailableQty());
            verify(inventoryRepository, times(1)).save(testInventory);
        }

        @Test
        @DisplayName("Should handle large quantity increase")
        void testIncreaseInventoryLargeQuantity() {
            // Arrange
            when(inventoryRepository.findByIdWithLock(1L)).thenReturn(Optional.of(testInventory));
            when(inventoryRepository.save(any(Inventory.class))).thenReturn(testInventory);

            // Act
            inventoryService.increaseInventory(1L, 1000L);

            // Assert
            assertEquals(1010L, testInventory.getAvailableQty());
        }

        @Test
        @DisplayName("Should throw exception when increasing non-existent inventory")
        void testIncreaseInventoryNotFound() {
            // Arrange
            when(inventoryRepository.findByIdWithLock(1L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThrows(ProductNotFoundException.class, () -> {
                inventoryService.increaseInventory(1L, 5L);
            });
        }
    }

    @Nested
    @DisplayName("getProductDetails() tests")
    class GetProductDetailsTests {

        @Test
        @DisplayName("Should return product details")
        void testGetProductDetailsSuccess() {
            // Arrange
            when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));

            // Act
            Product result = inventoryService.getProductDetails(1L);

            // Assert
            assertNotNull(result);
            assertEquals("Test Product", result.getProductName());
            assertEquals(500, result.getMassG());
        }

        @Test
        @DisplayName("Should throw ProductNotFoundException when product not found")
        void testGetProductDetailsNotFound() {
            // Arrange
            when(productRepository.findById(1L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThrows(ProductNotFoundException.class, () -> {
                inventoryService.getProductDetails(1L);
            });
        }
    }

    @Nested
    @DisplayName("productExistsInInventory() tests")
    class ProductExistsInInventoryTests {

        @Test
        @DisplayName("Should return true when product exists in inventory")
        void testProductExistsTrue() {
            // Arrange
            when(inventoryRepository.existsById(1L)).thenReturn(true);

            // Act
            boolean result = inventoryService.productExistsInInventory(1L);

            // Assert
            assertTrue(result);
        }

        @Test
        @DisplayName("Should return false when product does not exist in inventory")
        void testProductExistsFalse() {
            // Arrange
            when(inventoryRepository.existsById(1L)).thenReturn(false);

            // Act
            boolean result = inventoryService.productExistsInInventory(1L);

            // Assert
            assertFalse(result);
        }
    }

    @Nested
    @DisplayName("getInventoriesForProducts() tests")
    class GetInventoriesForProductsTests {

        @Test
        @DisplayName("Should return all inventories")
        void testGetAllInventories() {
            // Arrange
            Inventory inventory2 = Inventory.builder()
                    .productId(2L)
                    .availableQty(20L)
                    .build();

            List<Inventory> inventories = Arrays.asList(testInventory, inventory2);
            when(inventoryRepository.findAll()).thenReturn(inventories);

            // Act
            List<Inventory> result = inventoryService.getAllInventory();

            // Assert
            assertNotNull(result);
            assertEquals(2, result.size());
            verify(inventoryRepository, times(1)).findAll();
        }

        @Test
        @DisplayName("Should return empty list when no inventories found")
        void testGetAllInventoriesEmpty() {
            // Arrange
            when(inventoryRepository.findAll()).thenReturn(Arrays.asList());

            // Act
            List<Inventory> result = inventoryService.getAllInventory();

            // Assert
            assertNotNull(result);
            assertEquals(0, result.size());
            verify(inventoryRepository, times(1)).findAll();
        }
    }
}
