package sr.config;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpMethod;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import sr.controller.*;
import sr.dto.LoginResponse;
import sr.model.Usuario;
import sr.repository.UsuarioRepository;
import sr.service.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Exercita o roteamento MVC real; nenhum serviço acessa o banco. */
class AutorizacaoMvcTest {
    private final UsuarioRepository repo = mock(UsuarioRepository.class);
    private final ClienteService clientes = mock(ClienteService.class);
    private final AuditoriaUsuarioService auditoria = mock(AuditoriaUsuarioService.class);
    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(
        new ClienteController(clientes), new ContaController(mock(ContaService.class)),
        new TransacaoController(mock(TransacaoService.class)), new KycController(mock(KycService.class), jdbc),
        new AlertaPldController(mock(AlertaPldService.class)), new AlertaFraudeController(mock(AlertaFraudeService.class)),
        new ChargebackController(mock(ChargebackService.class)), new RegraRiscoController(mock(RegraRiscoService.class)),
        new RiscoClienteController(mock(RiscoClienteService.class)), new AuditoriaUsuarioController(auditoria)
    ).addInterceptors(new ProtecaoCsrfInterceptor(), new AutenticacaoInterceptor(repo)).build();

    private MockHttpSession sessao(String perfil) {
        Usuario user = new Usuario();
        user.setId(1L); user.setNome("Teste"); user.setEmail("teste@example.test");
        user.setStatus("ATIVO"); user.setPerfil(perfil);
        when(repo.findById(1L)).thenReturn(Optional.of(user));
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("usuarioLogado", new LoginResponse(1L, user.getNome(), user.getEmail(), perfil));
        session.setAttribute("sessaoVersao", 0L);
        ProtecaoCsrfInterceptor.token(session);
        return session;
    }

    static Stream<Arguments> escritas() {
        return Stream.of("clientes", "contas", "transacoes", "kycs", "alertas-pld", "alertas-fraude", "chargebacks", "regras-risco", "riscos-clientes")
            .flatMap(modulo -> Stream.of("ANALISTA", "GESTOR").flatMap(perfil ->
                Stream.of(modulo, "%" + Integer.toHexString(modulo.charAt(0)) + modulo.substring(1), modulo + ";teste=1")
                    .map(rota -> Arguments.of(perfil, "/api/" + rota))));
    }

    @ParameterizedTest
    @MethodSource("escritas")
    void escritaConsultivaNaoPossuiRotaMesmoComUrlEquivalente(String perfil, String rota) throws Exception {
        MockHttpSession session = sessao(perfil);
        for (HttpMethod method : List.of(HttpMethod.POST, HttpMethod.PUT, HttpMethod.DELETE)) {
            String url = rota + (method == HttpMethod.POST ? "" : "/TESTE");
            mvc.perform(request(method, URI.create(url)).session(session)
                .header("X-CSRF-Token", session.getAttribute("csrfToken"))
                .contentType("application/json").content("{}"))
                .andExpect(status().isMethodNotAllowed());
        }
        verifyNoInteractions(clientes, auditoria, jdbc);
    }

    @Test
    void consultaCodificadaDeClientesContinuaPermitidaAoGestor() throws Exception {
        when(clientes.listarTodos()).thenReturn(List.of());
        mvc.perform(get(URI.create("/api/%63lientes")).session(sessao("GESTOR")))
            .andExpect(status().isOk());
        verify(clientes).listarTodos();
    }

    @Test
    void auditoriaCodificadaContinuaExclusivaDoAdministrador() throws Exception {
        mvc.perform(get(URI.create("/api/%61uditoria-usuarios")).session(sessao("GESTOR")))
            .andExpect(status().isForbidden());
        verifyNoInteractions(auditoria);
    }
}
