package sr.controller;

import java.lang.reflect.Method;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Garante estruturalmente que os dados fornecidos pela origem externa
 * permanecem consultivos na API do SGR.
 */
class DominiosExternosSomenteLeituraTest {

    private static final List<Class<?>> DOMINIOS_EXTERNOS = List.of(
        ClienteController.class,
        ContaController.class,
        TransacaoController.class,
        AlertaPldController.class,
        AlertaFraudeController.class,
        ChargebackController.class,
        RegraRiscoController.class,
        RiscoClienteController.class
    );

    @Test
    void dominiosExternosNaoExpoemRotasDeEscrita() {
        for (Class<?> controller : DOMINIOS_EXTERNOS) {
            for (Method method : controller.getDeclaredMethods()) {
                assertFalse(method.isAnnotationPresent(PostMapping.class),
                    controller.getSimpleName() + " não deve expor POST");
                assertFalse(method.isAnnotationPresent(PutMapping.class),
                    controller.getSimpleName() + " não deve expor PUT");
                assertFalse(method.isAnnotationPresent(DeleteMapping.class),
                    controller.getSimpleName() + " não deve expor DELETE");
            }
        }
    }

    @Test
    void kycMantemSomentePostDeDecisaoInterna() {
        List<Method> posts = List.of(KycController.class.getDeclaredMethods())
            .stream()
            .filter(method -> method.isAnnotationPresent(PostMapping.class))
            .toList();

        assertEquals(1, posts.size());
        PostMapping mapping = posts.get(0).getAnnotation(PostMapping.class);
        assertTrue(List.of(mapping.value()).contains("/{id}/decisao"));

        for (Method method : KycController.class.getDeclaredMethods()) {
            assertFalse(method.isAnnotationPresent(PutMapping.class));
            assertFalse(method.isAnnotationPresent(DeleteMapping.class));
        }
    }
}
