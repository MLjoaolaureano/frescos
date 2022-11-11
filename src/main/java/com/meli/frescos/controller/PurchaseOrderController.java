package com.meli.frescos.controller;

import com.meli.frescos.controller.dto.PurchaseOrderRequest;
import com.meli.frescos.controller.dto.PurchaseOrderResponse;
import com.meli.frescos.service.PurchaseOrderService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.math.BigDecimal;

@RestController
@RequestMapping("purchase-order")
public class PurchaseOrderController {

    private final PurchaseOrderService purchaseOrderService;

    public PurchaseOrderController (PurchaseOrderService purchaseOrderService) {
        this.purchaseOrderService = purchaseOrderService;
    }

    @PostMapping
    ResponseEntity<PurchaseOrderResponse> save(@RequestBody @Valid PurchaseOrderRequest purchaseOrderRequest) {

        BigDecimal insertPurchase = purchaseOrderService.savePurchaseGetPrice(purchaseOrderRequest.toModel());

        return new ResponseEntity<>(PurchaseOrderResponse.toResponse(insertPurchase), HttpStatus.CREATED);

    }

    @PatchMapping("/{id}")
    ResponseEntity<PurchaseOrderResponse> updateStatus(@PathVariable Long id, @RequestBody String orderStatus) {
        PurchaseOrderModel updateStatus = purchaseOrderService.updateStatus(id, orderStatus);
        return new ResponseEntity<>(PurchaseOrderResponse.toResponse(updateStatus), HttpStatus.OK);
    }
}
