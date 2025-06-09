// src/test/kotlin/com/es/controller/UsuarioControllerTest.kt

import com.es.quicktasteAPIAplication
import com.es.dto.LoginUsuarioDTO
import com.es.dto.UsuarioDTO
import com.es.dto.UsuarioRegisterDTO
import com.es.service.TokenService
import com.es.service.UsuarioService
import com.fasterxml.jackson.databind.ObjectMapper
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

@SpringBootTest(classes = [quicktasteAPIAplication::class])
@AutoConfigureMockMvc
class UsuarioControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    private val objectMapper = ObjectMapper()

    @MockkBean
    private lateinit var authenticationManager: AuthenticationManager

    @MockkBean
    private lateinit var tokenService: TokenService

    @MockkBean
    private lateinit var usuarioService: UsuarioService

    // Sample user DTO for reuse in tests
    private val sampleDto = UsuarioDTO(
        email    = "u@example.com",
        username = "user1",
        password = "secret",
        roles    = "USER",
        image    = "img.png",
        wallet   = 50
    )

    // Test successful registration endpoint
    @Test
    @DisplayName("POST /usuarios/register → 201")
    fun `register success`() {
        val req = UsuarioRegisterDTO(
            email          = sampleDto.email,
            username       = sampleDto.username,
            password       = "pass",
            passwordRepeat = "pass",
            rol            = "USER",
            image          = sampleDto.image
        )
        every { usuarioService.register(req) } returns sampleDto

        mockMvc.perform(
            post("/usuarios/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.username").value("user1"))
    }

    @Nested
    @DisplayName("POST /usuarios/login")
    inner class LoginTests {
        // Test successful login returns JWT + user data
        @Test
        @DisplayName("→ 201 with token + user")
        fun `login success`() {
            val creds = LoginUsuarioDTO("user1", "pass")
            val authToken = UsernamePasswordAuthenticationToken("user1", "pass")
            every { authenticationManager.authenticate(any<UsernamePasswordAuthenticationToken>()) } returns authToken as Authentication
            every { tokenService.generarToken(authToken) } returns "JWT-TOKEN"
            every { usuarioService.findByUsername("user1") } returns sampleDto

            mockMvc.perform(
                post("/usuarios/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(creds))
            )
                .andExpect(status().isCreated)
                .andExpect(jsonPath("$.token").value("JWT-TOKEN"))
                .andExpect(jsonPath("$.user.username").value("user1"))
        }

        // Test login failure with bad credentials → 403
        @Test
        @DisplayName("→ 403 invalid credentials")
        fun `login failure`() {
            val creds = LoginUsuarioDTO("x", "y")
            every { authenticationManager.authenticate(any()) } throws BadCredentialsException("bad")

            mockMvc.perform(
                post("/usuarios/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(creds))
            )
                .andExpect(status().isForbidden)
        }
    }

    // Test fetching current user profile → 200
    @Test
    @DisplayName("GET /usuarios/me → 200")
    fun `me endpoint`() {
        every { usuarioService.findByUsername("user1") } returns sampleDto

        mockMvc.perform(
            get("/usuarios/me")
                .with(user("user1").roles("USER"))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.email").value("u@example.com"))
    }

    // Test updating own wallet → 200
    @Test
    @DisplayName("PUT /usuarios/me/wallet → 200")
    fun `updateMyWallet success`() {
        val updated = sampleDto.copy(wallet = 100)
        every { usuarioService.updateWallet("user1", 100) } returns updated

        mockMvc.perform(
            put("/usuarios/me/wallet")
                .with(user("user1").roles("USER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"wallet":100}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.wallet").value(100))
    }

    @Nested
    @DisplayName("GET /usuarios (admin only)")
    inner class GetAllUsers {
        // Regular USER should be forbidden from listing all users
        @Test @DisplayName("forbidden for USER")
        fun `user forbidden`() {
            mockMvc.perform(
                get("/usuarios")
                    .with(user("other").roles("USER"))
            )
                .andExpect(status().isForbidden)
        }
        // ADMIN can list all users → 200 + list content
        @Test @DisplayName("ok for ADMIN")
        fun `admin ok`() {
            every { usuarioService.findAll() } returns listOf(sampleDto)
            mockMvc.perform(
                get("/usuarios")
                    .with(user("admin").roles("ADMIN"))
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$[0].username").value("user1"))
        }
    }

    @Nested
    @DisplayName("GET /usuarios/{username}")
    inner class GetByUsername {
        // Owner can fetch own profile → 200
        @Test @DisplayName("owner ok")
        fun `owner ok`() {
            every { usuarioService.findByUsername("user1") } returns sampleDto
            mockMvc.perform(
                get("/usuarios/user1")
                    .with(user("user1").roles("USER"))
            )
                .andExpect(status().isOk)
        }
        // Other USER forbidden → 403
        @Test @DisplayName("other forbidden")
        fun `other forbidden`() {
            mockMvc.perform(
                get("/usuarios/user1")
                    .with(user("other").roles("USER"))
            )
                .andExpect(status().isForbidden)
        }
        // ADMIN can view any user → 200
        @Test @DisplayName("admin ok")
        fun `admin ok`() {
            every { usuarioService.findByUsername("user1") } returns sampleDto
            mockMvc.perform(
                get("/usuarios/user1")
                    .with(user("admin").roles("ADMIN"))
            )
                .andExpect(status().isOk)
        }
    }

    @Nested
    @DisplayName("PUT /usuarios/{username}")
    inner class UpdateUser {
        private val req = UsuarioRegisterDTO(
            email          = sampleDto.email,
            username       = sampleDto.username,
            password       = "",
            passwordRepeat = "",
            rol            = "USER",
            image          = sampleDto.image
        )
        // Owner updates own data → 200
        @Test @DisplayName("owner ok")
        fun `owner ok`() {
            every { usuarioService.update("user1", req) } returns sampleDto
            mockMvc.perform(
                put("/usuarios/user1")
                    .with(user("user1").roles("USER"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(req))
            )
                .andExpect(status().isOk)
        }
        // Other USER forbidden → 403
        @Test @DisplayName("other forbidden")
        fun `other forbidden`() {
            mockMvc.perform(
                put("/usuarios/user1")
                    .with(user("other").roles("USER"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(req))
            )
                .andExpect(status().isForbidden)
        }
        // ADMIN updates any user → 200
        @Test @DisplayName("admin ok")
        fun `admin ok`() {
            every { usuarioService.update("user1", req) } returns sampleDto
            mockMvc.perform(
                put("/usuarios/user1")
                    .with(user("admin").roles("ADMIN"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(req))
            )
                .andExpect(status().isOk)
        }
    }

    @Nested
    @DisplayName("DELETE /usuarios/{username}")
    inner class DeleteUser {
        // Owner deletes own account → 204
        @Test @DisplayName("owner ok")
        fun `owner ok`() {
            every { usuarioService.delete("user1") } returns Unit
            mockMvc.perform(
                delete("/usuarios/user1")
                    .with(user("user1").roles("USER"))
            )
                .andExpect(status().isNoContent)
        }
        // Other USER forbidden → 403
        @Test @DisplayName("other forbidden")
        fun `other forbidden`() {
            mockMvc.perform(
                delete("/usuarios/user1")
                    .with(user("other").roles("USER"))
            )
                .andExpect(status().isForbidden)
        }
        // ADMIN deletes any user → 204
        @Test @DisplayName("admin ok")
        fun `admin ok`() {
            every { usuarioService.delete("user1") } returns Unit
            mockMvc.perform(
                delete("/usuarios/user1")
                    .with(user("admin").roles("ADMIN"))
            )
                .andExpect(status().isNoContent)
        }
    }

    @Nested
    @DisplayName("PUT /usuarios/{username}/wallet")
    inner class UpdateWallet {
        // Valid wallet update → 200 + updated balance
        @Test @DisplayName("200 valid")
        fun `200 valid`() {
            val updated = sampleDto.copy(wallet = 200)
            every { usuarioService.updateWallet("user1", 200) } returns updated
            mockMvc.perform(
                put("/usuarios/user1/wallet")
                    .with(user("user1").roles("USER"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"wallet":200}""")
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.wallet").value(200))
        }
        // Missing wallet field → 400
        @Test @DisplayName("400 missing")
        fun `400 missing`() {
            mockMvc.perform(
                put("/usuarios/user1/wallet")
                    .with(user("user1").roles("USER"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{}""")
            )
                .andExpect(status().isBadRequest)
        }
    }
}