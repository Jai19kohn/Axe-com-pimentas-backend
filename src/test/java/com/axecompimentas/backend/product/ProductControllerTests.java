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
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.axecompimentas.backend.inventory.InventoryRepository;
import com.axecompimentas.backend.inventory.StockMovementRepository;
import com.axecompimentas.backend.supplier.SupplierRepository;

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

  @Autowired
  private StockMovementRepository stockMovementRepository;

  @Autowired
  private SupplierRepository supplierRepository;

  @BeforeEach
  void clearProducts() {
    stockMovementRepository.deleteAll();
    inventoryRepository.deleteAll();
    productRepository.deleteAll();
    supplierRepository.deleteAll();
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
          "fulfillmentMode": "MADE_TO_ORDER",
          "productionLeadTimeDays": 5
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
          "fulfillmentMode": "MADE_TO_ORDER",
          "productionLeadTimeDays": 5
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
        .andExpect(status().isConflict());
    mockMvc.perform(put(location)
        .header("Authorization", ADMIN_AUTHORIZATION)
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
            {
              "name": "Molho extra forte",
              "description": "Receita artesanal",
              "price": 21.90,
              "originalPrice": null,
              "categoryId": "molhos",
              "fulfillmentMode": "MADE_TO_ORDER",
              "productionLeadTimeDays": 5,
              "active": false
            }
            """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.active").value(false));
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
        .header("Authorization", ADMIN_AUTHORIZATION)
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
          "fulfillmentMode": "MADE_TO_ORDER",
          "productionLeadTimeDays": 5
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
          "fulfillmentMode": "CUSTOM",
          "productionLeadTimeDays": 10,
          "customizationDescription": "  Escolha até 3 pimentas  "
        }
        """;
    MvcResult createResult = mockMvc.perform(post("/api/products")
        .header("Authorization", ADMIN_AUTHORIZATION)
        .contentType(MediaType.APPLICATION_JSON)
        .content(product))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.productionLeadTimeDays").value(10))
        .andExpect(jsonPath("$.customizationDescription").value("Escolha até 3 pimentas"))
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
  void creatingProductWithInitialStockRecordsOneReceiptInAdministrativeHistory() throws Exception {
    MvcResult result = mockMvc.perform(post("/api/products")
        .header("Authorization", ADMIN_AUTHORIZATION)
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
            {
              "name": "Produto com estoque inicial",
              "description": "Descrição",
              "price": 10.00,
              "categoryId": "molhos",
              "fulfillmentMode": "READY_TO_SHIP",
              "initialStock": 8
            }
            """))
        .andExpect(status().isCreated())
        .andReturn();

    long id = productId(result.getResponse().getHeader("Location"));
    mockMvc.perform(get("/api/inventory/products/" + id + "/finished-stock"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.quantity").value(8));
    mockMvc.perform(get("/api/admin/inventory/movements").header("Authorization", ADMIN_AUTHORIZATION))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].productId").value(id))
        .andExpect(jsonPath("$[0].type").value("RECEIPT"))
        .andExpect(jsonPath("$[0].quantityDelta").value(8))
        .andExpect(jsonPath("$[0].resultingQuantity").value(8))
        .andExpect(jsonPath("$[0].reason").value("Estoque inicial"));
  }

  @Test
  void creatingProductsWithZeroOrMissingInitialStockDoesNotRecordMovement() throws Exception {
    for (String initialStock : new String[] { ", \"initialStock\": 0", "" }) {
      MvcResult result = mockMvc.perform(post("/api/products")
          .header("Authorization", ADMIN_AUTHORIZATION)
          .contentType(MediaType.APPLICATION_JSON)
          .content("""
              {
                "name": "Produto sem estoque",
                "description": "Descrição",
                "price": 10.00,
                "categoryId": "molhos",
                "fulfillmentMode": "READY_TO_SHIP" %s
              }
              """.formatted(initialStock)))
          .andExpect(status().isCreated())
          .andReturn();
      long id = productId(result.getResponse().getHeader("Location"));
      mockMvc.perform(get("/api/inventory/products/" + id + "/finished-stock"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.quantity").value(0));
    }

    mockMvc.perform(get("/api/admin/inventory/movements").header("Authorization", ADMIN_AUTHORIZATION))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(0));
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
  void movementEndpointRequiresAdminAndReplaysRequestsIdempotently() throws Exception {
    MvcResult createResult = mockMvc.perform(post("/api/products")
        .header("Authorization", ADMIN_AUTHORIZATION)
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
            {
              "name": "Produto",
              "description": "Descrição",
              "price": 10.00,
              "categoryId": "molhos",
              "fulfillmentMode": "READY_TO_SHIP"
            }
            """))
        .andExpect(status().isCreated())
        .andReturn();

    long productId = productId(createResult.getResponse().getHeader("Location"));
    String movementUrl = "/api/inventory/products/" + productId + "/finished-stock/movements";
    String requestKey = UUID.randomUUID().toString();
    String receipt = """
        {
          "type": "RECEIPT",
          "quantity": 8,
          "reason": "Compra recebida",
          "reference": "NF-123",
          "responsible": "Operador",
          "idempotencyKey": "%s"
        }
        """.formatted(requestKey);

    mockMvc.perform(post(movementUrl)
        .contentType(MediaType.APPLICATION_JSON)
        .content(receipt))
        .andExpect(status().isUnauthorized());
    mockMvc.perform(post(movementUrl)
        .header("Authorization", ADMIN_AUTHORIZATION)
        .contentType(MediaType.APPLICATION_JSON)
        .content(receipt))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.quantityDelta").value(8))
        .andExpect(jsonPath("$.resultingQuantity").value(8));
    mockMvc.perform(post(movementUrl)
        .header("Authorization", ADMIN_AUTHORIZATION)
        .contentType(MediaType.APPLICATION_JSON)
        .content(receipt))
        .andExpect(status().isOk());
    mockMvc.perform(get(movementUrl))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1));
    mockMvc.perform(get("/api/inventory/products/" + productId + "/finished-stock"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.quantity").value(8));
  }

  @Test
  void supplierCanBeCreatedSelectedAndSkuIsReservedAsSixDigits() throws Exception {
    mockMvc.perform(get("/api/products/next-sku")
        .header("Authorization", ADMIN_AUTHORIZATION))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.sku").value(org.hamcrest.Matchers.matchesPattern("\\d{6}")));

    mockMvc.perform(post("/api/suppliers")
        .header("Authorization", ADMIN_AUTHORIZATION)
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"name\":\"Fornecedor A\"}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.name").value("Fornecedor A"));
    long supplierId = supplierRepository.findAll().get(0).getId();

    mockMvc.perform(post("/api/products")
        .header("Authorization", ADMIN_AUTHORIZATION)
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
            {
              "name": "Produto",
              "description": "Descrição",
              "price": 10.00,
              "categoryId": "molhos",
              "fulfillmentMode": "READY_TO_SHIP",
              "sku": "654321",
              "supplierId": %d,
              "active": false
            }
            """.formatted(supplierId)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.supplier").value("Fornecedor A"))
        .andExpect(jsonPath("$.supplierId").value(supplierId))
        .andExpect(jsonPath("$.sku").value("654321"))
        .andExpect(jsonPath("$.active").value(false));

    mockMvc.perform(post("/api/suppliers")
        .header("Authorization", ADMIN_AUTHORIZATION)
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"name\":\"fornecedor a\"}"))
        .andExpect(status().isConflict());
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
        .header("Authorization", ADMIN_AUTHORIZATION)
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"quantity\":" + quantity + "}"))
        .andExpect(status().isOk());
  }

  private long productId(String location) {
    return Long.parseLong(location.substring(location.lastIndexOf('/') + 1));
  }
}