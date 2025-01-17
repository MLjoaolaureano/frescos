package com.meli.frescos.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meli.frescos.controller.dto.CommentRequest;
import com.meli.frescos.controller.dto.WarehouseRequest;
import com.meli.frescos.exception.InvalidCommentException;
import com.meli.frescos.model.*;
import com.meli.frescos.repository.*;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CommentControllerIT {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SellerRepository sellerRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private WarehouseRepository warehouseRepository;

    @Autowired
    private SectionRepository sectionRepository;

    @Autowired
    private CommentRepository commentRepository;
    @Autowired
    private BuyerRepository buyerRepository;
    @Autowired
    private PurchaseOrderRepository purchaseOrderRepository;
    @Autowired
    private BatchStockRepository batchStockRepository;
    @Autowired
    private OrderProductsRepository orderProductsRepository;
    @Autowired
    private RepresentativeRepository representativeRepository;

    @BeforeEach
    @AfterEach
    void setup() {

        orderProductsRepository.deleteAllInBatch();
        purchaseOrderRepository.deleteAllInBatch();
        batchStockRepository.deleteAllInBatch();
        representativeRepository.deleteAllInBatch();
        commentRepository.deleteAllInBatch();
        buyerRepository.deleteAllInBatch();
        productRepository.deleteAllInBatch();
        sellerRepository.deleteAllInBatch();
        sectionRepository.deleteAllInBatch();
        warehouseRepository.deleteAllInBatch();


    }

    @Test
    @DisplayName("Test Comment Successfull Creation - POST Endpoint")
    void create_returnsCreatedStatus_whenSuccess() throws Exception {
        WarehouseModel warehouse = createWarehouse();
        SectionModel section = createSection(warehouse.getId());
        SellerModel seller = createSeller();
        BuyerModel buyer = createBuyer("08392648609", "Test Buyer");
        createRepresentative(warehouse.getId());
        ProductModel product = createProduct(seller);
        createBatchStockRequest(section, product);
        PurchaseOrderModel purchaseOrder = createPurchaseOrder(OrderStatusEnum.CLOSED, buyer);
        createOrderProduct(product, purchaseOrder, 10);

        String comment = "Test";
        LocalDateTime date = LocalDateTime.now();
        CommentRequest commentRequest = CommentRequest.builder()
                .comment(comment)
                .createdAt(date)
                .buyerId(buyer.getId())
                .productId(product.getId())
                .build();

        ResultActions response = mockMvc.perform(
                post("/comment")
                        .content(objectMapper.writeValueAsString(commentRequest))
                        .contentType(MediaType.APPLICATION_JSON)
        );

        response.andExpect(status().isCreated());
    }

    @Test
    @DisplayName("Test Comment Creation with Open PurchaseOrder  - POST Endpoint")
    void create_throwsInvalidCommentException_whenPurchaseOrderIsOpen() throws Exception {
        WarehouseModel warehouse = createWarehouse();
        SectionModel section = createSection(warehouse.getId());
        SellerModel seller = createSeller();
        BuyerModel buyer = createBuyer("08392648609", "Test Buyer");
        createRepresentative(warehouse.getId());
        ProductModel product = createProduct(seller);
        createBatchStockRequest(section, product);
        PurchaseOrderModel purchaseOrder = createPurchaseOrder(OrderStatusEnum.OPEN, buyer);
        createOrderProduct(product, purchaseOrder, 10);

        String comment = "Test";
        LocalDateTime date = LocalDateTime.now();
        CommentRequest commentRequest = CommentRequest.builder()
                .comment(comment)
                .createdAt(date)
                .buyerId(buyer.getId())
                .productId(product.getId())
                .build();

        ResultActions response = mockMvc.perform(
                post("/comment")
                        .content(objectMapper.writeValueAsString(commentRequest))
                        .contentType(MediaType.APPLICATION_JSON)
        );


        response.andExpect(status().isBadRequest())
                .andExpect(result -> assertTrue(
                        result.getResolvedException() instanceof InvalidCommentException))
                .andExpect(jsonPath("$.message",
                        CoreMatchers.is(String.format("Comprador de ID %d não possui compra fechada do produto de ID %d", buyer.getId(), product.getId()))));
    }

    @Test
    @DisplayName("Test Comment Creation with Open PurchaseOrder  - POST Endpoint")
    void create_throwsInvalidCommentException_whenBuyerAlreadyPurchased() throws Exception {
        WarehouseModel warehouse = createWarehouse();
        SectionModel section = createSection(warehouse.getId());
        SellerModel seller = createSeller();
        BuyerModel buyer = createBuyer("08392648609", "Test Buyer");
        createRepresentative(warehouse.getId());
        ProductModel product = createProduct(seller);
        createBatchStockRequest(section, product);
        PurchaseOrderModel purchaseOrder = createPurchaseOrder(OrderStatusEnum.CLOSED, buyer);
        createOrderProduct(product, purchaseOrder, 10);

        String comment = "Test";
        LocalDateTime date = LocalDateTime.now();
        CommentRequest commentRequest = CommentRequest.builder()
                .comment(comment)
                .createdAt(date)
                .buyerId(buyer.getId())
                .productId(product.getId())
                .build();

        commentRepository.save(commentRequest.toModel());

        ResultActions response = mockMvc.perform(
                post("/comment")
                        .content(objectMapper.writeValueAsString(commentRequest))
                        .contentType(MediaType.APPLICATION_JSON)
        );


        response.andExpect(status().isBadRequest())
                .andExpect(result -> assertTrue(
                        result.getResolvedException() instanceof InvalidCommentException))
                .andExpect(jsonPath("$.message",
                        CoreMatchers.is(String.format("Comprador de ID %d já comentou no produto de ID %d", buyer.getId(), product.getId()))));
    }

    @Test
    @DisplayName("Test Comment Successfull return list of Commentary Ordered by Create Date - GET Endpoint")
    void getRecentComment_returnsListOfCommentary_whenSuccess() throws Exception {
        WarehouseModel warehouse = createWarehouse();
        SectionModel section = createSection(warehouse.getId());
        SellerModel seller = createSeller();
        createRepresentative(warehouse.getId());
        ProductModel product = createProduct(seller);
        createBatchStockRequest(section, product);

        BuyerModel buyer = createBuyer("08392648609", "Test Buyer");
        PurchaseOrderModel purchaseOrder = createPurchaseOrder(OrderStatusEnum.CLOSED, buyer);
        createOrderProduct(product, purchaseOrder, 10);

        BuyerModel buyer2 = createBuyer("27430121020", "Test Buyer");
        PurchaseOrderModel purchaseOrder2 = createPurchaseOrder(OrderStatusEnum.CLOSED, buyer2);
        createOrderProduct(product, purchaseOrder2, 10);

        BuyerModel buyer3 = createBuyer("58967427034", "Test Buyer");
        PurchaseOrderModel purchaseOrder3 = createPurchaseOrder(OrderStatusEnum.CLOSED, buyer3);
        createOrderProduct(product, purchaseOrder3, 10);

        commentRepository.save(CommentRequest.builder()
                .comment("Latest coment")
                .createdAt(LocalDateTime.now())
                .buyerId(buyer.getId())
                .productId(product.getId())
                .build()
                .toModel()
        );
        commentRepository.save(CommentRequest.builder()
                .comment("Mid comment")
                .createdAt(LocalDateTime.now())
                .buyerId(buyer2.getId())
                .productId(product.getId())
                .build()
                .toModel()
        );
        commentRepository.save(CommentRequest.builder()
                .comment("Earliest comment")
                .createdAt(LocalDateTime.now())
                .buyerId(buyer3.getId())
                .productId(product.getId())
                .build()
                .toModel()
        );

        ResultActions response = mockMvc.perform(
                get("/comment/{productId}", product.getId())
                        .contentType(MediaType.APPLICATION_JSON)
        );

        response
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.comments[0].comment", CoreMatchers.is("Earliest comment")))
                .andExpect(jsonPath("$.comments[1].comment", CoreMatchers.is("Mid comment")))
                .andExpect(jsonPath("$.comments[2].comment", CoreMatchers.is("Latest coment")))
        ;
    }

//
//    @Test
//    @DisplayName("Test Seller Creation with wrong rating - POST Endpoint")
//    void create_throwsMethodArgumentNotValidException_whenRatingIsAboveMax() throws Exception {
//        String name = "Seller";
//        String cpf = "41937616576";
//        Double rating = Double.valueOf(10.0);
//
//        SellerRequest sellerRequest = SellerRequest.builder()
//                .cpf(cpf)
//                .name(name)
//                .rating(rating)
//                .build();
//
//        ResultActions response = mockMvc.perform(
//                post("/seller")
//                        .content(objectMapper.writeValueAsString(sellerRequest))
//                        .contentType(MediaType.APPLICATION_JSON)
//        );
//
//        response.andExpect(status().isBadRequest())
//                .andExpect(result -> assertTrue(
//                        result.getResolvedException() instanceof MethodArgumentNotValidException))
//                .andExpect(jsonPath("$.fieldsMessages", CoreMatchers.containsString("A avaliação do vendedor não pode ser maior que 5 e deve conter no máximo duas casas decimais")))
//                .andExpect(jsonPath("$.fieldsMessages", CoreMatchers.containsString("A avaliação do vendedor deve ser de 0 à 5")));
//    }
//
//    @Test
//    @DisplayName("Test Get all Seller - GET Endpoint")
//    void getAll_returnListOfSeller_whenSuccess() throws Exception {
//        String name = "Seller";
//        String cpf = "41937616576";
//        Double rating = Double.valueOf(5.0);
//
//        SellerRequest sellerRequest = SellerRequest.builder()
//                .cpf(cpf)
//                .name(name)
//                .rating(rating)
//                .build();
//
//
//        sellerService.save(sellerRequest.toModel());
//
//        ResultActions response = mockMvc.perform(
//                get("/seller")
//                        .contentType(MediaType.APPLICATION_JSON)
//        );
//
//        response.andExpect(status().isOk())
//                .andExpect(jsonPath("$", hasSize(1)));
//    }


    WarehouseModel createWarehouse() {
        String city = "Tramandaí";
        String district = "Zona Nova";
        String state = "Rio Grande do Sul";
        String postalCode = "99999999";
        String street = "Avenida Emancipacao";

        WarehouseRequest newWarehouseRequest = WarehouseRequest
                .builder()
                .city(city)
                .street(street)
                .state(state)
                .postalCode(postalCode)
                .district(district).build();
        return warehouseRepository.save(newWarehouseRequest.toModel());
    }

    SectionModel createSection(long warehouseCode) {
        String description = "Laranja Bahia";
        CategoryEnum category = CategoryEnum.FRESH;
        Double totalSize = 5.5;
        Double temperature = 2.0;

        WarehouseModel warehouseResponse = new WarehouseModel();
        warehouseResponse.setId(warehouseCode);

        SectionModel sectionModel = new SectionModel();
        sectionModel.setCategory(category);
        sectionModel.setDescription(description);
        sectionModel.setTotalSize(totalSize);
        sectionModel.setTemperature(temperature);
        sectionModel.setWarehouse(warehouseResponse);

        return sectionRepository.save(sectionModel);
    }

    SellerModel createSeller() {
        Long id = 1L;
        String name = "Vendedor 1";
        String cpf = "12345678900";
        double rating = 4.2;

        SellerModel seller = new SellerModel(id, name, cpf, rating);

        return sellerRepository.save(seller);
    }

    BuyerModel createBuyer(String cpf, String name) {


        BuyerModel buyerModel = new BuyerModel();
        buyerModel.setName(name);
        buyerModel.setCpf(cpf);

        return buyerRepository.save(buyerModel);
    }

    RepresentativeModel createRepresentative(long warehouseCode) {
        String name = "Representative name";
        WarehouseModel warehouseResponse = new WarehouseModel();
        warehouseResponse.setId(warehouseCode);

        RepresentativeModel responseModel = new RepresentativeModel();
        responseModel.setName(name);
        responseModel.setWarehouse(warehouseResponse);

        return representativeRepository.save(responseModel);
    }

    ProductModel createProduct(SellerModel seller) {
        CategoryEnum categoryEnum = CategoryEnum.FROZEN;
        LocalDate createDate = LocalDate.now();
        String description = "Test Description";
        BigDecimal price = BigDecimal.valueOf(1L);
        String productTitle = "Test Title";
        Double unitVolume = Double.valueOf(1.0);
        Double unitWeight = Double.valueOf(1.0);

        ProductModel productModel = new ProductModel();
        productModel.setCreateDate(createDate);
        productModel.setPrice(price);
        productModel.setSeller(seller);
        productModel.setCategory(categoryEnum);
        productModel.setDescription(description);
        productModel.setUnitVolume(unitVolume);
        productModel.setUnitWeight(unitWeight);
        productModel.setProductTitle(productTitle);

        return productRepository.save(productModel);
    }

    BatchStockModel createBatchStockRequest(SectionModel sectionModel, ProductModel productModel) {
        String batchNumber = "TST-123";
        Integer quantity = 100;
        LocalDate manufacturingDate = LocalDate.now().minusWeeks(1);
        LocalDateTime manufacturingDatetime = LocalDateTime.now();
        LocalDate dueDate = LocalDate.now().plusWeeks(10);

        BatchStockModel batchStock = new BatchStockModel();
        batchStock.setProduct(productModel);
        batchStock.setQuantity(quantity);
        batchStock.setBatchNumber(batchNumber);
        batchStock.setSection(sectionModel);
        batchStock.setDueDate(dueDate);
        batchStock.setManufacturingDate(manufacturingDate);
        batchStock.setManufacturingTime(manufacturingDatetime);

        return batchStockRepository.save(batchStock);
    }

    PurchaseOrderModel createPurchaseOrder(OrderStatusEnum status, BuyerModel buyerModel) {
        LocalDate date = LocalDate.now();

        PurchaseOrderModel purchaseOrderModel = new PurchaseOrderModel();
        purchaseOrderModel.setBuyer(buyerModel);
        purchaseOrderModel.setOrderStatus(status);
        purchaseOrderModel.setDate(date);

        return purchaseOrderRepository.save(purchaseOrderModel);
    }

    OrderProductsModel createOrderProduct(ProductModel productModel, PurchaseOrderModel purchaseOrderModel, int quantity) {
        OrderProductsModel order = new OrderProductsModel();
        order.setQuantity(quantity);
        order.setProductModel(productModel);
        order.setPurchaseOrderModel(purchaseOrderModel);

        return orderProductsRepository.save(order);
    }


}