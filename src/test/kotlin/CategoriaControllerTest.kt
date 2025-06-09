// src/test/kotlin/com/es/controller/CategoriaControllerTest.kt

import com.es.dto.CategoriaDTO
import com.es.quicktasteAPIAplication
import com.es.service.CategoriaService
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
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

@SpringBootTest(classes = [quicktasteAPIAplication::class])
@AutoConfigureMockMvc
class CategoriaControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    private val mapper = ObjectMapper()

    @MockkBean
    private lateinit var categoriaService: CategoriaService

    private val sample = CategoriaDTO(
        name  = "cat1",
        image = "img.png"
    )

    @Test
    @DisplayName("GET /categorias → 200 and list")
    fun `getAll returns list`() {
        // Given: the service returns one sample category
        every { categoriaService.findAll() } returns listOf(sample)

        // When & Then: GET /categorias should return HTTP 200 with our sample
        mockMvc.perform(get("/categorias")
            .with(user("user1").roles("USER")))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].name").value("cat1"))
    }

    @Test
    @DisplayName("GET /categorias/{name} → 200 and single category")
    fun `getByName returns category`() {
        // Given: the service returns sample for name "cat1"
        every { categoriaService.findByName("cat1") } returns sample

        // When & Then: GET /categorias/cat1 should return HTTP 200 with that category
        mockMvc.perform(get("/categorias/cat1")
            .with(user("user1").roles("USER")))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.image").value("img.png"))
    }

    @Nested
    @DisplayName("POST /categorias")
    inner class Create {
        @Test
        @DisplayName("→ 201 ADMIN can create")
        fun `admin create`() {
            // Only ADMIN may call create
            every { categoriaService.create(match { it.name == "cat1" && it.image == "img.png" }) } returns sample

            mockMvc.perform(post("/categorias")
                .with(user("admin").roles("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(sample)))
                .andExpect(status().isCreated)
                .andExpect(jsonPath("$.name").value("cat1"))
        }

        @Test
        @DisplayName("→ 201 USER can create too")
        fun `user create`() {
            // Stub service for USER as well
            every { categoriaService.create(match { it.name == "cat1" && it.image == "img.png" }) } returns sample

            mockMvc.perform(post("/categorias")
                .with(user("user1").roles("USER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(sample)))
                .andExpect(status().isCreated)
                .andExpect(jsonPath("$.name").value("cat1"))
        }
    }

    @Nested
    @DisplayName("PUT /categorias/{name}")
    inner class Update {
        @Test
        @DisplayName("→ 200 ADMIN can update")
        fun `admin update`() {
            val updated = sample.copy(image = "new.png")
            every { categoriaService.update("cat1", match { it.image == "new.png" }) } returns updated

            mockMvc.perform(put("/categorias/cat1")
                .with(user("admin").roles("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(updated)))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.image").value("new.png"))
        }

        @Test
        @DisplayName("→ 200 USER can update too")
        fun `user update`() {
            val updated = sample.copy(image = "new.png")
            every { categoriaService.update("cat1", match { it.image == "new.png" }) } returns updated

            mockMvc.perform(put("/categorias/cat1")
                .with(user("user1").roles("USER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(updated)))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.image").value("new.png"))
        }
    }

    @Nested
    @DisplayName("DELETE /categorias/{name}")
    inner class Delete {
        @Test
        @DisplayName("→ 204 ADMIN can delete")
        fun `admin delete`() {
            every { categoriaService.delete("cat1") } just runs

            mockMvc.perform(delete("/categorias/cat1")
                .with(user("admin").roles("ADMIN")))
                .andExpect(status().isNoContent)
        }

        @Test
        @DisplayName("→ 204 USER can delete too")
        fun `user delete`() {
            every { categoriaService.delete("cat1") } just runs

            mockMvc.perform(delete("/categorias/cat1")
                .with(user("user1").roles("USER")))
                .andExpect(status().isNoContent)
        }
    }
}