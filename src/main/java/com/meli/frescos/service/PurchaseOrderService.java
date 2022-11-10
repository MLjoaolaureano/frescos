package com.meli.frescos.service;

import com.meli.frescos.controller.dto.OrderProductsRequest;
import com.meli.frescos.controller.dto.PurchaseOrderRequest;
import com.meli.frescos.model.BuyerModel;
import com.meli.frescos.model.PurchaseOrderModel;
import com.meli.frescos.repository.BuyerRepository;
import com.meli.frescos.repository.PurchaseOrderRepository;
import org.aspectj.weaver.ast.Or;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PurchaseOrderService implements IPurchaseOrderService {

    private final PurchaseOrderRepository purchaseOrderRepository;

    private final BuyerRepository buyerRepository;

    private final OrderProductService orderProductService;

    public PurchaseOrderService (PurchaseOrderRepository purchaseOrderRepository, BuyerRepository buyerRepository, OrderProductService orderProductService) {
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.buyerRepository = buyerRepository;
        this.orderProductService = orderProductService;
    }

    @Override
    public PurchaseOrderModel save(PurchaseOrderRequest purchaseOrderRequest) {
        BuyerModel finBuyer = buyerRepository.getReferenceById(purchaseOrderRequest.getBuyer());
        PurchaseOrderModel purchase = new PurchaseOrderModel();
        purchase.setBuyer(finBuyer);
        purchase.setOrderStatus(purchaseOrderRequest.getOrderStatus());
        purchase.setDate(purchaseOrderRequest.getDate());

        purchaseOrderRequest.getProducts().stream().map(p -> orderProductService.save(new OrderProductsRequest(
                p.getProductModel(),
                p.getQuantity()
        )));

        PurchaseOrderModel result = purchaseOrderRepository.save(purchase);

        return result;
    }

    @Override
    public List<PurchaseOrderModel> findAll() {
        return null;
    }
}