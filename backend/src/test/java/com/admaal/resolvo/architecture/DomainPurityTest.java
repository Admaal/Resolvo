package com.admaal.resolvo.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

/**
 * Pureza del dominio (ADR-0002). El dominio no conoce el framework, la persistencia ni el
 * proveedor de LLM; por eso se puede probar sin arrancar Spring.
 */
class DomainPurityTest {

  private static final JavaClasses PROJECT_CLASSES =
      new ClassFileImporter()
          .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
          .importPackages("com.admaal.resolvo");

  @Test
  void elDominioNoDependeDeSpringJpaNiSpringAi() {
    noClasses()
        .that()
        .resideInAPackage("com.admaal.resolvo.domain..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage(
            "org.springframework..",
            "jakarta.persistence..",
            "org.hibernate..",
            "com.admaal.resolvo.application..",
            "com.admaal.resolvo.infrastructure..",
            "com.admaal.resolvo.bootstrap..")
        .allowEmptyShould(true)
        .check(PROJECT_CLASSES);
  }
}
