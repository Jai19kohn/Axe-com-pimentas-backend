package com.axecompimentas.backend.product;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.axecompimentas.backend.inventory.InventoryRepository;

@SpringBootTest
@AutoConfigureMockMvc
class ProductControllerTests {

  private static final String ADMIN_AUTHORIZATION = "Basic " + Base64.getEncoder()
      .encodeToString("test-admin:test-password".getBytes(StandardCharsets.UTF_8));

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ProductRepository productRepository;

  @Autowired
  private InventoryRepository inventoryRepository;

  @BeforeEach
  void clearProducts() {
    inventoryRepository.deleteAll();
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
          "fulfillmentMode": "MADE_TO_ORDER"
        }
        """;

    MvcResult createResult = mockMvc.perform(post("/api/products")
        .header("Authorization", ADMIN_AUTHORIZATION)
        .contentType(MediaType.APPLICATION_JSON)
        .content(newProduct))
        .andExpect(status().isCreated())
        .andExpect(header().exists("Location"))
        .andExpect(jsonPath("$.name").value("Molho de pimenta"))
        .andExpect(jsonPath("$.stock").doesNotExist())
        .andReturn();

    String location = createResult.getResponse().getHeader("Location");
    setFinishedStock(productId(location), 25);
    mockMvc.perform(get(location))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.categoryId").value("molhos"))
        .andExpect(jsonPath("$.stock").doesNotExist());
    mockMvc.perform(get("/api/products"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].name").value("Molho de pimenta"))
        .andExpect(jsonPath("$[0].stock").doesNotExist());

    String updatedProduct = """
        {
          "name": "Molho extra forte",
          "description": "Receita artesanal",
          "price": 21.90,
          "originalPrice": null,
          "categoryId": "molhos",
          "fulfillmentMode": "MADE_TO_ORDER"
        }
        """;
    mockMvc.perform(put(location)
        .header("Authorization", ADMIN_AUTHORIZATION)
        .contentType(MediaType.APPLICATION_JSON)
        .content(updatedProduct))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Molho extra forte"))
        .andExpect(jsonPath("$.originalPrice").doesNotExist())
        .andExpect(jsonPath("$.stock").doesNotExist());

    setFinishedStock(productId(location), 8);
    mockMvc.perform(get("/api/inventory/products/" + productId(location) + "/finished-stock"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.quantity").value(8));

    mockMvc.perform(delete(location).header("Authorization", ADMIN_AUTHORIZATION))
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
          "fulfillmentMode": "READY_TO_SHIP"
        }
        """;

    mockMvc.perform(post("/api/products")
        .header("Authorization", ADMIN_AUTHORIZATION)
        .contentType(MediaType.APPLICATION_JSON)
        .content(invalidProduct))
        .andExpect(status().isBadRequest());
  }

  @Test
  void returnsNotFoundForMissingProductsOnUpdateAndDelete() throws Exception {
    mockMvc.perform(put("/api/products/999")
        .header("Authorization", ADMIN_AUTHORIZATION)
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
            {
              "name": "Molho",
              "description": "Molho artesanal",
              "price": 10.00,
              "originalPrice": null,
              "categoryId": "molhos",
              "fulfillmentMode": "READY_TO_SHIP"
            }
            """))
        .andExpect(status().isNotFound());
    mockMvc.perform(delete("/api/products/999").header("Authorization", ADMIN_AUTHORIZATION))
        .andExpect(status().isNotFound());
  }

  @Test
  void rejectsNegativeFinishedStock() throws Exception {
    mockMvc.perform(put("/api/inventory/products/1/finished-stock")
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
            { "quantity": -1 }
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
          "fulfillmentMode": "MADE_TO_ORDER"
        }
        """;
    MvcResult createResult = mockMvc.perform(post("/api/products")
        .header("Authorization", ADMIN_AUTHORIZATION)
        .contentType(MediaType.APPLICATION_JSON)
        .content(product))
        .andExpect(status().isCreated())
        .andReturn();

    String location = createResult.getResponse().getHeader("Location");
    setFinishedStock(productId(location), 4);
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
          "fulfillmentMode": "READY_TO_SHIP"
        }
        """;
    MvcResult createResult = mockMvc.perform(post("/api/products")
        .header("Authorization", ADMIN_AUTHORIZATION)
        .contentType(MediaType.APPLICATION_JSON)
        .content(product))
        .andExpect(status().isCreated())
        .andReturn();

    String location = createResult.getResponse().getHeader("Location");
    setFinishedStock(productId(location), 4);
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
          "fulfillmentMode": "CUSTOM"
        }
        """;
    MvcResult createResult = mockMvc.perform(post("/api/products")
        .header("Authorization", ADMIN_AUTHORIZATION)
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
  void protectsProductRegistrationAndValidatesAdminCredentials() throws Exception {
    mockMvc.perform(post("/api/products")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{}"))
        .andExpect(status().isUnauthorized());

    mockMvc.perform(get("/api/admin/session"))
        .andExpect(status().isUnauthorized());

    mockMvc.perform(get("/api/admin/session")
        .header("Authorization", ADMIN_AUTHORIZATION))
        .andExpect(status().isNoContent());
  }

  @Test
  void rejectsInvalidAdminCredentials() throws Exception {
    String invalidAuthorization = "Basic " + Base64.getEncoder()
        .encodeToString("test-admin:wrong-password".getBytes(StandardCharsets.UTF_8));

    mockMvc.perform(post("/api/products")
        .header("Authorization", invalidAuthorization)
        .contentType(MediaType.APPLICATION_JSON)
        .content("{}"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void inventoryEndpointReadsAndSetsFinishedStock() throws Exception {
    MvcResult createResult = mockMvc.perform(post("/api/products")
        .header("Authorization", ADMIN_AUTHORIZATION)
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
            {
              "name": "Produto com estoque",
              "description": "Descrição",
              "price": 10.00,
              "originalPrice": null,
              "categoryId": "molhos",
              "fulfillmentMode": "READY_TO_SHIP"
            }
            """))
        .andExpect(status().isCreated())
        .andReturn();

    long productId = productId(createResult.getResponse().getHeader("Location"));
    mockMvc.perform(get("/api/inventory/products/" + productId + "/finished-stock"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.quantity").value(0));

    setFinishedStock(productId, 12);
    mockMvc.perform(get("/api/inventory/products/" + productId + "/finished-stock"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.quantity").value(12));
  }

  @Test
  void inventoryEndpointReturnsNotFoundForUnknownProduct() throws Exception {
    mockMvc.perform(get("/api/inventory/products/999/finished-stock"))
        .andExpect(status().isNotFound());
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

  private void setFinishedStock(long productId, int quantity) throws Exception {
    mockMvc.perform(put("/api/inventory/products/" + productId + "/finished-stock")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"quantity\":" + quantity + "}"))
        .andExpect(status().isOk());
  }

  private long productId(String location) {
    return Long.parseLong(location.substring(location.lastIndexOf('/') + 1));
  }
}