package com.meli.frescos.controller.dto;

import com.meli.frescos.model.OrderProductsModel;
import com.meli.frescos.model.PurchaseOrderModel;
import lombok.*;

import javax.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PurchaseOrderRequest {

    @NotNull(message = "A data nao pode estar em branco")
    private LocalDate date;

    @NotNull(message = "O status nao pode estar em branco")
    private String orderStatus;

    @NotNull(message = "O comprador nao pode estar em branco")
    private Long buyer;

    @NotNull(message = "A lista de produtos nao pode estar vazia")
    private List<OrderProductsRequest> products;

    public PurchaseOrderRequest toModel() {
        return PurchaseOrderRequest.builder()
                .date(this.date)
                .orderStatus(this.orderStatus)
                .buyer(this.buyer)
                .products(this.products)
                .build();
    }
}