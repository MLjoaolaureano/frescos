package com.meli.frescos.service;

import com.meli.frescos.model.BatchStockModel;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class BatchStockServiceTest {

    @InjectMocks
    BatchStockService batchStockService;

    @Mock
    BatchStockModel batchStockModel;

}