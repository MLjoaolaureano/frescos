package com.meli.frescos.service;

import com.meli.frescos.controller.dto.OrderProductsRequest;
import com.meli.frescos.controller.dto.PurchaseOrderRequest;
import com.meli.frescos.exception.NotEnoughStockException;
import com.meli.frescos.exception.OrderProductIsInvalidException;
import com.meli.frescos.exception.PurchaseOrderByIdNotFoundException;
import com.meli.frescos.model.*;
import com.meli.frescos.repository.PurchaseOrderRepository;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * This class contains all PurchaseOrder related function
 * Using Spring Service
 */
@Service
public class PurchaseOrderService implements IPurchaseOrderService {

    private final PurchaseOrderRepository purchaseOrderRepository;

    private final IBuyerService iBuyerService;

    private final IOrderProductService iOrderProductService;

    private final IBatchStockService iBatchStockService;

    public PurchaseOrderService(PurchaseOrderRepository purchaseOrderRepository, IBuyerService iBuyerService, IOrderProductService iOrderProductService,
                                IBatchStockService iBatchStockService) {
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.iBuyerService = iBuyerService;
        this.iOrderProductService = iOrderProductService;
        this.iBatchStockService = iBatchStockService;
    }

    /**
     * This method save a purchaseOrder and orderProducts
     *
     * @param purchaseOrderRequest from PurchaseOrderModel instance
     * @return BigDecimal sum from all products
     */
    @Override
    public PurchaseOrderModel save(PurchaseOrderRequest purchaseOrderRequest) {
        BuyerModel finBuyer = iBuyerService.getById(purchaseOrderRequest.getBuyer());
        PurchaseOrderModel purchase = new PurchaseOrderModel();
        purchase.setBuyer(finBuyer);
        purchase.setOrderStatus(OrderStatusEnum.OPEN);
        purchase.setDate(purchaseOrderRequest.getDate());

        return purchaseOrderRepository.save(purchase);
    }

    /**
     * This method check if the product is expired
     *
     * @param productId from product Entity
     * @param desiredQuantity int required due date
     * @return Boolean checking due date
     */
    private boolean stockAvailable(Long productId, int desiredQuantity) {

        LocalDate dateRequirement = LocalDate.now().plusWeeks(3);

        List<BatchStockModel> batchStockList = this.iBatchStockService.findValidProductsByDueDate(productId, dateRequirement);

        int availableQuantity = batchStockList.stream().mapToInt(BatchStockModel::getQuantity).sum();

        return desiredQuantity <= availableQuantity;
    }

    /**
     * This method check if the quantity of  products is available
     *
     * @param orderProductsList List of OrderProduct Entity
     * @return boolean checking availability
     * @throws OrderProductIsInvalidException when order product is invalid
     */
    private boolean verifyOrderIsValid(List<OrderProductsRequest> orderProductsList) throws OrderProductIsInvalidException {
        Set<Long> productIdListException = new HashSet<>();
        boolean isFailure = false;
        for (OrderProductsRequest orderProduct : orderProductsList) {
            boolean response = stockAvailable(orderProduct.getProductModel(), orderProduct.getQuantity());
            if (!response) {
                isFailure = true;
                productIdListException.add(orderProduct.getProductModel());
            }
        }
        if (isFailure) {
            String auxMessage = productIdListException.stream().map(String::valueOf).collect(Collectors.joining(","));

            String exceptionMessage = String.format("Pedido de compra inválido. Produtos com ID %s em quantidades insuficiente", auxMessage);

            throw new OrderProductIsInvalidException(exceptionMessage);
        }

        return true;
    }

    /**
     * This method save the products in orderProducts Entity
     *
     * @param purchaseOrderRequest from purchaseOrder instance
     * @return BigDecimal with sum of price the all products listed
     * @throws OrderProductIsInvalidException when order product is invalid
     */
    @Override
    public BigDecimal savePurchaseGetPrice(PurchaseOrderRequest purchaseOrderRequest) {
        boolean isOrderValid = (verifyOrderIsValid(purchaseOrderRequest.getProducts()));
        if (isOrderValid) {

            PurchaseOrderModel purchaseOrderModel = save(purchaseOrderRequest);

            List<OrderProductsModel> orderProductsModels = new ArrayList<>();

            BigDecimal totalPrice = new BigDecimal(0);

            purchaseOrderRequest.getProducts().forEach(p -> orderProductsModels.add(iOrderProductService.save(
                    new OrderProductsRequest(
                            p.getProductModel(),
                            p.getQuantity(),
                            purchaseOrderModel.getId()
                    ))));

            for (OrderProductsModel orderProductsModel : orderProductsModels) {
                totalPrice = totalPrice.add(orderProductsModel.getProductModel().getPrice().multiply(BigDecimal.valueOf(orderProductsModel.getQuantity())));
            }
            return totalPrice;

        } else {
            throw new OrderProductIsInvalidException("Pedido de compra inválido");
        }
    }

    /**
     * Return PurchaseOrderModel given id
     *
     * @param purchaseId the PurchaseOrderModel id
     * @return a PurchaseOrderModel
     * @throws PurchaseOrderByIdNotFoundException when purchase order not found
     */
    @Override
    public PurchaseOrderModel getById(Long purchaseId) throws PurchaseOrderByIdNotFoundException {
        return purchaseOrderRepository.findById(purchaseId).orElseThrow(() -> new PurchaseOrderByIdNotFoundException(purchaseId));
    }

    /**
     * This method get all Purchase Order
     *
     * @return List of PurchaseOrder entity
     */
    @Override
    public List<PurchaseOrderModel> getAll() {
        return purchaseOrderRepository.findAll();
    }

    /**
     * This method update status from PurchaseOrder related
     *
     * @param id Long related an purchaseOrder
     * @throws Exception when insufficient stock or product has expired due date
     */
    @Override
    public void updateStatus(Long id) throws NotEnoughStockException {
        List<OrderProductsModel> orderProductsList = iOrderProductService.getByPurchaseId(id);
        List<OrderProductsRequest> orderProductsRequestList = new ArrayList<>();
        orderProductsList.forEach(item -> orderProductsRequestList.add(OrderProductsRequest.builder()
                .productModel(item.getProductModel().getId())
                .quantity(item.getQuantity())
                .purchaseOrderModel(item.getPurchaseOrderModel().getId())
                .build()));

        verifyOrderIsValid(orderProductsRequestList);

        PurchaseOrderModel findbyIdPurchaseOrder = getById(id);
        findbyIdPurchaseOrder.setOrderStatus(OrderStatusEnum.CLOSED);
        findbyIdPurchaseOrder = purchaseOrderRepository.save(findbyIdPurchaseOrder);

        iBatchStockService.consumeBatchStockOnPurchase(findbyIdPurchaseOrder);
    }
}
