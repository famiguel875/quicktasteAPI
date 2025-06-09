// src/test/kotlin/com/es/controller/ProductoControllerTest.kt

import com.es.dto.ProductoDTO
import com.es.quicktasteAPIAplication
import com.es.service.ProductoService
import com.es.service.UsuarioService
import com.fasterxml.jackson.databind.ObjectMapper
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.just
import io.mockk.runs
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest(classes = [quicktasteAPIAplication::class])
@AutoConfigureMockMvc  // dejamos los filtros activos
class ProductoControllerTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    private val mapper = ObjectMapper()

    @MockkBean
    lateinit var productoService: ProductoService

    @MockkBean
    lateinit var usuarioService: UsuarioService

    private val sample = ProductoDTO(
        name = "p1",
        category = "cat1",
        stock = 10,
        description = "desc",
        price = 5.0,
        image = "img.jpg",
        allowedEmails = listOf("u@example.com")
    )

    @Test @DisplayName("GET /productos → 200 and list")
    fun `getAll returns list`() {
        every { productoService.findAll() } returns listOf(sample)

        mockMvc.perform(get("/productos")
            .with(user("user1").roles("USER")))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].name").value("p1"))
    }

    @Test @DisplayName("GET /productos/{name} → 200 and product")
    fun `getByName returns product`() {
        every { productoService.findByName("p1") } returns sample

        mockMvc.perform(get("/productos/p1")
            .with(user("user1").roles("USER")))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.category").value("cat1"))
    }

    @Test @DisplayName("GET /productos/categoria/{cat} → 200 and filtered list")
    fun `getByCategory returns filtered`() {
        every { productoService.findByCategory("cat1") } returns listOf(sample)

        mockMvc.perform(get("/productos/categoria/cat1")
            .with(user("user1").roles("USER")))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].description").value("desc"))
    }

    @Test @DisplayName("POST /productos → 201 and created")
    fun `create returns created`() {
        every { productoService.create(sample) } returns sample

        mockMvc.perform(post("/productos")
            .with(user("admin").roles("ADMIN"))
            .contentType(MediaType.APPLICATION_JSON)
            .content(mapper.writeValueAsString(sample)))
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.price").value(5.0))
    }

    @Test @DisplayName("PUT /productos/{name} → 200 and updated")
    fun `update returns updated`() {
        val updated = sample.copy(price = 6.0)
        every { productoService.update("p1", updated) } returns updated

        mockMvc.perform(put("/productos/p1")
            .with(user("admin").roles("ADMIN"))
            .contentType(MediaType.APPLICATION_JSON)
            .content(mapper.writeValueAsString(updated)))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.price").value(6.0))
    }

    @Test @DisplayName("DELETE /productos/{name} → 204 no content")
    fun `delete returns no content`() {
        every { productoService.delete("p1") } just runs

        mockMvc.perform(delete("/productos/p1")
            .with(user("admin").roles("ADMIN")))
            .andExpect(status().isNoContent)
    }

    @Nested @DisplayName("PUT /productos/{name}/stock")
    inner class UpdateStock {
        @Test @DisplayName("200 when stock provided")
        fun `200 valid stock`() {
            every { productoService.updateStockForOrder("p1", 20) } returns sample.copy(stock = 20)

            mockMvc.perform(put("/productos/p1/stock")
                .with(user("user1").roles("USER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"stock":20}"""))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.stock").value(20))
        }

        @Test @DisplayName("400 when stock missing")
        fun `400 missing stock`() {
            mockMvc.perform(put("/productos/p1/stock")
                .with(user("user1").roles("USER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isBadRequest)
        }
    }

    @Nested @DisplayName("PUT /productos/{name}/price")
    inner class UpdatePrice {
        @Test @DisplayName("200 when price provided")
        fun `200 valid price`() {
            every { productoService.updatePrice("p1", 9.5) } returns sample.copy(price = 9.5)

            mockMvc.perform(put("/productos/p1/price")
                .with(user("user1").roles("USER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"price":9.5}"""))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.price").value(9.5))
        }

        @Test @DisplayName("400 when price missing")
        fun `400 missing price`() {
            mockMvc.perform(put("/productos/p1/price")
                .with(user("user1").roles("USER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isBadRequest)
        }
    }

    @Nested @DisplayName("PUT /productos/{name}/image")
    inner class UpdateImage {
        @Test @DisplayName("200 when image provided")
        fun `200 valid image`() {
            every { productoService.updateImage("p1", "new.jpg") } returns sample.copy(image = "new.jpg")

            mockMvc.perform(put("/productos/p1/image")
                .with(user("admin").roles("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"image":"new.jpg"}"""))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.image").value("new.jpg"))
        }

        @Test @DisplayName("400 when image missing")
        fun `400 missing image`() {
            mockMvc.perform(put("/productos/p1/image")
                .with(user("admin").roles("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isBadRequest)
        }
    }

    @Test @DisplayName("GET /productos/allowed → 200 filtered by user")
    fun `getAllowed returns filtered`() {
        every { usuarioService.findByUsername("user1") } returns
                com.es.dto.UsuarioDTO(
                    email    = "u@example.com",
                    username = "user1",
                    password = "",
                    roles    = "USER",
                    image    = "",
                    wallet   = 0   // <-- aquí como Int, no String
                )
        every { productoService.findAllowedForUser("u@example.com") } returns listOf(sample)

        mockMvc.perform(get("/productos/allowed")
            .with(user("user1").roles("USER")))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].allowedEmails[0]").value("u@example.com"))
    }
}
