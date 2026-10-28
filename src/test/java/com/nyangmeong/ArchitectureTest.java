package com.nyangmeong;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * [검사 - 의존 방향·@Transactional 규칙]
 * - 기준 : AGENTS.md 의존 방향, docs/convention/service.md
 * - 실패 시 수정 대상 : 이 테스트가 아니라 메시지의 코드
 */
class ArchitectureTest {

    private static final String ROOT = "com.nyangmeong";

    /** 도메인 층 표 (AGENTS.md "도메인 간 호출은 낮은 층으로만"과 함께 수정) */
    private static final Map<String, Integer> DOMAIN_LAYERS = domainLayers();

    /** 트랜잭션 불필요 Service public 메서드 ("클래스이름.메서드이름") */
    private static final Set<String> TRANSACTIONAL_EXCEPTIONS = Set.of();

    private static final JavaClasses CLASSES = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages(ROOT);

    private static Map<String, Integer> domainLayers() {
        Map<String, Integer> layers = new LinkedHashMap<>();
        layers.put("member", 0);
        layers.put("pet", 1);
        layers.put("auth", 1);
        layers.put("health", 2);
        layers.put("place", 2);
        layers.put("notification", 2);
        layers.put("expense", 3);
        layers.put("community", 3);
        layers.put("support", 3);
        layers.put("ai", 3);
        layers.put("admin", 4);
        layers.put("scheduler", 4);
        return layers;
    }

    private static String domainPackage(String domain) {
        return ROOT + "." + domain + "..";
    }

    /** 도메인별 layer 패키지 (빈 layer는 도메인 전체) */
    private static String[] domainPackages(String layer) {
        String suffix = layer.isEmpty() ? ".." : ".." + layer + "..";
        return DOMAIN_LAYERS.keySet().stream()
                .map(domain -> ROOT + "." + domain + suffix)
                .toArray(String[]::new);
    }

    @Test
    void global은_도메인을_참조하지_않는다() {
        noClasses().that().resideInAPackage(ROOT + ".global..")
                .should().dependOnClassesThat().resideInAnyPackage(domainPackages(""))
                .allowEmptyShould(true)
                .check(CLASSES);
    }

    @Test
    void 도메인_안에서는_controller_service_mapper_순서로만_부른다() {
        noClasses().that().resideInAnyPackage(domainPackages("controller"))
                .should().dependOnClassesThat().resideInAnyPackage(domainPackages("mapper"))
                .allowEmptyShould(true)
                .check(CLASSES);
        noClasses().that().resideInAnyPackage(domainPackages("service"))
                .should().dependOnClassesThat().resideInAnyPackage(domainPackages("controller"))
                .allowEmptyShould(true)
                .check(CLASSES);
        noClasses().that().resideInAnyPackage(domainPackages("mapper"))
                .should().dependOnClassesThat().resideInAnyPackage(domainPackages("controller"))
                .allowEmptyShould(true)
                .check(CLASSES);
        noClasses().that().resideInAnyPackage(domainPackages("mapper"))
                .should().dependOnClassesThat().resideInAnyPackage(domainPackages("service"))
                .allowEmptyShould(true)
                .check(CLASSES);
    }

    @Test
    void 다른_도메인의_mapper를_참조하지_않는다() {
        for (String domain : DOMAIN_LAYERS.keySet()) {
            String[] otherMappers = DOMAIN_LAYERS.keySet().stream()
                    .filter(other -> !other.equals(domain))
                    .map(other -> ROOT + "." + other + "..mapper..")
                    .toArray(String[]::new);

            noClasses().that().resideInAPackage(domainPackage(domain))
                    .should().dependOnClassesThat().resideInAnyPackage(otherMappers)
                    .allowEmptyShould(true)
                    .check(CLASSES);
        }
    }

    @Test
    void 도메인은_자기보다_낮은_층만_참조한다() {
        for (Map.Entry<String, Integer> entry : DOMAIN_LAYERS.entrySet()) {
            String domain = entry.getKey();
            int layer = entry.getValue();
            /** 같은 층끼리 호출 금지 */
            String[] sameOrHigher = DOMAIN_LAYERS.entrySet().stream()
                    .filter(other -> !other.getKey().equals(domain) && other.getValue() >= layer)
                    .map(other -> domainPackage(other.getKey()))
                    .toArray(String[]::new);
            if (sameOrHigher.length == 0) {
                continue;
            }

            noClasses().that().resideInAPackage(domainPackage(domain))
                    .should().dependOnClassesThat().resideInAnyPackage(sameOrHigher)
                    .allowEmptyShould(true)
                    .check(CLASSES);
        }
    }

    @Test
    void Service의_public_메서드에는_Transactional이_있다() {
        DescribedPredicate<JavaMethod> listedAsException = new DescribedPredicate<>("예외 목록에 있는 메서드") {
            @Override
            public boolean test(JavaMethod method) {
                return TRANSACTIONAL_EXCEPTIONS.contains(
                        method.getOwner().getSimpleName() + "." + method.getName());
            }
        };

        methods().that().arePublic()
                .and().areDeclaredInClassesThat().resideInAnyPackage(domainPackages(""))
                .and().areDeclaredInClassesThat().areAnnotatedWith(Service.class)
                .and(DescribedPredicate.not(listedAsException))
                .should().beAnnotatedWith(Transactional.class)
                .allowEmptyShould(true)
                .check(CLASSES);
    }
}
