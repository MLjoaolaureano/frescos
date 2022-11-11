package com.meli.frescos.service;

import com.meli.frescos.controller.dto.PurchaseOrderRequest;
import com.meli.frescos.model.BatchStockModel;
import com.meli.frescos.model.BuyerModel;
import com.meli.frescos.model.PurchaseOrderModel;
import com.meli.frescos.repository.BuyerRepository;
import com.meli.frescos.repository.PurchaseOrderRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class PurchaseOrderService implements IPurchaseOrderService {

    private final PurchaseOrderRepository purchaseOrderRepository;

    private final BuyerRepository buyerRepository;

    private final OrderProductService orderProductService;

    private final BatchStockService batchStockService;

    public PurchaseOrderService(PurchaseOrderRepository purchaseOrderRepository, BuyerRepository buyerRepository, OrderProductService orderProductService, BatchStockService batchStockService) {
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.buyerRepository = buyerRepository;
        this.orderProductService = orderProductService;
        this.batchStockService = batchStockService;
    }

    @Override
    public PurchaseOrderModel save(PurchaseOrderRequest purchaseOrderRequest) {
        BuyerModel finBuyer = buyerRepository.getReferenceById(purchaseOrderRequest.getBuyer());
        PurchaseOrderModel purchase = new PurchaseOrderModel();
        purchase.setBuyer(finBuyer);
        purchase.setOrderStatus(purchaseOrderRequest.getOrderStatus());
        purchase.setDate(purchaseOrderRequest.getDate());

//        PurchaseOrderModel result = purchaseOrderRepository.save(purchase);

        purchaseOrderRequest.getProducts().stream().forEach(p ->
                {
                    stockAvailable(p.getProductModel(), p.getQuantity());
                }
        );

//        purchaseOrderRequest.getProducts().stream().map(p -> orderProductService.save(new OrderProductsRequest(
//                p.getProductModel(),
//                p.getQuantity(),
//                result.getId()
//        )));

        return null;
    }

    private boolean stockAvailable(Long productId, int desiredQuantity) {

        LocalDate dateRequirement = LocalDate.now().plusWeeks(3);

        List<BatchStockModel> batchStockList = this.batchStockService.findValidProductsByDueDate(productId, dateRequirement);

        int availableQuantity = batchStockList.stream().mapToInt(BatchStockModel::getQuantity).sum();

        return desiredQuantity <= availableQuantity;
    }

    @Override
    public List<PurchaseOrderModel> getAll() {
        return null;
    }
}