// src/test/kotlin/com/es/controller/PedidoControllerTest.kt

import com.es.quicktasteAPIAplication
import com.es.dto.PedidoDTO
import com.es.model.PedidoStatus
import com.es.service.PedidoService
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
class PedidoControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    private val mapper = ObjectMapper()

    @MockkBean
    private lateinit var pedidoService: PedidoService

    // Sample order to use in tests
    private val sample = PedidoDTO(
        id        = "123",
        userEmail = "user@example.com",
        productos = listOf("p1", "p2"),
        cantidad  = 2,
        coste     = 50.0,
        direccion = "Calle Falsa 123",
        status    = PedidoStatus.PENDING
    )

    @Nested
    @DisplayName("GET /pedidos")
    inner class GetAll {
        @Test @DisplayName("→ 200 ADMIN retrieves all orders")
        fun `getAll admin`() {
            every { pedidoService.findAll() } returns listOf(sample)

            mockMvc.perform(get("/pedidos")
                .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$[0].id").value("123"))
        }

        @Test @DisplayName("→ 200 USER retrieves only own orders")
        fun `getAll user`() {
            every { pedidoService.findByUserEmail("user@example.com") } returns listOf(sample)

            mockMvc.perform(get("/pedidos")
                .with(user("user@example.com").roles("USER")))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$[0].userEmail").value("user@example.com"))
        }
    }

    @Nested
    @DisplayName("GET /pedidos/{id}")
    inner class GetById {
        @Test @DisplayName("→ 200 OWNER can view their order")
        fun `getById owner`() {
            every { pedidoService.findById("123") } returns sample

            mockMvc.perform(get("/pedidos/123")
                .with(user("user@example.com").roles("USER")))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.direccion").value("Calle Falsa 123"))
        }

        @Test @DisplayName("→ 403 USER cannot view others' orders")
        fun `getById other forbidden`() {
            every { pedidoService.findById("123") } returns sample

            mockMvc.perform(get("/pedidos/123")
                .with(user("other").roles("USER")))
                .andExpect(status().isForbidden)
        }

        @Test @DisplayName("→ 200 ADMIN can view any order")
        fun `getById admin`() {
            every { pedidoService.findById("123") } returns sample

            mockMvc.perform(get("/pedidos/123")
                .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk)
        }
    }

    @Nested
    @DisplayName("POST /pedidos")
    inner class Create {
        @Test @DisplayName("→ 201 USER creates order with email from token")
        fun `user create`() {
            // simulate creation: id assigned, email overridden by token
            every { pedidoService.create(match {
                it.id == null && it.userEmail == "user1"
            }) } returns sample.copy(id = "123", userEmail = "user1")

            val payload = sample.copy(id = null, userEmail = "ignored")
            mockMvc.perform(post("/pedidos")
                .with(user("user1").roles("USER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(payload)))
                .andExpect(status().isCreated)
                .andExpect(jsonPath("$.userEmail").value("user1"))
                .andExpect(jsonPath("$.id").value("123"))
        }

        @Test @DisplayName("→ 201 ADMIN creates order specifying email")
        fun `admin create`() {
            // admin can specify any email
            every { pedidoService.create(match { it.userEmail == "other@example.com" }) }
                .returns(sample.copy(id = "456", userEmail = "other@example.com"))

            val payload = sample.copy(id = null, userEmail = "other@example.com")
            mockMvc.perform(post("/pedidos")
                .with(user("admin").roles("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(payload)))
                .andExpect(status().isCreated)
                .andExpect(jsonPath("$.userEmail").value("other@example.com"))
                .andExpect(jsonPath("$.id").value("456"))
        }
    }

    @Nested
    @DisplayName("PUT /pedidos/{id}")
    inner class Update {
        @Test @DisplayName("→ 200 OWNER updates without changing status")
        fun `owner update`() {
            every { pedidoService.findById("123") } returns sample
            val mod = sample.copy(direccion = "Nueva 456", status = PedidoStatus.DELIVERED)
            // owner update forces status back to PENDING
            every { pedidoService.update("123", sample.copy(direccion = "Nueva 456")) }
                .returns(sample.copy(direccion = "Nueva 456"))

            mockMvc.perform(put("/pedidos/123")
                .with(user("user@example.com").roles("USER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(mod)))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.direccion").value("Nueva 456"))
                .andExpect(jsonPath("$.status").value("PENDING"))
        }

        @Test @DisplayName("→ 200 ADMIN updates with valid status change")
        fun `admin update valid status`() {
            every { pedidoService.findById("123") } returns sample
            val delivered = sample.copy(status = PedidoStatus.DELIVERED)
            every { pedidoService.update("123", delivered) } returns delivered

            mockMvc.perform(put("/pedidos/123")
                .with(user("admin").roles("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(delivered)))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.status").value("DELIVERED"))
        }

        @Test @DisplayName("→ 403 USER cannot update others' orders")
        fun `user update forbidden`() {
            every { pedidoService.findById("123") } returns sample

            mockMvc.perform(put("/pedidos/123")
                .with(user("other").roles("USER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(sample)))
                .andExpect(status().isForbidden)
        }
    }

    @Nested
    @DisplayName("DELETE /pedidos/{id}")
    inner class Delete {
        @Test @DisplayName("→ 204 OWNER deletes own order")
        fun `owner delete`() {
            every { pedidoService.findById("123") } returns sample
            every { pedidoService.delete("123") } just runs

            mockMvc.perform(delete("/pedidos/123")
                .with(user("user@example.com").roles("USER")))
                .andExpect(status().isNoContent)
        }

        @Test @DisplayName("→ 403 USER cannot delete others' orders")
        fun `user delete forbidden`() {
            every { pedidoService.findById("123") } returns sample

            mockMvc.perform(delete("/pedidos/123")
                .with(user("other").roles("USER")))
                .andExpect(status().isForbidden)
        }

        @Test @DisplayName("→ 204 ADMIN deletes any order")
        fun `admin delete`() {
            every { pedidoService.findById("123") } returns sample
            every { pedidoService.delete("123") } just runs

            mockMvc.perform(delete("/pedidos/123")
                .with(user("admin").roles("ADMIN")))
                .andExpect(status().isNoContent)
        }
    }
}

