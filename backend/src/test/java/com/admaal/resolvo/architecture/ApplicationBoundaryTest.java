package com.admaal.resolvo.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

/**
 * Frontera de los casos de uso (ADR-0002): {@code application} orquesta el dominio y habla con el
 * exterior solo a traves de puertos. Los adaptadores viven en {@code infrastructure}.
 */
class ApplicationBoundaryTest {

  private static final JavaClasses PROJECT_CLASSES =
      new ClassFileImporter()
          .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
          .importPackages("com.admaal.resolvo");

  @Test
  void laAplicacionSoloDependeDelDominio() {
    noClasses()
        .that()
        .resideInAPackage("com.admaal.resolvo.application..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage(
            "com.admaal.resolvo.infrastructure..",
            "com.admaal.resolvo.bootstrap..",
            "org.springframework..",
            "jakarta.persistence..",
            "org.hibernate..")
        .allowEmptyShould(true)
        .check(PROJECT_CLASSES);
  }
}
