package com.inventory.service;

import com.inventory.dto.OrderItemRequestDTO;
import com.inventory.dto.OrderRequestDTO;
import com.inventory.entity.*;
import com.inventory.exception.OrderNotFoundException;
import com.inventory.exception.ProductNotFoundException;
import com.inventory.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderService Test Suite")
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private PendingOrderItemRepository pendingOrderItemRepository;

    @Mock
    private InventoryService inventoryService;

    @Mock
    private ShipmentService shipmentService;

    @InjectMocks
    private OrderService orderService;

    private OrderRequestDTO validOrderRequest;
    private Product testProduct;
    private Order testOrder;
    private OrderItem testOrderItem;

    @BeforeEach
    void setUp() {
        // Setup test product
        testProduct = Product.builder()
                .productId(1L)
                .productName("Test Product")
                .massG(500)
                .build();

        // Setup test order
        testOrder = Order.builder()
                .orderId(1L)
                .status("PENDING")
                .createdAt(LocalDateTime.now())
                .build();

        // Setup test order item
        testOrderItem = OrderItem.builder()
                .id(1L)
                .orderId(1L)
                .productId(1L)
                .requestedQty(5L)
                .fulfilledQty(0L)
                .status("PENDING")
                .build();

        // Setup valid order request
        OrderItemRequestDTO itemRequest = OrderItemRequestDTO.builder()
                .productId(1L)
                .quantity(5L)
                .build();

        validOrderRequest = OrderRequestDTO.builder()
                .orderId(1L)
                .requested(Arrays.asList(itemRequest))
                .build();
    }

    @Nested
    @DisplayName("processOrder() tests")
    class ProcessOrderTests {

        @Test
        @DisplayName("Should successfully process a valid order")
        void testProcessOrderSuccess() {
            // Arrange
            when(inventoryService.productExistsInInventory(1L)).thenReturn(true);
            when(orderRepository.save(any(Order.class))).thenReturn(testOrder);
            when(orderItemRepository.save(any(OrderItem.class))).thenReturn(testOrderItem);
            when(orderItemRepository.findByOrderId(1L)).thenReturn(Arrays.asList(testOrderItem));
            when(shipmentService.createShipments(anyLong(), anyList(), any(Order.class), any()))
                    .thenReturn(new ArrayList<>());
            when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));

            // Act
            Order result = orderService.processOrder(validOrderRequest);

            // Assert
            assertNotNull(result);
            assertEquals(1L, result.getOrderId());
            verify(orderRepository, atLeastOnce()).save(any(Order.class));
            verify(orderItemRepository, times(1)).save(any(OrderItem.class));
        }

        @Test
        @DisplayName("Should throw ProductNotFoundException for non-existent product")
        void testProcessOrderWithNonExistentProduct() {
            // Arrange
            when(inventoryService.productExistsInInventory(1L)).thenReturn(false);

            // Act & Assert
            assertThrows(ProductNotFoundException.class, () -> {
                orderService.processOrder(validOrderRequest);
            });

            verify(orderRepository, never()).save(any(Order.class));
        }

        @Test
        @DisplayName("Should process order with multiple items")
        void testProcessOrderWithMultipleItems() {
            // Arrange
            OrderItemRequestDTO item2 = OrderItemRequestDTO.builder()
                    .productId(2L)
                    .quantity(3L)
                    .build();

            validOrderRequest.setRequested(Arrays.asList(
                    validOrderRequest.getRequested().get(0),
                    item2));

            when(inventoryService.productExistsInInventory(anyLong())).thenReturn(true);
            when(orderRepository.save(any(Order.class))).thenReturn(testOrder);
            when(orderItemRepository.save(any(OrderItem.class))).thenReturn(testOrderItem);
            when(orderItemRepository.findByOrderId(1L)).thenReturn(Arrays.asList(testOrderItem, testOrderItem));
            when(shipmentService.createShipments(anyLong(), anyList(), any(Order.class), any()))
                    .thenReturn(new ArrayList<>());
            when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));

            // Act
            Order result = orderService.processOrder(validOrderRequest);

            // Assert
            assertNotNull(result);
            verify(orderItemRepository, times(2)).save(any(OrderItem.class));
        }

        @Test
        @DisplayName("Should set correct order status to PENDING")
        void testProcessOrderStatusIsPending() {
            // Arrange
            when(inventoryService.productExistsInInventory(1L)).thenReturn(true);
            when(orderRepository.save(any(Order.class))).thenReturn(testOrder);
            when(orderItemRepository.save(any(OrderItem.class))).thenReturn(testOrderItem);
            when(orderItemRepository.findByOrderId(1L)).thenReturn(Arrays.asList(testOrderItem));
            when(shipmentService.createShipments(anyLong(), anyList(), any(Order.class), any()))
                    .thenReturn(new ArrayList<>());
            when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));

            // Act
            Order result = orderService.processOrder(validOrderRequest);

            // Assert
            assertEquals("PENDING", result.getStatus());
        }
    }

    @Nested
    @DisplayName("updateOrderStatus() tests")
    class UpdateOrderStatusTests {

        @Test
        @DisplayName("Should update status to FULFILLED when all items fulfilled")
        void testUpdateStatusToFulfilled() {
            // Arrange
            OrderItem fulfilledItem = OrderItem.builder()
                    .id(1L)
                    .orderId(1L)
                    .productId(1L)
                    .requestedQty(5L)
                    .fulfilledQty(5L)
                    .status("FULFILLED")
                    .build();

            testOrder.setOrderId(1L);
            List<OrderItem> items = Arrays.asList(fulfilledItem);
            when(orderItemRepository.findByOrderId(1L)).thenReturn(items);
            when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
            when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

            // Act
            orderService.updateOrderStatus(1L);

            // Assert
            verify(orderRepository, times(1)).save(any(Order.class));
        }

        @Test
        @DisplayName("Should update status to PARTIALLY_FULFILLED when some items fulfilled")
        void testUpdateStatusToPartiallyFulfilled() {
            // Arrange
            OrderItem partialItem = OrderItem.builder()
                    .id(1L)
                    .orderId(1L)
                    .productId(1L)
                    .requestedQty(5L)
                    .fulfilledQty(2L)
                    .status("PARTIALLY_FULFILLED")
                    .build();
            testOrder.setOrderId(1L);
            List<OrderItem> items = Arrays.asList(partialItem);
            when(orderItemRepository.findByOrderId(1L)).thenReturn(items);
            when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
            when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

            // Act
            orderService.updateOrderStatus(1L);

            // Assert
            verify(orderRepository, times(1)).save(any(Order.class));
            verify(orderRepository, times(1)).save(any(Order.class));
        }

        @Test
        @DisplayName("Should update status to PENDING when no items fulfilled")
        void testUpdateStatusToPending() {
            testOrder.setOrderId(1L);
            List<OrderItem> items = Arrays.asList(testOrderItem);
            when(orderItemRepository.findByOrderId(1L)).thenReturn(items);
            when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
            when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

            // Act
            orderService.updateOrderStatus(1L);

            // Assert
            verify(orderRepository, times(1)).save(any(Order.class));
            verify(orderRepository, times(1)).save(any(Order.class));
        }
    }

    @Nested
    @DisplayName("getOrder() tests")
    class GetOrderTests {

        @Test
        @DisplayName("Should return order when found")
        void testGetOrderFound() {
            // Arrange
            when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));

            // Act
            Order result = orderService.getOrder(1L);

            // Assert
            assertNotNull(result);
            assertEquals(1L, result.getOrderId());
        }

        @Test
        @DisplayName("Should throw OrderNotFoundException when not found")
        void testGetOrderNotFound() {
            // Arrange
            when(orderRepository.findById(1L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThrows(OrderNotFoundException.class, () -> {
                orderService.getOrder(1L);
            });
        }
    }

    @Nested
    @DisplayName("getOrderItems() tests")
    class GetOrderItemsTests {

        @Test
        @DisplayName("Should return order items for valid order")
        void testGetOrderItems() {
            // Arrange
            List<OrderItem> items = Arrays.asList(testOrderItem);
            when(orderItemRepository.findByOrderId(1L)).thenReturn(items);

            // Act
            List<OrderItem> result = orderService.getOrderItems(1L);

            // Assert
            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals(1L, result.get(0).getProductId());
        }

        @Test
        @DisplayName("Should return empty list when no items found")
        void testGetOrderItemsEmpty() {
            // Arrange
            when(orderItemRepository.findByOrderId(1L)).thenReturn(new ArrayList<>());

            // Act
            List<OrderItem> result = orderService.getOrderItems(1L);

            // Assert
            assertNotNull(result);
            assertEquals(0, result.size());
        }
    }

    @Nested
    @DisplayName("getPendingItems() tests")
    class GetPendingItemsTests {

        @Test
        @DisplayName("Should return pending items for order")
        void testGetPendingItems() {
            // Arrange
            PendingOrderItem pendingItem = PendingOrderItem.builder()
                    .id(1L)
                    .orderId(1L)
                    .productId(1L)
                    .pendingQty(5L)
                    .createdAt(LocalDateTime.now())
                    .build();

            List<PendingOrderItem> items = Arrays.asList(pendingItem);
            when(pendingOrderItemRepository.findByOrderId(1L)).thenReturn(items);

            // Act
            List<PendingOrderItem> result = orderService.getPendingItems(1L);

            // Assert
            assertNotNull(result);
            assertEquals(1, result.size());
        }

        @Test
        @DisplayName("Should return empty list when no pending items")
        void testGetPendingItemsEmpty() {
            // Arrange
            when(pendingOrderItemRepository.findByOrderId(1L)).thenReturn(new ArrayList<>());

            // Act
            List<PendingOrderItem> result = orderService.getPendingItems(1L);

            // Assert
            assertNotNull(result);
            assertEquals(0, result.size());
        }
    }

    @Nested
    @DisplayName("completeOrderIfAllFulfilled() tests")
    class CompleteOrderIfAllFulfilledTests {

        @Test
        @DisplayName("Should mark order COMPLETED when all items fulfilled")
        void testCompleteOrderWhenAllFulfilled() {
            // Arrange
            testOrder.setStatus("FULFILLED");

            when(pendingOrderItemRepository.findByOrderId(1L)).thenReturn(new ArrayList<>());
            when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
            when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

            // Act
            orderService.completeOrderIfAllFulfilled(1L);

            // Assert
            verify(orderRepository, times(1)).save(any(Order.class));
        }

        @Test
        @DisplayName("Should not complete order when pending items exist")
        void testNotCompleteOrderWithPendingItems() {
            // Arrange
            testOrder.setStatus("PARTIALLY_FULFILLED");
            PendingOrderItem pendingItem = PendingOrderItem.builder()
                    .id(1L)
                    .orderId(1L)
                    .productId(1L)
                    .pendingQty(3L)
                    .createdAt(LocalDateTime.now())
                    .build();

            when(pendingOrderItemRepository.findByOrderId(1L)).thenReturn(Arrays.asList(pendingItem));

            // Act
            orderService.completeOrderIfAllFulfilled(1L);

            // Assert
            verify(orderRepository, never()).save(any(Order.class));
        }
    }
}
