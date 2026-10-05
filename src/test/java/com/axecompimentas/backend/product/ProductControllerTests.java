package com.axecompimentas.backend.product;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class ProductControllerTests {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ProductRepository productRepository;

        @BeforeEach
        void clearProducts() {
                productRepository.deleteAll();
        }

        @Test
        void productEndpointsCompleteCrudLifecycle() throws Exception {
                String newProduct = """
                                {
                                  "name": "Molho de pimenta",
                                  "description": "Molho artesanal",
                                  "price": 19.90,
                                  "originalPrice": 24.90,
                                  "categoryId": "molhos",
                                  "stock": 25,
                                  "fulfillmentMode": "MADE_TO_ORDER"
                                }
                                """;

                MvcResult createResult = mockMvc.perform(post("/api/products")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(newProduct))
                                .andExpect(status().isCreated())
                                .andExpect(header().exists("Location"))
                                .andExpect(jsonPath("$.name").value("Molho de pimenta"))
                                .andExpect(jsonPath("$.stock").value(25))
                                .andReturn();

                String location = createResult.getResponse().getHeader("Location");
                mockMvc.perform(get(location))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.categoryId").value("molhos"))
                                .andExpect(jsonPath("$.stock").value(25));
                mockMvc.perform(get("/api/products"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0].name").value("Molho de pimenta"))
                                .andExpect(jsonPath("$[0].stock").value(25));

                String updatedProduct = """
                                {
                                  "name": "Molho extra forte",
                                  "description": "Receita artesanal",
                                  "price": 21.90,
                                  "originalPrice": null,
                                  "categoryId": "molhos",
                                  "stock": 8,
                                  "fulfillmentMode": "MADE_TO_ORDER"
                                }
                                """;
                mockMvc.perform(put(location)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(updatedProduct))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.name").value("Molho extra forte"))
                                .andExpect(jsonPath("$.originalPrice").doesNotExist())
                                .andExpect(jsonPath("$.stock").value(8));

                mockMvc.perform(delete(location))
                                .andExpect(status().isNoContent());
                mockMvc.perform(get(location))
                                .andExpect(status().isNotFound());
        }

        @Test
        void rejectsInvalidProductPayload() throws Exception {
                String invalidProduct = """
                                {
                                  "name": " ",
                                  "description": "Molho artesanal",
                                  "price": 0,
                                  "originalPrice": null,
                                  "categoryId": "molhos",
                                  "stock": 0,
                                  "fulfillmentMode": "READY_TO_SHIP"
                                }
                                """;

                mockMvc.perform(post("/api/products")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(invalidProduct))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void returnsNotFoundForMissingProductsOnUpdateAndDelete() throws Exception {
                mockMvc.perform(put("/api/products/999")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {
                                                  "name": "Molho",
                                                  "description": "Molho artesanal",
                                                  "price": 10.00,
                                                  "originalPrice": null,
                                                  "categoryId": "molhos",
                                                  "stock": 0,
                                                  "fulfillmentMode": "READY_TO_SHIP"
                                                }
                                                """))
                                .andExpect(status().isNotFound());
                mockMvc.perform(delete("/api/products/999"))
                                .andExpect(status().isNotFound());
        }

        @Test
        void rejectsNegativeStock() throws Exception {
                mockMvc.perform(post("/api/products")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {
                                                  "name": "Molho",
                                                  "description": "Molho artesanal",
                                                  "price": 10.00,
                                                  "originalPrice": null,
                                                  "categoryId": "molhos",
                                                  "stock": -1,
                                                  "fulfillmentMode": "READY_TO_SHIP"
                                                }
                                                """))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void fulfillmentPlanUsesFinishedStockBeforeProductionForMadeToOrder() throws Exception {
                String product = """
                                {
                                  "name": "Molho sob demanda",
                                  "description": "Molho artesanal",
                                  "price": 15.00,
                                  "originalPrice": null,
                                  "categoryId": "molhos",
                                  "stock": 4,
                                  "fulfillmentMode": "MADE_TO_ORDER"
                                }
                                """;
                MvcResult createResult = mockMvc.perform(post("/api/products")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(product))
                                .andExpect(status().isCreated())
                                .andReturn();

                String location = createResult.getResponse().getHeader("Location");
                mockMvc.perform(get(location + "/fulfillment-plan?quantity=7"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.fulfillmentMode").value("MADE_TO_ORDER"))
                                .andExpect(jsonPath("$.quantityFromStock").value(4))
                                .andExpect(jsonPath("$.quantityToProduce").value(3))
                                .andExpect(jsonPath("$.canBeOrdered").value(true));
        }

        @Test
        void fulfillmentPlanDoesNotAllowReadyToShipProductWithoutEnoughStock() throws Exception {
                String product = """
                                {
                                  "name": "Molho pronta entrega",
                                  "description": "Molho artesanal",
                                  "price": 15.00,
                                  "originalPrice": null,
                                  "categoryId": "molhos",
                                  "stock": 4,
                                  "fulfillmentMode": "READY_TO_SHIP"
                                }
                                """;
                MvcResult createResult = mockMvc.perform(post("/api/products")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(product))
                                .andExpect(status().isCreated())
                                .andReturn();

                String location = createResult.getResponse().getHeader("Location");
                mockMvc.perform(get(location + "/fulfillment-plan?quantity=5"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.quantityToProduce").value(0))
                                .andExpect(jsonPath("$.canBeOrdered").value(false));
        }

        @Test
        void fulfillmentPlanSendsCustomProductToProductionWithoutFinishedStock() throws Exception {
                String product = """
                                {
                                  "name": "Molho personalizado",
                                  "description": "Molho com escolha de pimentas",
                                  "price": 18.00,
                                  "originalPrice": null,
                                  "categoryId": "molhos",
                                  "stock": 0,
                                  "fulfillmentMode": "CUSTOM"
                                }
                                """;
                MvcResult createResult = mockMvc.perform(post("/api/products")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(product))
                                .andExpect(status().isCreated())
                                .andReturn();

                String location = createResult.getResponse().getHeader("Location");
                mockMvc.perform(get(location + "/fulfillment-plan?quantity=3"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.quantityFromStock").value(0))
                                .andExpect(jsonPath("$.quantityToProduce").value(3))
                                .andExpect(jsonPath("$.canBeOrdered").value(true));
        }

        @Test
        void fulfillmentPlanRequiresPositiveQuantity() throws Exception {
                mockMvc.perform(get("/api/products/999/fulfillment-plan?quantity=0"))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void allowsCorsPreflightForFrontendOrigin() throws Exception {
                mockMvc.perform(options("/api/products")
                                .header("Origin", "http://localhost:4200")
                                .header("Access-Control-Request-Method", "PUT"))
                                .andExpect(status().isOk())
                                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:4200"))
                                .andExpect(
                                                header().string("Access-Control-Allow-Methods",
                                                                org.hamcrest.Matchers.containsString("PUT")));
        }
}